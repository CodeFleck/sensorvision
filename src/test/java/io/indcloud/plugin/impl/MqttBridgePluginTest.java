package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.plugin.PluginValidationResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MqttBridgePluginTest {

    private MqttBridgePlugin plugin;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        plugin = new MqttBridgePlugin();
        objectMapper = new ObjectMapper();
    }

    @Test
    void testGetSupportedProvider() {
        assertEquals(PluginProvider.MQTT_BRIDGE, plugin.getSupportedProvider());
    }

    @Test
    void testValidateConfiguration_ValidConfig() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "clientId": "bridge-client",
                "username": "user",
                "password": "pass",
                "qos": 1,
                "topics": [
                    {
                        "topic": "devices/+/telemetry",
                        "deviceIdExtractor": "topic",
                        "topicPattern": "devices/{deviceId}/telemetry"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_MissingBrokerUrl() {
        String configJson = """
            {
                "topics": [
                    {
                        "topic": "test/topic"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("brokerUrl"));
    }

    @Test
    void testValidateConfiguration_InvalidBrokerUrl() {
        String configJson = """
            {
                "brokerUrl": "http://wrong-protocol.com",
                "topics": [
                    {
                        "topic": "test/topic"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("tcp://"));
    }

    @Test
    void testValidateConfiguration_MissingTopics() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883"
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("topics"));
    }

    @Test
    void testValidateConfiguration_EmptyTopicsArray() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": []
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("At least one topic"));
    }

    @Test
    void testValidateConfiguration_InvalidQoS() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "qos": 5,
                "topics": [
                    {
                        "topic": "test/topic"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("QoS must be 0, 1, or 2"));
    }

    @Test
    void testValidateConfiguration_MissingTopicField() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": [
                    {
                        "deviceIdExtractor": "topic"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("missing 'topic' field"));
    }

    @Test
    void testValidateConfiguration_EmptyTopic() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": [
                    {
                        "topic": ""
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertFalse(result.isValid());
        assertTrue(result.getErrors().get(0).contains("cannot be empty"));
    }

    @Test
    void testValidateConfiguration_SslBrokerUrl() {
        String configJson = """
            {
                "brokerUrl": "ssl://secure-mqtt.example.com:8883",
                "topics": [
                    {
                        "topic": "secure/topic"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testProcessWebhookMessage() throws Exception {
        // Test processing a standard JSON message
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "deviceIdField": "deviceId",
                "timestampField": "timestamp",
                "variablesField": "variables",
                "topics": [
                    {
                        "topic": "devices/+/telemetry"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        dataPlugin.setConfiguration(objectMapper.readTree(configJson));

        String mqttPayload = """
            {
                "deviceId": "sensor-001",
                "timestamp": "2024-01-01T12:00:00Z",
                "variables": {
                    "temperature": 23.5,
                    "humidity": 65.0
                }
            }
            """;

        // Process as webhook message
        List<TelemetryPayload> results = plugin.process(dataPlugin, mqttPayload);

        assertNotNull(results);
        assertEquals(1, results.size());

        TelemetryPayload payload = results.get(0);
        assertEquals("sensor-001", payload.deviceId());
        assertEquals(2, payload.variables().size());
        assertTrue(payload.variables().containsKey("temperature"));
        assertTrue(payload.variables().containsKey("humidity"));
    }

    @Test
    void testIsConnected_NoClient() {
        // Test with non-existent plugin ID
        assertFalse(plugin.isConnected(999L));
    }

    @Test
    void testPollMessages_NoQueue() {
        // Test polling from non-existent queue
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": [{"topic": "test"}]
            }
            """;

        try {
            List<TelemetryPayload> results = plugin.pollMessages(999L, objectMapper.readTree(configJson), 10, 100);
            assertNotNull(results);
            assertTrue(results.isEmpty());
        } catch (Exception e) {
            fail("Should not throw exception for non-existent queue");
        }
    }

    @Test
    void testValidateConfiguration_MultipleTopics() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": [
                    {
                        "topic": "warehouse/+/temperature"
                    },
                    {
                        "topic": "warehouse/+/humidity"
                    },
                    {
                        "topic": "warehouse/+/pressure"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }

    @Test
    void testValidateConfiguration_WildcardTopics() {
        String configJson = """
            {
                "brokerUrl": "tcp://mqtt.example.com:1883",
                "topics": [
                    {
                        "topic": "devices/+/telemetry"
                    },
                    {
                        "topic": "sensors/#"
                    }
                ]
            }
            """;

        DataPlugin dataPlugin = new DataPlugin();
        dataPlugin.setProvider(PluginProvider.MQTT_BRIDGE);
        try {
            dataPlugin.setConfiguration(objectMapper.readTree(configJson));
        } catch (Exception e) {
            fail("Failed to parse config JSON");
        }

        PluginValidationResult result = plugin.validateConfiguration(dataPlugin);
        assertTrue(result.isValid());
    }
}
