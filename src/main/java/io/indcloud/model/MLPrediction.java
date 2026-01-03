package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ML Prediction entity storing predictions from ML models.
 */
@Entity
@Table(name = "ml_predictions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLPrediction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = false)
    private MLModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "prediction_type", nullable = false, length = 50)
    private String predictionType;

    @Column(name = "prediction_value", precision = 20, scale = 6)
    private BigDecimal predictionValue;

    @Column(name = "prediction_label", length = 100)
    private String predictionLabel;

    @Column(precision = 5, scale = 4)
    private BigDecimal confidence;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "prediction_details", columnDefinition = "jsonb")
    private Map<String, Object> predictionDetails;

    @Column(name = "prediction_timestamp", nullable = false)
    private Instant predictionTimestamp;

    @Column(name = "prediction_horizon", length = 50)
    private String predictionHorizon;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "feedback_label", length = 100)
    private String feedbackLabel;

    @Column(name = "feedback_at")
    private Instant feedbackAt;

    @Column(name = "feedback_by")
    private Long feedbackBy;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
