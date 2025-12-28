package io.indcloud.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record AggregationResponse(
        String deviceId,
        String variable,
        String aggregation,
        Instant timestamp,
        BigDecimal value,
        Long count
) {
}