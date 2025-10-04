package org.sensorvision.mqtt;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

public record TelemetryPayload(
        String deviceId,
        Instant timestamp,
        Map<String, BigDecimal> variables,
        Map<String, Object> metadata
) {
}
