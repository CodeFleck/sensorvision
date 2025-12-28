package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.plugin.DataPluginProcessor;
import io.indcloud.plugin.NotificationPlugin;
import io.indcloud.plugin.PluginProcessingException;
import io.indcloud.plugin.PluginValidationResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Slack notification plugin - sends alerts to Slack channels via webhooks
 *
 * Configuration schema:
 * {
 *   "webhookUrl": "https://hooks.slack.com/services/YOUR/WEBHOOK/URL",
 *   "channel": "#alerts" (optional),
 *   "username": "SensorVision" (optional),
 *   "iconEmoji": ":robot_face:" (optional)
 * }
 */
@Component
public class SlackNotificationPlugin implements NotificationPlugin, DataPluginProcessor {

    private static final Logger logger = LoggerFactory.getLogger(SlackNotificationPlugin.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public SlackNotificationPlugin() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void sendNotification(JsonNode config, String message, String title,
                                String severity, JsonNode metadata) throws PluginProcessingException {
        try {
            String webhookUrl = config.get("webhookUrl").asText();

            // Build Slack message
            ObjectNode slackMessage = objectMapper.createObjectNode();

            // Add optional channel
            if (config.has("channel")) {
                slackMessage.put("channel", config.get("channel").asText());
            }

            // Add optional username
            if (config.has("username")) {
                slackMessage.put("username", config.get("username").asText());
            } else {
                slackMessage.put("username", "SensorVision");
            }

            // Add optional icon
            if (config.has("iconEmoji")) {
                slackMessage.put("icon_emoji", config.get("iconEmoji").asText());
            }

            // Build rich attachment
            ObjectNode attachment = objectMapper.createObjectNode();
            attachment.put("title", title);
            attachment.put("text", message);
            attachment.put("color", getSeverityColor(severity));

            // Add timestamp
            attachment.put("ts", System.currentTimeMillis() / 1000);

            // Add metadata as fields
            if (metadata != null && !metadata.isEmpty()) {
                var fields = objectMapper.createArrayNode();
                metadata.fieldNames().forEachRemaining(fieldName -> {
                    ObjectNode field = objectMapper.createObjectNode();
                    field.put("title", fieldName);
                    field.put("value", metadata.get(fieldName).asText());
                    field.put("short", true);
                    fields.add(field);
                });
                attachment.set("fields", fields);
            }

            var attachments = objectMapper.createArrayNode();
            attachments.add(attachment);
            slackMessage.set("attachments", attachments);

            // Send to Slack
            String jsonBody = objectMapper.writeValueAsString(slackMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new PluginProcessingException(
                        "Slack API error: " + response.statusCode() + " - " + response.body());
            }

            logger.info("Slack notification sent successfully: {}", title);

        } catch (Exception e) {
            logger.error("Failed to send Slack notification", e);
            throw new PluginProcessingException("Failed to send Slack notification: " + e.getMessage(), e);
        }
    }

    @Override
    public PluginValidationResult validateConfig(JsonNode config) {
        if (!config.has("webhookUrl") || config.get("webhookUrl").isNull()) {
            return PluginValidationResult.invalid("Missing required field: webhookUrl");
        }

        String webhookUrl = config.get("webhookUrl").asText();
        if (!webhookUrl.startsWith("https://hooks.slack.com/")) {
            return PluginValidationResult.invalid("Invalid Slack webhook URL");
        }

        return PluginValidationResult.valid();
    }

    @Override
    public List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException {
        throw new UnsupportedOperationException("Slack plugin is for notifications only");
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        return validateConfig(plugin.getConfiguration());
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.SLACK;
    }

    /**
     * Get Slack color based on severity
     */
    private String getSeverityColor(String severity) {
        if (severity == null) {
            return "#36a64f"; // green (good)
        }

        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return "#ff0000"; // red (danger)
            case "WARNING":
                return "#ff9900"; // orange (warning)
            case "INFO":
            default:
                return "#36a64f"; // green (good)
        }
    }
}
