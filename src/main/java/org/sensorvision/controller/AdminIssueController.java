package org.sensorvision.controller;

import jakarta.validation.Valid;
import org.sensorvision.dto.*;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.service.AdminIssueService;
import org.sensorvision.service.IssueCommentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/support")
@PreAuthorize("hasRole('ADMIN')")
public class AdminIssueController {

    private static final Logger logger = LoggerFactory.getLogger(AdminIssueController.class);

    private final AdminIssueService adminIssueService;
    private final IssueCommentService commentService;

    @Autowired
    public AdminIssueController(AdminIssueService adminIssueService,
                               IssueCommentService commentService) {
        this.adminIssueService = adminIssueService;
        this.commentService = commentService;
    }

    /**
     * Get all issues (admin view)
     * GET /api/v1/admin/support/issues
     */
    @GetMapping("/issues")
    public ResponseEntity<List<AdminIssueDto>> getAllIssues(
            @RequestParam(required = false) IssueStatus status) {
        logger.info("Admin fetching issues with status filter: {}", status);

        List<AdminIssueDto> issues = status != null
            ? adminIssueService.getIssuesByStatus(status)
            : adminIssueService.getAllIssues();

        return ResponseEntity.ok(issues);
    }

    /**
     * Get a specific issue by ID
     * GET /api/v1/admin/support/issues/{id}
     */
    @GetMapping("/issues/{id}")
    public ResponseEntity<IssueSubmissionResponse> getIssueById(@PathVariable Long id) {
        logger.info("Admin fetching issue: {}", id);
        IssueSubmissionResponse issue = adminIssueService.getIssueById(id);
        return ResponseEntity.ok(issue);
    }

    /**
     * Update issue status
     * PATCH /api/v1/admin/support/issues/{id}/status
     */
    @PatchMapping("/issues/{id}/status")
    public ResponseEntity<IssueSubmissionResponse> updateIssueStatus(
            @PathVariable Long id,
            @Valid @RequestBody IssueStatusUpdateRequest request) {
        logger.info("Admin updating issue {} status to {}", id, request.status());

        IssueSubmissionResponse updated = adminIssueService.updateIssueStatus(id, request.status());
        return ResponseEntity.ok(updated);
    }

    /**
     * Get all comments for an issue (including internal)
     * GET /api/v1/admin/support/issues/{id}/comments
     */
    @GetMapping("/issues/{id}/comments")
    public ResponseEntity<List<IssueCommentDto>> getIssueComments(@PathVariable Long id) {
        logger.info("Admin fetching comments for issue: {}", id);
        List<IssueCommentDto> comments = commentService.getAdminComments(id);
        return ResponseEntity.ok(comments);
    }

    /**
     * Add a comment to an issue
     * POST /api/v1/admin/support/issues/{id}/comments
     */
    @PostMapping("/issues/{id}/comments")
    public ResponseEntity<IssueCommentDto> addComment(
            @PathVariable Long id,
            @Valid @RequestBody IssueCommentRequest request) {
        logger.info("Admin adding {} comment to issue: {}",
            request.internal() ? "internal" : "public", id);

        IssueCommentDto comment = commentService.addAdminComment(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(comment);
    }

    /**
     * Download screenshot for an issue
     * GET /api/v1/admin/support/issues/{id}/screenshot
     */
    @GetMapping("/issues/{id}/screenshot")
    public ResponseEntity<byte[]> getIssueScreenshot(@PathVariable Long id) {
        logger.info("Admin downloading screenshot for issue: {}", id);

        try {
            byte[] screenshot = adminIssueService.getIssueScreenshot(id);
            String contentType = adminIssueService.getIssueScreenshotContentType(id);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType(contentType));
            headers.setContentLength(screenshot.length);
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"screenshot-" + id + ".png\"");

            return new ResponseEntity<>(screenshot, headers, HttpStatus.OK);
        } catch (RuntimeException e) {
            logger.error("Failed to get screenshot for issue {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }
}
