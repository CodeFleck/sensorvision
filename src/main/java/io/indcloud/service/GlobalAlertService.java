package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.GlobalAlertResponse;
import io.indcloud.model.GlobalAlert;
import io.indcloud.model.User;
import io.indcloud.repository.GlobalAlertRepository;
import io.indcloud.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service for managing global alerts
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalAlertService {

    private final GlobalAlertRepository globalAlertRepository;
    private final UserRepository userRepository;

    /**
     * Get all global alerts for an organization
     */
    public Page<GlobalAlertResponse> getGlobalAlerts(Long organizationId, Pageable pageable) {
        Page<GlobalAlert> alerts = globalAlertRepository.findByOrganizationId(organizationId, pageable);
        return alerts.map(this::toResponse);
    }

    /**
     * Get unacknowledged global alerts for an organization
     */
    public Page<GlobalAlertResponse> getUnacknowledgedAlerts(Long organizationId, Pageable pageable) {
        Page<GlobalAlert> alerts = globalAlertRepository.findByOrganizationIdAndAcknowledgedFalse(
                organizationId, pageable);
        return alerts.map(this::toResponse);
    }

    /**
     * Get a specific global alert
     */
    public GlobalAlertResponse getGlobalAlert(UUID alertId, Long organizationId) {
        GlobalAlert alert = globalAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Global alert not found: " + alertId));

        // Verify organization ownership
        if (!alert.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global alert does not belong to organization");
        }

        return toResponse(alert);
    }

    /**
     * Acknowledge a global alert
     */
    @Transactional
    public GlobalAlertResponse acknowledgeAlert(UUID alertId, Long userId, Long organizationId) {
        log.info("Acknowledging global alert: {} by user {}", alertId, userId);

        GlobalAlert alert = globalAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Global alert not found: " + alertId));

        // Verify organization ownership
        if (!alert.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global alert does not belong to organization");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        alert.setAcknowledged(true);
        alert.setAcknowledgedAt(Instant.now());
        alert.setAcknowledgedBy(user);

        alert = globalAlertRepository.save(alert);
        log.info("Acknowledged global alert: {}", alertId);

        return toResponse(alert);
    }

    /**
     * Resolve a global alert
     */
    @Transactional
    public GlobalAlertResponse resolveAlert(UUID alertId, Long userId, String resolutionNote, Long organizationId) {
        log.info("Resolving global alert: {} by user {}", alertId, userId);

        GlobalAlert alert = globalAlertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("Global alert not found: " + alertId));

        // Verify organization ownership
        if (!alert.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global alert does not belong to organization");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // Auto-acknowledge if not already acknowledged
        if (!alert.getAcknowledged()) {
            alert.setAcknowledged(true);
            alert.setAcknowledgedAt(Instant.now());
            alert.setAcknowledgedBy(user);
        }

        alert.setResolved(true);
        alert.setResolvedAt(Instant.now());
        alert.setResolvedBy(user);
        alert.setResolutionNote(resolutionNote);

        alert = globalAlertRepository.save(alert);
        log.info("Resolved global alert: {}", alertId);

        return toResponse(alert);
    }

    /**
     * Get count of unacknowledged alerts for an organization
     */
    public long countUnacknowledgedAlerts(Long organizationId) {
        return globalAlertRepository.countByOrganizationIdAndAcknowledgedFalse(organizationId);
    }

    /**
     * Get count of unresolved alerts for an organization
     */
    public long countUnresolvedAlerts(Long organizationId) {
        return globalAlertRepository.countByOrganizationIdAndResolvedFalse(organizationId);
    }

    /**
     * Convert GlobalAlert entity to response DTO
     */
    private GlobalAlertResponse toResponse(GlobalAlert alert) {
        return GlobalAlertResponse.builder()
                .id(alert.getId())
                .globalRuleId(alert.getGlobalRule().getId())
                .globalRuleName(alert.getGlobalRule().getName())
                .organizationId(alert.getOrganization().getId())
                .message(alert.getMessage())
                .severity(alert.getSeverity())
                .triggeredValue(alert.getTriggeredValue())
                .deviceCount(alert.getDeviceCount())
                .affectedDevices(alert.getAffectedDevices())
                .triggeredAt(alert.getTriggeredAt())
                .acknowledged(alert.getAcknowledged())
                .acknowledgedAt(alert.getAcknowledgedAt())
                .acknowledgedBy(alert.getAcknowledgedBy() != null ?
                        alert.getAcknowledgedBy().getId() : null)
                .acknowledgedByName(alert.getAcknowledgedBy() != null ?
                        alert.getAcknowledgedBy().getUsername() : null)
                .resolved(alert.getResolved())
                .resolvedAt(alert.getResolvedAt())
                .resolvedBy(alert.getResolvedBy() != null ?
                        alert.getResolvedBy().getId() : null)
                .resolvedByName(alert.getResolvedBy() != null ?
                        alert.getResolvedBy().getUsername() : null)
                .resolutionNote(alert.getResolutionNote())
                .createdAt(alert.getCreatedAt())
                .updatedAt(alert.getUpdatedAt())
                .build();
    }
}
