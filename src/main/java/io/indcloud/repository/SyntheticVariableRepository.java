package io.indcloud.repository;

import java.util.List;
import java.util.UUID;
import io.indcloud.model.SyntheticVariable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyntheticVariableRepository extends JpaRepository<SyntheticVariable, UUID> {

    /**
     * Find enabled synthetic variables for a device
     */
    List<SyntheticVariable> findByDeviceExternalIdAndEnabledTrue(String deviceExternalId);

    /**
     * Find all synthetic variables for a device
     */
    List<SyntheticVariable> findByDeviceExternalId(String deviceExternalId);

    /**
     * Count enabled synthetic variables
     */
    long countByEnabledTrue();
}