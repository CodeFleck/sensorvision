package org.sensorvision.model;

public enum IssueSeverity {
    LOW("Low - Minor issue"),
    MEDIUM("Medium - Noticeable issue"),
    HIGH("High - Significant issue"),
    CRITICAL("Critical - System unusable");

    private final String displayName;

    IssueSeverity(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
