package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.*;
import io.indcloud.model.Geofence;

import java.math.BigDecimal;

public record GeofenceRequest(
        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must not exceed 255 characters")
        String name,

        String description,

        @NotNull(message = "Shape is required")
        Geofence.GeofenceShape shape,

        @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
        @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
        BigDecimal centerLatitude,

        @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
        @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
        BigDecimal centerLongitude,

        @DecimalMin(value = "0.0", message = "Radius must be positive")
        BigDecimal radiusMeters,

        JsonNode polygonCoordinates,

        Boolean enabled
) {
}
