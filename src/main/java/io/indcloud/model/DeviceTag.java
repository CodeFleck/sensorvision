package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "device_tags")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = {"organization", "devices"})
public class DeviceTag {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 7)
    private String color;  // Hex color code like #FF5733

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @ManyToMany(mappedBy = "tags")
    @Builder.Default
    private Set<Device> devices = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
