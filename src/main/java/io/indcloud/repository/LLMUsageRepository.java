package io.indcloud.repository;

import io.indcloud.model.LLMFeatureType;
import io.indcloud.model.LLMProvider;
import io.indcloud.model.LLMUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repository for LLM usage tracking and billing queries.
 */
@Repository
public interface LLMUsageRepository extends JpaRepository<LLMUsage, UUID> {

    /**
     * Find all usage records for an organization.
     */
    Page<LLMUsage> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    /**
     * Find usage records for an organization within a date range.
     */
    List<LLMUsage> findByOrganizationIdAndCreatedAtBetweenOrderByCreatedAtDesc(
            Long organizationId, Instant startDate, Instant endDate);

    /**
     * Find usage records for a specific user.
     */
    Page<LLMUsage> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * Get total token usage for an organization in a period.
     */
    @Query("SELECT COALESCE(SUM(u.totalTokens), 0) FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since")
    Long getTotalTokensForOrganization(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Get total estimated cost for an organization in a period.
     */
    @Query("SELECT COALESCE(SUM(u.estimatedCostCents), 0) FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since")
    Long getTotalCostCentsForOrganization(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Get usage breakdown by provider for an organization.
     */
    @Query("SELECT u.provider, COALESCE(SUM(u.totalTokens), 0), COALESCE(SUM(u.estimatedCostCents), 0), COUNT(u) " +
           "FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since " +
           "GROUP BY u.provider")
    List<Object[]> getUsageByProvider(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Get usage breakdown by feature type for an organization.
     */
    @Query("SELECT u.featureType, COALESCE(SUM(u.totalTokens), 0), COALESCE(SUM(u.estimatedCostCents), 0), COUNT(u) " +
           "FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since " +
           "GROUP BY u.featureType")
    List<Object[]> getUsageByFeature(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Get daily usage for an organization (for charts).
     */
    @Query(value = "SELECT DATE(created_at) as day, " +
                   "COALESCE(SUM(total_tokens), 0) as tokens, " +
                   "COALESCE(SUM(estimated_cost_cents), 0) as cost, " +
                   "COUNT(*) as requests " +
                   "FROM llm_usage " +
                   "WHERE organization_id = :orgId AND created_at >= :since " +
                   "GROUP BY DATE(created_at) " +
                   "ORDER BY day",
           nativeQuery = true)
    List<Object[]> getDailyUsage(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Count successful requests for an organization.
     */
    @Query("SELECT COUNT(u) FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since AND u.success = true")
    Long countSuccessfulRequests(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Count failed requests for an organization.
     */
    @Query("SELECT COUNT(u) FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since AND u.success = false")
    Long countFailedRequests(@Param("orgId") Long organizationId, @Param("since") Instant since);

    /**
     * Get average latency for an organization.
     */
    @Query("SELECT COALESCE(AVG(u.latencyMs), 0) FROM LLMUsage u " +
           "WHERE u.organization.id = :orgId AND u.createdAt >= :since AND u.success = true")
    Double getAverageLatency(@Param("orgId") Long organizationId, @Param("since") Instant since);
}
