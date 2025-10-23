package org.sensorvision.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.RuleCreateRequest;
import org.sensorvision.dto.RuleResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Rule;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.RuleRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class RuleService {

    private final RuleRepository ruleRepository;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<RuleResponse> getAllRules() {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        return ruleRepository.findAll().stream()
                .filter(rule -> rule.getDevice().getOrganization().getId().equals(userOrg.getId()))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public RuleResponse getRule(UUID id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        // Verify rule's device belongs to user's organization
        if (!rule.getDevice().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to rule: " + id);
        }

        return toResponse(rule);
    }

    public RuleResponse createRule(RuleCreateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(request.deviceId())
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + request.deviceId()));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + request.deviceId());
        }

        Rule rule = Rule.builder()
                .name(request.name())
                .description(request.description())
                .device(device)
                .variable(request.variable())
                .operator(request.operator())
                .threshold(request.threshold())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .build();

        Rule saved = ruleRepository.save(rule);
        return toResponse(saved);
    }

    public RuleResponse updateRule(UUID id, RuleCreateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        // Verify rule's device belongs to user's organization
        if (!rule.getDevice().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to rule: " + id);
        }

        if (request.name() != null) {
            rule.setName(request.name());
        }
        if (request.description() != null) {
            rule.setDescription(request.description());
        }
        if (request.deviceId() != null) {
            Device device = deviceRepository.findByExternalId(request.deviceId())
                    .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + request.deviceId()));

            // Verify new device belongs to user's organization
            if (!device.getOrganization().getId().equals(userOrg.getId())) {
                throw new AccessDeniedException("Access denied to device: " + request.deviceId());
            }

            rule.setDevice(device);
        }
        if (request.variable() != null) {
            rule.setVariable(request.variable());
        }
        if (request.operator() != null) {
            rule.setOperator(request.operator());
        }
        if (request.threshold() != null) {
            rule.setThreshold(request.threshold());
        }
        if (request.enabled() != null) {
            rule.setEnabled(request.enabled());
        }

        Rule updated = ruleRepository.save(rule);
        return toResponse(updated);
    }

    public void deleteRule(UUID id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Rule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rule not found: " + id));

        // Verify rule's device belongs to user's organization
        if (!rule.getDevice().getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to rule: " + id);
        }

        ruleRepository.delete(rule);
    }

    private RuleResponse toResponse(Rule rule) {
        return new RuleResponse(
                rule.getId(),
                rule.getName(),
                rule.getDescription(),
                rule.getDevice().getExternalId(),
                rule.getDevice().getName(),
                rule.getVariable(),
                rule.getOperator(),
                rule.getThreshold(),
                rule.getEnabled(),
                rule.getCreatedAt()
        );
    }
}