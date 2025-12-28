package io.indcloud.dto;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;
import io.indcloud.model.DeviceStatus;

public record DeviceResponse(
                UUID id,
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
