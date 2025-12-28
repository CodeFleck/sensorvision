package io.indcloud.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
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
@Table(name = "synthetic_variable_values")
public class SyntheticVariableValue extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "synthetic_variable_id", nullable = false)
    private SyntheticVariable syntheticVariable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "telemetry_record_id", nullable = false)
    private TelemetryRecord telemetryRecord;

    @Column(name = "calculated_value", precision = 15, scale = 6)
    private BigDecimal calculatedValue;

    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}