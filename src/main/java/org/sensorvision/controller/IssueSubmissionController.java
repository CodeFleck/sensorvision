package org.sensorvision.controller;

import jakarta.validation.Valid;
import org.sensorvision.dto.IssueSubmissionRequest;
import org.sensorvision.dto.IssueSubmissionResponse;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.service.IssueSubmissionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/support")
@PreAuthorize("isAuthenticated()")
public class IssueSubmissionController {

    private static final Logger logger = LoggerFactory.getLogger(IssueSubmissionController.class);

    private final IssueSubmissionService issueSubmissionService;

    @Autowired
    public IssueSubmissionController(IssueSubmissionService issueSubmissionService) {
        this.issueSubmissionService = issueSubmissionService;
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
}
