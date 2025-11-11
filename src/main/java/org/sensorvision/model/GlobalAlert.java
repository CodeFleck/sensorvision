package org.sensorvision.model;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
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
@Table(name = "global_alerts")
public class GlobalAlert extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "global_rule_id", nullable = false)
    private GlobalRule globalRule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    @Builder.Default
    private AlertSeverity severity = AlertSeverity.MEDIUM;

    // Aggregation result at trigger time
    @Column(name = "triggered_value", precision = 15, scale = 6)
    private BigDecimal triggeredValue;

    @Column(name = "device_count")
    private Integer deviceCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "affected_devices", columnDefinition = "JSONB")
    private List<UUID> affectedDevices;

    // Alert lifecycle
    @Column(name = "triggered_at", nullable = false)
    @Builder.Default
    private Instant triggeredAt = Instant.now();

    @Column(name = "acknowledged", nullable = false)
    @Builder.Default
    private Boolean acknowledged = false;

    @Column(name = "acknowledged_at")
    private Instant acknowledgedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;

    // Resolution tracking
    @Column(name = "resolved", nullable = false)
    @Builder.Default
    private Boolean resolved = false;

    @Column(name = "resolved_at")
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resolved_by")
    private User resolvedBy;

    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
