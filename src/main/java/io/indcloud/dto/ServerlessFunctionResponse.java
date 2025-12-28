package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.FunctionRuntime;
import io.indcloud.model.ServerlessFunction;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

public record ServerlessFunctionResponse(
    Long id,
    String name,
    String description,
    FunctionRuntime runtime,
    String code,
    String handler,
    Boolean enabled,
    Integer timeoutSeconds,
    Integer memoryLimitMb,
    JsonNode environmentVariables,
    List<FunctionTriggerResponse> triggers,
    String createdBy,
    Instant createdAt,
    Instant updatedAt
) {
    public static ServerlessFunctionResponse fromEntity(ServerlessFunction function) {
        return new ServerlessFunctionResponse(
            function.getId(),
            function.getName(),
            function.getDescription(),
            function.getRuntime(),
            function.getCode(),
            function.getHandler(),
            function.getEnabled(),
            function.getTimeoutSeconds(),
            function.getMemoryLimitMb(),
            function.getEnvironmentVariables(),
            function.getTriggers().stream()
                .map(FunctionTriggerResponse::fromEntity)
                .collect(Collectors.toList()),
            function.getCreatedBy() != null ? function.getCreatedBy().getUsername() : null,
            function.getCreatedAt(),
            function.getUpdatedAt()
        );
    }

    public static ServerlessFunctionResponse fromEntityWithoutCode(ServerlessFunction function) {
        return new ServerlessFunctionResponse(
            function.getId(),
            function.getName(),
            function.getDescription(),
            function.getRuntime(),
            null,  // Don't return code in list view for performance
            function.getHandler(),
            function.getEnabled(),
            function.getTimeoutSeconds(),
            function.getMemoryLimitMb(),
            null,  // Don't return env vars in list view for security
            function.getTriggers().stream()
                .map(FunctionTriggerResponse::fromEntity)
                .collect(Collectors.toList()),
            function.getCreatedBy() != null ? function.getCreatedBy().getUsername() : null,
            function.getCreatedAt(),
            function.getUpdatedAt()
        );
    }
}
