package org.sensorvision.model;

public enum AlertSeverity {
    LOW("Low"),
    MEDIUM("Medium"),
    HIGH("High"),
    CRITICAL("Critical");

    private final String description;

    AlertSeverity(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}