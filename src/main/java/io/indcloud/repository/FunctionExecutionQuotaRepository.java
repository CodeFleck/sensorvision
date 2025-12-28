package io.indcloud.repository;

import io.indcloud.model.FunctionExecutionQuota;
import io.indcloud.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.util.Optional;

@Repository
public interface FunctionExecutionQuotaRepository extends JpaRepository<FunctionExecutionQuota, Long> {

    Optional<FunctionExecutionQuota> findByOrganization(Organization organization);

    /**
     * Find quota with pessimistic write lock to prevent race conditions.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<FunctionExecutionQuota> findByOrganizationId(Long organizationId);
}
