package io.indcloud.plugin;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Interface for notification plugins that send alerts to external channels
 */
public interface NotificationPlugin {

    /**
     * Send a notification to the external channel
     *
     * @param config Plugin configuration (webhook URLs, API keys, etc.)
     * @param message Notification message
     * @param title Notification title
     * @param severity Severity level (INFO, WARNING, CRITICAL)
     * @param metadata Additional metadata
     * @throws PluginProcessingException if notification fails
     */
    void sendNotification(JsonNode config, String message, String title,
                         String severity, JsonNode metadata) throws PluginProcessingException;

    /**
     * Validate notification plugin configuration
     *
     * @param config Plugin configuration
     * @return Validation result
     */
    PluginValidationResult validateConfig(JsonNode config);
}
