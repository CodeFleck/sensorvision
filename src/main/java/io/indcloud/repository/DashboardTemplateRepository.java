package io.indcloud.repository;

import io.indcloud.model.DashboardTemplate;
import io.indcloud.model.DashboardTemplateCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository for Dashboard Templates
 */
public interface DashboardTemplateRepository extends JpaRepository<DashboardTemplate, Long> {

    /**
     * Find all templates by category
     */
    List<DashboardTemplate> findByCategory(DashboardTemplateCategory category);

    /**
     * Find all system templates
     */
    List<DashboardTemplate> findByIsSystemTrue();

    /**
     * Find all user-created templates
     */
    List<DashboardTemplate> findByIsSystemFalse();

    /**
     * Find template by name
     */
    Optional<DashboardTemplate> findByName(String name);

    /**
     * Find most popular templates (by usage count)
     */
    @Query("SELECT t FROM DashboardTemplate t ORDER BY t.usageCount DESC")
    List<DashboardTemplate> findMostPopular();

    /**
     * Find recently created templates
     */
    @Query("SELECT t FROM DashboardTemplate t ORDER BY t.createdAt DESC")
    List<DashboardTemplate> findRecentlyCreated();
}
