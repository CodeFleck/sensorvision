package io.indcloud.repository;

import io.indcloud.model.WebhookTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WebhookTestRepository extends JpaRepository<WebhookTest, Long> {

    Page<WebhookTest> findByOrganizationIdOrderByCreatedAtDesc(Long organizationId, Pageable pageable);

    Page<WebhookTest> findByCreatedByIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
