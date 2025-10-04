package org.sensorvision.service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.AlertResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Alert;
import org.sensorvision.repository.AlertRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc(null).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnacknowledgedAlerts() {
        return alertRepository.findByAcknowledgedFalseOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void acknowledgeAlert(UUID alertId) {
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        if (!alert.getAcknowledged()) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(Instant.now());
            alertRepository.save(alert);
            log.info("Alert {} acknowledged", alertId);
        }
    }

    /**
     * Send notification for an alert (email, webhook, etc.)
     * This is a placeholder for actual notification implementation
     */
    public void sendAlertNotification(Alert alert) {
        // TODO: Implement actual notification sending
        // This could include:
        // - Email notifications
        // - Webhook calls
        // - Push notifications
        // - Slack/Teams integration
        // - SMS alerts

        log.info("Notification sent for alert: {} (severity: {})",
                alert.getMessage(), alert.getSeverity());

        // For demonstration, we'll just log the alert
        // In a real implementation, you might use:
        // - Spring Boot's JavaMailSender for email
        // - RestTemplate/WebClient for webhooks
        // - Third-party services like Twilio for SMS
        // - Integration with services like PagerDuty, Slack, etc.

        switch (alert.getSeverity()) {
            case CRITICAL -> log.error("🚨 CRITICAL ALERT: {}", alert.getMessage());
            case HIGH -> log.warn("⚠️ HIGH ALERT: {}", alert.getMessage());
            case MEDIUM -> log.info("ℹ️ MEDIUM ALERT: {}", alert.getMessage());
            case LOW -> log.debug("📝 LOW ALERT: {}", alert.getMessage());
        }
    }

    private AlertResponse toResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getRule().getId(),
                alert.getRule().getName(),
                alert.getDevice().getExternalId(),
                alert.getDevice().getName(),
                alert.getMessage(),
                alert.getSeverity(),
                alert.getTriggeredValue(),
                alert.getAcknowledged(),
                alert.getAcknowledgedAt(),
                alert.getCreatedAt()
        );
    }
}