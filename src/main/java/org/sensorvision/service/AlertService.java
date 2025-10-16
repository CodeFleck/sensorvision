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
import org.sensorvision.model.Organization;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AlertService {

    private final AlertRepository alertRepository;
    private final NotificationService notificationService;

    @Transactional(readOnly = true)
    public List<AlertResponse> getAllAlerts() {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        return alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrg).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> getUnacknowledgedAlerts() {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        return alertRepository.findByDeviceOrganizationAndAcknowledgedFalseOrderByCreatedAtDesc(userOrg).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public void acknowledgeAlert(UUID alertId) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Alert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new ResourceNotFoundException("Alert not found: " + alertId));

        // Verify alert belongs to user's organization
        if (!alert.getDevice().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to alert: " + alertId);
        }

        if (!alert.getAcknowledged()) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(Instant.now());
            alertRepository.save(alert);
            log.info("Alert {} acknowledged", alertId);
        }
    }

    /**
     * Save an alert to the database
     */
    public Alert saveAlert(Alert alert) {
        Alert saved = alertRepository.save(alert);
        log.info("Alert saved: {} for device: {}", saved.getId(), alert.getDevice().getId());

        // Send notifications for the alert
        sendAlertNotification(saved);

        return saved;
    }

    /**
     * Send notification for an alert through configured channels
     */
    public void sendAlertNotification(Alert alert) {
        log.info("Triggering notifications for alert: {} (severity: {})",
                alert.getMessage(), alert.getSeverity());

        // Delegate to NotificationService to handle all channels
        notificationService.sendAlertNotifications(alert);
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