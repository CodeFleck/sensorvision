package org.sensorvision.service;

import org.sensorvision.dto.IssueCommentDto;
import org.sensorvision.dto.IssueCommentRequest;
import org.sensorvision.model.IssueComment;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.model.User;
import org.sensorvision.repository.IssueCommentRepository;
import org.sensorvision.repository.IssueSubmissionRepository;
import org.sensorvision.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IssueCommentService {

    private static final Logger logger = LoggerFactory.getLogger(IssueCommentService.class);

    private final IssueCommentRepository commentRepository;
    private final IssueSubmissionRepository issueRepository;
    private final SecurityUtils securityUtils;
    private final EmailNotificationService emailNotificationService;

    @Autowired
    public IssueCommentService(IssueCommentRepository commentRepository,
                              IssueSubmissionRepository issueRepository,
                              SecurityUtils securityUtils,
                              EmailNotificationService emailNotificationService) {
        this.commentRepository = commentRepository;
        this.issueRepository = issueRepository;
        this.securityUtils = securityUtils;
        this.emailNotificationService = emailNotificationService;
    }

    /**
     * Add a comment to an issue (user perspective)
     * Users can only comment on their own issues and cannot create internal comments
     */
    @Transactional
    public IssueCommentDto addUserComment(Long issueId, IssueCommentRequest request) {
        return addUserComment(issueId, request, null);
    }

    /**
     * Add a comment with optional file attachment to an issue (user perspective)
     * Users can only comment on their own issues and cannot create internal comments
     */
    @Transactional
    public IssueCommentDto addUserComment(Long issueId, IssueCommentRequest request, MultipartFile attachment) {
        User currentUser = securityUtils.getCurrentUser();

        IssueSubmission issue = issueRepository.findByIdAndUser(issueId, currentUser)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setAuthor(currentUser);
        comment.setMessage(sanitizeHtml(request.message()));
        comment.setInternal(false); // Users cannot create internal comments

        // Handle file attachment if provided
        if (attachment != null && !attachment.isEmpty()) {
            processAttachment(comment, attachment);
        }

        IssueComment savedComment = commentRepository.save(comment);
        logger.info("User {} added comment to issue {}", currentUser.getUsername(), issueId);

        // Note: We intentionally DON'T update the parent issue's updatedAt here.
        // User comments shouldn't trigger the unread badge - only support/admin replies should.
        // This prevents the badge from lighting up when users reply to their own tickets.

        return IssueCommentDto.fromEntity(savedComment);
    }

    /**
     * Add a comment to an issue (admin perspective)
     * Admins can comment on any issue and can create internal comments
     */
    @Transactional
    public IssueCommentDto addAdminComment(Long issueId, IssueCommentRequest request) {
        return addAdminComment(issueId, request, null);
    }

    /**
     * Add a comment with optional file attachment to an issue (admin perspective)
     * Admins can comment on any issue and can create internal comments
     */
    @Transactional
    public IssueCommentDto addAdminComment(Long issueId, IssueCommentRequest request, MultipartFile attachment) {
        User currentUser = securityUtils.getCurrentUser();

        IssueSubmission issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setAuthor(currentUser);
        comment.setMessage(sanitizeHtml(request.message()));
        comment.setInternal(request.internal()); // Admins can set internal flag

        // Handle file attachment if provided
        if (attachment != null && !attachment.isEmpty()) {
            processAttachment(comment, attachment);
        }

        IssueComment savedComment = commentRepository.save(comment);
        logger.info("Admin {} added {} comment to issue {}",
            currentUser.getUsername(),
            request.internal() ? "internal" : "public",
            issueId);

        // For public comments, update the parent issue's lastPublicReplyAt timestamp
        // This ensures the unread badge will show for users when support replies
        // Unlike updatedAt, this field ONLY changes when admins post public replies (not status changes)
        if (!request.internal()) {
            issue.setLastPublicReplyAt(java.time.Instant.now());
            issueRepository.save(issue);
            logger.debug("Updated parent issue #{} lastPublicReplyAt for unread tracking", issueId);
        }

        // Send email notification if this is a public reply (not internal)
        // Honor user's email notification preference
        if (!request.internal()) {
            User ticketOwner = issue.getUser();

            // Check if user has opted in to email notifications
            if (Boolean.TRUE.equals(ticketOwner.getEmailNotificationsEnabled())) {
                try {
                    String userEmail = ticketOwner.getEmail();

                    // Email is sent asynchronously - won't block HTTP response
                    emailNotificationService.sendTicketReplyEmail(
                        issue,
                        request.message(),
                        userEmail
                    );

                    logger.info("Async email notification queued for {} on ticket #{}", userEmail, issueId);
                } catch (Exception e) {
                    logger.error("Error queuing email notification for ticket #{}: {}", issueId, e.getMessage(), e);
                    // Don't fail the comment creation if email queueing fails
                }
            } else {
                logger.info("Email notification skipped for ticket #{} - user opted out", issueId);
            }
        }

        return IssueCommentDto.fromEntity(savedComment);
    }

    /**
     * Get comments for an issue (user perspective)
     * Returns only public comments
     */
    @Transactional(readOnly = true)
    public List<IssueCommentDto> getUserComments(Long issueId) {
        User currentUser = securityUtils.getCurrentUser();

        IssueSubmission issue = issueRepository.findByIdAndUser(issueId, currentUser)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        return commentRepository.findPublicCommentsByIssue(issue).stream()
            .map(IssueCommentDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all comments for an issue (admin perspective)
     * Returns both public and internal comments
     */
    @Transactional(readOnly = true)
    public List<IssueCommentDto> getAdminComments(Long issueId) {
        IssueSubmission issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        return commentRepository.findAllCommentsByIssue(issue).stream()
            .map(IssueCommentDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get comment count for an issue
     */
    @Transactional(readOnly = true)
    public long getCommentCount(IssueSubmission issue) {
        return commentRepository.countPublicCommentsByIssue(issue);
    }

    /**
     * Get comment attachment with authorization checks
     * Users can only download attachments from their own tickets' public comments
     * Admins can download all attachments
     */
    @Transactional(readOnly = true)
    public IssueComment getCommentWithAttachment(Long commentId) {
        User currentUser = securityUtils.getCurrentUser();
        boolean isAdmin = currentUser.getRoles().stream()
            .anyMatch(role -> role.getName().equals("ROLE_ADMIN"));

        IssueComment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));

        if (!comment.hasAttachment()) {
            throw new RuntimeException("Comment has no attachment");
        }

        // Admins can access all attachments
        if (isAdmin) {
            return comment;
        }

        // Regular users can only access attachments from their own tickets' public comments
        IssueSubmission issue = comment.getIssue();

        // Check ownership
        if (!issue.getUser().getId().equals(currentUser.getId())) {
            throw new RuntimeException("You do not have permission to access this attachment");
        }

        // Check if comment is internal (users should not see internal attachments)
        if (comment.isInternal()) {
            throw new RuntimeException("You do not have permission to access this attachment");
        }

        return comment;
    }

    /**
     * Process and validate file attachment for a comment
     */
    private void processAttachment(IssueComment comment, MultipartFile file) {
        final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB limit

        try {
            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                throw new RuntimeException("File size exceeds maximum limit of 10 MB");
            }

            // Validate file type (allow common file types)
            String contentType = file.getContentType();
            if (contentType == null || !isAllowedFileType(contentType)) {
                throw new RuntimeException("File type not allowed. Allowed types: images, text files, PDFs, JSON, XML, logs");
            }

            // Set attachment fields
            comment.setAttachmentFilename(file.getOriginalFilename());
            comment.setAttachmentData(file.getBytes());
            comment.setAttachmentContentType(contentType);
            comment.setAttachmentSizeBytes(file.getSize());

            logger.info("Processed file attachment: {} ({} bytes)", file.getOriginalFilename(), file.getSize());

        } catch (IOException e) {
            logger.error("Failed to process file attachment", e);
            throw new RuntimeException("Failed to process file attachment: " + e.getMessage(), e);
        }
    }

    /**
     * Check if the file type is allowed for attachments
     */
    private boolean isAllowedFileType(String contentType) {
        return contentType.startsWith("image/") ||
               contentType.startsWith("text/") ||
               contentType.equals("application/pdf") ||
               contentType.equals("application/json") ||
               contentType.equals("application/xml") ||
               contentType.equals("text/xml") ||
               contentType.equals("application/zip") ||
               contentType.equals("application/x-gzip") ||
               contentType.equals("application/octet-stream"); // For .log files
    }

    /**
     * Sanitize HTML content to prevent XSS attacks
     * Allows basic formatting tags but strips dangerous elements
     */
    private String sanitizeHtml(String htmlContent) {
        if (htmlContent == null) {
            return null;
        }

        // Define allowed HTML tags for basic rich text formatting
        Safelist safelist = Safelist.relaxed()
            .addTags("code", "pre", "kbd", "samp", "var") // Code elements
            .addAttributes("a", "target", "rel") // Allow links to open in new tab
            .addAttributes("code", "class") // For syntax highlighting
            .addAttributes("pre", "class"); // For code blocks

        // Sanitize the HTML
        String sanitized = Jsoup.clean(htmlContent, safelist);

        logger.debug("Sanitized HTML content (original length: {}, sanitized length: {})",
            htmlContent.length(), sanitized.length());

        return sanitized;
    }
}
