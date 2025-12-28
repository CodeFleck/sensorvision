package io.indcloud.dto;

import java.math.BigDecimal;
import java.util.Map;

/**
 * HTTP REST API request for telemetry data ingestion
 * Supports both single variable and bulk variable ingestion
 */
public record HttpTelemetryRequest(
        String deviceId,
        String timestamp,  // ISO-8601 format, optional - defaults to now
        Map<String, BigDecimal> variables,
        Map<String, Object> metadata
) {
}
