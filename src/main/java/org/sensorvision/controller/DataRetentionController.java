package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.CreateRetentionPolicyRequest;
import org.sensorvision.dto.DataArchiveExecutionDto;
import org.sensorvision.dto.DataRetentionPolicyDto;
import org.sensorvision.model.DataArchiveExecution;
import org.sensorvision.model.DataRetentionPolicy;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DataArchiveExecutionRepository;
import org.sensorvision.repository.DataRetentionPolicyRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.DataRetentionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for managing data retention policies
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/retention-policies")
@RequiredArgsConstructor
public class DataRetentionController {

    private final DataRetentionPolicyRepository policyRepository;
    private final DataArchiveExecutionRepository executionRepository;
    private final DataRetentionService retentionService;
    private final SecurityUtils securityUtils;

    /**
     * Get retention policy for the authenticated user's organization
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<DataRetentionPolicyDto> getPolicy() {
        Organization org = securityUtils.getCurrentUserOrganization();

        return policyRepository.findByOrganizationId(org.getId())
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Create or update retention policy
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataRetentionPolicyDto> createOrUpdatePolicy(
            @Valid @RequestBody CreateRetentionPolicyRequest request
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataRetentionPolicy policy = policyRepository.findByOrganizationId(org.getId())
                .orElse(new DataRetentionPolicy());

        policy.setOrganization(org);
        policy.setEnabled(request.enabled());
        policy.setRetentionDays(request.retentionDays());
        policy.setArchiveEnabled(request.archiveEnabled());
        policy.setArchiveStorageType(request.archiveStorageType());
        policy.setArchiveStorageConfig(request.archiveStorageConfig());

        if (request.archiveScheduleCron() != null) {
            policy.setArchiveScheduleCron(request.archiveScheduleCron());
        }

        policy = policyRepository.save(policy);
        log.info("Created/updated retention policy for organization {} (ID: {})",
                org.getId(), policy.getId());

        return ResponseEntity.ok(toDto(policy));
    }

    /**
     * Manually trigger archival execution
     */
    @PostMapping("/execute")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DataArchiveExecutionDto> executeArchival() {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataRetentionPolicy policy = policyRepository.findByOrganizationId(org.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "No retention policy found for organization"));

        if (!policy.getEnabled()) {
            return ResponseEntity.badRequest().build();
        }

        log.info("Manually triggering archival for organization {} (policy ID: {})",
                org.getId(), policy.getId());

        DataArchiveExecution execution = retentionService.executeArchival(policy);

        return ResponseEntity.ok(toExecutionDto(execution));
    }

    /**
     * Get archival execution history
     */
    @GetMapping("/executions")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DataArchiveExecutionDto>> getExecutions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        Pageable pageable = PageRequest.of(page, size, Sort.by("startedAt").descending());
        Page<DataArchiveExecution> executions = executionRepository
                .findByOrganizationIdOrderByStartedAtDesc(org.getId(), pageable);

        Page<DataArchiveExecutionDto> dtos = executions.map(this::toExecutionDto);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Delete retention policy
     */
    @DeleteMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletePolicy() {
        Organization org = securityUtils.getCurrentUserOrganization();

        return policyRepository.findByOrganizationId(org.getId())
                .map(policy -> {
                    policyRepository.delete(policy);
                    log.info("Deleted retention policy for organization {}", org.getId());
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private DataRetentionPolicyDto toDto(DataRetentionPolicy policy) {
        return new DataRetentionPolicyDto(
                policy.getId(),
                policy.getEnabled(),
                policy.getRetentionDays(),
                policy.getArchiveEnabled(),
                policy.getArchiveStorageType(),
                policy.getArchiveStorageConfig(),
                policy.getArchiveScheduleCron(),
                policy.getLastArchiveRun(),
                policy.getLastArchiveStatus(),
                policy.getLastArchiveError(),
                policy.getTotalRecordsArchived(),
                policy.getTotalArchiveSizeBytes(),
                policy.getCreatedAt(),
                policy.getUpdatedAt()
        );
    }

    private DataArchiveExecutionDto toExecutionDto(DataArchiveExecution execution) {
        return new DataArchiveExecutionDto(
                execution.getId(),
                execution.getPolicy().getId(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getStatus(),
                execution.getArchiveFromDate(),
                execution.getArchiveToDate(),
                execution.getRecordsArchived(),
                execution.getArchiveFilePath(),
                execution.getArchiveSizeBytes(),
                execution.getDurationMs(),
                execution.getErrorMessage()
        );
    }
}
