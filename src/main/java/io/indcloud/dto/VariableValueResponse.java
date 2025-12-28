package io.indcloud.dto;

import io.indcloud.model.VariableValue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Response DTO for variable time-series values.
 */
public record VariableValueResponse(
        UUID id,
        Long variableId,
        Instant timestamp,
        BigDecimal value,
        Map<String, Object> context,
        Instant createdAt
) {
    /**
     * Create from VariableValue entity.
     */
    public static VariableValueResponse from(VariableValue variableValue) {
        return new VariableValueResponse(
                variableValue.getId(),
                variableValue.getVariable().getId(),
                variableValue.getTimestamp(),
                variableValue.getValue(),
                variableValue.getContext(),
                variableValue.getCreatedAt()
        );
    }
}
