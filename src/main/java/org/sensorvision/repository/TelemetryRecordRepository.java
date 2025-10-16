package org.sensorvision.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.model.TelemetryRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TelemetryRecordRepository extends JpaRepository<TelemetryRecord, UUID> {

    List<TelemetryRecord> findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
            String externalId,
            Instant from,
            Instant to
    );

    List<TelemetryRecord> findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
            String externalId,
            Instant from,
            Instant to
    );

    Page<TelemetryRecord> findByDeviceExternalIdOrderByTimestampDesc(String externalId, Pageable pageable);

    @Query("""
            SELECT t FROM TelemetryRecord t
            WHERE t.device.externalId IN :externalIds
              AND t.timestamp = (
                  SELECT MAX(t2.timestamp) FROM TelemetryRecord t2 WHERE t2.device = t.device
              )
            """)
    List<TelemetryRecord> findLatestForDevices(@Param("externalIds") Collection<String> externalIds);

    // Location history queries
    Page<TelemetryRecord> findByDeviceId(UUID deviceId, Pageable pageable);

    @Query("SELECT t FROM TelemetryRecord t WHERE t.device.id = :deviceId AND t.timestamp BETWEEN :startTime AND :endTime")
    Page<TelemetryRecord> findByDeviceIdAndTimestampBetween(
            @Param("deviceId") UUID deviceId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );

    // Report generation queries
    List<TelemetryRecord> findByDeviceAndTimestampBetween(
            Device device,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @Query("SELECT t FROM TelemetryRecord t WHERE t.device.organization = :organization AND t.timestamp BETWEEN :startTime AND :endTime ORDER BY t.timestamp DESC")
    List<TelemetryRecord> findByDeviceOrganizationAndTimestampBetween(
            @Param("organization") Organization organization,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
