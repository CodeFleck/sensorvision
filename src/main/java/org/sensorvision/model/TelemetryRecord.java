package org.sensorvision.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "telemetry_records", indexes = {
        @Index(name = "idx_telemetry_device_time", columnList = "device_id, measurement_timestamp DESC")
})
public class TelemetryRecord extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "device_id")
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    @Column(name = "measurement_timestamp", nullable = false)
    private Instant timestamp;

    @Column(name = "kw_consumption", precision = 12, scale = 3)
    private BigDecimal kwConsumption;

    @Column(name = "voltage", precision = 10, scale = 3)
    private BigDecimal voltage;

    @Column(name = "current", precision = 10, scale = 3)
    private BigDecimal current;

    @Column(name = "power_factor", precision = 5, scale = 3)
    private BigDecimal powerFactor;

    @Column(name = "frequency", precision = 6, scale = 3)
    private BigDecimal frequency;

    // Geolocation fields (added by V11 migration for moving devices)
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "altitude", precision = 10, scale = 2)
    private BigDecimal altitude;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    // Data retention field (added by V35 migration)
    @Builder.Default
    @Column(name = "archived", nullable = false)
    private Boolean archived = false;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
