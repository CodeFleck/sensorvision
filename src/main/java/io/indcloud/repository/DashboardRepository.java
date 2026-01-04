package io.indcloud.repository;

import io.indcloud.model.Dashboard;
import io.indcloud.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashboardRepository extends JpaRepository<Dashboard, Long> {

    /**
     * Find the default dashboard
     */
    Optional<Dashboard> findByIsDefaultTrue();

    /**
     * Check if a dashboard name already exists (for uniqueness validation)
     */
    boolean existsByName(String name);

    /**
     * Find dashboard with its widgets eagerly loaded
     */
    @Query("SELECT d FROM Dashboard d LEFT JOIN FETCH d.widgets WHERE d.id = :id")
    Optional<Dashboard> findByIdWithWidgets(Long id);

    /**
     * Find all dashboards for an organization
     */
    List<Dashboard> findByOrganization(Organization organization);

    /**
     * Find dashboard by ID and organization (for ownership verification)
     */
    Optional<Dashboard> findByIdAndOrganization(Long id, Organization organization);

    /**
     * Find default dashboard for an organization
     */
    Optional<Dashboard> findByOrganizationAndIsDefaultTrue(Organization organization);

    /**
     * Find default dashboard for an organization with widgets eagerly loaded
     */
    @Query("SELECT d FROM Dashboard d LEFT JOIN FETCH d.widgets WHERE d.organization = :organization AND d.isDefault = true")
    Optional<Dashboard> findByOrganizationAndIsDefaultTrueWithWidgets(@Param("organization") Organization organization);

    /**
     * Find public dashboard by share token (with widgets loaded)
     */
    @Query("SELECT d FROM Dashboard d LEFT JOIN FETCH d.widgets WHERE d.publicShareToken = :token")
    Optional<Dashboard> findByPublicShareToken(@Param("token") String token);

    /**
     * Check if a dashboard with given name exists for an organization
     */
    boolean existsByOrganizationAndName(Organization organization, String name);
}
