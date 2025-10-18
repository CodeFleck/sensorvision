package org.sensorvision.model;

public enum IssueStatus {
    SUBMITTED("Submitted - Awaiting review"),
    IN_REVIEW("In Review - Being investigated"),
    RESOLVED("Resolved - Issue fixed"),
    CLOSED("Closed - No further action");

    private final String displayName;

    IssueStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
