package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "device_types")
public class DeviceType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "deviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceTypeVariable> variables = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    /**
     * Add a variable to this device type
     */
    public void addVariable(DeviceTypeVariable variable) {
        variables.add(variable);
        variable.setDeviceType(this);
    }

    /**
     * Remove a variable from this device type
     */
    public void removeVariable(DeviceTypeVariable variable) {
        variables.remove(variable);
        variable.setDeviceType(null);
    }
}
