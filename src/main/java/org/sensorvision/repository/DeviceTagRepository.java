package org.sensorvision.repository;

import org.sensorvision.model.DeviceTag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DeviceTagRepository extends JpaRepository<DeviceTag, Long> {

    List<DeviceTag> findByOrganizationId(Long organizationId);

    Optional<DeviceTag> findByOrganizationIdAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
