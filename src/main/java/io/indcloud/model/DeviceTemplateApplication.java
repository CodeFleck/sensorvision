package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Tracks which device type templates have been applied to which devices.
 * This enables auditing and prevents duplicate application of templates.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "device_template_applications",
       uniqueConstraints = @UniqueConstraint(columnNames = {"device_id", "device_type_id"}))
public class DeviceTemplateApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_id", nullable = false)
    private UUID deviceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    @Column(name = "applied_by", length = 255)
    private String appliedBy;

    @Column(name = "variables_created", nullable = false)
    @Builder.Default
    private Integer variablesCreated = 0;

    @Column(name = "rules_created", nullable = false)
    @Builder.Default
    private Integer rulesCreated = 0;

    @Column(name = "dashboard_created", nullable = false)
    @Builder.Default
    private Boolean dashboardCreated = false;

    @PrePersist
    protected void onCreate() {
        if (appliedAt == null) {
            appliedAt = Instant.now();
        }
    }
}
