package io.indcloud.dto;

import io.indcloud.model.Event;

import java.time.LocalDateTime;

public record EventExportRequest(
        Event.EventType eventType,  // Optional: filter by event type
        Event.EventSeverity severity,  // Optional: filter by severity
        LocalDateTime startTime,
        LocalDateTime endTime,
        String format  // "excel" or "csv"
) {
}
