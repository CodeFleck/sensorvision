package io.indcloud.controller;

import io.indcloud.dto.ml.MLAnomalyResolveRequest;
import io.indcloud.dto.ml.MLAnomalyResponse;
import io.indcloud.model.*;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.ml.MLAnomalyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for ML anomaly management.
 * Provides endpoints for viewing and managing detected anomalies.
 */
@RestController
@RequestMapping("/api/v1/ml/anomalies")
@RequiredArgsConstructor
@Slf4j
public class MLAnomalyController {

    private final MLAnomalyService mlAnomalyService;
    private final SecurityUtils securityUtils;

    /**
     * List all anomalies for the current organization.
     */
    @GetMapping
    public Page<MLAnomalyResponse> getAnomalies(
            @PageableDefault(size = 20) Pageable pageable,
            @RequestParam(required = false) MLAnomalyStatus status,
            @RequestParam(required = false) MLAnomalySeverity severity) {

        Organization org = securityUtils.getCurrentUserOrganization();
        Page<MLAnomaly> anomalies;

        if (status != null) {
            anomalies = toPage(mlAnomalyService.getAnomaliesByStatus(org.getId(), status), pageable);
        } else if (severity != null) {
            anomalies = toPage(mlAnomalyService.getAnomaliesBySeverity(org.getId(), severity), pageable);
        } else {
            anomalies = mlAnomalyService.getAnomalies(org.getId(), pageable);
        }

        return anomalies.map(this::toResponse);
    }

    /**
     * Get anomalies for a specific device.
     */
    @GetMapping("/device/{deviceId}")
    public Page<MLAnomalyResponse> getDeviceAnomalies(
            @PathVariable UUID deviceId,
            @PageableDefault(size = 20) Pageable pageable) {

        return mlAnomalyService.getAnomaliesByDevice(deviceId, pageable)
                .map(this::toResponse);
    }

