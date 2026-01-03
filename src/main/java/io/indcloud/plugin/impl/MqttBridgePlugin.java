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
 * Wrapper to preserve topic information with MQTT message
 */
record MqttMessageWithTopic(String topic, byte[] payload, int qos, boolean retained) {}

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

    // Store message queues per plugin (plugin ID -> message queue with topic)
    private final Map<Long, BlockingQueue<MqttMessageWithTopic>> messageQueues = new ConcurrentHashMap<>();

    // Store topic configurations for re-subscription on reconnect
    private final Map<Long, List<String>> pluginTopics = new ConcurrentHashMap<>();

    // Default max queue size to prevent memory leaks
    private static final int DEFAULT_MAX_QUEUE_SIZE = 10000;

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

            // Create bounded message queue for this plugin (prevents memory leak)
            int maxQueueSize = config.has("maxQueueSize")
                ? config.get("maxQueueSize").asInt(DEFAULT_MAX_QUEUE_SIZE)
                : DEFAULT_MAX_QUEUE_SIZE;
            BlockingQueue<MqttMessageWithTopic> messageQueue = new LinkedBlockingQueue<>(maxQueueSize);
            messageQueues.put(pluginId, messageQueue);

            // Subscribe to configured topics and store for reconnection
            int qos = config.has("qos") ? config.get("qos").asInt(1) : 1;
            JsonNode topicsConfig = config.get("topics");
            List<String> topicList = new ArrayList<>();
            for (JsonNode topicConfig : topicsConfig) {
                topicList.add(topicConfig.get("topic").asText());
            }
            pluginTopics.put(pluginId, topicList);

            // Set callback for incoming messages with reconnection support
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    if (reconnect) {
                        log.info("MQTT Bridge plugin {} reconnected to {}", plugin.getName(), serverURI);
                        // Re-subscribe to topics after reconnection
                        try {
                            for (String topic : topicList) {
                                client.subscribe(topic, qos);
                                log.info("MQTT Bridge plugin {} re-subscribed to topic: {}", plugin.getName(), topic);
                            }
                        } catch (MqttException e) {
                            log.error("Failed to re-subscribe after reconnection: {}", e.getMessage());
                        }
                    }
                }

                @Override
                public void connectionLost(Throwable cause) {
                    log.error("MQTT Bridge plugin {} lost connection: {}",
                        plugin.getName(), cause.getMessage());
                }

                @Override
                public void messageArrived(String topic, MqttMessage message) {
                    log.debug("MQTT Bridge plugin {} received message on topic: {}", plugin.getName(), topic);

                    // Preserve topic information with the message
                    MqttMessageWithTopic enrichedMessage = new MqttMessageWithTopic(
                        topic, message.getPayload(), message.getQos(), message.isRetained());

                    // Add to queue for processing (bounded queue prevents memory leak)
                    if (!messageQueue.offer(enrichedMessage)) {
                        log.warn("Message queue full for plugin {} (max: {}), dropping message",
                            plugin.getName(), maxQueueSize);
                    }
                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken token) {
                    // Not used for subscriber
                }
            });

            // Connect to broker (mask credentials in log)
            String maskedUrl = maskCredentialsInUrl(brokerUrl);
            log.info("MQTT Bridge plugin {} connecting to {}", plugin.getName(), maskedUrl);
            client.connect(connOpts);

            // Subscribe to configured topics
            for (String topic : topicList) {
                client.subscribe(topic, qos);
                log.info("MQTT Bridge plugin {} subscribed to topic: {}", plugin.getName(), topic);
            }

            // Store client for later use
            activeClients.put(pluginId, client);

            log.info("MQTT Bridge plugin {} successfully connected to {}", plugin.getName(), maskedUrl);

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
        pluginTopics.remove(pluginId);
    }

    /**
     * Mask credentials in broker URL for safe logging
     */
    private String maskCredentialsInUrl(String url) {
        // Mask user:pass in URLs like tcp://user:pass@host:port
        return url.replaceAll("://([^:]+):([^@]+)@", "://$1:***@");
    }

    /**
     * Poll for messages from the message queue
     * This is called periodically by the polling scheduler
     */
    public List<TelemetryPayload> pollMessages(Long pluginId, JsonNode config, int maxMessages, int timeoutMs) {
        List<TelemetryPayload> payloads = new ArrayList<>();

        BlockingQueue<MqttMessageWithTopic> queue = messageQueues.get(pluginId);
        if (queue == null) {
            log.warn("No message queue found for plugin {}", pluginId);
            return payloads;
        }

        try {
            // Poll messages from queue with timeout
            int collected = 0;
            while (collected < maxMessages) {
                MqttMessageWithTopic message = queue.poll(timeoutMs, TimeUnit.MILLISECONDS);
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
    private TelemetryPayload parseMessage(MqttMessageWithTopic message, JsonNode config) throws PluginProcessingException {
        String payloadStr = new String(message.payload());
        String topic = message.topic();

        try {
            JsonNode payloadJson = objectMapper.readTree(payloadStr);

            // Try to extract device ID from topic pattern first
            String deviceId = extractDeviceIdFromTopic(topic, config);
            if (deviceId == null) {
                // Fall back to extracting from payload
                deviceId = extractDeviceId(payloadJson, config);
            }

            Instant timestamp = extractTimestamp(payloadJson, config);
            Map<String, BigDecimal> variables = extractVariables(payloadJson, config);

            return new TelemetryPayload(deviceId, timestamp, variables, new HashMap<>());

        } catch (Exception e) {
            throw new PluginProcessingException("Failed to parse MQTT message: " + e.getMessage(), e);
        }
    }

    /**
     * Extract device ID from topic using configured pattern
     * e.g., topic "devices/sensor-001/telemetry" with pattern "devices/{deviceId}/telemetry"
     */
    private String extractDeviceIdFromTopic(String topic, JsonNode config) {
        if (!config.has("topics") || !config.get("topics").isArray()) {
            return null;
        }

        for (JsonNode topicConfig : config.get("topics")) {
            if (!topicConfig.has("topicPattern")) {
                continue;
            }

            String pattern = topicConfig.get("topicPattern").asText();
            String subscribeTopic = topicConfig.get("topic").asText();

            // Check if this topic matches the subscribe pattern (with wildcards)
            if (!topicMatchesPattern(topic, subscribeTopic)) {
                continue;
            }

            // Extract device ID using the pattern
            String deviceId = extractFromPattern(topic, pattern, "{deviceId}");
            if (deviceId != null) {
                return deviceId;
            }
        }

        return null;
    }

    /**
     * Check if topic matches MQTT wildcard pattern
     */
    private boolean topicMatchesPattern(String topic, String pattern) {
        String regex = pattern
            .replace("+", "[^/]+")
            .replace("#", ".*");
        return topic.matches(regex);
    }

    /**
     * Extract value from topic using pattern placeholder
     */
    private String extractFromPattern(String topic, String pattern, String placeholder) {
        int placeholderIndex = pattern.indexOf(placeholder);
        if (placeholderIndex == -1) {
            return null;
        }

        String prefix = pattern.substring(0, placeholderIndex);
        String suffix = pattern.substring(placeholderIndex + placeholder.length());

        if (!topic.startsWith(prefix)) {
            return null;
        }

        String remaining = topic.substring(prefix.length());
        if (!suffix.isEmpty()) {
            int suffixIndex = remaining.indexOf(suffix.charAt(0) == '/' ? "/" : suffix);
            if (suffixIndex == -1) {
                return null;
            }
            return remaining.substring(0, suffixIndex);
        }

        return remaining;
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
