package org.sensorvision.repository;

import org.sensorvision.model.OrganizationSmsSettings;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for OrganizationSmsSettings entities.
 * Manages organization-level SMS configuration and budget controls.
 */
@Repository
public interface OrganizationSmsSettingsRepository extends JpaRepository<OrganizationSmsSettings, UUID> {

    /**
     * Find SMS settings for a specific organization
     */
    Optional<OrganizationSmsSettings> findByOrganizationId(Long organizationId);

    /**
     * Find all organizations with SMS enabled
     */
    List<OrganizationSmsSettings> findByEnabledTrue();

    /**
     * Check if SMS is enabled for an organization
     */
    boolean existsByOrganizationIdAndEnabledTrue(Long organizationId);

    /**
     * Delete SMS settings for an organization
     */
    void deleteByOrganizationId(Long organizationId);
}