    /**
     * Get a specific anomaly by ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<MLAnomalyResponse> getAnomaly(@PathVariable UUID id) {
        return mlAnomalyService.getAnomaly(id)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get new (unacknowledged) anomalies.
     */
    @GetMapping("/new")
    public List<MLAnomalyResponse> getNewAnomalies() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return mlAnomalyService.getNewAnomalies(org.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Get critical anomalies (HIGH and CRITICAL severity).
     */
    @GetMapping("/critical")
    public List<MLAnomalyResponse> getCriticalAnomalies() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return mlAnomalyService.getCriticalAnomalies(org.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Acknowledge an anomaly.
     */
    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<MLAnomalyResponse> acknowledgeAnomaly(@PathVariable UUID id) {
        User user = securityUtils.getCurrentUser();

        try {
            MLAnomaly anomaly = mlAnomalyService.acknowledgeAnomaly(id, user.getId());
            log.info("User {} acknowledged anomaly {}", user.getUsername(), id);
            return ResponseEntity.ok(toResponse(anomaly));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Start investigation on an anomaly.
     */
    @PostMapping("/{id}/investigate")
    public ResponseEntity<MLAnomalyResponse> startInvestigation(@PathVariable UUID id) {
        try {
            MLAnomaly anomaly = mlAnomalyService.startInvestigation(id);
            log.info("Investigation started for anomaly {}", id);
            return ResponseEntity.ok(toResponse(anomaly));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Resolve an anomaly.
     */
    @PostMapping("/{id}/resolve")
    public ResponseEntity<MLAnomalyResponse> resolveAnomaly(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) MLAnomalyResolveRequest request) {

        User user = securityUtils.getCurrentUser();
        String note = request != null ? request.getResolutionNote() : null;

        try {
            MLAnomaly anomaly = mlAnomalyService.resolveAnomaly(id, user.getId(), note);
            log.info("User {} resolved anomaly {}", user.getUsername(), id);
            return ResponseEntity.ok(toResponse(anomaly));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Mark an anomaly as false positive.
     */
    @PostMapping("/{id}/false-positive")
    public ResponseEntity<MLAnomalyResponse> markFalsePositive(
            @PathVariable UUID id,
            @Valid @RequestBody(required = false) MLAnomalyResolveRequest request) {

        User user = securityUtils.getCurrentUser();
        String note = request != null ? request.getResolutionNote() : null;

        try {
            MLAnomaly anomaly = mlAnomalyService.markFalsePositive(id, user.getId(), note);
            log.info("User {} marked anomaly {} as false positive", user.getUsername(), id);
            return ResponseEntity.ok(toResponse(anomaly));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * Link anomaly to a global alert.
     */
    @PostMapping("/{id}/link-alert/{alertId}")
    public ResponseEntity<MLAnomalyResponse> linkToAlert(
            @PathVariable UUID id,
            @PathVariable UUID alertId) {

        try {
            MLAnomaly anomaly = mlAnomalyService.linkToAlert(id, alertId);
            log.info("Linked anomaly {} to alert {}", id, alertId);
            return ResponseEntity.ok(toResponse(anomaly));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Get anomaly statistics.
     */
    @GetMapping("/stats")
    public AnomalyStatsResponse getAnomalyStats(
            @RequestParam(defaultValue = "24") int hours) {

        Organization org = securityUtils.getCurrentUserOrganization();
        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);

        long newCount = mlAnomalyService.countNewAnomalies(org.getId());
        Map<MLAnomalySeverity, Long> bySeverity = mlAnomalyService.getAnomalyCountsBySeverity(org.getId(), since);

        return new AnomalyStatsResponse(newCount, bySeverity, hours);
    }

    /**
     * Convert page of list to proper page.
     */
    private Page<MLAnomaly> toPage(List<MLAnomaly> list, Pageable pageable) {
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), list.size());
        List<MLAnomaly> pageContent = start > list.size() ? List.of() : list.subList(start, end);
        return new org.springframework.data.domain.PageImpl<>(pageContent, pageable, list.size());
    }

    /**
     * Convert MLAnomaly entity to response DTO.
     */
    private MLAnomalyResponse toResponse(MLAnomaly anomaly) {
        return MLAnomalyResponse.builder()
                .id(anomaly.getId())
                .predictionId(anomaly.getPrediction() != null ? anomaly.getPrediction().getId() : null)
                .deviceId(anomaly.getDevice() != null ? anomaly.getDevice().getId() : null)
                .deviceName(anomaly.getDevice() != null ? anomaly.getDevice().getName() : null)
                .organizationId(anomaly.getOrganization() != null ? anomaly.getOrganization().getId() : null)
                .anomalyScore(anomaly.getAnomalyScore())
                .severity(anomaly.getSeverity())
                .status(anomaly.getStatus())
                .anomalyType(anomaly.getAnomalyType())
                .affectedVariables(anomaly.getAffectedVariables())
                .expectedValues(anomaly.getExpectedValues())
                .actualValues(anomaly.getActualValues())
                .contextWindow(null) // Context window not implemented yet
                .detectedAt(anomaly.getDetectedAt())
                .acknowledgedBy(anomaly.getAcknowledgedBy())
                .acknowledgedAt(anomaly.getAcknowledgedAt())
                .resolvedBy(anomaly.getResolvedBy())
                .resolvedAt(anomaly.getResolvedAt())
                .resolutionNote(anomaly.getResolutionNote())
                .globalAlertId(anomaly.getGlobalAlertId())
                .createdAt(anomaly.getCreatedAt())
                .predictionType(anomaly.getPrediction() != null ? anomaly.getPrediction().getPredictionType() : null)
                .predictionConfidence(anomaly.getPrediction() != null ? anomaly.getPrediction().getConfidence() : null)
                .build();
    }

    /**
     * Anomaly statistics response.
     */
    public record AnomalyStatsResponse(
            long newCount,
            Map<MLAnomalySeverity, Long> bySeverity,
            int periodHours
    ) {}
}
