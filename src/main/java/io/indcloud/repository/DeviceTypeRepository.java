package io.indcloud.repository;

import io.indcloud.model.DeviceType;
import io.indcloud.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {

    List<DeviceType> findByOrganization(Organization organization);

    List<DeviceType> findByOrganizationAndIsActiveTrue(Organization organization);

    Optional<DeviceType> findByOrganizationAndName(Organization organization, String name);

    boolean existsByOrganizationAndName(Organization organization, String name);
}
