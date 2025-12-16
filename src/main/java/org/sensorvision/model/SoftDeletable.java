package org.sensorvision.model;

import java.time.Instant;

/**
 * Interface for entities that support soft delete functionality.
 * Entities implementing this interface can be "deleted" without permanent removal,
 * allowing for restoration within a 30-day window.
 */
public interface SoftDeletable {

    /**
     * Get the timestamp when the entity was soft-deleted.
     * @return deletion timestamp, or null if not deleted
     */
    Instant getDeletedAt();

    /**
     * Set the deletion timestamp.
     * @param deletedAt the timestamp of deletion
     */
    void setDeletedAt(Instant deletedAt);

    /**
     * Get the username of the admin who deleted this entity.
     * @return username of the deleter, or null if not deleted
     */
    String getDeletedBy();

    /**
     * Set the username of the admin who deleted this entity.
     * @param deletedBy username of the deleter
     */
    void setDeletedBy(String deletedBy);

    /**
     * Get the reason for deletion (optional).
     * @return deletion reason, or null if not provided
     */
    String getDeletionReason();

    /**
     * Set the reason for deletion.
     * @param reason the deletion reason
     */
    void setDeletionReason(String reason);

    /**
     * Check if the entity is currently soft-deleted.
     * @return true if deleted, false otherwise
     */
    default boolean isDeleted() {
        return getDeletedAt() != null;
    }

    /**
     * Get a unique identifier for this entity (for trash log).
     * @return the entity's identifier as a string
     */
    String getEntityId();

    /**
     * Get a display name for this entity (for trash log).
     * @return a human-readable name
     */
    String getEntityName();

    /**
     * Get the entity type name (e.g., "USER", "DEVICE", "ORGANIZATION").
     * @return the entity type
     */
    String getEntityType();
}
