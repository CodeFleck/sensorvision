package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "device_properties")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = "device")
public class DeviceProperty extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "property_key", nullable = false, length = 100)
    private String key;

    @Column(name = "property_value", columnDefinition = "TEXT")
    private String value;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", length = 50)
    @Builder.Default
    private DataType dataType = DataType.STRING;

    public enum DataType {
        STRING,
        NUMBER,
        BOOLEAN,
        JSON
    }
}
