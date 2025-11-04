package org.sensorvision.controller;

import jakarta.validation.Valid;
import org.sensorvision.dto.IssueCommentDto;
import org.sensorvision.dto.IssueCommentRequest;
import org.sensorvision.dto.IssueSubmissionRequest;
import org.sensorvision.dto.IssueSubmissionResponse;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.service.IssueCommentService;
import org.sensorvision.service.IssueSubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.sensorvision.model.IssueComment;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
@PreAuthorize("isAuthenticated()")
public class IssueSubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(IssueSubmissionController.class);

    private final IssueSubmissionService issueSubmissionService;
    private final IssueCommentService commentService;

    @Autowired
    public IssueSubmissionController(IssueSubmissionService issueSubmissionService,
                                    IssueCommentService commentService) {
        this.issueSubmissionService = issueSubmissionService;
        this.commentService = commentService;
    }

    /**
     * Submit a new issue/bug report
     * POST /api/v1/support/issues
     */
    @PostMapping("/issues")
    public ResponseEntity<IssueSubmissionResponse> submitIssue(
            @Valid @RequestBody IssueSubmissionRequest request) {
        logger.info("Received issue submission request: {}", request.title());

        try {
            IssueSubmissionResponse response = issueSubmissionService.submitIssue(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            logger.error("Failed to submit issue: {}", e.getMessage());
            // Return 400 Bad Request with error message for client to display
            throw e;
        }
    }

    /**
     * Get all issues submitted by the current user
     * GET /api/v1/support/issues
     */
    @GetMapping("/issues")
    public ResponseEntity<List<IssueSubmissionResponse>> getUserIssues() {
        logger.info("Fetching issues for current user");
        List<IssueSubmissionResponse> issues = issueSubmissionService.getUserIssues();
        return ResponseEntity.ok(issues);
    }

    /**
     * Get a specific issue by ID
     * GET /api/v1/support/issues/{id}
     */
    @GetMapping("/issues/{id}")
    public ResponseEntity<IssueSubmissionResponse> getIssueById(@PathVariable Long id) {
        logger.info("Fetching issue with id: {}", id);
        IssueSubmissionResponse issue = issueSubmissionService.getIssueById(id);
        return ResponseEntity.ok(issue);
    }

    /**
     * Get issues by status for current user
     * GET /api/v1/support/issues/status/{status}
     */
    @GetMapping("/issues/status/{status}")
    public ResponseEntity<List<IssueSubmissionResponse>> getUserIssuesByStatus(
            @PathVariable IssueStatus status) {
        logger.info("Fetching issues with status: {}", status);
        List<IssueSubmissionResponse> issues = issueSubmissionService.getUserIssuesByStatus(status);
        return ResponseEntity.ok(issues);
    }

    /**
     * Get issue count for current user
     * GET /api/v1/support/issues/count
     */
    @GetMapping("/issues/count")
    public ResponseEntity<Map<String, Long>> getUserIssueCount() {
        logger.info("Fetching issue count for current user");
        long count = issueSubmissionService.getUserIssueCount();
        return ResponseEntity.ok(Map.of("count", count));
    }

    /**
     * Get comments for an issue (user can only see their own issue comments)
     * GET /api/v1/support/issues/{id}/comments
     */
    @GetMapping("/issues/{id}/comments")
    public ResponseEntity<List<IssueCommentDto>> getIssueComments(@PathVariable Long id) {
        logger.info("User fetching comments for issue: {}", id);
        List<IssueCommentDto> comments = commentService.getUserComments(id);
        return ResponseEntity.ok(comments);
    }

    /**
     * Add a comment to an issue (JSON - no attachment)
     * POST /api/v1/support/issues/{id}/comments
     * Content-Type: application/json
     */
    @PostMapping(value = "/issues/{id}/comments", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<IssueCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody IssueCommentRequest request) {
        logger.info("User adding comment to issue: {}", id);
        IssueCommentDto comment = commentService.addUserComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /**
     * Add a comment to an issue with file attachment
     * POST /api/v1/support/issues/{id}/comments
     * Content-Type: multipart/form-data
     */
    @PostMapping(value = "/issues/{id}/comments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<IssueCommentDto> addCommentWithAttachment(
            @PathVariable Long id,
            @RequestParam("message") String message,
            @RequestParam(value = "internal", defaultValue = "false") boolean internal,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        logger.info("User adding comment to issue: {} (hasAttachment: {})", id, file != null && !file.isEmpty());

        // Manual validation since we can't use @Valid with @RequestParam
        // CRITICAL: Use HTML-aware validation to prevent empty Quill markup like <p><br></p>
        if (message == null || !org.sensorvision.util.HtmlUtils.hasTextContent(message)) {
            throw new IllegalArgumentException("Message is required");
        }
        if (message.length() > 5000) {
            throw new IllegalArgumentException("Message must not exceed 5000 characters");
        }

        IssueCommentRequest request = new IssueCommentRequest(message.trim(), internal);
        IssueCommentDto comment = commentService.addUserComment(id, request, file);

        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /**
     * Download a comment attachment (with authorization checks)
     * GET /api/v1/support/comments/{commentId}/attachment
     *
     * Security: Users can only download attachments from their own tickets' public comments
     * Admins can download all attachments
     */
    @GetMapping("/comments/{commentId}/attachment")
    public ResponseEntity<byte[]> downloadCommentAttachment(@PathVariable Long commentId) {
        logger.info("Downloading attachment for comment: {}", commentId);

        // Use service method with authorization checks
        IssueComment comment = commentService.getCommentWithAttachment(commentId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(comment.getAttachmentContentType()));
        headers.setContentDispositionFormData("attachment", comment.getAttachmentFilename());
        headers.setContentLength(comment.getAttachmentSizeBytes());

        return ResponseEntity.ok()
            .headers(headers)
            .body(comment.getAttachmentData());
    }

    /**
     * Mark ticket as viewed (for unread badge tracking)
     * POST /api/v1/support/issues/{id}/mark-viewed
     */
    @PostMapping("/issues/{id}/mark-viewed")
    public ResponseEntity<Void> markAsViewed(@PathVariable Long id) {
        logger.info("User marking issue {} as viewed", id);
        issueSubmissionService.markAsViewed(id);
        return ResponseEntity.ok().build();
    }

    /**
     * Get count of tickets with unread replies
     * GET /api/v1/support/issues/unread-count
     */
    @GetMapping("/issues/unread-count")
    public ResponseEntity<Map<String, Long>> getUnreadCount() {
        logger.info("Fetching unread ticket count for current user");
        long count = issueSubmissionService.getUnreadTicketCount();
        return ResponseEntity.ok(Map.of("unreadCount", count));
    }
}
