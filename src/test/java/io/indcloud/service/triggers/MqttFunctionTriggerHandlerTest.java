package io.indcloud.service.triggers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import io.indcloud.model.ServerlessFunction;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.service.FunctionExecutionService;

import java.time.Instant;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MqttFunctionTriggerHandlerTest {

    @Mock
    private FunctionTriggerRepository triggerRepository;

    @Mock
    private FunctionExecutionService executionService;

    private ObjectMapper objectMapper;
    private MqttFunctionTriggerHandler handler;

    @Captor
    private ArgumentCaptor<JsonNode> eventCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new MqttFunctionTriggerHandler(triggerRepository, executionService, objectMapper);
        handler.clearPatternCache();
        handler.clearDebounceTracking();
    }

    @Test
    void testGetSupportedTriggerType() {
        assertThat(handler.getSupportedTriggerType()).isEqualTo(FunctionTriggerType.MQTT.name());
    }

    @Test
    void testMatchesTrigger_SimpleTopicPattern() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"topicPattern\": \"indcloud/devices/+/telemetry\"}");

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_WildcardTopicPattern() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"topicPattern\": \"indcloud/#\"}");

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry/voltage")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_DeviceIdPattern() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "deviceFilter": {
                    "externalIdPattern": "sensor-.*"
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .deviceId("sensor-001")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_DeviceIdPatternNoMatch() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "deviceFilter": {
                    "externalIdPattern": "production-.*"
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .deviceId("sensor-001")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void testMatchesTrigger_VariableFilterGreaterThan() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "variableFilter": {
                    "temperature": { "gt": 50 }
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode telemetry = objectMapper.createObjectNode();
        telemetry.put("temperature", 75.5);
        event.set("telemetry", telemetry);

        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_VariableFilterLessThan() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "variableFilter": {
                    "voltage": { "lt": 200 }
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode telemetry = objectMapper.createObjectNode();
        telemetry.put("voltage", 180.0);
        event.set("telemetry", telemetry);

        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_VariableFilterNoMatch() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "variableFilter": {
                    "temperature": { "gt": 100 }
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode telemetry = objectMapper.createObjectNode();
        telemetry.put("temperature", 50.0);
        event.set("telemetry", telemetry);

        TriggerContext context = TriggerContext.builder()
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void testBuildEventPayload() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"topicPattern\": \"indcloud/#\"}");

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode telemetry = objectMapper.createObjectNode();
        telemetry.put("temperature", 75.5);
        telemetry.put("humidity", 60.0);
        event.set("telemetry", telemetry);

        ObjectNode device = objectMapper.createObjectNode();
        device.put("externalId", "sensor-001");
        device.put("name", "Temperature Sensor 1");
        event.set("device", device);

        TriggerContext context = TriggerContext.builder()
            .eventType("mqtt.message")
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .deviceId("sensor-001")
            .timestamp(System.currentTimeMillis())
            .build();

        // When
        JsonNode payload = handler.buildEventPayload(trigger, event, context);

        // Then
        assertThat(payload.get("eventType").asText()).isEqualTo("mqtt.message");
        assertThat(payload.get("topic").asText()).isEqualTo("indcloud/devices/sensor-001/telemetry");
        assertThat(payload.get("deviceId").asText()).isEqualTo("sensor-001");
        assertThat(payload.has("timestamp")).isTrue();
        assertThat(payload.get("telemetry").get("temperature").asDouble()).isEqualTo(75.5);
        assertThat(payload.get("device").get("name").asText()).isEqualTo("Temperature Sensor 1");
    }

    @Test
    void testHandleEvent_MatchingTrigger() {
        // Given
        ServerlessFunction function = createFunction(1L, "test-function");
        FunctionTrigger trigger = createTrigger("{\"topicPattern\": \"indcloud/devices/+/telemetry\"}");
        trigger.setFunction(function);

        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT))
            .thenReturn(List.of(trigger));

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode telemetry = objectMapper.createObjectNode();
        telemetry.put("temperature", 75.5);
        event.set("telemetry", telemetry);

        TriggerContext context = TriggerContext.builder()
            .eventType("mqtt.message")
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .deviceId("sensor-001")
            .timestamp(System.currentTimeMillis())
            .build();

        // When
        handler.handleEvent(event, context);

        // Then
        verify(executionService, times(1)).executeFunctionAsync(
            eq(1L),
            any(JsonNode.class),
            eq(trigger)
        );
    }

    @Test
    void testHandleEvent_NoMatchingTrigger() {
        // Given
        ServerlessFunction function = createFunction(1L, "test-function");
        FunctionTrigger trigger = createTrigger("{\"topicPattern\": \"other/topic/pattern\"}");
        trigger.setFunction(function);

        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT))
            .thenReturn(List.of(trigger));

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("mqtt.message")
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .build();

        // When
        handler.handleEvent(event, context);

        // Then
        verify(executionService, never()).executeFunctionAsync(any(), any(), any());
    }

    @Test
    void testHandleEvent_WithDebounce() {
        // Given
        ServerlessFunction function = createFunction(1L, "test-function");
        FunctionTrigger trigger = createTrigger("""
            {
                "topicPattern": "indcloud/devices/+/telemetry",
                "debounceSeconds": 60
            }
            """);
        trigger.setFunction(function);

        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT))
            .thenReturn(List.of(trigger));

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("mqtt.message")
            .eventSource("indcloud/devices/sensor-001/telemetry")
            .timestamp(System.currentTimeMillis())
            .build();

        // When - First execution should work
        handler.handleEvent(event, context);
        verify(executionService, times(1)).executeFunctionAsync(any(), any(), any());

        // When - Second execution within debounce period should be skipped
        handler.handleEvent(event, context);
        verify(executionService, times(1)).executeFunctionAsync(any(), any(), any()); // Still 1

        // Clear debounce and try again
        handler.clearDebounceTracking();
        handler.handleEvent(event, context);
        verify(executionService, times(2)).executeFunctionAsync(any(), any(), any());
    }

    @Test
    void testGetEnabledTriggers() {
        // Given
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT))
            .thenReturn(Collections.emptyList());

        // When
        List<FunctionTrigger> triggers = handler.getEnabledTriggers();

        // Then
        assertThat(triggers).isEmpty();
        verify(triggerRepository, times(1)).findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT);
    }

    // Helper methods

    private FunctionTrigger createTrigger(String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);
            FunctionTrigger trigger = new FunctionTrigger();
            trigger.setId(1L);
            trigger.setTriggerType(FunctionTriggerType.MQTT);
            trigger.setTriggerConfig(config);
            trigger.setEnabled(true);
            return trigger;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private ServerlessFunction createFunction(Long id, String name) {
        ServerlessFunction function = new ServerlessFunction();
        function.setId(id);
        function.setName(name);
        function.setEnabled(true);
        return function;
    }
}
