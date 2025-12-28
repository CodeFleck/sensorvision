package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

/**
 * Audit log for soft-deleted items.
 * Tracks deletion, restoration, and permanent deletion events.
 * Items can be restored within 30 days of deletion.
 */
@Entity
@Table(name = "trash_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrashLog {

    public static final int RETENTION_DAYS = 30;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "entity_type", nullable = false, length = 50)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private String entityId;

    @Column(name = "entity_name")
    private String entityName;

    /**
     * JSON snapshot of the entity at deletion time.
     * Used to display entity details in trash view and potentially for restore.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_snapshot", columnDefinition = "jsonb")
    private Map<String, Object> entitySnapshot;

    @Column(name = "deleted_at", nullable = false)
    private Instant deletedAt;

    @Column(name = "deleted_by", nullable = false)
    private String deletedBy;

    @Column(name = "deletion_reason", length = 500)
    private String deletionReason;

    @Column(name = "restored_at")
    private Instant restoredAt;

    @Column(name = "restored_by")
    private String restoredBy;

    @Column(name = "permanently_deleted_at")
    private Instant permanentlyDeletedAt;

    /**
     * When this item expires and will be permanently deleted.
     * Calculated as deletedAt + 30 days.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id")
    private Organization organization;

    /**
     * Check if this item can still be restored.
     * @return true if not yet restored and not permanently deleted
     */
    public boolean canRestore() {
        return restoredAt == null && permanentlyDeletedAt == null;
    }

    /**
     * Check if this item has been restored.
     * @return true if restored
     */
    public boolean isRestored() {
        return restoredAt != null;
    }

    /**
     * Check if this item has been permanently deleted.
     * @return true if permanently deleted
     */
    public boolean isPermanentlyDeleted() {
        return permanentlyDeletedAt != null;
    }

    /**
     * Get the number of days remaining before permanent deletion.
     * @return days remaining, or 0 if already expired
     */
    public long getDaysRemaining() {
        if (permanentlyDeletedAt != null || restoredAt != null) {
            return 0;
        }
        long remaining = java.time.Duration.between(Instant.now(), expiresAt).toDays();
        return Math.max(0, remaining);
    }

    /**
     * Create a TrashLog entry for a soft-deleted entity.
     */
    public static TrashLog createFor(SoftDeletable entity, String deletedBy, String reason,
                                     Map<String, Object> snapshot, Organization org) {
        Instant now = Instant.now();
        return TrashLog.builder()
                .entityType(entity.getEntityType())
                .entityId(entity.getEntityId())
                .entityName(entity.getEntityName())
                .entitySnapshot(snapshot)
                .deletedAt(now)
                .deletedBy(deletedBy)
                .deletionReason(reason)
                .expiresAt(now.plus(java.time.Duration.ofDays(RETENTION_DAYS)))
                .organization(org)
                .build();
    }
}
