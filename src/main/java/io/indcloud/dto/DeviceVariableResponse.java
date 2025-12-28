package io.indcloud.dto;

import io.indcloud.model.Variable;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for device-specific variables (EAV pattern).
 */
public record DeviceVariableResponse(
        Long id,
        UUID deviceId,
        String deviceExternalId,
        String name,
        String displayName,
        String description,
        String unit,
        Variable.DataType dataType,
        Variable.DataSource dataSource,
        String icon,
        String color,
        Double minValue,
        Double maxValue,
        Integer decimalPlaces,
        BigDecimal lastValue,
        Instant lastValueAt,
        Instant createdAt,
        Instant updatedAt
) {
    /**
     * Create from Variable entity.
     */
    public static DeviceVariableResponse from(Variable variable) {
        return new DeviceVariableResponse(
                variable.getId(),
                variable.getDevice() != null ? variable.getDevice().getId() : null,
                variable.getDevice() != null ? variable.getDevice().getExternalId() : null,
                variable.getName(),
                variable.getEffectiveDisplayName(),
                variable.getDescription(),
                variable.getUnit(),
                variable.getDataType(),
                variable.getDataSource(),
                variable.getIcon(),
                variable.getColor(),
                variable.getMinValue(),
                variable.getMaxValue(),
                variable.getDecimalPlaces(),
                variable.getLastValue(),
                variable.getLastValueAt(),
                variable.getCreatedAt(),
                variable.getUpdatedAt()
        );
    }
}
