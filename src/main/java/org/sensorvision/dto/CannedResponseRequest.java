package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CannedResponseRequest(
    @NotBlank(message = "Title is required")
    @Size(max = 100, message = "Title must not exceed 100 characters")
    String title,

    @NotBlank(message = "Body is required")
    String body,

    String category,

    Boolean active
) {
}
