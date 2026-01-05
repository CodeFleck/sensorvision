package io.indcloud.dto;

import io.indcloud.model.DeviceTypeVariable.VariableDataType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request DTO for device type variable definition.
 */
public record DeviceTypeVariableRequest(
        @NotBlank(message = "Variable name is required")
        @Size(max = 100)
        String name,

        @NotBlank(message = "Label is required")
        @Size(max = 200)
        String label,

        @Size(max = 50)
        String unit,

        VariableDataType dataType,

        BigDecimal minValue,

        BigDecimal maxValue,

        Boolean required,

        String defaultValue,

        @Size(max = 500)
        String description,

        Integer displayOrder
) {}
