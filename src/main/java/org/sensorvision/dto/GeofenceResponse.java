package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.Geofence;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record GeofenceResponse(
        Long id,
        Long organizationId,
        String name,
        String description,
        Geofence.GeofenceShape shape,
        BigDecimal centerLatitude,
        BigDecimal centerLongitude,
        BigDecimal radiusMeters,
        JsonNode polygonCoordinates,
        Boolean enabled,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        Integer assignedDeviceCount
) {
}
