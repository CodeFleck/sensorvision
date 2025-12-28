package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import io.indcloud.model.Variable;

/**
 * Request DTO for creating/updating variables
 */
public record VariableRequest(
        @NotBlank(message = "Name is required")
        String name,

        String displayName,

        String description,

        String unit,

        @NotNull(message = "Data type is required")
        Variable.DataType dataType,

        String icon,

        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color must be a valid hex code (e.g., #FF5733)")
        String color,

        Double minValue,

        Double maxValue,

        Integer decimalPlaces
) {
}
