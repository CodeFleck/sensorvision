package org.sensorvision.service;

import org.sensorvision.dto.IssueSubmissionRequest;
import org.sensorvision.dto.IssueSubmissionResponse;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.IssueSubmissionRepository;
import org.sensorvision.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class IssueSubmissionService {

    private static final Logger logger = LoggerFactory.getLogger(IssueSubmissionService.class);
    private static final int MAX_ISSUES_PER_DAY = 3;
    private static final int RATE_LIMIT_HOURS = 24;

    private final IssueSubmissionRepository issueSubmissionRepository;
    private final EmailNotificationService emailNotificationService;
    private final SecurityUtils securityUtils;

    @Value("${support.issue.enabled:true}")
    private boolean issueSubmissionEnabled;

    @Value("${support.issue.email:support@sensorvision.com}")
    private String supportEmail;

    @Autowired
    public IssueSubmissionService(IssueSubmissionRepository issueSubmissionRepository,
                                 EmailNotificationService emailNotificationService,
                                 SecurityUtils securityUtils) {
        this.issueSubmissionRepository = issueSubmissionRepository;
        this.emailNotificationService = emailNotificationService;
        this.securityUtils = securityUtils;
    }

    /**
     * Submit a new issue/bug report
     */
    @Transactional
    public IssueSubmissionResponse submitIssue(IssueSubmissionRequest request) {
        if (!issueSubmissionEnabled) {
            throw new RuntimeException("Issue submission is currently disabled");
        }

        User currentUser = securityUtils.getCurrentUser();
        Organization organization = securityUtils.getCurrentUserOrganization();

        // Check rate limit: max 3 issues per 24 hours per user
        LocalDateTime rateLimitStart = LocalDateTime.now().minusHours(RATE_LIMIT_HOURS);
        long recentIssueCount = issueSubmissionRepository.countByUserSince(currentUser, rateLimitStart);

        if (recentIssueCount >= MAX_ISSUES_PER_DAY) {
            String errorMessage = String.format(
                "You've reached the maximum limit of %d issue submissions per 24 hours. " +
                "We appreciate your feedback! Please wait a bit before submitting another issue, " +
                "or contact support directly at %s if this is urgent.",
                MAX_ISSUES_PER_DAY, supportEmail
            );
            logger.warn("User {} exceeded issue submission rate limit ({}/{})",
                currentUser.getUsername(), recentIssueCount, MAX_ISSUES_PER_DAY);
            throw new RuntimeException(errorMessage);
        }

        logger.info("Submitting issue '{}' by user: {} ({}/{} issues in last 24h)",
            request.title(), currentUser.getUsername(), recentIssueCount, MAX_ISSUES_PER_DAY);

        // Create issue submission entity
        IssueSubmission issue = new IssueSubmission();
        issue.setUser(currentUser);
        issue.setOrganization(organization);
        issue.setTitle(request.title());
        issue.setDescription(request.description());
        issue.setCategory(request.category());
        issue.setSeverity(request.severity());
        issue.setStatus(IssueStatus.SUBMITTED);

        // Process screenshot if provided
        if (request.screenshotBase64() != null && !request.screenshotBase64().isEmpty()) {
            try {
                // Remove data URI prefix if present (e.g., "data:image/png;base64,")
                String base64Data = request.screenshotBase64();
                if (base64Data.contains(",")) {
                    String[] parts = base64Data.split(",");
                    base64Data = parts[1];
                    // Extract content type from data URI
                    if (parts[0].contains("image/")) {
                        String contentType = parts[0].split(":")[1].split(";")[0];
                        issue.setScreenshotContentType(contentType);
                    }
                }

                byte[] imageBytes = Base64.getDecoder().decode(base64Data);
                issue.setScreenshotData(imageBytes);
                issue.setScreenshotFilename(request.screenshotFilename() != null ?
                    request.screenshotFilename() : "screenshot.png");

                if (issue.getScreenshotContentType() == null) {
                    issue.setScreenshotContentType("image/png");
                }

                logger.info("Screenshot attached, size: {} bytes", imageBytes.length);
            } catch (IllegalArgumentException e) {
                logger.error("Failed to decode screenshot base64 data", e);
                throw new RuntimeException("Invalid screenshot data format");
            }
        }

        // Set browser and context information
        issue.setBrowserInfo(request.browserInfo());
        issue.setPageUrl(request.pageUrl());
        issue.setUserAgent(request.userAgent());
        issue.setScreenResolution(request.screenResolution());

        // Save to database
        IssueSubmission savedIssue = issueSubmissionRepository.save(issue);
        logger.info("Issue saved with ID: {}", savedIssue.getId());

        // Send email notification to support team
        try {
            emailNotificationService.sendIssueReportEmail(savedIssue, supportEmail);
            logger.info("Support email sent to: {}", supportEmail);
        } catch (Exception e) {
            logger.error("Failed to send issue report email, but issue was saved successfully", e);
            // Don't fail the whole operation if email fails
        }

        return IssueSubmissionResponse.fromEntity(savedIssue);
    }

    /**
     * Get all issues submitted by the current user
     */
    @Transactional(readOnly = true)
    public List<IssueSubmissionResponse> getUserIssues() {
        User currentUser = securityUtils.getCurrentUser();
        return issueSubmissionRepository.findByUserOrderByCreatedAtDesc(currentUser).stream()
            .map(IssueSubmissionResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific issue by ID (user can only see their own issues)
     */
    @Transactional(readOnly = true)
    public IssueSubmissionResponse getIssueById(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        IssueSubmission issue = issueSubmissionRepository.findByIdAndUser(id, currentUser)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));
        return IssueSubmissionResponse.fromEntity(issue);
    }

    /**
     * Get issues by status for current user
     */
    @Transactional(readOnly = true)
    public List<IssueSubmissionResponse> getUserIssuesByStatus(IssueStatus status) {
        User currentUser = securityUtils.getCurrentUser();
        return issueSubmissionRepository.findByUserAndStatusOrderByCreatedAtDesc(currentUser, status).stream()
            .map(IssueSubmissionResponse::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get issue count for current user
     */
    @Transactional(readOnly = true)
    public long getUserIssueCount() {
        User currentUser = securityUtils.getCurrentUser();
        return issueSubmissionRepository.countByUser(currentUser);
    }
}
