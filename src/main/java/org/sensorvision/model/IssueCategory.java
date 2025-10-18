package org.sensorvision.model;

public enum IssueCategory {
    BUG("Bug Report"),
    FEATURE_REQUEST("Feature Request"),
    QUESTION("Question/Help"),
    OTHER("Other");

    private final String displayName;

    IssueCategory(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
