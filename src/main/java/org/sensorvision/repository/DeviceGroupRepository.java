package org.sensorvision.repository;

import org.sensorvision.model.DeviceGroup;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceGroupRepository extends JpaRepository<DeviceGroup, Long> {

    List<DeviceGroup> findByOrganizationId(Long organizationId);

    Optional<DeviceGroup> findByOrganizationIdAndName(Long organizationId, String name);

    @Query("SELECT g FROM DeviceGroup g JOIN g.devices d WHERE d.externalId = :deviceId")
    List<DeviceGroup> findByDeviceExternalId(@Param("deviceId") String deviceId);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
