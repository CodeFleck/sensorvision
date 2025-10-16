package org.sensorvision.dto;

import org.sensorvision.model.Variable;

import java.time.Instant;

/**
 * Response DTO for variables
 */
public record VariableResponse(
        Long id,
        String name,
        String displayName,
        String description,
        String unit,
        Variable.DataType dataType,
        String icon,
        String color,
        Double minValue,
        Double maxValue,
        Integer decimalPlaces,
        Boolean isSystemVariable,
        Instant createdAt,
        Instant updatedAt
) {
}
