package io.indcloud.repository;

import java.util.List;
import java.util.UUID;
import io.indcloud.model.Rule;
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

    /**
     * Check if a rule with given name exists for a device
     */
    @Query("SELECT COUNT(r) > 0 FROM Rule r WHERE r.device = :device AND r.name = :name")
    boolean existsByDeviceAndName(@Param("device") io.indcloud.model.Device device, @Param("name") String name);
}