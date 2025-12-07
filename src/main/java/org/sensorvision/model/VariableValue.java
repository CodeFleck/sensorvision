package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a single data point for a variable in the time-series.
 * This is the "Value" part of the EAV (Entity-Attribute-Value) pattern.
 *
 * Each record stores one value for one variable at one point in time,
 * enabling flexible storage of any telemetry data without schema changes.
 */
@Entity
@Table(name = "variable_values",
    indexes = {
        @Index(name = "idx_variable_values_variable_id", columnList = "variable_id"),
        @Index(name = "idx_variable_values_timestamp", columnList = "timestamp DESC"),
        @Index(name = "idx_variable_values_variable_timestamp", columnList = "variable_id, timestamp DESC")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"variable"})
public class VariableValue {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "variable_id", nullable = false)
    private Variable variable;

    /**
     * Timestamp when this value was recorded (from the device/source).
     */
    @Column(name = "timestamp", nullable = false)
    private Instant timestamp;

    /**
     * The actual numeric value.
     */
    @Column(name = "value", nullable = false, precision = 20, scale = 6)
    private BigDecimal value;

    /**
     * Optional context data (e.g., location, quality flags, metadata).
     * Stored as JSONB for flexibility.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "context", columnDefinition = "jsonb")
    private Map<String, Object> context;

    /**
     * Timestamp when this record was created in the database.
     */
    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }
}
