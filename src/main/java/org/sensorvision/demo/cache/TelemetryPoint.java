package org.sensorvision.demo.cache;

import java.time.Instant;
import java.util.Map;

/**
 * Immutable value object representing a single telemetry data point.
 *
 * Used by the rolling cache to store recent telemetry readings in memory
 * for fast retrieval without database queries.
 *
 * @param timestamp The measurement timestamp
 * @param variables Map of variable name to value (e.g., "temperature" -> 60.5)
 */
public record TelemetryPoint(
    Instant timestamp,
    Map<String, Object> variables
) {
    /**
     * Creates a defensive copy to ensure immutability
     */
    public TelemetryPoint {
        variables = Map.copyOf(variables);
    }

    /**
     * Get a variable value as Double (convenience method)
     */
    public Double getDoubleValue(String key) {
        Object value = variables.get(key);
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
}
