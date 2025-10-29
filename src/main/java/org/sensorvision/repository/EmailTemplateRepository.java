package org.sensorvision.repository;

import org.sensorvision.model.EmailTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {

    Page<EmailTemplate> findByOrganizationId(Long organizationId, Pageable pageable);

    List<EmailTemplate> findByOrganizationIdAndActive(Long organizationId, Boolean active);

    List<EmailTemplate> findByOrganizationIdAndTemplateType(Long organizationId, String templateType);

    Optional<EmailTemplate> findByOrganizationIdAndTemplateTypeAndIsDefault(
            Long organizationId,
            String templateType,
            Boolean isDefault
    );

    @Query("SELECT et FROM EmailTemplate et WHERE et.organization.id = :organizationId " +
           "AND (:templateType IS NULL OR et.templateType = :templateType) " +
           "AND (:active IS NULL OR et.active = :active)")
    Page<EmailTemplate> findByFilters(
            @Param("organizationId") Long organizationId,
            @Param("templateType") String templateType,
            @Param("active") Boolean active,
            Pageable pageable
    );

    boolean existsByOrganizationIdAndTemplateTypeAndIsDefaultAndIdNot(
            Long organizationId,
            String templateType,
            Boolean isDefault,
            Long id
    );
}
