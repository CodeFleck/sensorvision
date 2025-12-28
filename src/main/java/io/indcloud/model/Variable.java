package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Represents a variable definition that can be either:
 * - Organization-level template (device_id IS NULL, is_system_variable = true)
 * - Device-specific variable (device_id IS NOT NULL, auto-created on first telemetry)
 *
 * This supports the EAV (Entity-Attribute-Value) pattern for dynamic variables
 * similar to Ubidots, where devices can send any variable without schema changes.
 */
@Entity
@Table(name = "variables")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = {"organization", "device"})
public class Variable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    /**
     * Device this variable belongs to. NULL for organization-level templates.
     * When set, this variable is device-specific and stores actual telemetry data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id")
    private Device device;

    @Column(nullable = false, length = 100)
    private String name;  // Internal identifier (e.g., kwConsumption, temperature)

    @Column(name = "display_name", length = 255)
    private String displayName;  // Human-readable name

    @Column(length = 500)
    private String description;

    @Column(length = 50)
    private String unit;  // kW, °C, V, A, Hz

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    @Builder.Default
    private DataType dataType = DataType.NUMBER;

    @Column(length = 100)
    private String icon;

    @Column(length = 7)
    private String color;  // Hex color code like #FF5733

    @Column(name = "min_value", columnDefinition = "numeric")
    private Double minValue;

    @Column(name = "max_value", columnDefinition = "numeric")
    private Double maxValue;

    @Column(name = "decimal_places")
    @Builder.Default
    private Integer decimalPlaces = 2;

    @Column(name = "is_system_variable", nullable = false)
    @Builder.Default
    private Boolean isSystemVariable = false;

    /**
     * How the variable was created:
     * - 'manual': User created via UI/API
     * - 'auto': Auto-provisioned from incoming telemetry data
     * - 'synthetic': Calculated from other variables
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "data_source", length = 50)
    @Builder.Default
    private DataSource dataSource = DataSource.MANUAL;

    /**
     * Most recent value for quick access without querying time-series table.
     */
    @Column(name = "last_value", precision = 20, scale = 6)
    private BigDecimal lastValue;

    /**
     * Timestamp of the most recent value.
     */
    @Column(name = "last_value_at")
    private Instant lastValueAt;

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

    /**
     * Returns true if this is a device-specific variable (not an org-level template).
     */
    public boolean isDeviceVariable() {
        return device != null;
    }

    /**
     * Returns the display name, falling back to name if not set.
     */
    public String getEffectiveDisplayName() {
        return displayName != null && !displayName.isBlank() ? displayName : name;
    }

    public enum DataType {
        NUMBER,
        BOOLEAN,
        STRING,
        JSON
    }

    public enum DataSource {
        MANUAL,    // User created via UI/API
        AUTO,      // Auto-provisioned from incoming telemetry
        SYNTHETIC  // Calculated from other variables
    }
}
