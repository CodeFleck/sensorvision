package io.indcloud.service.triggers;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Context information for trigger execution.
 * Provides additional metadata about the event that triggered the function.
 */
@Data
@Builder
public class TriggerContext {

    /**
     * The type of event that triggered the function (e.g., "mqtt.message", "http.request")
     */
    private String eventType;

    /**
     * The source of the event (e.g., MQTT topic, HTTP endpoint)
     */
    private String eventSource;

    /**
     * Timestamp when the event occurred
     */
    private Long timestamp;

    /**
     * Device ID if the event is device-related
     */
    private String deviceId;

    /**
     * Additional context data
     */
    @Builder.Default
    private Map<String, Object> metadata = new HashMap<>();

    /**
     * Add metadata to the context.
     */
    public void addMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    /**
     * Get metadata value.
     */
    public Object getMetadata(String key) {
        return this.metadata.get(key);
    }
}
