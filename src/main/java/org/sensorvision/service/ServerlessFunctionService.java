package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.sensorvision.dto.FunctionTriggerRequest;
import org.sensorvision.dto.ServerlessFunctionRequest;
import org.sensorvision.dto.ServerlessFunctionResponse;
import org.sensorvision.model.FunctionTrigger;
import org.sensorvision.model.Organization;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.model.User;
import org.sensorvision.repository.FunctionTriggerRepository;
import org.sensorvision.repository.ServerlessFunctionRepository;
import org.sensorvision.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service for managing serverless functions.
 */
@Service
public class ServerlessFunctionService {

    private static final Logger logger = LoggerFactory.getLogger(ServerlessFunctionService.class);

    private final ServerlessFunctionRepository functionRepository;
    private final FunctionTriggerRepository triggerRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    public ServerlessFunctionService(
        ServerlessFunctionRepository functionRepository,
        FunctionTriggerRepository triggerRepository,
        SecurityUtils securityUtils,
        ObjectMapper objectMapper
    ) {
        this.functionRepository = functionRepository;
        this.triggerRepository = triggerRepository;
        this.securityUtils = securityUtils;
        this.objectMapper = objectMapper;
    }

    /**
     * Get all functions for current user's organization.
     */
    @Transactional(readOnly = true)
    public List<ServerlessFunctionResponse> getAllFunctions() {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        return functionRepository.findByOrganization(userOrg).stream()
            .map(ServerlessFunctionResponse::fromEntityWithoutCode)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific function by ID.
     */
    @Transactional(readOnly = true)
    public ServerlessFunctionResponse getFunction(Long id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findByIdWithTriggers(id)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + id));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + id);
        }

        return ServerlessFunctionResponse.fromEntity(function);
    }

    /**
     * Get function entity (for internal use by services).
     */
    @Transactional(readOnly = true)
    public ServerlessFunction getFunctionEntity(Long id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + id));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + id);
        }

        return function;
    }

    /**
     * Create a new serverless function.
     */
    @Transactional
    public ServerlessFunctionResponse createFunction(ServerlessFunctionRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        User currentUser = securityUtils.getCurrentUser();

        logger.info("Creating serverless function: {} for organization: {}", request.name(), userOrg.getId());

        // Check if function name already exists
        if (functionRepository.existsByOrganizationAndName(userOrg, request.name())) {
            throw new RuntimeException("Function with name '" + request.name() + "' already exists");
        }

        ServerlessFunction function = new ServerlessFunction();
        function.setOrganization(userOrg);
        function.setName(request.name());
        function.setDescription(request.description());
        function.setRuntime(request.runtime());
        function.setCode(request.code());
        function.setHandler(request.handler() != null ? request.handler() : "main");
        function.setEnabled(request.enabled() != null ? request.enabled() : true);
        function.setTimeoutSeconds(request.timeoutSeconds() != null ? request.timeoutSeconds() : 30);
        function.setMemoryLimitMb(request.memoryLimitMb() != null ? request.memoryLimitMb() : 512);
        function.setEnvironmentVariables(request.environmentVariables());
        function.setCreatedBy(currentUser);

        ServerlessFunction saved = functionRepository.save(function);
        logger.info("Serverless function created with id: {}", saved.getId());

        return ServerlessFunctionResponse.fromEntity(saved);
    }

    /**
     * Update an existing serverless function.
     */
    @Transactional
    public ServerlessFunctionResponse updateFunction(Long id, ServerlessFunctionRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Updating serverless function: {}", id);

        ServerlessFunction function = functionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + id));

        // Initialize collections
        function.getTriggers().size();

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + id);
        }

        // Check if renaming to existing name
        if (request.name() != null && !request.name().equals(function.getName())) {
            if (functionRepository.existsByOrganizationAndName(userOrg, request.name())) {
                throw new RuntimeException("Function with name '" + request.name() + "' already exists");
            }
            function.setName(request.name());
        }

        if (request.description() != null) {
            function.setDescription(request.description());
        }
        if (request.runtime() != null) {
            function.setRuntime(request.runtime());
        }
        if (request.code() != null) {
            function.setCode(request.code());
        }
        if (request.handler() != null) {
            function.setHandler(request.handler());
        }
        if (request.enabled() != null) {
            function.setEnabled(request.enabled());
        }
        if (request.timeoutSeconds() != null) {
            function.setTimeoutSeconds(request.timeoutSeconds());
        }
        if (request.memoryLimitMb() != null) {
            function.setMemoryLimitMb(request.memoryLimitMb());
        }
        if (request.environmentVariables() != null) {
            function.setEnvironmentVariables(request.environmentVariables());
        }

        ServerlessFunction updated = functionRepository.save(function);
        return ServerlessFunctionResponse.fromEntity(updated);
    }

    /**
     * Delete a serverless function.
     */
    @Transactional
    public void deleteFunction(Long id) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        logger.info("Deleting serverless function: {}", id);

        ServerlessFunction function = functionRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + id));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + id);
        }

        functionRepository.delete(function);
        logger.info("Serverless function deleted: {}", id);
    }

    /**
     * Add a trigger to a function.
     */
    @Transactional
    public ServerlessFunctionResponse addTrigger(Long functionId, FunctionTriggerRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findById(functionId)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + functionId));

        // Initialize collections
        function.getTriggers().size();

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + functionId);
        }

        FunctionTrigger trigger = new FunctionTrigger();
        trigger.setFunction(function);
        trigger.setTriggerType(request.triggerType());
        trigger.setTriggerConfig(request.triggerConfig());
        trigger.setEnabled(request.enabled() != null ? request.enabled() : true);

        function.addTrigger(trigger);
        functionRepository.save(function);

        logger.info("Added trigger to function: {} (type={})", functionId, request.triggerType());

        return ServerlessFunctionResponse.fromEntity(function);
    }

    /**
     * Remove a trigger from a function.
     */
    @Transactional
    public void removeTrigger(Long functionId, Long triggerId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findById(functionId)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + functionId));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + functionId);
        }

        FunctionTrigger trigger = triggerRepository.findById(triggerId)
            .orElseThrow(() -> new RuntimeException("Trigger not found with id: " + triggerId));

        function.removeTrigger(trigger);
        functionRepository.save(function);

        logger.info("Removed trigger from function: {} (triggerId={})", functionId, triggerId);
    }
}
