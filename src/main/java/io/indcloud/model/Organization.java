package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "organizations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString
public class Organization extends AuditableEntity implements SoftDeletable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    @Builder.Default
    private boolean enabled = true;

    // Soft delete fields
    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Column(name = "deleted_by")
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    // SoftDeletable implementation
    @Override
    public String getEntityId() {
        return id != null ? id.toString() : null;
    }

    @Override
    public String getEntityName() {
        return name;
    }

    @Override
    public String getEntityType() {
        return "ORGANIZATION";
    }
}
