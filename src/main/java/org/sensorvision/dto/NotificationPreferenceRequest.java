package org.sensorvision.dto;

import jakarta.validation.constraints.NotNull;
import org.sensorvision.model.AlertSeverity;
import org.sensorvision.model.NotificationChannel;

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
