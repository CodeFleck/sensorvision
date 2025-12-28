package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TeamsNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${notifications.teams.webhook-url:}")
    private String webhookUrl;

    @Value("${notifications.teams.enabled:false}")
    private boolean enabled;

    public void sendAlertNotification(Alert alert) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Teams notifications disabled or webhook URL not configured");
            return;
        }

        try {
            Map<String, Object> message = buildAlertMessage(alert);
            sendMessage(message);
            log.info("Teams notification sent for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send Teams notification for alert: {}", alert.getId(), e);
        }
    }

    public void sendCustomNotification(String title, String message, String themeColor) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Teams notifications disabled or webhook URL not configured");
            return;
        }

        try {
            Map<String, Object> teamsMessage = buildCustomMessage(title, message, themeColor);
            sendMessage(teamsMessage);
            log.info("Custom Teams notification sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send custom Teams notification", e);
        }
    }

    private Map<String, Object> buildAlertMessage(Alert alert) {
        String themeColor = getSeverityColor(alert.getSeverity());
        String emoji = getSeverityEmoji(alert.getSeverity());

        Map<String, Object> message = new HashMap<>();
        message.put("@type", "MessageCard");
        message.put("@context", "https://schema.org/extensions");
        message.put("summary", "Alert: " + alert.getRule().getName());
        message.put("themeColor", themeColor);
        message.put("title", emoji + " Alert: " + alert.getRule().getName());
        message.put("text", alert.getMessage());

        // Add sections with facts
        Map<String, Object> section = new HashMap<>();

        Map<String, Object> fact1 = new HashMap<>();
        fact1.put("name", "Device:");
        fact1.put("value", alert.getDevice().getExternalId());

        Map<String, Object> fact2 = new HashMap<>();
        fact2.put("name", "Severity:");
        fact2.put("value", alert.getSeverity().toString());

        Map<String, Object> fact3 = new HashMap<>();
        fact3.put("name", "Value:");
        fact3.put("value", String.format("%.2f", alert.getTriggeredValue()));

        Map<String, Object> fact4 = new HashMap<>();
        fact4.put("name", "Time:");
        fact4.put("value", alert.getTriggeredAt().atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));

        section.put("facts", List.of(fact1, fact2, fact3, fact4));
        message.put("sections", List.of(section));

        return message;
    }

    private Map<String, Object> buildCustomMessage(String title, String text, String themeColor) {
        Map<String, Object> message = new HashMap<>();
        message.put("@type", "MessageCard");
        message.put("@context", "https://schema.org/extensions");
        message.put("summary", title);
        message.put("themeColor", themeColor != null ? themeColor : "0078D4");
        message.put("title", title);
        message.put("text", text);

        return message;
    }

    private void sendMessage(Map<String, Object> message) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            String jsonPayload = objectMapper.writeValueAsString(message);
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    webhookUrl,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                log.warn("Teams webhook returned non-success status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending message to Teams", e);
            throw new RuntimeException("Failed to send Teams notification", e);
        }
    }

    private String getSeverityColor(io.indcloud.model.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "FF0000"; // Red
            case HIGH -> "FFA500"; // Orange
            case MEDIUM -> "FFD700"; // Gold
            case LOW -> "0078D4"; // Blue
        };
    }

    private String getSeverityEmoji(io.indcloud.model.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "🚨";
            case HIGH -> "⚠️";
            case MEDIUM -> "🔔";
            case LOW -> "ℹ️";
        };
    }

    public void testConnection() {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalStateException("Teams notifications are not configured");
        }

        sendCustomNotification(
                "Test Notification",
                "This is a test message from SensorVision. If you're seeing this, Teams integration is working correctly!",
                "0078D4"
        );
    }
}
