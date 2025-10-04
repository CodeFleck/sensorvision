package org.sensorvision.dto;

import java.time.Instant;
import org.sensorvision.model.DeviceStatus;

public record DeviceResponse(
        String externalId,
        String name,
        String location,
        String sensorType,
        String firmwareVersion,
        DeviceStatus status,
        Instant lastSeenAt
) {
}
