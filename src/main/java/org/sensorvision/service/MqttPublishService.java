package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.MessageChannel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service for publishing commands to devices via MQTT
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MqttPublishService {

    private final MessageChannel mqttOutboundChannel;
    private final ObjectMapper objectMapper;

    /**
     * Publish a command to a specific device
     * Topic: sensorvision/devices/{deviceId}/commands
     *
     * @param deviceId The target device ID
     * @param command Command type (e.g., "toggle_relay", "set_value", "reboot")
     * @param payload Command payload (can be null)
     */
    public void publishCommand(String deviceId, String command, Object payload) {
        try {
            String topic = String.format("sensorvision/devices/%s/commands", deviceId);

            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("payload", payload);
            message.put("timestamp", Instant.now().toString());

            String jsonMessage = objectMapper.writeValueAsString(message);

            mqttOutboundChannel.send(
                    MessageBuilder
                            .withPayload(jsonMessage)
                            .setHeader("mqtt_topic", topic)
                            .build()
            );

            log.info("Published MQTT command '{}' to device {} on topic: {}", command, deviceId, topic);

        } catch (Exception e) {
            log.error("Failed to publish MQTT command to device: {}", deviceId, e);
            throw new RuntimeException("Failed to publish MQTT command", e);
        }
    }

    /**
     * Publish a command with specific value
     */
    public void publishValueCommand(String deviceId, String variable, Object value) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("variable", variable);
        payload.put("value", value);

        publishCommand(deviceId, "set_value", payload);
    }

    /**
     * Publish a toggle command (for relays, LEDs, etc.)
     */
    public void publishToggleCommand(String deviceId, String target) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("target", target);

        publishCommand(deviceId, "toggle", payload);
    }

    /**
     * Publish a simple command without payload
     */
    public void publishSimpleCommand(String deviceId, String command) {
        publishCommand(deviceId, command, null);
    }

    /**
     * Broadcast a command to all devices
     * Topic: sensorvision/devices/broadcast/commands
     */
    public void broadcastCommand(String command, Object payload) {
        try {
            String topic = "sensorvision/devices/broadcast/commands";

            Map<String, Object> message = new HashMap<>();
            message.put("command", command);
            message.put("payload", payload);
            message.put("timestamp", Instant.now().toString());

            String jsonMessage = objectMapper.writeValueAsString(message);

            mqttOutboundChannel.send(
                    MessageBuilder
                            .withPayload(jsonMessage)
                            .setHeader("mqtt_topic", topic)
                            .build()
            );

            log.info("Broadcast MQTT command '{}' to all devices on topic: {}", command, topic);

        } catch (Exception e) {
            log.error("Failed to broadcast MQTT command", e);
            throw new RuntimeException("Failed to broadcast MQTT command", e);
        }
    }
}
