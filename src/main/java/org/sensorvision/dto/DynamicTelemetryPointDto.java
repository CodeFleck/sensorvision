package org.sensorvision.dto;

import java.time.Instant;
import java.util.Map;

/**
 * DTO for dynamic telemetry data with all variables.
 * This supports the EAV pattern where any variable can be sent.
 *
 * Example WebSocket message:
 * {
 *   "type": "DYNAMIC_TELEMETRY",
 *   "deviceId": "sensor-001",
 *   "timestamp": "2024-01-15T10:30:00Z",
 *   "variables": {
 *     "temperature": 23.5,
 *     "humidity": 65.2,
 *     "custom_sensor": 42.0
 *   }
 * }
 */
public record DynamicTelemetryPointDto(
        String deviceId,
        Instant timestamp,
        Map<String, Double> variables
) {
}
