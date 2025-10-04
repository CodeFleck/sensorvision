package org.sensorvision.mqtt;

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
import org.sensorvision.service.TelemetryIngestionService;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class TelemetryMessageHandler {

    private final ObjectMapper objectMapper;
    private final TelemetryIngestionService telemetryIngestionService;

    @ServiceActivator(inputChannel = "mqttInboundChannel")
    public void handleMessage(Message<String> message) {
        String raw = message.getPayload();
        try {
            JsonNode root = objectMapper.readTree(raw);
            TelemetryPayload telemetryPayload = toPayload(root);
            telemetryIngestionService.ingest(telemetryPayload);
        } catch (Exception ex) {
            String topic = (String) message.getHeaders().getOrDefault("mqtt_receivedTopic", "unknown");
            log.error("Failed to process telemetry message from topic {}: {}", topic, ex.getMessage(), ex);
        }
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
            metadata = objectMapper.convertValue(metadataNode, Map.class);
        }

        return new TelemetryPayload(deviceId, timestamp, variables, metadata);
    }

    private Instant parseTimestamp(String value) {
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException first) {
            return LocalDateTime.parse(value).toInstant(ZoneOffset.UTC);
        }
    }

    private String textValue(JsonNode node, String field) {
        JsonNode valueNode = node.get(field);
        return valueNode != null && !valueNode.isNull() ? valueNode.asText() : null;
    }
}
