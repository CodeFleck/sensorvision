package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.PluginProvider;
import io.indcloud.plugin.BaseWebhookPlugin;
import io.indcloud.plugin.PluginProcessingException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Generic HTTP Webhook Plugin.
 * Accepts webhook data in SensorVision's standard telemetry format.
 *
 * Expected payload format:
 * {
 *   "deviceId": "device-001",
 *   "timestamp": "2024-01-01T12:00:00Z",  // optional
 *   "variables": {
 *     "temperature": 25.5,
 *     "humidity": 60.0
 *   },
 *   "metadata": {  // optional
 *     "location": "Building A"
 *   }
 * }
 */
@Component
public class HttpWebhookPlugin extends BaseWebhookPlugin {

    @Override
    protected String extractDeviceId(JsonNode payload, JsonNode config) throws PluginProcessingException {
        // Check if custom device ID field is specified in config
        String deviceIdField = "deviceId";
        if (config.has("deviceIdField")) {
            deviceIdField = config.get("deviceIdField").asText();
        }

        JsonNode deviceIdNode = payload.get(deviceIdField);
        if (deviceIdNode == null || deviceIdNode.isNull()) {
            throw new PluginProcessingException("Missing required field: " + deviceIdField);
        }

        return deviceIdNode.asText();
    }

    @Override
    protected Instant extractTimestamp(JsonNode payload, JsonNode config) {
        String timestampField = "timestamp";
        if (config.has("timestampField")) {
            timestampField = config.get("timestampField").asText();
        }

        JsonNode timestampNode = payload.get(timestampField);
        if (timestampNode != null && !timestampNode.isNull()) {
            try {
                return Instant.parse(timestampNode.asText());
            } catch (Exception e) {
                // Fall back to current time
            }
        }

        return Instant.now();
    }

    @Override
    protected Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) throws PluginProcessingException {
        String variablesField = "variables";
        if (config.has("variablesField")) {
            variablesField = config.get("variablesField").asText();
        }

        JsonNode variablesNode = payload.get(variablesField);
        if (variablesNode == null || !variablesNode.isObject()) {
            throw new PluginProcessingException("Missing or invalid 'variables' field");
        }

        Map<String, BigDecimal> variables = new HashMap<>();
        variablesNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                variables.put(entry.getKey(), entry.getValue().decimalValue());
            }
        });

        return variables;
    }

    @Override
    protected Map<String, Object> extractMetadata(JsonNode payload, JsonNode config) {
        String metadataField = "metadata";
        if (config.has("metadataField")) {
            metadataField = config.get("metadataField").asText();
        }

        JsonNode metadataNode = payload.get(metadataField);
        if (metadataNode != null && metadataNode.isObject()) {
            return objectMapper.convertValue(metadataNode, Map.class);
        }

        return new HashMap<>();
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.HTTP_WEBHOOK;
    }
}
