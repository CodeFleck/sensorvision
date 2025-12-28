package io.indcloud.service.triggers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.service.FunctionExecutionService;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Handler for device event-triggered serverless functions.
 * Matches device lifecycle events against configured triggers and executes functions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventFunctionTriggerHandler implements FunctionTriggerHandler {

    private final FunctionTriggerRepository triggerRepository;
    private final FunctionExecutionService executionService;
    private final ObjectMapper objectMapper;

    @Override
    public String getSupportedTriggerType() {
        return FunctionTriggerType.DEVICE_EVENT.name();
    }

    @Override
    public void handleEvent(JsonNode event, TriggerContext context) {
        List<FunctionTrigger> enabledTriggers = getEnabledTriggers();

        for (FunctionTrigger trigger : enabledTriggers) {
            try {
                if (matchesTrigger(trigger, event, context)) {
                    // Build event payload and execute
                    JsonNode functionInput = buildEventPayload(trigger, event, context);
                    executionService.executeFunctionAsync(
                        trigger.getFunction().getId(),
                        functionInput,
                        trigger
                    );

                    log.info("Executed function {} via device event trigger {} for event type {}",
                        trigger.getFunction().getName(),
                        trigger.getId(),
                        context.getEventType());
                }
            } catch (Exception e) {
                log.error("Error processing device event trigger {}: {}",
                    trigger.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean matchesTrigger(FunctionTrigger trigger, JsonNode event, TriggerContext context) {
        JsonNode config = trigger.getTriggerConfig();

        // Match event type (e.g., "device.created", "device.updated", "device.deleted")
        String eventTypePattern = config.path("eventType").asText(null);
        if (eventTypePattern != null && context.getEventType() != null) {
            // Support wildcards: "device.*" matches all device events
            String regex = eventTypePattern.replace("*", ".*");
            Pattern pattern = Pattern.compile(regex);
            if (!pattern.matcher(context.getEventType()).matches()) {
                return false;
            }
        }

        // Match device filter
        JsonNode deviceFilter = config.path("deviceFilter");
        if (!deviceFilter.isMissingNode() && context.getDeviceId() != null) {
            // Check device ID pattern
            String deviceIdPattern = deviceFilter.path("externalIdPattern").asText(null);
            if (deviceIdPattern != null) {
                Pattern pattern = Pattern.compile(deviceIdPattern);
                if (!pattern.matcher(context.getDeviceId()).matches()) {
                    return false;
                }
            }

            // Check device tags (if provided in event)
            JsonNode requiredTags = deviceFilter.path("tags");
            if (requiredTags.isArray() && requiredTags.size() > 0) {
                JsonNode deviceTags = event.path("device").path("tags");
                if (!deviceTags.isArray()) {
                    return false;
                }

                // Check if device has all required tags
                for (JsonNode requiredTag : requiredTags) {
                    boolean hasTag = false;
                    for (JsonNode deviceTag : deviceTags) {
                        if (deviceTag.asText().equals(requiredTag.asText())) {
                            hasTag = true;
                            break;
                        }
                    }
                    if (!hasTag) {
                        return false;
                    }
                }
            }
        }

        // Match specific event properties (for updated events)
        JsonNode propertyFilter = config.path("propertyFilter");
        if (!propertyFilter.isMissingNode() && propertyFilter.isObject()) {
            JsonNode changes = event.path("changes");
            if (changes.isMissingNode()) {
                return false;
            }

            // Check if specific properties changed
            var it = propertyFilter.fields();
            while (it.hasNext()) {
                var entry = it.next();
                String propertyName = entry.getKey();
                JsonNode expectedValue = entry.getValue();

                JsonNode actualChange = changes.path(propertyName);
                if (actualChange.isMissingNode()) {
                    return false;
                }

                // If expectedValue is provided, check if new value matches
                if (!expectedValue.isNull()) {
                    JsonNode newValue = actualChange.path("newValue");
                    if (!newValue.equals(expectedValue)) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    @Override
    public JsonNode buildEventPayload(FunctionTrigger trigger, JsonNode event, TriggerContext context) {
        ObjectNode payload = objectMapper.createObjectNode();

        // Add event type and source
        payload.put("eventType", context.getEventType());

        // Add device information
        if (context.getDeviceId() != null) {
            payload.put("deviceId", context.getDeviceId());
        }

        // Add timestamp
        payload.put("timestamp", context.getTimestamp() != null ?
            Instant.ofEpochMilli(context.getTimestamp()).toString() :
            Instant.now().toString());

        // Add device data if available
        if (event.has("device")) {
            payload.set("device", event.get("device"));
        }

        // Add changes for update events
        if (event.has("changes")) {
            payload.set("changes", event.get("changes"));
        }

        // Add previous state for update/delete events
        if (event.has("previousState")) {
            payload.set("previousState", event.get("previousState"));
        }

        // Add any additional metadata from context
        if (!context.getMetadata().isEmpty()) {
            ObjectNode metadata = objectMapper.valueToTree(context.getMetadata());
            payload.set("metadata", metadata);
        }

        return payload;
    }

    @Override
    public List<FunctionTrigger> getEnabledTriggers() {
        return triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.DEVICE_EVENT);
    }
}
