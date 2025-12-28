package io.indcloud.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import io.indcloud.model.AlertSeverity;

public record AlertResponse(
        UUID id,
        UUID ruleId,
        String ruleName,
        String deviceId,
        String deviceName,
        String message,
        AlertSeverity severity,
        BigDecimal triggeredValue,
        Boolean acknowledged,
        Instant acknowledgedAt,
        Instant timestamp
) {
}