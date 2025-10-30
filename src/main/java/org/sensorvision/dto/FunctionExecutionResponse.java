package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.FunctionExecution;
import org.sensorvision.model.FunctionExecutionStatus;

import java.time.Instant;

public record FunctionExecutionResponse(
    Long id,
    Long functionId,
    String functionName,
    FunctionExecutionStatus status,
    Instant startedAt,
    Instant completedAt,
    Integer durationMs,
    JsonNode inputData,
    JsonNode outputData,
    String errorMessage,
    String errorStack,
    Integer memoryUsedMb
) {
    public static FunctionExecutionResponse fromEntity(FunctionExecution execution) {
        return new FunctionExecutionResponse(
            execution.getId(),
            execution.getFunction().getId(),
            execution.getFunction().getName(),
            execution.getStatus(),
            execution.getStartedAt(),
            execution.getCompletedAt(),
            execution.getDurationMs(),
            execution.getInputData(),
            execution.getOutputData(),
            execution.getErrorMessage(),
            execution.getErrorStack(),
            execution.getMemoryUsedMb()
        );
    }

    public static FunctionExecutionResponse fromEntityWithoutDetails(FunctionExecution execution) {
        return new FunctionExecutionResponse(
            execution.getId(),
            execution.getFunction().getId(),
            execution.getFunction().getName(),
            execution.getStatus(),
            execution.getStartedAt(),
            execution.getCompletedAt(),
            execution.getDurationMs(),
            null,  // Don't include input/output in list view
            null,
            execution.getErrorMessage(),
            null,  // Don't include stack trace in list view
            execution.getMemoryUsedMb()
        );
    }
}
