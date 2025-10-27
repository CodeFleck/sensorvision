package org.sensorvision.service;

import org.sensorvision.dto.AdminIssueDto;
import org.sensorvision.dto.IssueSubmissionResponse;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.repository.IssueCommentRepository;
import org.sensorvision.repository.IssueSubmissionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AdminIssueService {

    private static final Logger logger = LoggerFactory.getLogger(AdminIssueService.class);

    private final IssueSubmissionRepository issueRepository;
    private final IssueCommentRepository commentRepository;

    @Autowired
    public AdminIssueService(IssueSubmissionRepository issueRepository,
                            IssueCommentRepository commentRepository) {
        this.issueRepository = issueRepository;
        this.commentRepository = commentRepository;
    }

    /**
     * Get all issues (admin view)
     */
    @Transactional(readOnly = true)
    public List<AdminIssueDto> getAllIssues() {
        return issueRepository.findAll().stream()
            .map(this::mapToAdminDto)
            .collect(Collectors.toList());
    }

    /**
     * Get issues by status
     */
    @Transactional(readOnly = true)
    public List<AdminIssueDto> getIssuesByStatus(IssueStatus status) {
        return issueRepository.findByStatusOrderByCreatedAtDesc(status).stream()
            .map(this::mapToAdminDto)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific issue by ID
     */
    @Transactional(readOnly = true)
    public IssueSubmissionResponse getIssueById(Long id) {
        IssueSubmission issue = issueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));
        return IssueSubmissionResponse.fromEntity(issue);
    }

    /**
     * Update issue status
     */
    @Transactional
    public IssueSubmissionResponse updateIssueStatus(Long id, IssueStatus newStatus) {
        IssueSubmission issue = issueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        IssueStatus oldStatus = issue.getStatus();
        issue.setStatus(newStatus);
        IssueSubmission updated = issueRepository.save(issue);

        logger.info("Issue {} status changed from {} to {}", id, oldStatus, newStatus);

        return IssueSubmissionResponse.fromEntity(updated);
    }

    /**
     * Get screenshot data for an issue
     */
    @Transactional(readOnly = true)
    public byte[] getIssueScreenshot(Long id) {
        IssueSubmission issue = issueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        if (issue.getScreenshotData() == null || issue.getScreenshotData().length == 0) {
            throw new RuntimeException("No screenshot available for issue: " + id);
        }

        return issue.getScreenshotData();
    }

    /**
     * Get screenshot content type for an issue
     */
    @Transactional(readOnly = true)
    public String getIssueScreenshotContentType(Long id) {
        IssueSubmission issue = issueRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Issue not found with id: " + id));

        return issue.getScreenshotContentType() != null
            ? issue.getScreenshotContentType()
            : "image/png";
    }

    /**
     * Map IssueSubmission to AdminIssueDto with comment count
     */
    private AdminIssueDto mapToAdminDto(IssueSubmission issue) {
        long commentCount = commentRepository.countPublicCommentsByIssue(issue);

        return new AdminIssueDto(
            issue.getId(),
            issue.getTitle(),
            issue.getDescription(),
            issue.getCategory(),
            issue.getSeverity(),
            issue.getStatus(),
            issue.getScreenshotData() != null && issue.getScreenshotData().length > 0,
            issue.getUser().getUsername(),
            issue.getUser().getEmail(),
            issue.getUser().getId(),
            issue.getOrganization().getName(),
            commentCount,
            issue.getCreatedAt(),
            issue.getUpdatedAt()
        );
    }
}
