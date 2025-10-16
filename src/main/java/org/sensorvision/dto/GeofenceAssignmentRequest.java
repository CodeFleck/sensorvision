package org.sensorvision.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record GeofenceAssignmentRequest(
        @NotNull(message = "Device ID is required")
        UUID deviceId,

        Boolean alertOnEnter,

        Boolean alertOnExit
) {
}
