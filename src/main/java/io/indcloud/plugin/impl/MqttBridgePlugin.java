package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.plugin.BaseWebhookPlugin;
import io.indcloud.plugin.PluginProcessingException;
import io.indcloud.plugin.PluginValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.paho.client.mqttv3.*;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.springframework.stereotype.Component;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

/**
 * MQTT Bridge Plugin - Connect to external MQTT brokers and forward messages to SensorVision
 *
 * This plugin acts as a bridge between external MQTT brokers and the SensorVision platform.
 * It subscribes to topics on external brokers and forwards messages to the telemetry ingestion system.
 *
 * Configuration format:
 * {
 *   "brokerUrl": "tcp://mqtt.example.com:1883",
 *   "clientId": "sensorvision-bridge",
 *   "username": "user",              // Optional
 *   "password": "pass",              // Optional
 *   "cleanSession": true,
 *   "qos": 1,
 *   "topics": [
 *     {
 *       "topic": "devices/+/telemetry",
 *       "deviceIdExtractor": "topic",    // "topic" or "payload"
 *       "topicPattern": "devices/{deviceId}/telemetry",
 *       "payloadFormat": "json",         // "json" or "custom"
 *       "variablesField": "variables",   // For JSON format
 *       "timestampField": "timestamp"    // Optional, for JSON format
 *     }
 *   ],
 *   "connectionTimeoutSeconds": 30,
 *   "keepAliveSeconds": 60,
 *   "reconnectDelaySeconds": 10,
 *   "maxReconnectAttempts": 5
 * }
 *
 * Device ID extraction modes:
 * - "topic": Extract device ID from topic using pattern (e.g., "devices/{deviceId}/telemetry")
 * - "payload": Extract device ID from message payload field (requires JSON payload)
 *
 * Payload formats:
 * - "json": Standard SensorVision JSON format with variables object
 * - "custom": Custom format requiring transformation (future enhancement)
 */
@Slf4j
@Component
public class MqttBridgePlugin extends BaseWebhookPlugin {

    // Store active MQTT clients per plugin (plugin ID -> MQTT client)
    private final Map<Long, MqttClient> activeClients = new ConcurrentHashMap<>();

    // Store message queues per plugin (plugin ID -> message queue)
    private final Map<Long, BlockingQueue<MqttMessage>> messageQueues = new ConcurrentHashMap<>();

    @Override
    protected String extractDeviceId(JsonNode payload, JsonNode config) throws PluginProcessingException {
        // This method is called for webhook processing
        // For MQTT bridge, we handle this differently in the message callback
        // But we need to implement it for the base class
        String deviceIdField = config.has("deviceIdField") ? config.get("deviceIdField").asText("deviceId") : "deviceId";

        JsonNode deviceIdNode = payload.path(deviceIdField);
        if (deviceIdNode.isMissingNode() || deviceIdNode.isNull()) {
            throw new PluginProcessingException("Missing device ID field: " + deviceIdField);
        }

        return deviceIdNode.asText();
    }

    @Override
    protected Instant extractTimestamp(JsonNode payload, JsonNode config) {
        String timestampField = config.has("timestampField") ? config.get("timestampField").asText("timestamp") : "timestamp";

        JsonNode timestampNode = payload.path(timestampField);
        if (!timestampNode.isMissingNode() && !timestampNode.isNull()) {
            try {
                return Instant.parse(timestampNode.asText());
            } catch (Exception e) {
                log.warn("Failed to parse timestamp, using current time: {}", e.getMessage());
            }
        }

        return Instant.now();
    }

