package io.indcloud.repository;

import io.indcloud.model.GlobalAlert;
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
public interface GlobalAlertRepository extends JpaRepository<GlobalAlert, UUID> {

    /**
     * Find all global alerts for an organization
     */
    Page<GlobalAlert> findByOrganizationId(Long organizationId, Pageable pageable);

    /**
     * Find unacknowledged global alerts for an organization
     */
    Page<GlobalAlert> findByOrganizationIdAndAcknowledgedFalse(Long organizationId, Pageable pageable);

    /**
     * Find recent alerts for a specific global rule
     */
    @Query("SELECT ga FROM GlobalAlert ga WHERE ga.globalRule.id = :ruleId " +
           "AND ga.triggeredAt >= :since ORDER BY ga.triggeredAt DESC")
    List<GlobalAlert> findRecentAlertsForRule(@Param("ruleId") UUID ruleId, @Param("since") Instant since);

    /**
     * Count unacknowledged alerts for an organization
     */
    long countByOrganizationIdAndAcknowledgedFalse(Long organizationId);

    /**
     * Count unresolved alerts for an organization
     */
    long countByOrganizationIdAndResolvedFalse(Long organizationId);

    /**
     * Find all alerts for a specific global rule
     */
    Page<GlobalAlert> findByGlobalRuleId(UUID globalRuleId, Pageable pageable);
}
