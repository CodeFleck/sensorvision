package org.sensorvision.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DeviceRepository extends JpaRepository<Device, UUID> {

    Optional<Device> findByExternalId(String externalId);

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.organization WHERE d.externalId = :externalId")
    Optional<Device> findByExternalIdWithOrganization(@Param("externalId") String externalId);

    Optional<Device> findByApiToken(String apiToken);

    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.organization WHERE d.apiToken = :apiToken")
    Optional<Device> findByApiTokenWithOrganization(@Param("apiToken") String apiToken);

    List<Device> findAllByApiTokenIsNotNull();

    List<Device> findByOrganizationId(Long organizationId);

    List<Device> findByOrganization(Organization organization);

    @Query("SELECT d FROM Device d WHERE d.organization.id = :organizationId AND d.active = true")
    List<Device> findByOrganizationIdAndActiveTrue(@Param("organizationId") Long organizationId);

    @Query("SELECT COUNT(t) > 0 FROM TelemetryRecord t WHERE t.device.externalId = :externalId")
    boolean hasTelemetry(@Param("externalId") String externalId);

    // Admin dashboard methods
    List<Device> findTop10ByOrderByCreatedAtDesc();

    @Query("SELECT DISTINCT d FROM Device d JOIN d.tags t WHERE d.organization = :organization AND t.name = :tagName")
    List<Device> findByOrganizationAndTagName(@Param("organization") Organization organization,
            @Param("tagName") String tagName);

    @Query("SELECT DISTINCT d FROM Device d JOIN d.groups g WHERE d.organization = :organization AND g.id = :groupId")
    List<Device> findByOrganizationAndGroupId(@Param("organization") Organization organization,
            @Param("groupId") Long groupId);

    // Soft delete support
    @Query("SELECT d FROM Device d LEFT JOIN FETCH d.organization WHERE d.deletedAt IS NULL")
    List<Device> findAllActive();

    @Query("SELECT d FROM Device d WHERE d.organization = :organization AND d.deletedAt IS NULL")
    List<Device> findActiveByOrganization(@Param("organization") Organization organization);

    @Query("SELECT d FROM Device d WHERE d.id = :id AND d.deletedAt IS NULL")
    Optional<Device> findActiveById(@Param("id") UUID id);

    @Query("SELECT d FROM Device d WHERE d.externalId = :externalId AND d.deletedAt IS NULL")
    Optional<Device> findActiveByExternalId(@Param("externalId") String externalId);
}
