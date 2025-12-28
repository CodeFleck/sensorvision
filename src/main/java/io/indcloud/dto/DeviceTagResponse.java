package io.indcloud.dto;

import java.time.Instant;
import java.util.Set;

/**
 * Response DTO for device tags
 */
public record DeviceTagResponse(
        Long id,
        String name,
        String color,
        Integer deviceCount,
        Set<String> deviceIds,  // External IDs of devices with this tag
        Instant createdAt
) {
}
