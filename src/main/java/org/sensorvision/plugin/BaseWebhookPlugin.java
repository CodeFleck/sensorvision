package org.sensorvision.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sensorvision.model.DataPlugin;
import org.sensorvision.mqtt.TelemetryPayload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Base class for webhook receiver plugins.
 * Webhook plugins receive HTTP POST requests and transform them into telemetry data.
 */
public abstract class BaseWebhookPlugin implements DataPluginProcessor {

    protected final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Extract device ID from the webhook payload
     *
     * @param payload Webhook JSON payload
     * @param config Plugin configuration
     * @return Device ID
     * @throws PluginProcessingException if device ID cannot be extracted
     */
    protected abstract String extractDeviceId(JsonNode payload, JsonNode config) throws PluginProcessingException;

    /**
     * Extract timestamp from the webhook payload
     *
     * @param payload Webhook JSON payload
     * @param config Plugin configuration
     * @return Timestamp, or null to use current time
     */
    protected abstract Instant extractTimestamp(JsonNode payload, JsonNode config);

    /**
     * Extract telemetry variables from the webhook payload
     *
     * @param payload Webhook JSON payload
     * @param config Plugin configuration
     * @return Map of variable names to values
     * @throws PluginProcessingException if variables cannot be extracted
     */
    protected abstract Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) throws PluginProcessingException;

    /**
     * Extract optional metadata from the webhook payload
     *
     * @param payload Webhook JSON payload
     * @param config Plugin configuration
     * @return Map of metadata, or empty map
     */
    protected Map<String, Object> extractMetadata(JsonNode payload, JsonNode config) {
        return new HashMap<>();
    }

    @Override
    public List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException {
        if (!(rawData instanceof String)) {
            throw new PluginProcessingException("Expected JSON string payload");
        }

        try {
            JsonNode payload = objectMapper.readTree((String) rawData);
            JsonNode config = plugin.getConfiguration();

            String deviceId = extractDeviceId(payload, config);
            Instant timestamp = extractTimestamp(payload, config);
            if (timestamp == null) {
                timestamp = Instant.now();
            }
            Map<String, BigDecimal> variables = extractVariables(payload, config);
            Map<String, Object> metadata = extractMetadata(payload, config);

            TelemetryPayload telemetryPayload = new TelemetryPayload(
                    deviceId,
                    timestamp,
                    variables,
                    metadata
            );

            List<TelemetryPayload> result = new ArrayList<>();
            result.add(telemetryPayload);
            return result;
        } catch (Exception e) {
            throw new PluginProcessingException("Failed to process webhook payload", e);
        }
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        // Base validation - subclasses can override
        return PluginValidationResult.valid();
    }
}
