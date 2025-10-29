package org.sensorvision.model;

public enum ArchiveExecutionStatus {
    RUNNING("Running"),
    SUCCESS("Success"),
    FAILED("Failed");

    private final String displayName;

    ArchiveExecutionStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
