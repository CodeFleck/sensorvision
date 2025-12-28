package io.indcloud.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record GeofenceAssignmentResponse(
        Long geofenceId,
        String geofenceName,
        UUID deviceId,
        String deviceName,
        Boolean alertOnEnter,
        Boolean alertOnExit,
        LocalDateTime assignedAt,
        Boolean deviceCurrentlyInside
) {
}
