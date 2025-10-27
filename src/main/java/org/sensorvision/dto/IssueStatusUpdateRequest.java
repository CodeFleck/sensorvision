package org.sensorvision.dto;

import jakarta.validation.constraints.NotNull;
import org.sensorvision.model.IssueStatus;

public record IssueStatusUpdateRequest(
    @NotNull(message = "Status is required")
    IssueStatus status
) {
}
