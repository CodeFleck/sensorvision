package org.sensorvision.model;

/**
 * Execution status for serverless function runs.
 */
public enum FunctionExecutionStatus {
    RUNNING("Currently executing"),
    SUCCESS("Completed successfully"),
    ERROR("Failed with error"),
    TIMEOUT("Exceeded time limit"),
    CANCELLED("Manually cancelled");

    private final String description;

    FunctionExecutionStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
