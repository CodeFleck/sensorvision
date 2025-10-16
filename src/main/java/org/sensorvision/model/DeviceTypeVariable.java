package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "device_type_variables")
public class DeviceTypeVariable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "label", nullable = false, length = 200)
    private String label;

    @Column(name = "unit", length = 50)
    private String unit;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 20)
    private VariableDataType dataType;

    @Column(name = "min_value", precision = 20, scale = 6)
    private BigDecimal minValue;

    @Column(name = "max_value", precision = 20, scale = 6)
    private BigDecimal maxValue;

    @Column(name = "required", nullable = false)
    @Builder.Default
    private Boolean required = false;

    @Column(name = "default_value", length = 500)
    private String defaultValue;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "display_order")
    private Integer displayOrder;

    public enum VariableDataType {
        NUMBER,
        STRING,
        BOOLEAN,
        LOCATION,
        DATETIME,
        JSON
    }
}
