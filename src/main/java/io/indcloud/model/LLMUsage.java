package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks LLM API usage for billing and analytics.
 * Each record represents a single LLM API call.
 */
@Entity
@Table(name = "llm_usage", indexes = {
    @Index(name = "idx_llm_usage_org_created", columnList = "organization_id, created_at"),
    @Index(name = "idx_llm_usage_org_feature", columnList = "organization_id, feature_type"),
    @Index(name = "idx_llm_usage_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LLMUsage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LLMProvider provider;

    @Column(name = "model_id", nullable = false, length = 50)
    private String modelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feature_type", nullable = false, length = 30)
    private LLMFeatureType featureType;

    @Column(name = "input_tokens", nullable = false)
    private Integer inputTokens;

    @Column(name = "output_tokens", nullable = false)
    private Integer outputTokens;

    @Column(name = "total_tokens", nullable = false)
    private Integer totalTokens;

    /**
     * Estimated cost in USD cents for this request.
     * Calculated based on provider pricing at time of request.
     */
    @Column(name = "estimated_cost_cents")
    private Integer estimatedCostCents;

    /**
     * Request latency in milliseconds.
     */
    @Column(name = "latency_ms")
    private Integer latencyMs;

    /**
     * Whether the request was successful.
     */
    @Column(nullable = false)
    @Builder.Default
    private Boolean success = true;

    /**
     * Error message if request failed.
     */
    @Column(name = "error_message", length = 500)
    private String errorMessage;

    /**
     * Optional reference to the entity this request was about (e.g., anomaly ID).
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (totalTokens == null && inputTokens != null && outputTokens != null) {
            totalTokens = inputTokens + outputTokens;
        }
    }
}
