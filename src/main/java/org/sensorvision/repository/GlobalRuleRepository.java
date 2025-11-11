package org.sensorvision.repository;

import org.sensorvision.model.GlobalRule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface GlobalRuleRepository extends JpaRepository<GlobalRule, UUID> {

    /**
     * Find all enabled global rules for an organization
     */
    List<GlobalRule> findByOrganizationIdAndEnabledTrue(Long organizationId);

    /**
     * Find all global rules for an organization
     */
    List<GlobalRule> findByOrganizationId(Long organizationId);

    /**
     * Find rules that are due for evaluation based on their interval
     */
    @Query("SELECT gr FROM GlobalRule gr WHERE gr.enabled = true " +
           "AND (gr.lastEvaluatedAt IS NULL OR gr.lastEvaluatedAt < :threshold)")
    List<GlobalRule> findRulesDueForEvaluation(@Param("threshold") Instant threshold);

    /**
     * Count enabled global rules for an organization
     */
    long countByOrganizationIdAndEnabledTrue(Long organizationId);
}
