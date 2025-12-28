package io.indcloud.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.indcloud.model.RuleOperator;

public record RuleResponse(
        UUID id,
        String name,
        String description,
        String deviceId,
        String deviceName,
        String variable,
        RuleOperator operator,
        BigDecimal threshold,
        Boolean enabled,
        Instant createdAt
) {
}