package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * Request DTO for creating/updating device tags
 */
public record DeviceTagRequest(
        @NotBlank(message = "Name is required")
        String name,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g., #FF5733)")
        String color
) {
}
