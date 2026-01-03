package io.indcloud.repository;

import io.indcloud.model.MLTrainingJob;
import io.indcloud.model.MLTrainingJobStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MLTrainingJobRepository extends JpaRepository<MLTrainingJob, UUID> {

    Page<MLTrainingJob> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<MLTrainingJob> findByModelId(UUID modelId, Pageable pageable);

    List<MLTrainingJob> findByStatus(MLTrainingJobStatus status);

    List<MLTrainingJob> findByOrganizationIdAndStatus(Long organizationId, MLTrainingJobStatus status);

    @Query("SELECT j FROM MLTrainingJob j WHERE j.model.id = :modelId ORDER BY j.createdAt DESC")
    List<MLTrainingJob> findByModelIdOrderByCreatedAtDesc(@Param("modelId") UUID modelId, Pageable pageable);

    Optional<MLTrainingJob> findFirstByModelIdAndStatusOrderByCreatedAtDesc(UUID modelId, MLTrainingJobStatus status);

    @Query("SELECT j FROM MLTrainingJob j WHERE j.status = 'RUNNING' AND j.startedAt < :staleThreshold")
    List<MLTrainingJob> findStaleRunningJobs(@Param("staleThreshold") java.time.Instant staleThreshold);

    @Query("SELECT COUNT(j) FROM MLTrainingJob j WHERE j.organization.id = :orgId AND j.status = 'RUNNING'")
    long countRunningJobsByOrganization(@Param("orgId") Long organizationId);

    @Query("SELECT COUNT(j) FROM MLTrainingJob j WHERE j.model.id = :modelId AND j.status = 'COMPLETED'")
    long countCompletedJobsByModel(@Param("modelId") UUID modelId);
}
