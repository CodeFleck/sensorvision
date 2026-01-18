package io.indcloud.repository;

import io.indcloud.model.VariableValue;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for VariableValue entities - the time-series storage for EAV pattern.
 * Optimized for time-series queries and aggregations.
 */
@Repository
public interface VariableValueRepository extends JpaRepository<VariableValue, UUID> {

    /**
     * Find all values for a variable, ordered by timestamp descending (most recent first).
     */
    List<VariableValue> findByVariableIdOrderByTimestampDesc(Long variableId);

    /**
     * Find values for a variable with pagination.
     */
    Page<VariableValue> findByVariableIdOrderByTimestampDesc(Long variableId, Pageable pageable);

    /**
     * Find the most recent value for a variable.
     */
    Optional<VariableValue> findFirstByVariableIdOrderByTimestampDesc(Long variableId);

    /**
     * Find values for a variable within a time range.
     */
    @Query("SELECT v FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime " +
           "ORDER BY v.timestamp ASC")
    List<VariableValue> findByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find values for a variable within a time range (descending order).
     */
    @Query("SELECT v FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime " +
           "ORDER BY v.timestamp DESC")
    List<VariableValue> findByVariableIdAndTimeRangeDesc(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find values for multiple variables within a time range (for charting).
     */
    @Query("SELECT v FROM VariableValue v WHERE v.variable.id IN :variableIds " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime " +
           "ORDER BY v.timestamp ASC")
    List<VariableValue> findByVariableIdsAndTimeRange(
            @Param("variableIds") List<Long> variableIds,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find the latest N values for a variable.
     */
    @Query("SELECT v FROM VariableValue v WHERE v.variable.id = :variableId " +
           "ORDER BY v.timestamp DESC")
    List<VariableValue> findLatestByVariableId(@Param("variableId") Long variableId, Pageable pageable);

    /**
     * Count values for a variable.
     */
    long countByVariableId(Long variableId);

    /**
     * Count values for a variable within a time range.
     */
    @Query("SELECT COUNT(v) FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime")
    long countByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    // Aggregation queries for analytics

    /**
     * Calculate average value for a variable within a time range.
     */
    @Query("SELECT AVG(v.value) FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime")
    Double calculateAverageByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate min value for a variable within a time range.
     */
    @Query("SELECT MIN(v.value) FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime")
    Double calculateMinByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate max value for a variable within a time range.
     */
    @Query("SELECT MAX(v.value) FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime")
    Double calculateMaxByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Calculate sum for a variable within a time range.
     */
    @Query("SELECT SUM(v.value) FROM VariableValue v WHERE v.variable.id = :variableId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime")
    Double calculateSumByVariableIdAndTimeRange(
            @Param("variableId") Long variableId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Find all values for a device's variables within a time range.
     * Useful for getting all telemetry for a device.
     */
    @Query("SELECT v FROM VariableValue v WHERE v.variable.device.id = :deviceId " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime " +
           "ORDER BY v.timestamp ASC")
    List<VariableValue> findByDeviceIdAndTimeRange(
            @Param("deviceId") UUID deviceId,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Delete old values for data retention.
     */
    @Modifying
    @Query("DELETE FROM VariableValue v WHERE v.variable.id = :variableId AND v.timestamp < :cutoffTime")
    void deleteOldValues(@Param("variableId") Long variableId, @Param("cutoffTime") Instant cutoffTime);

    /**
     * Delete all values for a variable.
     */
    void deleteByVariableId(Long variableId);

    /**
     * Batch calculate statistics (avg, min, max, count) for multiple variables in a single query.
     * Returns Object[] with: [variableId, avg, min, max, count]
     * This avoids N+1 queries when processing multiple variables.
     */
    @Query("SELECT v.variable.id, AVG(v.value), MIN(v.value), MAX(v.value), COUNT(v) " +
           "FROM VariableValue v WHERE v.variable.id IN :variableIds " +
           "AND v.timestamp >= :startTime AND v.timestamp <= :endTime " +
           "GROUP BY v.variable.id")
    List<Object[]> calculateBatchStatistics(
            @Param("variableIds") List<Long> variableIds,
            @Param("startTime") Instant startTime,
            @Param("endTime") Instant endTime);

    /**
     * Batch fetch the latest value for multiple variables in a single query.
     * Uses a subquery to find max timestamp per variable.
     */
    @Query("SELECT vv FROM VariableValue vv WHERE vv.id IN (" +
           "SELECT v.id FROM VariableValue v WHERE v.variable.id IN :variableIds " +
           "AND v.timestamp = (SELECT MAX(v2.timestamp) FROM VariableValue v2 WHERE v2.variable.id = v.variable.id))")
    List<VariableValue> findLatestValuesByVariableIds(@Param("variableIds") List<Long> variableIds);
}
