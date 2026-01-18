package io.indcloud.repository;

import io.indcloud.model.Device;
import io.indcloud.model.Variable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VariableRepository extends JpaRepository<Variable, Long> {

    // Organization-level queries (for templates/system variables)
    List<Variable> findByOrganizationId(Long organizationId);

    Optional<Variable> findByOrganizationIdAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);

    // Find organization-level templates only (device_id IS NULL)
    @Query("SELECT v FROM Variable v WHERE v.organization.id = :orgId AND v.device IS NULL")
    List<Variable> findOrganizationTemplates(@Param("orgId") Long organizationId);

    // Device-specific variable queries (EAV pattern)

    /**
     * Find a specific variable for a device by name.
     * This is the main query for auto-provisioning variables.
     */
    Optional<Variable> findByDeviceAndName(Device device, String name);

    /**
     * Find a specific variable for a device by device ID and variable name.
     */
    @Query("SELECT v FROM Variable v WHERE v.device.id = :deviceId AND v.name = :name")
    Optional<Variable> findByDeviceIdAndName(@Param("deviceId") UUID deviceId, @Param("name") String name);

    /**
     * Find all variables for a specific device.
     */
    List<Variable> findByDeviceId(UUID deviceId);

    /**
     * Find all variables for a device, ordered by name.
     * Uses JOIN FETCH to eagerly load the device association to avoid
     * LazyInitializationException when mapping to DTOs.
     */
    @Query("SELECT v FROM Variable v JOIN FETCH v.device WHERE v.device.id = :deviceId ORDER BY v.name ASC")
    List<Variable> findByDeviceIdOrderByNameAsc(@Param("deviceId") UUID deviceId);

    /**
     * Check if a variable exists for a specific device.
     */
    boolean existsByDeviceIdAndName(UUID deviceId, String name);

    /**
     * Find a variable by ID with device eagerly fetched.
     * Prevents LazyInitializationException when mapping to DTOs.
     */
    @Query("SELECT v FROM Variable v JOIN FETCH v.device WHERE v.id = :id")
    Optional<Variable> findByIdWithDevice(@Param("id") Long id);

    /**
     * Find all device-specific variables for an organization.
     */
    @Query("SELECT v FROM Variable v WHERE v.organization.id = :orgId AND v.device IS NOT NULL")
    List<Variable> findDeviceVariablesByOrganizationId(@Param("orgId") Long organizationId);

    /**
     * Find all variables (both templates and device-specific) for an organization.
     */
    @Query("SELECT v FROM Variable v WHERE v.organization.id = :orgId ORDER BY v.device.id NULLS FIRST, v.name")
    List<Variable> findAllByOrganizationIdOrdered(@Param("orgId") Long organizationId);

    /**
     * Count variables per device for statistics.
     */
    @Query("SELECT v.device.id, COUNT(v) FROM Variable v WHERE v.device IS NOT NULL GROUP BY v.device.id")
    List<Object[]> countVariablesPerDevice();

    /**
     * Find all variables for multiple devices in a single query (batch fetch to avoid N+1).
     */
    @Query("SELECT v FROM Variable v WHERE v.device.id IN :deviceIds ORDER BY v.device.id, v.name")
    List<Variable> findByDeviceIds(@Param("deviceIds") List<UUID> deviceIds);
}
