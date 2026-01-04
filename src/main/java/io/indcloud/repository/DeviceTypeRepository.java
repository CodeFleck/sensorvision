package io.indcloud.repository;

import io.indcloud.model.DeviceType;
import io.indcloud.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTypeRepository extends JpaRepository<DeviceType, Long> {

    List<DeviceType> findByOrganization(Organization organization);

    List<DeviceType> findByOrganizationAndIsActiveTrue(Organization organization);

    Optional<DeviceType> findByOrganizationAndName(Organization organization, String name);

    boolean existsByOrganizationAndName(Organization organization, String name);

    // Find system templates (available to all organizations)
    List<DeviceType> findByIsSystemTemplateTrue();

    // Find by category
    List<DeviceType> findByOrganizationAndTemplateCategory(Organization organization, DeviceType.TemplateCategory category);

    // Find active templates including system templates
    @Query("SELECT dt FROM DeviceType dt WHERE dt.isActive = true AND (dt.organization = :org OR dt.isSystemTemplate = true)")
    List<DeviceType> findAvailableTemplates(@Param("org") Organization organization);

    // Find by category including system templates
    @Query("SELECT dt FROM DeviceType dt WHERE dt.isActive = true AND dt.templateCategory = :category AND (dt.organization = :org OR dt.isSystemTemplate = true)")
    List<DeviceType> findAvailableTemplatesByCategory(@Param("org") Organization organization, @Param("category") DeviceType.TemplateCategory category);
}
