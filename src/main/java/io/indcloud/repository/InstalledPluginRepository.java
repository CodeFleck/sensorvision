package io.indcloud.repository;

import io.indcloud.model.InstalledPlugin;
import io.indcloud.model.Organization;
import io.indcloud.model.PluginInstallationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InstalledPluginRepository extends JpaRepository<InstalledPlugin, Long> {

    List<InstalledPlugin> findByOrganization(Organization organization);

    List<InstalledPlugin> findByOrganizationAndStatus(Organization organization, PluginInstallationStatus status);

    Optional<InstalledPlugin> findByOrganizationAndPluginKey(Organization organization, String pluginKey);

    @Query("SELECT COUNT(ip) FROM InstalledPlugin ip WHERE ip.pluginKey = :pluginKey")
    Long countInstallationsByPluginKey(@Param("pluginKey") String pluginKey);

    @Query("SELECT ip FROM InstalledPlugin ip WHERE ip.organization = :organization AND ip.status = 'ACTIVE'")
    List<InstalledPlugin> findActivePluginsByOrganization(@Param("organization") Organization organization);

    boolean existsByOrganizationAndPluginKey(Organization organization, String pluginKey);
}
