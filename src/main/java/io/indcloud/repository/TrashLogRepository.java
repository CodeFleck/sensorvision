package io.indcloud.repository;

import io.indcloud.model.TrashLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface TrashLogRepository extends JpaRepository<TrashLog, Long> {

    /**
     * Find all active trash items (not restored, not permanently deleted).
     */
    @Query("SELECT t FROM TrashLog t WHERE t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL ORDER BY t.deletedAt DESC")
    List<TrashLog> findAllActive();

    /**
     * Find active trash items with pagination.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    Page<TrashLog> findAllActive(Pageable pageable);

    /**
     * Find active trash items by entity type.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.entityType = :entityType AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL ORDER BY t.deletedAt DESC")
    List<TrashLog> findActiveByEntityType(@Param("entityType") String entityType);

    /**
     * Find active trash items by organization.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.organization.id = :organizationId AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL ORDER BY t.deletedAt DESC")
    List<TrashLog> findActiveByOrganization(@Param("organizationId") Long organizationId);

    /**
     * Find a specific trash item by entity type and ID.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.entityType = :entityType AND t.entityId = :entityId AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    Optional<TrashLog> findActiveByEntityTypeAndId(@Param("entityType") String entityType, @Param("entityId") String entityId);

    /**
     * Find items that have expired and should be permanently deleted.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.expiresAt < :now AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    List<TrashLog> findExpiredItems(@Param("now") Instant now);

    /**
     * Count active items by entity type.
     */
    @Query("SELECT COUNT(t) FROM TrashLog t WHERE t.entityType = :entityType AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    long countActiveByEntityType(@Param("entityType") String entityType);

    /**
     * Count all active trash items.
     */
    @Query("SELECT COUNT(t) FROM TrashLog t WHERE t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    long countAllActive();

    /**
     * Find trash item by ID that is still restorable.
     */
    @Query("SELECT t FROM TrashLog t WHERE t.id = :id AND t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL")
    Optional<TrashLog> findRestorableById(@Param("id") Long id);

    /**
     * Get statistics grouped by entity type.
     */
    @Query("SELECT t.entityType, COUNT(t) FROM TrashLog t WHERE t.restoredAt IS NULL AND t.permanentlyDeletedAt IS NULL GROUP BY t.entityType")
    List<Object[]> getTrashStatsByEntityType();
}
