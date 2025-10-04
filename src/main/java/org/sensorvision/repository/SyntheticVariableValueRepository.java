package org.sensorvision.repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.sensorvision.model.SyntheticVariableValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntheticVariableValueRepository extends JpaRepository<SyntheticVariableValue, UUID> {

    /**
     * Find values for a synthetic variable within a time range
     */
    List<SyntheticVariableValue> findBySyntheticVariableIdAndTimestampBetweenOrderByTimestampDesc(
            UUID syntheticVariableId,
            Instant from,
            Instant to
    );

    /**
     * Find latest values for all synthetic variables of a device
     */
    @Query("""
            SELECT svv FROM SyntheticVariableValue svv
            WHERE svv.syntheticVariable.device.externalId = :deviceExternalId
              AND svv.timestamp = (
                  SELECT MAX(svv2.timestamp) FROM SyntheticVariableValue svv2
                  WHERE svv2.syntheticVariable = svv.syntheticVariable
              )
            """)
    List<SyntheticVariableValue> findLatestForDevice(@Param("deviceExternalId") String deviceExternalId);

    /**
     * Find latest value for a specific synthetic variable
     */
    SyntheticVariableValue findFirstBySyntheticVariableIdOrderByTimestampDesc(UUID syntheticVariableId);
}