package org.sensorvision.repository;

import org.sensorvision.model.IssueCategory;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface IssueSubmissionRepository extends JpaRepository<IssueSubmission, Long> {

    /**
     * Find all issues submitted by a specific user
     */
    List<IssueSubmission> findByUserOrderByCreatedAtDesc(User user);

    /**
     * Find all issues for an organization
     */
    List<IssueSubmission> findByOrganizationOrderByCreatedAtDesc(Organization organization);

    /**
     * Find issues by category
     */
    List<IssueSubmission> findByCategoryOrderByCreatedAtDesc(IssueCategory category);

    /**
     * Find issues by status
     */
    List<IssueSubmission> findByStatusOrderByCreatedAtDesc(IssueStatus status);

    /**
     * Find a specific issue by ID and user (for security - users can only see their own issues)
     */
    Optional<IssueSubmission> findByIdAndUser(Long id, User user);

    /**
     * Find issues by user and status
     */
    List<IssueSubmission> findByUserAndStatusOrderByCreatedAtDesc(User user, IssueStatus status);

    /**
     * Count issues by user
     */
    long countByUser(User user);

    /**
     * Count issues by organization
     */
    long countByOrganization(Organization organization);

    /**
     * Count issues submitted by a user after a specific timestamp (for rate limiting)
     */
    @Query("SELECT COUNT(i) FROM IssueSubmission i WHERE i.user = :user AND i.createdAt >= :since")
    long countByUserSince(@Param("user") User user, @Param("since") Instant since);
}
