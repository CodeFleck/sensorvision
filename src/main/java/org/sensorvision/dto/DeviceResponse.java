package org.sensorvision.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import org.sensorvision.model.DeviceStatus;

public record DeviceResponse(
                String externalId,
                String name,
                String description,
                Boolean active,
                String location,
                String sensorType,
                String firmwareVersion,
                DeviceStatus status,
                Instant lastSeenAt,
                Integer healthScore,
                String healthStatus,
                LocalDateTime lastHealthCheckAt,
                java.util.List<DeviceTagDto> tags,
                java.util.List<DeviceGroupDto> groups) {
}
