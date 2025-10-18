package org.sensorvision.dto;

import org.sensorvision.model.IssueCategory;
import org.sensorvision.model.IssueSeverity;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.model.IssueSubmission;

import java.time.Instant;

/**
 * DTO for issue submission response (excludes binary screenshot data)
 */
public record IssueSubmissionResponse(
    Long id,
    String title,
    String description,
    IssueCategory category,
    IssueSeverity severity,
    IssueStatus status,
    String screenshotFilename,
    boolean hasScreenshot,
    String browserInfo,
    String pageUrl,
    String userAgent,
    String screenResolution,
    String username,
    String organizationName,
    Instant createdAt,
    Instant updatedAt
) {
    public static IssueSubmissionResponse fromEntity(IssueSubmission issue) {
        return new IssueSubmissionResponse(
            issue.getId(),
            issue.getTitle(),
            issue.getDescription(),
            issue.getCategory(),
            issue.getSeverity(),
            issue.getStatus(),
            issue.getScreenshotFilename(),
            issue.getScreenshotData() != null && issue.getScreenshotData().length > 0,
            issue.getBrowserInfo(),
            issue.getPageUrl(),
            issue.getUserAgent(),
            issue.getScreenResolution(),
            issue.getUser().getUsername(),
            issue.getOrganization().getName(),
            issue.getCreatedAt(),
            issue.getUpdatedAt()
        );
    }
}
