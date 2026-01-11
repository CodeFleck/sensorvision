package io.indcloud.repository;

import io.indcloud.model.MLModel;
import io.indcloud.model.MLModelStatus;
import io.indcloud.model.MLModelType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface MLModelRepository extends JpaRepository<MLModel, UUID> {

    Page<MLModel> findByOrganizationId(Long organizationId, Pageable pageable);

    List<MLModel> findByOrganizationIdAndStatus(Long organizationId, MLModelStatus status);

    List<MLModel> findByOrganizationIdAndModelType(Long organizationId, MLModelType modelType);

    Optional<MLModel> findByIdAndOrganizationId(UUID id, Long organizationId);

    boolean existsByIdAndOrganizationId(UUID id, Long organizationId);

    @Query("SELECT m FROM MLModel m WHERE m.status = :status AND m.nextInferenceAt <= :now")
    List<MLModel> findModelsReadyForInference(@Param("status") MLModelStatus status, @Param("now") Instant now);

    default List<MLModel> findDeployedModelsReadyForInference(Instant now) {
        return findModelsReadyForInference(MLModelStatus.DEPLOYED, now);
    }

    @Query("SELECT m FROM MLModel m WHERE m.organization.id = :orgId AND m.status = :status AND m.modelType = :type")
    List<MLModel> findByOrganizationStatusAndType(
            @Param("orgId") Long organizationId,
            @Param("status") MLModelStatus status,
            @Param("type") MLModelType modelType
    );

    @Query("SELECT COUNT(m) FROM MLModel m WHERE m.organization.id = :orgId AND m.status = :status")
    long countModelsByOrganizationAndStatus(@Param("orgId") Long organizationId, @Param("status") MLModelStatus status);

    default long countDeployedModelsByOrganization(Long organizationId) {
        return countModelsByOrganizationAndStatus(organizationId, MLModelStatus.DEPLOYED);
    }

    boolean existsByOrganizationIdAndNameAndVersion(Long organizationId, String name, String version);
}
