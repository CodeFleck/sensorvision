package io.indcloud.dto;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating dashboard sharing settings
 */
public record DashboardSharingRequest(
        @NotNull(message = "isPublic is required")
        Boolean isPublic
) {
}
