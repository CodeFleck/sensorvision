package org.sensorvision.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.sensorvision.model.Alert;
import org.sensorvision.model.AlertSeverity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AlertRepository extends JpaRepository<Alert, UUID> {

    /**
     * Find recent alerts ordered by creation time (most recent first)
     */
    List<Alert> findAllByOrderByCreatedAtDesc(Pageable pageable);

    /**
     * Find unacknowledged alerts
     */
    List<Alert> findByAcknowledgedFalseOrderByCreatedAtDesc();

    /**
     * Find alerts by severity
     */
    List<Alert> findBySeverityOrderByCreatedAtDesc(AlertSeverity severity);

    /**
     * Find alerts for a specific device
     */
    @Query("SELECT a FROM Alert a WHERE a.device.externalId = :deviceExternalId ORDER BY a.createdAt DESC")
    List<Alert> findByDeviceExternalIdOrderByCreatedAtDesc(@Param("deviceExternalId") String deviceExternalId);

    /**
     * Find alerts within a time range
     */
    List<Alert> findByCreatedAtBetweenOrderByCreatedAtDesc(Instant from, Instant to);

    /**
     * Count unacknowledged alerts
     */
    long countByAcknowledgedFalse();

    /**
     * Count alerts by severity
     */
    long countBySeverity(AlertSeverity severity);

    /**
     * Find recent alerts for rule (to prevent spam)
     */
    @Query("SELECT a FROM Alert a WHERE a.rule.id = :ruleId AND a.createdAt > :since ORDER BY a.createdAt DESC")
    List<Alert> findRecentAlertsForRule(@Param("ruleId") UUID ruleId, @Param("since") Instant since);
}