package io.indcloud.repository;

import io.indcloud.model.MLPrediction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Repository
public interface MLPredictionRepository extends JpaRepository<MLPrediction, UUID> {

    Page<MLPrediction> findByOrganizationId(Long organizationId, Pageable pageable);

    Page<MLPrediction> findByDeviceId(UUID deviceId, Pageable pageable);

    Page<MLPrediction> findByModelId(UUID modelId, Pageable pageable);

    @Query("SELECT p FROM MLPrediction p WHERE p.device.id = :deviceId AND p.predictionType = :type ORDER BY p.predictionTimestamp DESC")
    List<MLPrediction> findLatestByDeviceAndType(
            @Param("deviceId") UUID deviceId,
            @Param("type") String predictionType,
            Pageable pageable
    );

    @Query("SELECT p FROM MLPrediction p WHERE p.device.id = :deviceId AND p.predictionTimestamp BETWEEN :start AND :end ORDER BY p.predictionTimestamp DESC")
    List<MLPrediction> findByDeviceAndTimeRange(
            @Param("deviceId") UUID deviceId,
            @Param("start") Instant start,
            @Param("end") Instant end
    );

    @Query("SELECT p FROM MLPrediction p WHERE p.organization.id = :orgId AND p.predictionType = :type AND p.predictionTimestamp >= :since ORDER BY p.predictionTimestamp DESC")
    List<MLPrediction> findRecentByOrganizationAndType(
            @Param("orgId") Long organizationId,
            @Param("type") String predictionType,
            @Param("since") Instant since
    );

    @Query("SELECT COUNT(p) FROM MLPrediction p WHERE p.model.id = :modelId AND p.predictionTimestamp >= :since")
    long countPredictionsSince(@Param("modelId") UUID modelId, @Param("since") Instant since);

    @Modifying
    @Transactional
    void deleteByModelId(UUID modelId);
}
