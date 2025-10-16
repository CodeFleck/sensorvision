package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
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
public class SlackNotificationService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper;

    @Value("${notifications.slack.webhook-url:}")
    private String webhookUrl;

    @Value("${notifications.slack.enabled:false}")
    private boolean enabled;

    public void sendAlertNotification(Alert alert) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Slack notifications disabled or webhook URL not configured");
            return;
        }

        try {
            Map<String, Object> message = buildAlertMessage(alert);
            sendMessage(message);
            log.info("Slack notification sent for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to send Slack notification for alert: {}", alert.getId(), e);
        }
    }

    public void sendCustomNotification(String title, String message, String color) {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            log.debug("Slack notifications disabled or webhook URL not configured");
            return;
        }

        try {
            Map<String, Object> slackMessage = buildCustomMessage(title, message, color);
            sendMessage(slackMessage);
            log.info("Custom Slack notification sent: {}", title);
        } catch (Exception e) {
            log.error("Failed to send custom Slack notification", e);
        }
    }

    private Map<String, Object> buildAlertMessage(Alert alert) {
        String color = getSeverityColor(alert.getSeverity());
        String emoji = getSeverityEmoji(alert.getSeverity());

        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color);
        attachment.put("title", emoji + " Alert: " + alert.getRule().getName());
        attachment.put("text", alert.getMessage());
        attachment.put("footer", "SensorVision Alert");
        attachment.put("ts", alert.getTriggeredAt().atZone(java.time.ZoneId.systemDefault()).toEpochSecond());

        Map<String, Object> field1 = new HashMap<>();
        field1.put("title", "Device");
        field1.put("value", alert.getDevice().getExternalId());
        field1.put("short", true);

        Map<String, Object> field2 = new HashMap<>();
        field2.put("title", "Severity");
        field2.put("value", alert.getSeverity().toString());
        field2.put("short", true);

        Map<String, Object> field3 = new HashMap<>();
        field3.put("title", "Value");
        field3.put("value", String.format("%.2f", alert.getTriggeredValue()));
        field3.put("short", true);

        Map<String, Object> field4 = new HashMap<>();
        field4.put("title", "Time");
        field4.put("value", alert.getTriggeredAt().atZone(java.time.ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        field4.put("short", true);

        attachment.put("fields", List.of(field1, field2, field3, field4));

        Map<String, Object> message = new HashMap<>();
        message.put("attachments", List.of(attachment));

        return message;
    }

    private Map<String, Object> buildCustomMessage(String title, String text, String color) {
        Map<String, Object> attachment = new HashMap<>();
        attachment.put("color", color != null ? color : "#36a64f");
        attachment.put("title", title);
        attachment.put("text", text);
        attachment.put("footer", "SensorVision");
        attachment.put("ts", System.currentTimeMillis() / 1000);

        Map<String, Object> message = new HashMap<>();
        message.put("attachments", List.of(attachment));

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
                log.warn("Slack webhook returned non-success status: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            log.error("Error sending message to Slack", e);
            throw new RuntimeException("Failed to send Slack notification", e);
        }
    }

    private String getSeverityColor(org.sensorvision.model.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> "#FF0000"; // Red
            case HIGH -> "#FFA500"; // Orange
            case MEDIUM -> "#FFD700"; // Gold
            case LOW -> "#36a64f"; // Green
        };
    }

    private String getSeverityEmoji(org.sensorvision.model.AlertSeverity severity) {
        return switch (severity) {
            case CRITICAL -> ":rotating_light:";
            case HIGH -> ":warning:";
            case MEDIUM -> ":bell:";
            case LOW -> ":information_source:";
        };
    }

    public void testConnection() {
        if (!enabled || webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalStateException("Slack notifications are not configured");
        }

        sendCustomNotification(
                "Test Notification",
                "This is a test message from SensorVision. If you're seeing this, Slack integration is working correctly!",
                "#36a64f"
        );
    }
}
