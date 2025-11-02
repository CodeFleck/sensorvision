package org.sensorvision.mqtt;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Device;
import org.sensorvision.service.DeviceService;
import org.sensorvision.service.DeviceTokenService;
import org.sensorvision.service.TelemetryIngestionService;
import org.sensorvision.service.triggers.MqttFunctionTriggerHandler;
import org.sensorvision.service.triggers.TriggerContext;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryMessageHandler {

    private final ObjectMapper objectMapper;
    private final TelemetryIngestionService telemetryIngestionService;
    private final DeviceService deviceService;
    private final DeviceTokenService deviceTokenService;
    private final MqttFunctionTriggerHandler mqttTriggerHandler;

    @Value("${mqtt.device-auth.required:true}")
    private boolean deviceAuthRequired;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleMessage(Message<String> message) {
        String raw = message.getPayload();
        try {
            JsonNode root = objectMapper.readTree(raw);

            // Extract API token if provided
            String apiToken = textValue(root, "apiToken");

            TelemetryPayload telemetryPayload = toPayload(root);

            // Authenticate device if token is required
            if (deviceAuthRequired) {
                if (apiToken == null || apiToken.isEmpty()) {
                    log.warn("MQTT telemetry rejected: no API token provided for device {}",
                            telemetryPayload.deviceId());
                    return;
                }

                Device device = deviceTokenService.getDeviceByToken(apiToken).orElse(null);
                if (device == null) {
                    log.warn("MQTT telemetry rejected: invalid API token for device {}",
                            telemetryPayload.deviceId());
                    return;
                }

                // Verify the device ID matches the token
                if (!device.getExternalId().equals(telemetryPayload.deviceId())) {
                    log.warn("MQTT telemetry rejected: device ID mismatch. Token for {}, payload has {}",
                            device.getExternalId(), telemetryPayload.deviceId());
                    return;
                }

                // Update last used timestamp
                deviceTokenService.updateTokenLastUsed(apiToken);

                log.debug("Device {} authenticated via MQTT token", device.getExternalId());
            }

            // Ingest telemetry data
            telemetryIngestionService.ingest(telemetryPayload);

            // Trigger MQTT-based serverless functions
            try {
                String topic = (String) message.getHeaders().getOrDefault("mqtt_receivedTopic", "unknown");
                triggerMqttFunctions(telemetryPayload, topic);
            } catch (Exception ex) {
                log.error("Error triggering MQTT functions: {}", ex.getMessage(), ex);
                // Don't fail the whole telemetry ingestion if function triggers fail
            }
        } catch (Exception ex) {
            String topic = (String) message.getHeaders().getOrDefault("mqtt_receivedTopic", "unknown");
            log.error("Failed to process telemetry message from topic {}: {}", topic, ex.getMessage(), ex);
        }
    }

    /**
     * Trigger MQTT-based serverless functions for this telemetry message.
     */
    private void triggerMqttFunctions(TelemetryPayload payload, String topic) {
        // Build event data from telemetry
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("deviceId", payload.deviceId());
        eventData.put("timestamp", payload.timestamp().toString());

        // Add telemetry variables
        Map<String, Object> telemetry = new HashMap<>();
        payload.variables().forEach((key, value) -> telemetry.put(key, value.doubleValue()));
        eventData.put("telemetry", telemetry);

        // Add metadata if present
        if (payload.metadata() != null && !payload.metadata().isEmpty()) {
            eventData.put("metadata", payload.metadata());
        }

        JsonNode eventJson = objectMapper.valueToTree(eventData);

        // Build trigger context
        TriggerContext context = TriggerContext.builder()
            .eventType("mqtt.message")
            .eventSource(topic)
            .timestamp(payload.timestamp().toEpochMilli())
            .deviceId(payload.deviceId())
            .build();

        // Handle the event (will match against configured triggers)
        mqttTriggerHandler.handleEvent(eventJson, context);
    }

    private TelemetryPayload toPayload(JsonNode root) {
        String deviceId = textValue(root, "deviceId");
        if (deviceId == null || deviceId.isBlank()) {
            throw new IllegalArgumentException("deviceId is required in telemetry payload");
        }
        String timestampValue = textValue(root, "timestamp");
        Instant timestamp = timestampValue != null ? parseTimestamp(timestampValue) : Instant.now();

        Map<String, BigDecimal> variables = new HashMap<>();
        JsonNode variablesNode = root.path("variables");
        if (variablesNode.isObject()) {
            variablesNode.fields().forEachRemaining(entry -> {
                JsonNode value = entry.getValue();
                if (value.isNumber()) {
                    variables.put(entry.getKey(), value.decimalValue());
                }
            });
        }

        Map<String, Object> metadata = Map.of();
        JsonNode metadataNode = root.path("metadata");
        if (metadataNode.isObject()) {
            metadata = objectMapper.convertValue(metadataNode, new TypeReference<Map<String, Object>>() {});
        }

        return new TelemetryPayload(deviceId, timestamp, variables, metadata);
    }

    private Instant parseTimestamp(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException first) {
            try {
                return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
            } catch (DateTimeParseException second) {
                log.error("Failed to parse timestamp: {}. Using current time.", value);
                return Instant.now();
            }
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        return valueNode != null && !valueNode.isNull() ? valueNode.asText() : null;
    }
}
