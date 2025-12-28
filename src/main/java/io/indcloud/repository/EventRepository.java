package io.indcloud.repository;

import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    // Find all events for an organization with pagination
    Page<Event> findByOrganizationOrderByCreatedAtDesc(Organization organization, Pageable pageable);

    // Find events by type
    Page<Event> findByOrganizationAndEventTypeOrderByCreatedAtDesc(
            Organization organization,
            Event.EventType eventType,
            Pageable pageable
    );

    // Find events by severity
    Page<Event> findByOrganizationAndSeverityOrderByCreatedAtDesc(
            Organization organization,
            Event.EventSeverity severity,
            Pageable pageable
    );

    // Find events by device
    Page<Event> findByOrganizationAndDeviceIdOrderByCreatedAtDesc(
            Organization organization,
            String deviceId,
            Pageable pageable
    );

    // Find events by entity type and ID
    Page<Event> findByOrganizationAndEntityTypeAndEntityIdOrderByCreatedAtDesc(
            Organization organization,
            String entityType,
            String entityId,
            Pageable pageable
    );

    // Find events within a time range
    Page<Event> findByOrganizationAndCreatedAtBetweenOrderByCreatedAtDesc(
            Organization organization,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    );

    // Complex query with multiple filters
    // Note: Using CAST for timestamp parameters to fix PostgreSQL null parameter type inference issue
    @Query("SELECT e FROM Event e WHERE e.organization = :organization " +
           "AND (:eventType IS NULL OR e.eventType = :eventType) " +
           "AND (:severity IS NULL OR e.severity = :severity) " +
           "AND (:deviceId IS NULL OR e.deviceId = :deviceId) " +
           "AND (:entityType IS NULL OR e.entityType = :entityType) " +
           "AND (CAST(:startTime AS timestamp) IS NULL OR e.createdAt >= :startTime) " +
           "AND (CAST(:endTime AS timestamp) IS NULL OR e.createdAt <= :endTime) " +
           "ORDER BY e.createdAt DESC")
    Page<Event> findByFilters(
            @Param("organization") Organization organization,
            @Param("eventType") Event.EventType eventType,
            @Param("severity") Event.EventSeverity severity,
            @Param("deviceId") String deviceId,
            @Param("entityType") String entityType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    // Get recent events (last N hours)
    @Query("SELECT e FROM Event e WHERE e.organization = :organization " +
           "AND e.createdAt >= :since ORDER BY e.createdAt DESC")
    List<Event> findRecentEvents(
            @Param("organization") Organization organization,
            @Param("since") LocalDateTime since
    );

    // Count events by type for statistics
    @Query("SELECT e.eventType, COUNT(e) FROM Event e WHERE e.organization = :organization " +
           "AND e.createdAt >= :since GROUP BY e.eventType")
    List<Object[]> countEventsByType(
            @Param("organization") Organization organization,
            @Param("since") LocalDateTime since
    );

    // Count events by severity for statistics
    @Query("SELECT e.severity, COUNT(e) FROM Event e WHERE e.organization = :organization " +
           "AND e.createdAt >= :since GROUP BY e.severity")
    List<Object[]> countEventsBySeverity(
            @Param("organization") Organization organization,
            @Param("since") LocalDateTime since
    );

    // Delete old events (data retention policy)
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
