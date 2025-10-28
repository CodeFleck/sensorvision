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
        User currentUser = securityUtils.getCurrentUser();

        IssueSubmission issue = issueRepository.findByIdAndUser(issueId, currentUser)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setAuthor(currentUser);
        comment.setMessage(request.message());
        comment.setInternal(false); // Users cannot create internal comments

        IssueComment savedComment = commentRepository.save(comment);
        logger.info("User {} added comment to issue {}", currentUser.getUsername(), issueId);

        // Update the parent issue's updatedAt timestamp for activity tracking
        issue.setUpdatedAt(java.time.Instant.now());
        issueRepository.save(issue);
        logger.debug("Updated parent issue #{} timestamp for activity tracking", issueId);

        return IssueCommentDto.fromEntity(savedComment);
    }

    /**
     * Add a comment to an issue (admin perspective)
     * Admins can comment on any issue and can create internal comments
     */
    @Transactional
    public IssueCommentDto addAdminComment(Long issueId, IssueCommentRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        IssueSubmission issue = issueRepository.findById(issueId)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + issueId));

        IssueComment comment = new IssueComment();
        comment.setIssue(issue);
        comment.setAuthor(currentUser);
        comment.setMessage(request.message());
        comment.setInternal(request.internal()); // Admins can set internal flag

        IssueComment savedComment = commentRepository.save(comment);
        logger.info("Admin {} added {} comment to issue {}",
            currentUser.getUsername(),
            request.internal() ? "internal" : "public",
            issueId);

        // For public comments, update the parent issue's updatedAt timestamp
        // This ensures the unread badge will show for users when support replies
        if (!request.internal()) {
            // Touch the issue to trigger AuditableEntity's updatedAt refresh
            issue.setUpdatedAt(java.time.Instant.now());
            issueRepository.save(issue);
            logger.debug("Updated parent issue #{} timestamp for unread tracking", issueId);
        }

        // Send email notification if this is a public reply (not internal)
        if (!request.internal()) {
            try {
                String userEmail = issue.getUser().getEmail();
                boolean emailSent = emailNotificationService.sendTicketReplyEmail(
                    issue,
                    request.message(),
                    userEmail
                );
                if (emailSent) {
                    logger.info("Email notification sent to {} for ticket #{}", userEmail, issueId);
                } else {
                    logger.warn("Email notification failed or disabled for ticket #{}", issueId);
                }
            } catch (Exception e) {
                logger.error("Error sending email notification for ticket #{}: {}", issueId, e.getMessage(), e);
                // Don't fail the comment creation if email fails
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
}
