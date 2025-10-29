package org.sensorvision.repository;

import org.sensorvision.model.DataArchiveExecution;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DataArchiveExecutionRepository extends JpaRepository<DataArchiveExecution, Long> {

    Page<DataArchiveExecution> findByPolicyIdOrderByStartedAtDesc(Long policyId, Pageable pageable);

    Page<DataArchiveExecution> findByOrganizationIdOrderByStartedAtDesc(Long organizationId, Pageable pageable);
}
