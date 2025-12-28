package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.indcloud.model.FunctionRuntime;

public record ServerlessFunctionRequest(
    @NotBlank(message = "Function name is required")
    String name,

    String description,

    @NotNull(message = "Runtime is required")
    FunctionRuntime runtime,

    @NotBlank(message = "Code is required")
    String code,

    String handler,

    Boolean enabled,

    @Min(value = 1, message = "Timeout must be at least 1 second")
    @Max(value = 300, message = "Timeout cannot exceed 300 seconds")
    Integer timeoutSeconds,

    @Min(value = 128, message = "Memory limit must be at least 128 MB")
    @Max(value = 2048, message = "Memory limit cannot exceed 2048 MB")
    Integer memoryLimitMb,

    JsonNode environmentVariables
) {
}