    @Override
    protected Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) throws PluginProcessingException {
        String variablesField = config.has("variablesField") ? config.get("variablesField").asText("variables") : "variables";

        JsonNode variablesNode = payload.path(variablesField);
        if (variablesNode.isMissingNode() || !variablesNode.isObject()) {
            throw new PluginProcessingException("Missing or invalid variables field: " + variablesField);
        }

        Map<String, BigDecimal> variables = new HashMap<>();
        variablesNode.fields().forEachRemaining(entry -> {
            if (entry.getValue().isNumber()) {
                variables.put(entry.getKey(), entry.getValue().decimalValue());
            }
        });

        if (variables.isEmpty()) {
            throw new PluginProcessingException("No numeric variables found in payload");
        }

        return variables;
    }

    /**
     * Connect to external MQTT broker and start listening
     * This is called when the plugin is enabled/configured
     */
    public void connect(DataPlugin plugin) throws PluginProcessingException {
        Long pluginId = plugin.getId();

        // Disconnect existing client if any
        disconnect(pluginId);

        try {
            JsonNode config = plugin.getConfiguration();

            String brokerUrl = config.get("brokerUrl").asText();
            String clientId = config.has("clientId")
                ? config.get("clientId").asText()
                : "sensorvision-bridge-" + pluginId;

            int connectionTimeout = config.has("connectionTimeoutSeconds")
                ? config.get("connectionTimeoutSeconds").asInt(30)
                : 30;
            int keepAlive = config.has("keepAliveSeconds")
                ? config.get("keepAliveSeconds").asInt(60)
                : 60;
            boolean cleanSession = config.has("cleanSession")
                ? config.get("cleanSession").asBoolean(true)
                : true;

            // Create MQTT client
            MqttClient client = new MqttClient(brokerUrl, clientId, new MemoryPersistence());

            // Configure connection options
            MqttConnectOptions connOpts = new MqttConnectOptions();
            connOpts.setCleanSession(cleanSession);
            connOpts.setConnectionTimeout(connectionTimeout);
            connOpts.setKeepAliveInterval(keepAlive);

            // Set credentials if provided
            if (config.has("username") && !config.get("username").isNull()) {
                connOpts.setUserName(config.get("username").asText());
            }
            if (config.has("password") && !config.get("password").isNull()) {
                connOpts.setPassword(config.get("password").asText().toCharArray());
            }

            // Set up automatic reconnection
            connOpts.setAutomaticReconnect(true);
            int maxReconnect = config.has("maxReconnectAttempts")
                ? config.get("maxReconnectAttempts").asInt(5)
                : 5;
            connOpts.setMaxReconnectDelay(maxReconnect * 1000);

            // Create message queue for this plugin
            BlockingQueue<MqttMessage> messageQueue = new LinkedBlockingQueue<>();
            messageQueues.put(pluginId, messageQueue);

            // Set callback for incoming messages
            client.setCallback(new MqttCallback() {
                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT Bridge plugin {} lost connection to {}: {}",
                        plugin.getName(), brokerUrl, cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    log.debug("MQTT Bridge plugin {} received message on topic: {}", plugin.getName(), topic);

                    // Store topic in message properties for later extraction
                    MqttMessage enrichedMessage = new MqttMessage(message.getPayload());
                    enrichedMessage.setQos(message.getQos());
                    enrichedMessage.setRetained(message.isRetained());

                    // Add to queue for processing
                    if (!messageQueue.offer(enrichedMessage)) {
                        log.warn("Message queue full for plugin {}, dropping message", plugin.getName());
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriber
                }
            });

            // Connect to broker
            log.info("MQTT Bridge plugin {} connecting to {}", plugin.getName(), brokerUrl);
            client.connect(connOpts);

            // Subscribe to configured topics
            int qos = config.has("qos") ? config.get("qos").asInt(1) : 1;
            JsonNode topics = config.get("topics");

            for (JsonNode topicConfig : topics) {
                String topic = topicConfig.get("topic").asText();
                client.subscribe(topic, qos);
                log.info("MQTT Bridge plugin {} subscribed to topic: {}", plugin.getName(), topic);
            }

            // Store client for later use
            activeClients.put(pluginId, client);

            log.info("MQTT Bridge plugin {} successfully connected to {}", plugin.getName(), brokerUrl);

        } catch (MqttException e) {
            throw new PluginProcessingException("Failed to connect to MQTT broker: " + e.getMessage(), e);
        }
    }

    /**
     * Disconnect from external MQTT broker
     */
    public void disconnect(Long pluginId) {
        MqttClient client = activeClients.remove(pluginId);
        if (client != null) {
            try {
                if (client.isConnected()) {
                    client.disconnect();
                }
                client.close();
                log.info("MQTT Bridge plugin {} disconnected", pluginId);
            } catch (MqttException e) {
                log.error("Error disconnecting MQTT client for plugin {}: {}", pluginId, e.getMessage());
            }
        }

        messageQueues.remove(pluginId);
    }

    /**
     * Poll for messages from the message queue
     * This is called periodically by the polling scheduler
     */
    public List<TelemetryPayload> pollMessages(Long pluginId, JsonNode config, int maxMessages, int timeoutMs) {
        List<TelemetryPayload> payloads = new ArrayList<>();

        BlockingQueue<MqttMessage> queue = messageQueues.get(pluginId);
        if (queue == null) {
            log.warn("No message queue found for plugin {}", pluginId);
            return payloads;
        }

        try {
            // Poll messages from queue with timeout
            int collected = 0;
            while (collected < maxMessages) {
                MqttMessage message = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
                if (message == null) {
                    break; // Timeout, no more messages
                }

                try {
                    TelemetryPayload payload = parseMessage(message, config);
                    payloads.add(payload);
                    collected++;
                } catch (Exception e) {
                    log.error("Failed to parse MQTT message: {}", e.getMessage());
                    // Continue with other messages
                }
            }

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.warn("Interrupted while polling messages for plugin {}", pluginId);
        }

        return payloads;
    }

    /**
     * Parse MQTT message into TelemetryPayload
     */
    private TelemetryPayload parseMessage(MqttMessage message, JsonNode config) throws PluginProcessingException {
        String payload = new String(message.getPayload());

        try {
            JsonNode payloadJson = objectMapper.readTree(payload);

            String deviceId = extractDeviceId(payloadJson, config);
            Instant timestamp = extractTimestamp(payloadJson, config);
            Map<String, BigDecimal> variables = extractVariables(payloadJson, config);

            return new TelemetryPayload(deviceId, timestamp, variables, new HashMap<>());

        } catch (Exception e) {
            throw new PluginProcessingException("Failed to parse MQTT message: " + e.getMessage(), e);
        }
    }

    /**
     * Check if plugin is connected
     */
    public boolean isConnected(Long pluginId) {
        MqttClient client = activeClients.get(pluginId);
        return client != null && client.isConnected();
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        PluginValidationResult baseResult = super.validateConfiguration(plugin);
        if (!baseResult.isValid()) {
            return baseResult;
        }

        JsonNode config = plugin.getConfiguration();

        // Validate broker URL
        if (!config.has("brokerUrl") || config.get("brokerUrl").isNull()) {
            return PluginValidationResult.invalid("Missing required field: brokerUrl");
        }

        String brokerUrl = config.get("brokerUrl").asText();
        if (!brokerUrl.startsWith("tcp://") && !brokerUrl.startsWith("ssl://")) {
            return PluginValidationResult.invalid("brokerUrl must start with tcp:// or ssl://");
        }

        // Validate topics array
        if (!config.has("topics") || !config.get("topics").isArray()) {
            return PluginValidationResult.invalid("Missing or invalid 'topics' array");
        }

        JsonNode topics = config.get("topics");
        if (topics.size() == 0) {
            return PluginValidationResult.invalid("At least one topic must be configured");
        }

        for (int i = 0; i < topics.size(); i++) {
            JsonNode topicConfig = topics.get(i);

            if (!topicConfig.has("topic")) {
                return PluginValidationResult.invalid("Topic " + i + " missing 'topic' field");
            }

            String topic = topicConfig.get("topic").asText();
            if (topic.isEmpty()) {
                return PluginValidationResult.invalid("Topic " + i + " cannot be empty");
            }
        }

        // Validate QoS
        if (config.has("qos")) {
            int qos = config.get("qos").asInt();
            if (qos < 0 || qos > 2) {
                return PluginValidationResult.invalid("QoS must be 0, 1, or 2");
            }
        }

        return PluginValidationResult.valid();
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.MQTT_BRIDGE;
    }

    /**
     * Cleanup on application shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("Disconnecting all MQTT Bridge clients...");
        for (Long pluginId : new ArrayList<>(activeClients.keySet())) {
            disconnect(pluginId);
        }
    }
}
