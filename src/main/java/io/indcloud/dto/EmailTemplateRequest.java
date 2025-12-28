package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EmailTemplateRequest(
        @NotBlank(message = "Template name is required")
        @Size(max = 255, message = "Template name must not exceed 255 characters")
        String name,

        String description,

        @NotBlank(message = "Template type is required")
        @Size(max = 50, message = "Template type must not exceed 50 characters")
        String templateType,

        @NotBlank(message = "Email subject is required")
        @Size(max = 500, message = "Email subject must not exceed 500 characters")
        String subject,

        @NotBlank(message = "Email body is required")
        String body,

        JsonNode variables,

        @NotNull(message = "isDefault field is required")
        Boolean isDefault,

        @NotNull(message = "active field is required")
        Boolean active
) {
}
