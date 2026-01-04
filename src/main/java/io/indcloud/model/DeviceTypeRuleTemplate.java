package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Template for rules that will be automatically created when a device
 * is assigned to this device type.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "device_type_rule_templates")
public class DeviceTypeRuleTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "variable_name", nullable = false, length = 100)
    private String variableName;

    @Enumerated(EnumType.STRING)
    @Column(name = "operator", nullable = false, length = 20)
    private RuleOperator operator;

    @Column(name = "threshold_value", nullable = false, precision = 20, scale = 6)
    private BigDecimal thresholdValue;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private RuleSeverity severity = RuleSeverity.WARNING;

    @Column(name = "notification_message", columnDefinition = "TEXT")
    private String notificationMessage;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum RuleOperator {
        GT,   // Greater than
        GTE,  // Greater than or equal
        LT,   // Less than
        LTE,  // Less than or equal
        EQ    // Equal
    }

    public enum RuleSeverity {
        INFO,
        WARNING,
        CRITICAL
    }
}
