package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "geofence_assignments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@IdClass(GeofenceAssignmentId.class)
public class GeofenceAssignment {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "geofence_id", nullable = false)
    private Geofence geofence;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @Column(name = "alert_on_enter", nullable = false)
    @Builder.Default
    private Boolean alertOnEnter = true;

    @Column(name = "alert_on_exit", nullable = false)
    @Builder.Default
    private Boolean alertOnExit = true;

    @Column(name = "assigned_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime assignedAt = LocalDateTime.now();

    @Transient
    private Boolean deviceCurrentlyInside;
}
