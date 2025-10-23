package org.sensorvision.service;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.AlertSeverity;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class EmailTemplateService {

    @Value("${app.frontend.url:http://localhost:3001}")
    private String frontendUrl;

    /**
     * Generate password reset email HTML
     */
    public String generatePasswordResetEmail(String resetLink) {
        try {
            String template = loadTemplate("password-reset.html");
            Map<String, String> variables = new HashMap<>();
            variables.put("RESET_LINK", resetLink);
            return replaceVariables(template, variables);
        } catch (IOException e) {
            log.error("Failed to load password reset template, using fallback", e);
            return generatePasswordResetFallback(resetLink);
        }
    }

    /**
     * Generate alert notification email HTML
     */
    public String generateAlertNotificationEmail(Alert alert) {
        try {
            String template = loadTemplate("alert-notification.html");

            String severityClass = switch (alert.getSeverity()) {
                case CRITICAL -> "critical";
                case HIGH -> "high";
                case MEDIUM -> "medium";
                case LOW -> "low";
            };

            String dashboardLink = frontendUrl + "/alerts";

            Map<String, String> variables = new HashMap<>();
            variables.put("SEVERITY", alert.getSeverity().name());
            variables.put("SEVERITY_CLASS", severityClass);
            variables.put("RULE_NAME", alert.getRule().getName());
            variables.put("ALERT_MESSAGE", alert.getMessage());
            variables.put("DEVICE_NAME", alert.getDevice().getName());
            variables.put("TRIGGERED_VALUE", alert.getTriggeredValue() != null ? alert.getTriggeredValue().toString() : "N/A");
            variables.put("CREATED_AT", alert.getCreatedAt() != null ? alert.getCreatedAt().toString() : "N/A");
            variables.put("DASHBOARD_LINK", dashboardLink);

            return replaceVariables(template, variables);
        } catch (IOException e) {
            log.error("Failed to load alert notification template, using fallback", e);
            return generateAlertNotificationFallback(alert);
        }
    }

    /**
     * Load template from resources
     */
    private String loadTemplate(String templateName) throws IOException {
        ClassPathResource resource = new ClassPathResource("templates/email/" + templateName);
        return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }

    /**
     * Replace template variables
     */
    private String replaceVariables(String template, Map<String, String> variables) {
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return result;
    }

    /**
     * Fallback password reset email (if template fails to load)
     */
    private String generatePasswordResetFallback(String resetLink) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f3f4f6;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px;">
                        <h2 style="color: #111827;">Password Reset Request</h2>
                        <p>We received a request to reset your password for your SensorVision account.</p>
                        <p>Click the link below to reset your password:</p>
                        <p><a href="%s" style="display: inline-block; padding: 12px 24px; background-color: #3b82f6; color: white; text-decoration: none; border-radius: 6px;">Reset Password</a></p>
                        <p>Or copy and paste this link into your browser:</p>
                        <p style="background-color: #f3f4f6; padding: 10px; border-radius: 4px; word-break: break-all;">%s</p>
                        <p><strong>This link will expire in 1 hour.</strong></p>
                        <p>If you didn't request a password reset, you can safely ignore this email.</p>
                        <hr style="margin: 30px 0; border: none; border-top: 1px solid #e5e7eb;">
                        <p style="color: #6b7280; font-size: 14px;">This is an automated message from SensorVision IoT Platform</p>
                    </div>
                </body>
                </html>
                """, resetLink, resetLink);
    }

    /**
     * Fallback alert notification email (if template fails to load)
     */
    private String generateAlertNotificationFallback(Alert alert) {
        return String.format("""
                <html>
                <body style="font-family: Arial, sans-serif; padding: 20px; background-color: #f3f4f6;">
                    <div style="max-width: 600px; margin: 0 auto; background-color: white; padding: 30px; border-radius: 8px;">
                        <h2 style="color: #111827;">Alert Notification</h2>
                        <p><strong>Rule:</strong> %s</p>
                        <p><strong>Device:</strong> %s</p>
                        <p><strong>Severity:</strong> %s</p>
                        <p><strong>Message:</strong> %s</p>
                        <p><strong>Triggered Value:</strong> %s</p>
                        <p><strong>Time:</strong> %s</p>
                        <hr style="margin: 20px 0;">
                        <p style="color: #6b7280; font-size: 14px;">This is an automated alert from SensorVision IoT Platform</p>
                    </div>
                </body>
                </html>
                """,
                alert.getRule().getName(),
                alert.getDevice().getName(),
                alert.getSeverity(),
                alert.getMessage(),
                alert.getTriggeredValue(),
                alert.getCreatedAt());
    }
}
