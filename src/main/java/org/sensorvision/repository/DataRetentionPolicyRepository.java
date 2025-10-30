package org.sensorvision.repository;

import org.sensorvision.model.DataRetentionPolicy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, Long> {

    Optional<DataRetentionPolicy> findByOrganizationId(Long organizationId);

    @Query("SELECT p FROM DataRetentionPolicy p WHERE p.enabled = true")
    List<DataRetentionPolicy> findAllEnabled();

    boolean existsByOrganizationId(Long organizationId);
}
