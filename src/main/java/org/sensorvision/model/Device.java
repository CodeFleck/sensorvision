package org.sensorvision.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
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
@Table(name = "devices")
public class Device extends AuditableEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "external_id", nullable = false, unique = true, length = 64)
    private String externalId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "location", length = 255)
    private String location;

    @Column(name = "sensor_type", length = 100)
    private String sensorType;

    @Column(name = "firmware_version", length = 100)
    private String firmwareVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    @Builder.Default
    private DeviceStatus status = DeviceStatus.UNKNOWN;

    @Column(name = "last_seen_at")
    private Instant lastSeenAt;

    // Geolocation fields (added by V11 migration)
    @Column(name = "latitude", precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(name = "altitude", precision = 10, scale = 2)
    private BigDecimal altitude;

    @Column(name = "location_updated_at")
    private LocalDateTime locationUpdatedAt;

    // Device API Token (for per-device authentication)
    @Column(name = "api_token", unique = true, length = 64)
    private String apiToken;

    @Column(name = "token_created_at")
    private LocalDateTime tokenCreatedAt;

    @Column(name = "token_last_used_at")
    private LocalDateTime tokenLastUsedAt;

    // Device Health Score (0-100)
    @Column(name = "health_score")
    @Builder.Default
    private Integer healthScore = 100;

    @Column(name = "last_health_check_at")
    private LocalDateTime lastHealthCheckAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id")
    private DeviceType deviceType;

    // Device Groups
    @ManyToMany(mappedBy = "devices")
    @Builder.Default
    private Set<DeviceGroup> groups = new HashSet<>();

    // Device Tags
    @ManyToMany
    @JoinTable(
            name = "device_tag_assignments",
            joinColumns = @JoinColumn(name = "device_id"),
            inverseJoinColumns = @JoinColumn(name = "tag_id")
    )
    @Builder.Default
    private Set<DeviceTag> tags = new HashSet<>();

    // Custom Properties
    @OneToMany(mappedBy = "device", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<DeviceProperty> properties = new HashSet<>();

    @PrePersist
    void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
