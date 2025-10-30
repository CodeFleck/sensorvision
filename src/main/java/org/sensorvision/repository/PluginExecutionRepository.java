package org.sensorvision.repository;

import org.sensorvision.model.PluginExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface PluginExecutionRepository extends JpaRepository<PluginExecution, Long> {

    @Query("SELECT pe FROM PluginExecution pe JOIN FETCH pe.plugin WHERE pe.plugin.id = :pluginId ORDER BY pe.executedAt DESC")
    Page<PluginExecution> findByPluginIdOrderByExecutedAtDesc(@Param("pluginId") Long pluginId, Pageable pageable);

    List<PluginExecution> findByPluginIdAndExecutedAtAfter(Long pluginId, Instant after);

    Page<PluginExecution> findByPluginOrganizationIdOrderByExecutedAtDesc(Long organizationId, Pageable pageable);
}
