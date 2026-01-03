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
 * ML Model entity representing trained machine learning models.
 * Supports anomaly detection, predictive maintenance, energy forecasting, and RUL estimation.
 */
@Entity
@Table(name = "ml_models")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private MLModelType modelType;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(nullable = false, length = 100)
    private String algorithm;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> hyperparameters;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_columns", columnDefinition = "jsonb")
    private List<String> featureColumns;

    @Column(name = "target_column", length = 100)
    private String targetColumn;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MLModelStatus status = MLModelStatus.DRAFT;

    @Column(name = "model_path", length = 500)
    private String modelPath;

    @Column(name = "model_size_bytes")
    private Long modelSizeBytes;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "training_metrics", columnDefinition = "jsonb")
    private Map<String, Object> trainingMetrics;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "validation_metrics", columnDefinition = "jsonb")
    private Map<String, Object> validationMetrics;

    @Column(name = "device_scope", length = 30)
    @Builder.Default
    private String deviceScope = "ALL";

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "device_ids", columnDefinition = "jsonb")
    private List<UUID> deviceIds;

    @Column(name = "device_group_id")
    private Long deviceGroupId;

    @Column(name = "inference_schedule", length = 100)
    @Builder.Default
    private String inferenceSchedule = "0 0 * * * *";

    @Column(name = "last_inference_at")
    private Instant lastInferenceAt;

    @Column(name = "next_inference_at")
    private Instant nextInferenceAt;

    @Column(name = "confidence_threshold", precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal confidenceThreshold = new BigDecimal("0.8");

    @Column(name = "anomaly_threshold", precision = 10, scale = 6)
    @Builder.Default
    private BigDecimal anomalyThreshold = new BigDecimal("0.5");

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "trained_by")
    private Long trainedBy;

    @Column(name = "trained_at")
    private Instant trainedAt;

    @Column(name = "deployed_at")
    private Instant deployedAt;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at")
    @Builder.Default
    private Instant updatedAt = Instant.now();

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }
}
