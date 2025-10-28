package org.sensorvision.dto;

import org.sensorvision.model.IssueCategory;
import org.sensorvision.model.IssueSeverity;
import org.sensorvision.model.IssueStatus;

import java.time.Instant;

/**
 * Extended DTO for admin issue list view with comment count
 */
public record AdminIssueDto(
    Long id,
    String title,
    String description,
    IssueCategory category,
    IssueSeverity severity,
    IssueStatus status,
    boolean hasScreenshot,
    String username,
    String userEmail,
    Long userId,
    String organizationName,
    Long commentCount,
    Instant createdAt,
    Instant updatedAt
) {
}
