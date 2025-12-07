package org.sensorvision.repository;

import org.sensorvision.model.PilotFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository for managing pilot feedback data
 */
@Repository
public interface PilotFeedbackRepository extends JpaRepository<PilotFeedback, Long> {

    /**
     * Find feedback by organization ID
     */
    List<PilotFeedback> findByOrganizationId(String organizationId);

    /**
     * Find feedback by organization ID and date range
     */
    List<PilotFeedback> findByOrganizationIdAndSubmittedAtAfter(String organizationId, LocalDateTime since);

    /**
     * Find feedback submitted after a specific date
     */
    List<PilotFeedback> findBySubmittedAtAfter(LocalDateTime since);

    /**
     * Find feedback by rating
     */
    List<PilotFeedback> findByRating(Integer rating);

    /**
     * Find feedback by rating range
     */
    List<PilotFeedback> findByRatingBetween(Integer minRating, Integer maxRating);

    /**
     * Find feedback by category
     */
    List<PilotFeedback> findByCategory(String category);

    /**
     * Find feedback by status
     */
    List<PilotFeedback> findByStatus(String status);

    /**
     * Find feedback by user ID
     */
    List<PilotFeedback> findByUserId(String userId);

    /**
     * Count feedback by organization
     */
    long countByOrganizationId(String organizationId);

    /**
     * Count feedback by rating
     */
    long countByRating(Integer rating);

    /**
     * Count feedback submitted after a specific date
     */
    long countBySubmittedAtAfter(LocalDateTime since);

    /**
     * Get average rating for all feedback
     */
    @Query("SELECT AVG(f.rating) FROM PilotFeedback f")
    Double getAverageRating();

    /**
     * Get average rating for a specific organization
     */
    @Query("SELECT AVG(f.rating) FROM PilotFeedback f WHERE f.organizationId = :organizationId")
    Double getAverageRatingByOrganization(@Param("organizationId") String organizationId);

    /**
     * Get average rating for feedback submitted after a specific date
     */
    @Query("SELECT AVG(f.rating) FROM PilotFeedback f WHERE f.submittedAt > :since")
    Double getAverageRatingAfter(@Param("since") LocalDateTime since);

    /**
     * Get feedback statistics by organization
     */
    @Query("SELECT f.organizationId, COUNT(f), AVG(f.rating) " +
           "FROM PilotFeedback f " +
           "GROUP BY f.organizationId")
    List<Object[]> getFeedbackStatsByOrganization();

    /**
     * Get feedback statistics by category
     */
    @Query("SELECT f.category, COUNT(f), AVG(f.rating) " +
           "FROM PilotFeedback f " +
           "WHERE f.category IS NOT NULL " +
           "GROUP BY f.category")
    List<Object[]> getFeedbackStatsByCategory();

    /**
     * Get recent critical feedback (rating <= 2)
     */
    @Query("SELECT f FROM PilotFeedback f " +
           "WHERE f.rating <= 2 " +
           "ORDER BY f.submittedAt DESC")
    List<PilotFeedback> getCriticalFeedback();

    /**
     * Get recent critical feedback with limit
     */
    @Query("SELECT f FROM PilotFeedback f " +
           "WHERE f.rating <= 2 " +
           "ORDER BY f.submittedAt DESC " +
           "LIMIT :limit")
    List<PilotFeedback> getCriticalFeedback(@Param("limit") int limit);

    /**
     * Get feedback trends by day
     */
    @Query("SELECT DATE(f.submittedAt), COUNT(f), AVG(f.rating) " +
           "FROM PilotFeedback f " +
           "WHERE f.submittedAt >= :since " +
           "GROUP BY DATE(f.submittedAt) " +
           "ORDER BY DATE(f.submittedAt)")
    List<Object[]> getFeedbackTrends(@Param("since") LocalDateTime since);

    /**
     * Find unresolved feedback older than specified days
     */
    @Query("SELECT f FROM PilotFeedback f " +
           "WHERE f.status != 'RESOLVED' " +
           "AND f.submittedAt < :cutoffDate " +
           "ORDER BY f.submittedAt ASC")
    List<PilotFeedback> getUnresolvedFeedbackOlderThan(@Param("cutoffDate") LocalDateTime cutoffDate);

    /**
     * Get Net Promoter Score (NPS) calculation
     */
    @Query("SELECT " +
           "SUM(CASE WHEN f.rating >= 9 THEN 1 ELSE 0 END) as promoters, " +
           "SUM(CASE WHEN f.rating <= 6 THEN 1 ELSE 0 END) as detractors, " +
           "COUNT(f) as total " +
           "FROM PilotFeedback f " +
           "WHERE f.submittedAt >= :since")
    Object[] getNPSData(@Param("since") LocalDateTime since);

    /**
     * Get feedback distribution by rating
     */
    @Query("SELECT f.rating, COUNT(f) " +
           "FROM PilotFeedback f " +
           "WHERE f.submittedAt >= :since " +
           "GROUP BY f.rating " +
           "ORDER BY f.rating")
    List<Object[]> getRatingDistribution(@Param("since") LocalDateTime since);

    /**
     * Search feedback by text content
     */
    @Query("SELECT f FROM PilotFeedback f " +
           "WHERE LOWER(f.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "OR LOWER(f.message) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY f.submittedAt DESC")
    List<PilotFeedback> searchFeedback(@Param("searchTerm") String searchTerm);

    /**
     * Get feedback requiring follow-up (critical feedback without admin notes)
     */
    @Query("SELECT f FROM PilotFeedback f " +
           "WHERE f.rating <= 2 " +
           "AND (f.adminNotes IS NULL OR f.adminNotes = '') " +
           "AND f.status != 'RESOLVED' " +
           "ORDER BY f.submittedAt ASC")
    List<PilotFeedback> getFeedbackRequiringFollowUp();
}