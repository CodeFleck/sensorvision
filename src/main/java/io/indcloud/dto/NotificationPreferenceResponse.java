package io.indcloud.dto;

import io.indcloud.model.AlertSeverity;
import io.indcloud.model.NotificationChannel;

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
