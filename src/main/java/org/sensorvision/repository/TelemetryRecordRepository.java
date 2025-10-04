package org.sensorvision.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
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
}
