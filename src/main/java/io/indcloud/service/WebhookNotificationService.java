package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import io.indcloud.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class WebhookNotificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    // Allowlist of permitted protocols
    private static final List<String> ALLOWED_PROTOCOLS = List.of("https");

    // Blocklist of private/internal IP ranges and hostnames
    private static final List<String> BLOCKED_HOSTS = List.of(
        "localhost", "127.0.0.1", "0.0.0.0", "[::]", "[::1]",
        "169.254.", "10.", "172.16.", "172.17.", "172.18.", "172.19.",
        "172.20.", "172.21.", "172.22.", "172.23.", "172.24.", "172.25.",
        "172.26.", "172.27.", "172.28.", "172.29.", "172.30.", "172.31.",
        "192.168.", "metadata.google.internal"
    );

    @Value("${notification.webhook.enabled:false}")
    private boolean webhookEnabled;

    @Value("${notification.webhook.timeout-ms:5000}")
    private int timeoutMs;

    public WebhookNotificationService(
            RestTemplateBuilder restTemplateBuilder,
            ObjectMapper objectMapper,
            @Value("${notification.webhook.timeout-ms:5000}") int timeoutMs) {
        this.objectMapper = objectMapper;
        this.timeoutMs = timeoutMs;
        // Configure RestTemplate with proper timeouts
        this.restTemplate = restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeoutMs))
                .setReadTimeout(Duration.ofMillis(timeoutMs))
                .build();
    }

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Validate webhook URL to prevent SSRF attacks
     */
    private void validateWebhookUrl(String webhookUrl) {
        if (webhookUrl == null || webhookUrl.isEmpty()) {
            throw new IllegalArgumentException("Webhook URL cannot be empty");
        }

        URI uri;
        try {
            uri = new URI(webhookUrl);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid webhook URL format: " + e.getMessage());
        }

        // Validate protocol (must be HTTPS only)
        String scheme = uri.getScheme();
        if (scheme == null || !ALLOWED_PROTOCOLS.contains(scheme.toLowerCase())) {
            throw new IllegalArgumentException(
                "Webhook URL must use HTTPS protocol. Found: " + scheme);
        }

        // Validate host is not in blocklist
        String host = uri.getHost();
        if (host == null) {
            throw new IllegalArgumentException("Webhook URL must have a valid host");
        }

        String hostLower = host.toLowerCase();
        for (String blockedPrefix : BLOCKED_HOSTS) {
            if (hostLower.startsWith(blockedPrefix) || hostLower.equals(blockedPrefix.replaceAll("\\.$", ""))) {
                throw new IllegalArgumentException(
                    "Webhook URL points to blocked internal/private host: " + host);
            }
        }

        log.debug("Webhook URL validated successfully: {}", webhookUrl);
    }

    /**
     * Send webhook notification for an alert
     * Supports custom webhook URLs with JSON payloads
     */
    public boolean sendAlertWebhook(User user, Alert alert, String webhookUrl) {
        if (!webhookEnabled) {
            log.info("Webhook notifications disabled. Would have sent webhook to: {}", webhookUrl);
            return false;
        }

        if (webhookUrl == null || webhookUrl.isEmpty()) {
            log.warn("Webhook URL not provided for user: {}", user.getUsername());
            return false;
        }

        try {
            // Validate URL to prevent SSRF
            validateWebhookUrl(webhookUrl);
            // Build webhook payload
            Map<String, Object> payload = buildWebhookPayload(alert);

            // Set headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "SensorVision/1.0");
            headers.set("X-SensorVision-Event", "alert.triggered");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, headers);

            log.info("Sending webhook notification to: {}", webhookUrl);

            // Send POST request to webhook URL
            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl,
                request,
                String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("Webhook sent successfully to {} with status: {}",
                    webhookUrl, response.getStatusCode());
                return true;
            } else {
                log.warn("Webhook to {} returned non-success status: {}",
                    webhookUrl, response.getStatusCode());
                return false;
            }
        } catch (Exception e) {
            log.error("Failed to send webhook notification to {}", webhookUrl, e);
            return false;
        }
    }

    /**
     * Build webhook payload with alert information
     */
    private Map<String, Object> buildWebhookPayload(Alert alert) {
        Map<String, Object> payload = new HashMap<>();

        // Event metadata
        payload.put("event", "alert.triggered");
        payload.put("timestamp", alert.getTriggeredAt().format(DATE_FORMATTER));

        // Alert information
        Map<String, Object> alertData = new HashMap<>();
        alertData.put("id", alert.getId());
        alertData.put("severity", alert.getSeverity().toString());
        alertData.put("message", alert.getMessage());
        alertData.put("triggeredValue", alert.getTriggeredValue());
        alertData.put("acknowledged", alert.getAcknowledged());

        // Rule information
        if (alert.getRule() != null) {
            Map<String, Object> ruleData = new HashMap<>();
            ruleData.put("id", alert.getRule().getId());
            ruleData.put("name", alert.getRule().getName());
            ruleData.put("variable", alert.getRule().getVariable());
            ruleData.put("operator", alert.getRule().getOperator().toString());
            ruleData.put("threshold", alert.getRule().getThreshold());
            alertData.put("rule", ruleData);
        }

        // Device information
        if (alert.getDevice() != null) {
            Map<String, Object> deviceData = new HashMap<>();
            deviceData.put("id", alert.getDevice().getId());
            deviceData.put("externalId", alert.getDevice().getExternalId());
            deviceData.put("name", alert.getDevice().getName());
            deviceData.put("location", alert.getDevice().getLocation());
            deviceData.put("sensorType", alert.getDevice().getSensorType());

            if (alert.getDevice().getOrganization() != null) {
                deviceData.put("organizationId", alert.getDevice().getOrganization().getId());
                deviceData.put("organizationName", alert.getDevice().getOrganization().getName());
            }

            alertData.put("device", deviceData);
        }

        payload.put("alert", alertData);

        return payload;
    }

    /**
     * Test webhook connectivity
     */
    public boolean testWebhook(String webhookUrl) {
        try {
            // Validate URL to prevent SSRF
            validateWebhookUrl(webhookUrl);

            Map<String, Object> testPayload = new HashMap<>();
            testPayload.put("event", "webhook.test");
            testPayload.put("message", "This is a test webhook from Industrial Cloud");
            testPayload.put("timestamp", java.time.LocalDateTime.now().format(DATE_FORMATTER));

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("User-Agent", "SensorVision/1.0");
            headers.set("X-SensorVision-Event", "webhook.test");

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(testPayload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                webhookUrl,
                request,
                String.class
            );

            return response.getStatusCode().is2xxSuccessful();
        } catch (IllegalArgumentException e) {
            log.error("Webhook URL validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.error("Webhook test failed for URL: {}", webhookUrl, e);
            return false;
        }
    }
}
