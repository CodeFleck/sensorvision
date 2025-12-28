package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating/updating device groups
 */
public record DeviceGroupRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description
) {
}
