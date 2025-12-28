package io.indcloud.dto;

import io.indcloud.model.IssueCategory;
import io.indcloud.model.IssueSeverity;
import io.indcloud.model.IssueStatus;

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
