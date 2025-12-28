package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record EmailTemplateResponse(
        Long id,
        String name,
        String description,
        String templateType,
        String subject,
        String body,
        JsonNode variables,
        Boolean isDefault,
        Boolean active,
        String createdBy,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
