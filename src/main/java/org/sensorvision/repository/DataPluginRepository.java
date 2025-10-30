package org.sensorvision.repository;

import org.sensorvision.model.DataPlugin;
import org.sensorvision.model.PluginType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataPluginRepository extends JpaRepository<DataPlugin, Long> {

    Page<DataPlugin> findByOrganizationId(Long organizationId, Pageable pageable);

    List<DataPlugin> findByOrganizationIdAndEnabled(Long organizationId, Boolean enabled);

    List<DataPlugin> findByOrganizationIdAndPluginType(Long organizationId, PluginType pluginType);

    Optional<DataPlugin> findByOrganizationIdAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
