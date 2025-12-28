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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

/**
 * Handler for MQTT-triggered serverless functions.
 * Matches MQTT messages against configured triggers and executes functions.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class MqttFunctionTriggerHandler implements FunctionTriggerHandler {

    private final FunctionTriggerRepository triggerRepository;
    private final FunctionExecutionService executionService;
    private final ObjectMapper objectMapper;

    // Cache for topic patterns to avoid recompiling regexes
    private final Map<Long, Pattern> topicPatternCache = new ConcurrentHashMap<>();

    // Debounce tracking: triggerId -> last execution timestamp
    private final Map<Long, Instant> lastExecutionTime = new ConcurrentHashMap<>();

    @Override
    public String getSupportedTriggerType() {
        return FunctionTriggerType.MQTT.name();
    }

    @Override
    public void handleEvent(JsonNode event, TriggerContext context) {
        List<FunctionTrigger> enabledTriggers = getEnabledTriggers();

        for (FunctionTrigger trigger : enabledTriggers) {
            try {
                if (matchesTrigger(trigger, event, context)) {
                    // Check debounce
                    if (shouldDebounce(trigger)) {
                        log.debug("Skipping trigger {} due to debounce", trigger.getId());
                        continue;
                    }

                    // Build event payload and execute
                    JsonNode functionInput = buildEventPayload(trigger, event, context);
                    executionService.executeFunctionAsync(
                        trigger.getFunction().getId(),
                        functionInput,
                        trigger
                    );

                    // Update last execution time for debouncing
                    lastExecutionTime.put(trigger.getId(), Instant.now());

                    log.info("Executed function {} via MQTT trigger {} for topic {}",
                        trigger.getFunction().getName(),
                        trigger.getId(),
                        context.getEventSource());
                }
            } catch (Exception e) {
                log.error("Error processing MQTT trigger {}: {}",
                    trigger.getId(), e.getMessage(), e);
            }
        }
    }

    @Override
    public boolean matchesTrigger(FunctionTrigger trigger, JsonNode event, TriggerContext context) {
        JsonNode config = trigger.getTriggerConfig();

        // Match topic pattern
        String topicPattern = config.path("topicPattern").asText(null);
        if (topicPattern != null && context.getEventSource() != null) {
            Pattern pattern = getTopicPattern(trigger.getId(), topicPattern);
            if (!pattern.matcher(context.getEventSource()).matches()) {
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

            // Check device tags (if we add tag support in the future)
            // JsonNode tags = deviceFilter.path("tags");
            // if (tags.isArray()) {
            //     // Check if device has all required tags
            // }
        }

        // Match variable filter
        JsonNode variableFilter = config.path("variableFilter");
        if (!variableFilter.isMissingNode() && variableFilter.isObject()) {
            JsonNode telemetry = event.path("telemetry");
            if (telemetry.isMissingNode() || !telemetry.isObject()) {
                return false;
            }

            // Check each variable filter
            var it = variableFilter.fields();
            while (it.hasNext()) {
                var entry = it.next();
                String varName = entry.getKey();
                JsonNode conditions = entry.getValue();

                JsonNode varValue = telemetry.path(varName);
                if (varValue.isMissingNode() || !varValue.isNumber()) {
                    return false;
                }

                double value = varValue.asDouble();

                // Check gt (greater than)
                if (conditions.has("gt") && value <= conditions.get("gt").asDouble()) {
                    return false;
                }

                // Check gte (greater than or equal)
                if (conditions.has("gte") && value < conditions.get("gte").asDouble()) {
                    return false;
                }

                // Check lt (less than)
                if (conditions.has("lt") && value >= conditions.get("lt").asDouble()) {
                    return false;
                }

                // Check lte (less than or equal)
                if (conditions.has("lte") && value > conditions.get("lte").asDouble()) {
                    return false;
                }

                // Check eq (equals)
                if (conditions.has("eq") && value != conditions.get("eq").asDouble()) {
                    return false;
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
        payload.put("topic", context.getEventSource());

        // Add device information
        if (context.getDeviceId() != null) {
            payload.put("deviceId", context.getDeviceId());
        }

        // Add timestamp
        payload.put("timestamp", context.getTimestamp() != null ?
            Instant.ofEpochMilli(context.getTimestamp()).toString() :
            Instant.now().toString());

        // Add telemetry data
        if (event.has("telemetry")) {
            payload.set("telemetry", event.get("telemetry"));
        }

        // Add device metadata if available
        if (event.has("device")) {
            payload.set("device", event.get("device"));
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
        return triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.MQTT);
    }

    /**
     * Get or compile topic pattern for matching.
     * Converts MQTT wildcard syntax (+, #) to regex.
     */
    private Pattern getTopicPattern(Long triggerId, String mqttPattern) {
        return topicPatternCache.computeIfAbsent(triggerId, id -> {
            // Convert MQTT wildcard pattern to regex
            // + matches a single level
            // # matches multiple levels
            String regex = mqttPattern
                .replace("+", "[^/]+")  // + matches one level
                .replace("#", ".*");     // # matches zero or more levels
            return Pattern.compile(regex);
        });
    }

    /**
     * Check if trigger should be debounced.
     */
    private boolean shouldDebounce(FunctionTrigger trigger) {
        JsonNode config = trigger.getTriggerConfig();
        int debounceSeconds = config.path("debounceSeconds").asInt(0);

        if (debounceSeconds <= 0) {
            return false;
        }

        Instant lastExec = lastExecutionTime.get(trigger.getId());
        if (lastExec == null) {
            return false;
        }

        return Instant.now().isBefore(lastExec.plusSeconds(debounceSeconds));
    }

    /**
     * Clear the pattern cache (useful for testing or when triggers are updated).
     */
    public void clearPatternCache() {
        topicPatternCache.clear();
    }

    /**
     * Clear the debounce tracking (useful for testing).
     */
    public void clearDebounceTracking() {
        lastExecutionTime.clear();
    }
}
