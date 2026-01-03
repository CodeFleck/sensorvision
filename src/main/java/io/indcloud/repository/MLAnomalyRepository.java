package io.indcloud.repository;

import io.indcloud.model.MLAnomaly;
import io.indcloud.model.MLAnomalySeverity;
import io.indcloud.model.MLAnomalyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MLAnomalyRepository extends JpaRepository<MLAnomaly, UUID> {

    Page<MLAnomaly> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<MLAnomaly> findByDeviceId(UUID deviceId, Pageable pageable);

    List<MLAnomaly> findByOrganizationIdAndStatus(Long organizationId, MLAnomalyStatus status);

    List<MLAnomaly> findByOrganizationIdAndSeverity(Long organizationId, MLAnomalySeverity severity);

    @Query("SELECT a FROM MLAnomaly a WHERE a.organization.id = :orgId AND a.status = :status ORDER BY a.detectedAt DESC")
    List<MLAnomaly> findByOrganizationAndStatusOrderByDetectedAtDesc(@Param("orgId") Long organizationId, @Param("status") MLAnomalyStatus status);

    default List<MLAnomaly> findNewAnomaliesByOrganization(Long organizationId) {
        return findByOrganizationAndStatusOrderByDetectedAtDesc(organizationId, MLAnomalyStatus.NEW);
    }

    @Query("SELECT a FROM MLAnomaly a WHERE a.device.id = :deviceId AND a.detectedAt BETWEEN :start AND :end ORDER BY a.detectedAt DESC")
    List<MLAnomaly> findByDeviceAndTimeRange(
            @Param("deviceId") UUID deviceId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("SELECT a FROM MLAnomaly a WHERE a.organization.id = :orgId AND a.severity IN :severities AND a.status = :status ORDER BY a.detectedAt DESC")
    List<MLAnomaly> findBySeveritiesAndStatus(
            @Param("orgId") Long organizationId,
            @Param("severities") List<MLAnomalySeverity> severities,
            @Param("status") MLAnomalyStatus status
    );

    default List<MLAnomaly> findCriticalNewAnomalies(Long organizationId, List<MLAnomalySeverity> severities) {
        return findBySeveritiesAndStatus(organizationId, severities, MLAnomalyStatus.NEW);
    }

    @Query("SELECT COUNT(a) FROM MLAnomaly a WHERE a.organization.id = :orgId AND a.status = :status")
    long countByOrganizationAndStatus(@Param("orgId") Long organizationId, @Param("status") MLAnomalyStatus status);

    default long countNewAnomaliesByOrganization(Long organizationId) {
        return countByOrganizationAndStatus(organizationId, MLAnomalyStatus.NEW);
    }

    @Query("SELECT COUNT(a) FROM MLAnomaly a WHERE a.device.id = :deviceId AND a.detectedAt >= :since")
    long countAnomaliesSince(@Param("deviceId") UUID deviceId, @Param("since") Instant since);

    @Query("SELECT a.severity, COUNT(a) FROM MLAnomaly a WHERE a.organization.id = :orgId AND a.detectedAt >= :since GROUP BY a.severity")
    List<Object[]> countBySeveritySince(@Param("orgId") Long organizationId, @Param("since") Instant since);
}
