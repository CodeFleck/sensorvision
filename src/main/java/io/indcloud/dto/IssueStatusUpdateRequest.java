package io.indcloud.dto;

import jakarta.validation.constraints.NotNull;
import io.indcloud.model.IssueStatus;

public record IssueStatusUpdateRequest(
    @NotNull(message = "Status is required")
    IssueStatus status
) {
}
