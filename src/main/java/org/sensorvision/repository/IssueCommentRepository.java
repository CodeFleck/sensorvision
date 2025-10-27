package org.sensorvision.repository;

import org.sensorvision.model.IssueComment;
import org.sensorvision.model.IssueSubmission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface IssueCommentRepository extends JpaRepository<IssueComment, Long> {

    /**
     * Find all comments for a specific issue, ordered by creation time
     */
    List<IssueComment> findByIssueOrderByCreatedAtAsc(IssueSubmission issue);

    /**
     * Find all non-internal comments for a specific issue (visible to users)
     */
    @Query("SELECT c FROM IssueComment c WHERE c.issue = :issue AND c.internal = false ORDER BY c.createdAt ASC")
    List<IssueComment> findPublicCommentsByIssue(@Param("issue") IssueSubmission issue);

    /**
     * Find all comments (including internal) for a specific issue (admin view)
     */
    @Query("SELECT c FROM IssueComment c WHERE c.issue = :issue ORDER BY c.createdAt ASC")
    List<IssueComment> findAllCommentsByIssue(@Param("issue") IssueSubmission issue);

    /**
     * Count comments for a specific issue
     */
    long countByIssue(IssueSubmission issue);

    /**
     * Count non-internal comments for a specific issue
     */
    @Query("SELECT COUNT(c) FROM IssueComment c WHERE c.issue = :issue AND c.internal = false")
    long countPublicCommentsByIssue(@Param("issue") IssueSubmission issue);
}
