package io.indcloud.model;

/**
 * Types of triggers that can invoke serverless functions.
 */
public enum FunctionTriggerType {
    HTTP("HTTP POST Request", "Triggered by HTTP POST to custom endpoint"),
    MQTT("MQTT Topic", "Triggered when message published to MQTT topic"),
    SCHEDULED("Scheduled (Cron)", "Triggered on schedule using cron expression"),
    DEVICE_EVENT("Device Event", "Triggered by device telemetry or status change");

    private final String displayName;
    private final String description;

    FunctionTriggerType(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
