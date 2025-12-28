package io.indcloud.dto;

import io.indcloud.model.Event;

import java.time.LocalDateTime;
import java.util.Map;

public record EventResponse(
        Long id,
        String eventType,
        String severity,
        String entityType,
        String entityId,
        String title,
        String description,
        Map<String, Object> metadata,
        Long userId,
        String deviceId,
        LocalDateTime createdAt
) {
    public static EventResponse fromEvent(Event event) {
        return new EventResponse(
                event.getId(),
                event.getEventType().name(),
                event.getSeverity().name(),
                event.getEntityType(),
                event.getEntityId(),
                event.getTitle(),
                event.getDescription(),
                event.getMetadata(),
                event.getUser() != null ? event.getUser().getId() : null,
                event.getDeviceId(),
                event.getCreatedAt()
        );
    }
}
