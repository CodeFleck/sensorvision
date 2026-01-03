package io.indcloud.service.ml;

import io.indcloud.model.*;
import io.indcloud.repository.MLAnomalyRepository;
import io.indcloud.repository.MLPredictionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing ML-detected anomalies.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MLAnomalyService {

    private final MLAnomalyRepository mlAnomalyRepository;
    private final MLPredictionRepository mlPredictionRepository;

    /**
     * Get all anomalies for an organization.
     */
    @Transactional(readOnly = true)
    public Page<MLAnomaly> getAnomalies(Long organizationId, Pageable pageable) {
        return mlAnomalyRepository.findByOrganizationId(organizationId, pageable);
    }

    /**
     * Get anomalies for a device.
     */
    @Transactional(readOnly = true)
    public Page<MLAnomaly> getAnomaliesByDevice(UUID deviceId, Pageable pageable) {
        return mlAnomalyRepository.findByDeviceId(deviceId, pageable);
    }

    /**
     * Get an anomaly by ID.
     */
    @Transactional(readOnly = true)
    public Optional<MLAnomaly> getAnomaly(UUID anomalyId) {
        return mlAnomalyRepository.findById(anomalyId);
    }

    /**
     * Get new (unacknowledged) anomalies.
     */
    @Transactional(readOnly = true)
    public List<MLAnomaly> getNewAnomalies(Long organizationId) {
        return mlAnomalyRepository.findNewAnomaliesByOrganization(organizationId);
    }

    /**
     * Get anomalies by status.
     */
    @Transactional(readOnly = true)
    public List<MLAnomaly> getAnomaliesByStatus(Long organizationId, MLAnomalyStatus status) {
        return mlAnomalyRepository.findByOrganizationIdAndStatus(organizationId, status);
    }

    /**
     * Get anomalies by severity.
     */
    @Transactional(readOnly = true)
    public List<MLAnomaly> getAnomaliesBySeverity(Long organizationId, MLAnomalySeverity severity) {
        return mlAnomalyRepository.findByOrganizationIdAndSeverity(organizationId, severity);
    }

    /**
     * Get critical unresolved anomalies.
     */
    @Transactional(readOnly = true)
    public List<MLAnomaly> getCriticalAnomalies(Long organizationId) {
        return mlAnomalyRepository.findCriticalNewAnomalies(
                organizationId,
                List.of(MLAnomalySeverity.CRITICAL, MLAnomalySeverity.HIGH));
    }

    /**
     * Get anomalies in time range.
     */
    @Transactional(readOnly = true)
    public List<MLAnomaly> getAnomaliesInTimeRange(UUID deviceId, Instant start, Instant end) {
        return mlAnomalyRepository.findByDeviceAndTimeRange(deviceId, start, end);
    }

    /**
     * Create a new anomaly from prediction.
     */
    @Transactional
    public MLAnomaly createAnomaly(MLPrediction prediction, BigDecimal anomalyScore,
                                    String anomalyType, MLAnomalySeverity severity,
                                    List<String> affectedVariables,
                                    Map<String, Object> expectedValues,
                                    Map<String, Object> actualValues) {
        MLAnomaly anomaly = MLAnomaly.builder()
                .prediction(prediction)
                .device(prediction.getDevice())
                .organization(prediction.getOrganization())
                .anomalyScore(anomalyScore)
                .anomalyType(anomalyType)
                .severity(severity)
                .affectedVariables(affectedVariables)
                .expectedValues(expectedValues)
                .actualValues(actualValues)
                .status(MLAnomalyStatus.NEW)
                .detectedAt(prediction.getPredictionTimestamp())
                .build();

        MLAnomaly saved = mlAnomalyRepository.save(anomaly);
        log.info("Created anomaly: {} (severity={}, score={})",
                saved.getId(), severity, anomalyScore);
        return saved;
    }

    /**
     * Acknowledge an anomaly.
     */
    @Transactional
    public MLAnomaly acknowledgeAnomaly(UUID anomalyId, UUID userId) {
        MLAnomaly anomaly = mlAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        if (anomaly.getStatus() != MLAnomalyStatus.NEW) {
            throw new IllegalStateException("Can only acknowledge NEW anomalies");
        }

        anomaly.setStatus(MLAnomalyStatus.ACKNOWLEDGED);
        anomaly.setAcknowledgedAt(Instant.now());
        anomaly.setAcknowledgedBy(userId);

        MLAnomaly updated = mlAnomalyRepository.save(anomaly);
        log.info("Acknowledged anomaly: {} by user: {}", anomalyId, userId);
        return updated;
    }

    /**
     * Start investigating an anomaly.
     */
    @Transactional
    public MLAnomaly startInvestigation(UUID anomalyId) {
        MLAnomaly anomaly = mlAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        if (anomaly.getStatus() == MLAnomalyStatus.RESOLVED ||
            anomaly.getStatus() == MLAnomalyStatus.FALSE_POSITIVE) {
            throw new IllegalStateException("Cannot investigate closed anomaly");
        }

        anomaly.setStatus(MLAnomalyStatus.INVESTIGATING);

        return mlAnomalyRepository.save(anomaly);
    }

    /**
     * Resolve an anomaly.
     */
    @Transactional
    public MLAnomaly resolveAnomaly(UUID anomalyId, UUID userId, String resolutionNote) {
        MLAnomaly anomaly = mlAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        if (anomaly.getStatus() == MLAnomalyStatus.RESOLVED ||
            anomaly.getStatus() == MLAnomalyStatus.FALSE_POSITIVE) {
            throw new IllegalStateException("Anomaly is already closed");
        }

        anomaly.setStatus(MLAnomalyStatus.RESOLVED);
        anomaly.setResolvedAt(Instant.now());
        anomaly.setResolvedBy(userId);
        anomaly.setResolutionNote(resolutionNote);

        MLAnomaly updated = mlAnomalyRepository.save(anomaly);
        log.info("Resolved anomaly: {} by user: {}", anomalyId, userId);
        return updated;
    }

    /**
     * Mark anomaly as false positive.
     */
    @Transactional
    public MLAnomaly markFalsePositive(UUID anomalyId, UUID userId, String note) {
        MLAnomaly anomaly = mlAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        anomaly.setStatus(MLAnomalyStatus.FALSE_POSITIVE);
        anomaly.setResolvedAt(Instant.now());
        anomaly.setResolvedBy(userId);
        anomaly.setResolutionNote(note);

        MLAnomaly updated = mlAnomalyRepository.save(anomaly);
        log.info("Marked anomaly as false positive: {} by user: {}", anomalyId, userId);

        // Update prediction feedback for model improvement
        if (anomaly.getPrediction() != null) {
            MLPrediction prediction = anomaly.getPrediction();
            prediction.setFeedbackLabel("FALSE_POSITIVE");
            prediction.setFeedbackAt(Instant.now());
            prediction.setFeedbackBy(userId);
            mlPredictionRepository.save(prediction);
        }

        return updated;
    }

    /**
     * Link anomaly to a global alert.
     */
    @Transactional
    public MLAnomaly linkToAlert(UUID anomalyId, UUID globalAlertId) {
        MLAnomaly anomaly = mlAnomalyRepository.findById(anomalyId)
                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

        anomaly.setGlobalAlertId(globalAlertId);

        return mlAnomalyRepository.save(anomaly);
    }

    /**
     * Count new anomalies for an organization.
     */
    @Transactional(readOnly = true)
    public long countNewAnomalies(Long organizationId) {
        return mlAnomalyRepository.countNewAnomaliesByOrganization(organizationId);
    }

    /**
     * Count anomalies for a device since timestamp.
     */
    @Transactional(readOnly = true)
    public long countAnomaliesSince(UUID deviceId, Instant since) {
        return mlAnomalyRepository.countAnomaliesSince(deviceId, since);
    }

    /**
     * Get anomaly count by severity.
     */
    @Transactional(readOnly = true)
    public Map<MLAnomalySeverity, Long> getAnomalyCountsBySeverity(Long organizationId, Instant since) {
        List<Object[]> results = mlAnomalyRepository.countBySeveritySince(organizationId, since);

        return results.stream()
                .collect(java.util.stream.Collectors.toMap(
                        row -> (MLAnomalySeverity) row[0],
                        row -> (Long) row[1]
                ));
    }

    /**
     * Determine severity based on anomaly score.
     */
    public MLAnomalySeverity determineSeverity(BigDecimal anomalyScore, BigDecimal threshold) {
        double score = anomalyScore.doubleValue();
        double thresholdVal = threshold.doubleValue();

        if (score >= thresholdVal + 0.35) {
            return MLAnomalySeverity.CRITICAL;
        } else if (score >= thresholdVal + 0.2) {
            return MLAnomalySeverity.HIGH;
        } else if (score >= thresholdVal) {
            return MLAnomalySeverity.MEDIUM;
        } else {
            return MLAnomalySeverity.LOW;
        }
    }
}
