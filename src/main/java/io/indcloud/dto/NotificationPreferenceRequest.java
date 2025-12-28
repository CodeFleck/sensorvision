package io.indcloud.dto;

import jakarta.validation.constraints.NotNull;
import io.indcloud.model.AlertSeverity;
import io.indcloud.model.NotificationChannel;

public record NotificationPreferenceRequest(
        @NotNull(message = "Channel is required")
        NotificationChannel channel,

        Boolean enabled,

        String destination,

        AlertSeverity minSeverity,

        Boolean immediate,

        Integer digestIntervalMinutes
) {
}
