package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.GlobalRuleCreateRequest;
import io.indcloud.dto.GlobalRuleResponse;
import io.indcloud.model.GlobalRule;
import io.indcloud.model.Organization;
import io.indcloud.repository.GlobalRuleRepository;
import io.indcloud.repository.OrganizationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for managing global rules (CRUD operations)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlobalRuleService {

    private final GlobalRuleRepository globalRuleRepository;
    private final OrganizationRepository organizationRepository;
    private final GlobalRuleEvaluatorService evaluatorService;

    /**
     * Create a new global rule
     */
    @Transactional
    public GlobalRuleResponse createGlobalRule(GlobalRuleCreateRequest request, Long organizationId) {
        log.info("Creating global rule: {} for organization {}", request.getName(), organizationId);

        Organization organization = organizationRepository.findById(organizationId)
                .orElseThrow(() -> new IllegalArgumentException("Organization not found: " + organizationId));

        // Validate aggregation function
        validateAggregationFunction(request.getAggregationFunction(), request.getAggregationVariable());

        GlobalRule rule = GlobalRule.builder()
                .name(request.getName())
                .description(request.getDescription())
                .organization(organization)
                .selectorType(request.getSelectorType())
                .selectorValue(request.getSelectorValue())
                .aggregationFunction(request.getAggregationFunction())
                .aggregationVariable(request.getAggregationVariable())
                .aggregationParams(request.getAggregationParams())
                .operator(request.getOperator())
                .threshold(request.getThreshold())
                .enabled(request.getEnabled())
                .evaluationInterval(request.getEvaluationInterval())
                .cooldownMinutes(request.getCooldownMinutes())
                .sendSms(request.getSendSms())
                .smsRecipients(request.getSmsRecipients())
                .build();

        rule = globalRuleRepository.save(rule);
        log.info("Created global rule: {} ({})", rule.getName(), rule.getId());

        return toResponse(rule);
    }

    /**
     * Update an existing global rule
     */
    @Transactional
    public GlobalRuleResponse updateGlobalRule(UUID ruleId, GlobalRuleCreateRequest request, Long organizationId) {
        log.info("Updating global rule: {}", ruleId);

        GlobalRule rule = globalRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Global rule not found: " + ruleId));

        // Verify organization ownership
        if (!rule.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global rule does not belong to organization");
        }

        // Validate aggregation function
        validateAggregationFunction(request.getAggregationFunction(), request.getAggregationVariable());

        rule.setName(request.getName());
        rule.setDescription(request.getDescription());
        rule.setSelectorType(request.getSelectorType());
        rule.setSelectorValue(request.getSelectorValue());
        rule.setAggregationFunction(request.getAggregationFunction());
        rule.setAggregationVariable(request.getAggregationVariable());
        rule.setAggregationParams(request.getAggregationParams());
        rule.setOperator(request.getOperator());
        rule.setThreshold(request.getThreshold());
        rule.setEnabled(request.getEnabled());
        rule.setEvaluationInterval(request.getEvaluationInterval());
        rule.setCooldownMinutes(request.getCooldownMinutes());
        rule.setSendSms(request.getSendSms());
        rule.setSmsRecipients(request.getSmsRecipients());

        rule = globalRuleRepository.save(rule);
        log.info("Updated global rule: {}", ruleId);

        return toResponse(rule);
    }

    /**
     * Get a global rule by ID
     */
    public GlobalRuleResponse getGlobalRule(UUID ruleId, Long organizationId) {
        GlobalRule rule = globalRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Global rule not found: " + ruleId));

        // Verify organization ownership
        if (!rule.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global rule does not belong to organization");
        }

        return toResponse(rule);
    }

    /**
     * Get all global rules for an organization
     */
    public List<GlobalRuleResponse> getGlobalRules(Long organizationId) {
        List<GlobalRule> rules = globalRuleRepository.findByOrganizationId(organizationId);
        return rules.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Delete a global rule
     */
    @Transactional
    public void deleteGlobalRule(UUID ruleId, Long organizationId) {
        log.info("Deleting global rule: {}", ruleId);

        GlobalRule rule = globalRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Global rule not found: " + ruleId));

        // Verify organization ownership
        if (!rule.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global rule does not belong to organization");
        }

        globalRuleRepository.delete(rule);
        log.info("Deleted global rule: {}", ruleId);
    }

    /**
     * Toggle global rule enabled status
     */
    @Transactional
    public GlobalRuleResponse toggleGlobalRule(UUID ruleId, Long organizationId) {
        log.info("Toggling global rule: {}", ruleId);

        GlobalRule rule = globalRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Global rule not found: " + ruleId));

        // Verify organization ownership
        if (!rule.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global rule does not belong to organization");
        }

        rule.setEnabled(!rule.getEnabled());
        rule = globalRuleRepository.save(rule);

        log.info("Toggled global rule {} to {}", ruleId, rule.getEnabled() ? "enabled" : "disabled");
        return toResponse(rule);
    }

    /**
     * Manually evaluate a global rule (for testing)
     */
    @Transactional
    public void evaluateGlobalRule(UUID ruleId, Long organizationId) {
        log.info("Manually evaluating global rule: {}", ruleId);

        GlobalRule rule = globalRuleRepository.findById(ruleId)
                .orElseThrow(() -> new IllegalArgumentException("Global rule not found: " + ruleId));

        // Verify organization ownership
        if (!rule.getOrganization().getId().equals(organizationId)) {
            throw new IllegalArgumentException("Global rule does not belong to organization");
        }

        evaluatorService.evaluateRule(rule);
        log.info("Completed manual evaluation of global rule: {}", ruleId);
    }

    /**
     * Validate aggregation function and variable
     */
    private void validateAggregationFunction(String functionName, String variable) {
        io.indcloud.model.FleetAggregationFunction function;

        try {
            function = io.indcloud.model.FleetAggregationFunction.fromString(functionName);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid aggregation function: " + functionName);
        }

        if (function.requiresVariable() && (variable == null || variable.isBlank())) {
            throw new IllegalArgumentException(
                    "Aggregation function '" + functionName + "' requires a variable");
        }
    }

    /**
     * Convert GlobalRule entity to response DTO
     */
    private GlobalRuleResponse toResponse(GlobalRule rule) {
        return GlobalRuleResponse.builder()
                .id(rule.getId())
                .name(rule.getName())
                .description(rule.getDescription())
                .organizationId(rule.getOrganization().getId())
                .selectorType(rule.getSelectorType())
                .selectorValue(rule.getSelectorValue())
                .aggregationFunction(rule.getAggregationFunction())
                .aggregationVariable(rule.getAggregationVariable())
                .aggregationParams(rule.getAggregationParams())
                .operator(rule.getOperator())
                .threshold(rule.getThreshold())
                .enabled(rule.getEnabled())
                .evaluationInterval(rule.getEvaluationInterval())
                .cooldownMinutes(rule.getCooldownMinutes())
                .lastEvaluatedAt(rule.getLastEvaluatedAt())
                .lastTriggeredAt(rule.getLastTriggeredAt())
                .sendSms(rule.getSendSms())
                .smsRecipients(rule.getSmsRecipients())
                .createdAt(rule.getCreatedAt())
                .updatedAt(rule.getUpdatedAt())
                .build();
    }
}
