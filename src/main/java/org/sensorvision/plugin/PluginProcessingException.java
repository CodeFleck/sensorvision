package org.sensorvision.plugin;

/**
 * Exception thrown when plugin processing fails
 */
public class PluginProcessingException extends Exception {

    public PluginProcessingException(String message) {
        super(message);
    }

    public PluginProcessingException(String message, Throwable cause) {
        super(message, cause);
    }
}
