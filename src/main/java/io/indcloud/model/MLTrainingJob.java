package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * ML Training Job entity for tracking training history with progress and results.
 */
@Entity
@Table(name = "ml_training_jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MLTrainingJob {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private MLModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Enumerated(EnumType.STRING)
    @Column(name = "job_type", nullable = false, length = 50)
    private MLTrainingJobType jobType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private MLTrainingJobStatus status = MLTrainingJobStatus.PENDING;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "training_config", nullable = false, columnDefinition = "jsonb")
    @Builder.Default
    private Map<String, Object> trainingConfig = Map.of();

    @Column(name = "training_data_start")
    private Instant trainingDataStart;

    @Column(name = "training_data_end")
    private Instant trainingDataEnd;

    @Column(name = "record_count")
    private Long recordCount;

    @Column(name = "device_count")
    private Integer deviceCount;

    @Column(name = "progress_percent")
    @Builder.Default
    private Integer progressPercent = 0;

    @Column(name = "current_step", length = 100)
    private String currentStep;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_metrics", columnDefinition = "jsonb")
    private Map<String, Object> resultMetrics;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "error_stack_trace", columnDefinition = "TEXT")
    private String errorStackTrace;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();
}
