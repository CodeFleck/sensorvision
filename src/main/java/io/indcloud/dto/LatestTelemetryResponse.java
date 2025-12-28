package io.indcloud.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record LatestTelemetryResponse(
        String externalId,
        Instant timestamp,
        BigDecimal kwConsumption,
        BigDecimal voltage,
        BigDecimal current,
        BigDecimal powerFactor,
        BigDecimal frequency,
        Map<String, Object> metadata
) {
}
