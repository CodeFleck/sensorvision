package org.sensorvision.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "global_rules")
public class GlobalRule extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    // Device selection criteria
    @Enumerated(EnumType.STRING)
    @Column(name = "selector_type", nullable = false, length = 50)
    private DeviceSelectorType selectorType;

    @Column(name = "selector_value", columnDefinition = "TEXT")
    private String selectorValue;

    // Aggregation function and condition
    @Column(name = "aggregation_function", nullable = false, length = 100)
    private String aggregationFunction;

    @Column(name = "aggregation_variable", length = 100)
    private String aggregationVariable;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "aggregation_params", columnDefinition = "JSONB")
    private Map<String, Object> aggregationParams;

    // Condition
    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 10)
    private RuleOperator operator;

    @Column(name = "threshold", nullable = false, precision = 15, scale = 6)
    private BigDecimal threshold;

    // Evaluation settings
    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "evaluation_interval", nullable = false, length = 50)
    @Builder.Default
    private String evaluationInterval = "5m";

    @Column(name = "cooldown_minutes", nullable = false)
    @Builder.Default
    private Integer cooldownMinutes = 5;

    // Metadata
    @Column(name = "last_evaluated_at")
    private Instant lastEvaluatedAt;

    @Column(name = "last_triggered_at")
    private Instant lastTriggeredAt;

    // Notification settings
    @Column(name = "send_sms")
    @Builder.Default
    private Boolean sendSms = false;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "sms_recipients", columnDefinition = "TEXT[]")
    private String[] smsRecipients;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
