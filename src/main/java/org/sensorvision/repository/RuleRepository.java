package org.sensorvision.repository;

import java.util.List;
import java.util.UUID;
import org.sensorvision.model.Rule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RuleRepository extends JpaRepository<Rule, UUID> {

    /**
     * Find all enabled rules for a specific device
     */
    List<Rule> findByDeviceExternalIdAndEnabledTrue(String deviceExternalId);

    /**
     * Find all enabled rules across all devices
     */
    List<Rule> findByEnabledTrue();

    /**
     * Find rules by device ID
     */
    @Query("SELECT r FROM Rule r WHERE r.device.id = :deviceId")
    List<Rule> findByDeviceId(@Param("deviceId") UUID deviceId);

    /**
     * Count total rules
     */
    long count();

    /**
     * Count enabled rules
     */
    long countByEnabledTrue();
}