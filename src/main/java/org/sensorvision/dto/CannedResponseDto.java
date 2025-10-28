package org.sensorvision.dto;

import org.sensorvision.model.CannedResponse;

import java.time.Instant;

public record CannedResponseDto(
    Long id,
    String title,
    String body,
    String category,
    boolean active,
    int useCount,
    Long createdById,
    String createdByName,
    Instant createdAt,
    Instant updatedAt
) {
    public static CannedResponseDto fromEntity(CannedResponse response) {
        return new CannedResponseDto(
            response.getId(),
            response.getTitle(),
            response.getBody(),
            response.getCategory(),
            response.isActive(),
            response.getUseCount(),
            response.getCreatedBy().getId(),
            response.getCreatedBy().getUsername(),
            response.getCreatedAt(),
            response.getUpdatedAt()
        );
    }
}
