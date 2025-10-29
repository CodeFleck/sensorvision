package org.sensorvision.model;

/**
 * Supported runtime environments for serverless functions.
 */
public enum FunctionRuntime {
    PYTHON_3_11("Python 3.11+", "python3", ".py"),
    NODEJS_18("Node.js 18+", "node", ".js");

    private final String displayName;
    private final String command;
    private final String fileExtension;

    FunctionRuntime(String displayName, String command, String fileExtension) {
        this.displayName = displayName;
        this.command = command;
        this.fileExtension = fileExtension;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getCommand() {
        return command;
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
