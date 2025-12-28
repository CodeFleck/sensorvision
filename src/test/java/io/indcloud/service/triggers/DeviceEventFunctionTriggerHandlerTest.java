package io.indcloud.service.triggers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
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

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeviceEventFunctionTriggerHandlerTest {

    @Mock
    private FunctionTriggerRepository triggerRepository;

    @Mock
    private FunctionExecutionService executionService;

    private ObjectMapper objectMapper;
    private DeviceEventFunctionTriggerHandler handler;

    @Captor
    private ArgumentCaptor<JsonNode> eventCaptor;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        handler = new DeviceEventFunctionTriggerHandler(triggerRepository, executionService, objectMapper);
    }

    @Test
    void testGetSupportedTriggerType() {
        assertThat(handler.getSupportedTriggerType()).isEqualTo(FunctionTriggerType.DEVICE_EVENT.name());
    }

    @Test
    void testMatchesTrigger_DeviceCreatedEvent() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.created\"}");

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_WildcardEventType() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.*\"}");

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
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
                "eventType": "device.created",
                "deviceFilter": {
                    "externalIdPattern": "production-.*"
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .deviceId("production-sensor-001")
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
                "eventType": "device.created",
                "deviceFilter": {
                    "externalIdPattern": "production-.*"
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .deviceId("test-sensor-001")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void testMatchesTrigger_DeviceTagsFilter() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "eventType": "device.created",
                "deviceFilter": {
                    "tags": ["production", "critical"]
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode device = objectMapper.createObjectNode();
        ArrayNode tags = objectMapper.createArrayNode();
        tags.add("production");
        tags.add("critical");
        tags.add("monitored");
        device.set("tags", tags);
        event.set("device", device);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .deviceId("sensor-001")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_DeviceTagsFilterMissingTag() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "eventType": "device.created",
                "deviceFilter": {
                    "tags": ["production", "critical"]
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode device = objectMapper.createObjectNode();
        ArrayNode tags = objectMapper.createArrayNode();
        tags.add("production");  // Missing "critical" tag
        device.set("tags", tags);
        event.set("device", device);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .deviceId("sensor-001")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isFalse();
    }

    @Test
    void testMatchesTrigger_PropertyFilter() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "eventType": "device.updated",
                "propertyFilter": {
                    "status": null
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode changes = objectMapper.createObjectNode();
        ObjectNode statusChange = objectMapper.createObjectNode();
        statusChange.put("oldValue", "OFFLINE");
        statusChange.put("newValue", "ONLINE");
        changes.set("status", statusChange);
        event.set("changes", changes);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.updated")
            .eventSource("device-lifecycle")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testMatchesTrigger_PropertyFilterWithValue() {
        // Given
        FunctionTrigger trigger = createTrigger("""
            {
                "eventType": "device.updated",
                "propertyFilter": {
                    "status": "ONLINE"
                }
            }
            """);

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode changes = objectMapper.createObjectNode();
        ObjectNode statusChange = objectMapper.createObjectNode();
        statusChange.put("oldValue", "OFFLINE");
        statusChange.put("newValue", "ONLINE");
        changes.set("status", statusChange);
        event.set("changes", changes);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.updated")
            .eventSource("device-lifecycle")
            .build();

        // When
        boolean matches = handler.matchesTrigger(trigger, event, context);

        // Then
        assertThat(matches).isTrue();
    }

    @Test
    void testBuildEventPayload() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.created\"}");

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode device = objectMapper.createObjectNode();
        device.put("externalId", "sensor-001");
        device.put("name", "Temperature Sensor 1");
        event.set("device", device);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .deviceId("sensor-001")
            .timestamp(System.currentTimeMillis())
            .build();

        // When
        JsonNode payload = handler.buildEventPayload(trigger, event, context);

        // Then
        assertThat(payload.get("eventType").asText()).isEqualTo("device.created");
        assertThat(payload.get("deviceId").asText()).isEqualTo("sensor-001");
        assertThat(payload.has("timestamp")).isTrue();
        assertThat(payload.get("device").get("name").asText()).isEqualTo("Temperature Sensor 1");
    }

    @Test
    void testBuildEventPayload_WithChanges() {
        // Given
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.updated\"}");

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode changes = objectMapper.createObjectNode();
        ObjectNode statusChange = objectMapper.createObjectNode();
        statusChange.put("oldValue", "OFFLINE");
        statusChange.put("newValue", "ONLINE");
        changes.set("status", statusChange);
        event.set("changes", changes);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.updated")
            .eventSource("device-lifecycle")
            .timestamp(System.currentTimeMillis())
            .build();

        // When
        JsonNode payload = handler.buildEventPayload(trigger, event, context);

        // Then
        assertThat(payload.get("eventType").asText()).isEqualTo("device.updated");
        assertThat(payload.has("changes")).isTrue();
        assertThat(payload.get("changes").get("status").get("newValue").asText()).isEqualTo("ONLINE");
    }

    @Test
    void testHandleEvent_MatchingTrigger() {
        // Given
        ServerlessFunction function = createFunction(1L, "device-created-handler");
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.created\"}");
        trigger.setFunction(function);

        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.DEVICE_EVENT))
            .thenReturn(List.of(trigger));

        ObjectNode event = objectMapper.createObjectNode();
        ObjectNode device = objectMapper.createObjectNode();
        device.put("externalId", "sensor-001");
        device.put("name", "Temperature Sensor 1");
        event.set("device", device);

        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
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
        ServerlessFunction function = createFunction(1L, "device-created-handler");
        FunctionTrigger trigger = createTrigger("{\"eventType\": \"device.deleted\"}");
        trigger.setFunction(function);

        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.DEVICE_EVENT))
            .thenReturn(List.of(trigger));

        ObjectNode event = objectMapper.createObjectNode();
        TriggerContext context = TriggerContext.builder()
            .eventType("device.created")
            .eventSource("device-lifecycle")
            .build();

        // When
        handler.handleEvent(event, context);

        // Then
        verify(executionService, never()).executeFunctionAsync(any(), any(), any());
    }

    @Test
    void testGetEnabledTriggers() {
        // Given
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.DEVICE_EVENT))
            .thenReturn(Collections.emptyList());

        // When
        List<FunctionTrigger> triggers = handler.getEnabledTriggers();

        // Then
        assertThat(triggers).isEmpty();
        verify(triggerRepository, times(1)).findByTriggerTypeAndEnabledTrue(FunctionTriggerType.DEVICE_EVENT);
    }

    // Helper methods

    private FunctionTrigger createTrigger(String configJson) {
        try {
            JsonNode config = objectMapper.readTree(configJson);
            FunctionTrigger trigger = new FunctionTrigger();
            trigger.setId(1L);
            trigger.setTriggerType(FunctionTriggerType.DEVICE_EVENT);
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
