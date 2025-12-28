package io.indcloud.dto;

import io.indcloud.model.NotificationChannel;
import io.indcloud.model.NotificationLog;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationLogResponse(
        Long id,
        UUID alertId,
        NotificationChannel channel,
        String destination,
        String subject,
        String message,
        NotificationLog.NotificationStatus status,
        String errorMessage,
        LocalDateTime sentAt,
        LocalDateTime createdAt
) {
}
