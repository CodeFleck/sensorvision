package org.sensorvision.dto;

import java.time.Instant;
import java.util.Set;

/**
 * Response DTO for device groups
 */
public record DeviceGroupResponse(
        Long id,
        String name,
        String description,
        Integer deviceCount,
        Set<String> deviceIds,  // External IDs of devices in group
        Instant createdAt,
        Instant updatedAt
) {
}
