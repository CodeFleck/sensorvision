package org.sensorvision.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import org.sensorvision.model.DeviceStatus;

public record DeviceResponse(
        String externalId,
        String name,
        String location,
        String sensorType,
        String firmwareVersion,
        DeviceStatus status,
        Instant lastSeenAt,
        Integer healthScore,
        String healthStatus,
        LocalDateTime lastHealthCheckAt
) {
}
