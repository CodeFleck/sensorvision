package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "variables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"organization_id", "name"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = {"organization"})
public class Variable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

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

    public enum DataType {
        NUMBER,
        BOOLEAN,
        STRING,
        JSON
    }
}
