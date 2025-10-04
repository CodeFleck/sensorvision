package org.sensorvision.repository;

import java.util.Optional;
import java.util.UUID;
import org.sensorvision.model.Device;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByExternalId(String externalId);

    @Query("SELECT COUNT(t) > 0 FROM TelemetryRecord t WHERE t.device.externalId = :externalId")
    boolean hasTelemetry(@Param("externalId") String externalId);
}
