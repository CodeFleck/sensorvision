package org.sensorvision.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByExternalId(String externalId);

    Optional<Device> findByApiToken(String apiToken);

    List<Device> findAllByApiTokenIsNotNull();

    List<Device> findByOrganizationId(Long organizationId);

    List<Device> findByOrganization(Organization organization);

    @Query("SELECT COUNT(t) > 0 FROM TelemetryRecord t WHERE t.device.externalId = :externalId")
    boolean hasTelemetry(@Param("externalId") String externalId);
}
