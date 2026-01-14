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
 * Discord notification plugin - sends alerts to Discord channels via webhooks
 *
 * Configuration schema:
 * {
 *   "webhookUrl": "https://discord.com/api/webhooks/YOUR/WEBHOOK/URL",
 *   "username": "Industrial Cloud" (optional),
 *   "avatarUrl": "https://..." (optional)
 * }
 */
@Component
public class DiscordNotificationPlugin implements NotificationPlugin, DataPluginProcessor {

    private static final Logger logger = LoggerFactory.getLogger(DiscordNotificationPlugin.class);
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient;

    public DiscordNotificationPlugin() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public void sendNotification(JsonNode config, String message, String title,
                                String severity, JsonNode metadata) throws PluginProcessingException {
        try {
            String webhookUrl = config.get("webhookUrl").asText();

            // Build Discord message
            ObjectNode discordMessage = objectMapper.createObjectNode();

            // Add optional username
            if (config.has("username")) {
                discordMessage.put("username", config.get("username").asText());
            } else {
                discordMessage.put("username", "Industrial Cloud");
            }

            // Add optional avatar
            if (config.has("avatarUrl")) {
                discordMessage.put("avatar_url", config.get("avatarUrl").asText());
            }

            // Build rich embed
            ObjectNode embed = objectMapper.createObjectNode();
            embed.put("title", title);
            embed.put("description", message);
            embed.put("color", getSeverityColor(severity));

            // Add timestamp (ISO 8601 format)
            embed.put("timestamp", java.time.Instant.now().toString());

            // Add metadata as fields
            if (metadata != null && !metadata.isEmpty()) {
                var fields = objectMapper.createArrayNode();
                metadata.fieldNames().forEachRemaining(fieldName -> {
                    ObjectNode field = objectMapper.createObjectNode();
                    field.put("name", fieldName);
                    field.put("value", metadata.get(fieldName).asText());
                    field.put("inline", true);
                    fields.add(field);
                });
                embed.set("fields", fields);
            }

            // Add footer
            ObjectNode footer = objectMapper.createObjectNode();
            footer.put("text", "Industrial Cloud Alert System");
            embed.set("footer", footer);

            var embeds = objectMapper.createArrayNode();
            embeds.add(embed);
            discordMessage.set("embeds", embeds);

            // Send to Discord
            String jsonBody = objectMapper.writeValueAsString(discordMessage);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(webhookUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(10))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 204 && response.statusCode() != 200) {
                throw new PluginProcessingException(
                        "Discord API error: " + response.statusCode() + " - " + response.body());
            }

            logger.info("Discord notification sent successfully: {}", title);

        } catch (Exception e) {
            logger.error("Failed to send Discord notification", e);
            throw new PluginProcessingException("Failed to send Discord notification: " + e.getMessage(), e);
        }
    }

    @Override
    public PluginValidationResult validateConfig(JsonNode config) {
        if (!config.has("webhookUrl") || config.get("webhookUrl").isNull()) {
            return PluginValidationResult.invalid("Missing required field: webhookUrl");
        }

        String webhookUrl = config.get("webhookUrl").asText();
        if (!webhookUrl.startsWith("https://discord.com/api/webhooks/") &&
            !webhookUrl.startsWith("https://discordapp.com/api/webhooks/")) {
            return PluginValidationResult.invalid("Invalid Discord webhook URL");
        }

        return PluginValidationResult.valid();
    }

    @Override
    public List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException {
        throw new UnsupportedOperationException("Discord plugin is for notifications only");
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        return validateConfig(plugin.getConfiguration());
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.DISCORD;
    }

    /**
     * Get Discord embed color (integer) based on severity
     */
    private int getSeverityColor(String severity) {
        if (severity == null) {
            return 3066993; // green (#2ECC71)
        }

        switch (severity.toUpperCase()) {
            case "CRITICAL":
                return 15158332; // red (#E74C3C)
            case "WARNING":
                return 15105570; // orange (#E67E22)
            case "INFO":
            default:
                return 3066993; // green (#2ECC71)
        }
    }
}
