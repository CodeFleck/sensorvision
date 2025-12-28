package io.indcloud.repository;

import io.indcloud.model.DataRetentionPolicy;
import io.indcloud.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DataRetentionPolicyRepository extends JpaRepository<DataRetentionPolicy, Long> {

    Optional<DataRetentionPolicy> findByOrganization(Organization organization);

    Optional<DataRetentionPolicy> findByOrganizationId(Long organizationId);

    boolean existsByOrganization(Organization organization);

    @Query("SELECT p FROM DataRetentionPolicy p WHERE p.enabled = true")
    List<DataRetentionPolicy> findAllEnabled();
}
