package io.indcloud.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record PlaylistItemCreateRequest(
    @NotNull(message = "Dashboard ID is required")
    Long dashboardId,

    @Min(value = 5, message = "Display duration must be at least 5 seconds")
    @Max(value = 3600, message = "Display duration must not exceed 3600 seconds (1 hour)")
    Integer displayDurationSeconds
) {
}
