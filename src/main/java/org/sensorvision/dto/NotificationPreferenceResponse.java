package org.sensorvision.dto;

import org.sensorvision.model.AlertSeverity;
import org.sensorvision.model.NotificationChannel;

import java.time.LocalDateTime;

public record NotificationPreferenceResponse(
        Long id,
        NotificationChannel channel,
        Boolean enabled,
        String destination,
        AlertSeverity minSeverity,
        Boolean immediate,
        Integer digestIntervalMinutes,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
