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

    /**
     * Returns the OS-appropriate command for this runtime.
     * On Windows, Python is invoked as "python" instead of "python3".
     */
    public String getCommand() {
        if (this == PYTHON_3_11 && isWindows()) {
            return "python";
        }
        return command;
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
