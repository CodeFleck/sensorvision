package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * ML Anomaly entity for detected anomalies with status tracking and resolution workflow.
 */
@Entity
@Table(name = "ml_anomalies")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
public class MLAnomaly {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prediction_id", nullable = false)
    private MLPrediction prediction;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "anomaly_score", nullable = false, precision = 10, scale = 6)
    private BigDecimal anomalyScore;

    @Column(name = "anomaly_type", length = 100)
    @Builder.Default
    private String anomalyType = "POINT_ANOMALY";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private MLAnomalySeverity severity = MLAnomalySeverity.MEDIUM;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_variables", columnDefinition = "jsonb")
    private List<String> affectedVariables;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "expected_values", columnDefinition = "jsonb")
    private Map<String, Object> expectedValues;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actual_values", columnDefinition = "jsonb")
    private Map<String, Object> actualValues;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MLAnomalyStatus status = MLAnomalyStatus.NEW;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @Column(name = "acknowledged_by")
    private UUID acknowledgedBy;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @Column(name = "resolved_by")
    private UUID resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @Column(name = "global_alert_id")
    private UUID globalAlertId;

    @Column(name = "detected_at", nullable = false)
    private Instant detectedAt;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
