package io.indcloud.model;

import java.io.File;
import java.util.Arrays;
import java.util.List;

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
     * Attempts to find the full path to the executable if not in PATH.
     */
    public String getCommand() {
        if (this == PYTHON_3_11) {
            return findPythonCommand();
        }
        return command;
    }

    /**
     * Find Python executable, trying multiple common locations on Windows.
     */
    private static String findPythonCommand() {
        if (isWindows()) {
            // Try common Windows Python locations
            List<String> possiblePaths = Arrays.asList(
                "python",  // In PATH
                "python3", // In PATH (less common on Windows)
                System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python311\\python.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python312\\python.exe",
                System.getenv("LOCALAPPDATA") + "\\Programs\\Python\\Python310\\python.exe",
                System.getenv("LOCALAPPDATA") + "\\Microsoft\\WindowsApps\\python.exe",
                "C:\\Python311\\python.exe",
                "C:\\Python312\\python.exe",
                "C:\\Python310\\python.exe",
                "C:\\Program Files\\Python311\\python.exe",
                "C:\\Program Files\\Python312\\python.exe",
                "C:\\Program Files\\Python310\\python.exe"
            );

            for (String path : possiblePaths) {
                if (path != null && !path.equals("python") && !path.equals("python3")) {
                    File file = new File(path);
                    if (file.exists() && file.canExecute()) {
                        return path;
                    }
                }
            }
            // Fallback to "python" in PATH
            return "python";
        }
        return "python3";
    }

    private static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }

    public String getFileExtension() {
        return fileExtension;
    }
}
