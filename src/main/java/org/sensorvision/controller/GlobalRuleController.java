package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.GlobalRuleCreateRequest;
import org.sensorvision.dto.GlobalRuleResponse;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.GlobalRuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST API controller for global rules management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/global-rules")
@RequiredArgsConstructor
public class GlobalRuleController {

    private final GlobalRuleService globalRuleService;
    private final SecurityUtils securityUtils;

    /**
     * Create a new global rule
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalRuleResponse> createGlobalRule(
            @Valid @RequestBody GlobalRuleCreateRequest request) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Creating global rule: {} for organization {}", request.getName(), organizationId);

        GlobalRuleResponse response = globalRuleService.createGlobalRule(request, organizationId);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Get all global rules for the current organization
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<List<GlobalRuleResponse>> getGlobalRules() {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.debug("Fetching global rules for organization {}", organizationId);

        List<GlobalRuleResponse> rules = globalRuleService.getGlobalRules(organizationId);

        return ResponseEntity.ok(rules);
    }

    /**
     * Get a specific global rule by ID
     */
    @GetMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalRuleResponse> getGlobalRule(@PathVariable UUID ruleId) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.debug("Fetching global rule: {}", ruleId);

        GlobalRuleResponse rule = globalRuleService.getGlobalRule(ruleId, organizationId);

        return ResponseEntity.ok(rule);
    }

    /**
     * Update a global rule
     */
    @PutMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalRuleResponse> updateGlobalRule(
            @PathVariable UUID ruleId,
            @Valid @RequestBody GlobalRuleCreateRequest request) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Updating global rule: {}", ruleId);

        GlobalRuleResponse response = globalRuleService.updateGlobalRule(ruleId, request, organizationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Delete a global rule
     */
    @DeleteMapping("/{ruleId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> deleteGlobalRule(@PathVariable UUID ruleId) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Deleting global rule: {}", ruleId);

        globalRuleService.deleteGlobalRule(ruleId, organizationId);

        return ResponseEntity.noContent().build();
    }

    /**
     * Toggle global rule enabled status
     */
    @PostMapping("/{ruleId}/toggle")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalRuleResponse> toggleGlobalRule(@PathVariable UUID ruleId) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Toggling global rule: {}", ruleId);

        GlobalRuleResponse response = globalRuleService.toggleGlobalRule(ruleId, organizationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Manually evaluate a global rule (for testing)
     */
    @PostMapping("/{ruleId}/evaluate")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Void> evaluateGlobalRule(@PathVariable UUID ruleId) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Manually evaluating global rule: {}", ruleId);

        globalRuleService.evaluateGlobalRule(ruleId, organizationId);

        return ResponseEntity.ok().build();
    }
}
