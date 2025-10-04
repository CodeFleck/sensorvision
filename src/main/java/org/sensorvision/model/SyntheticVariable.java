package org.sensorvision.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
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
@Table(name = "synthetic_variables")
public class SyntheticVariable extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "expression", nullable = false, columnDefinition = "TEXT")
    private String expression; // Formula like: "kwConsumption * voltage" or "voltage / current"

    @Column(name = "unit")
    private String unit; // Unit of measurement for the synthetic variable

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}