package io.indcloud.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TelemetryPointDto(
        String deviceId,
        Instant timestamp,
        Double kwConsumption,
        Double voltage,
        Double current,
        Double powerFactor,
        Double frequency
) {
}
