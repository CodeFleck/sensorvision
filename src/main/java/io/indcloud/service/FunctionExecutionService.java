package io.indcloud.service;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.dto.FunctionExecutionResponse;
import io.indcloud.model.*;
import io.indcloud.repository.FunctionExecutionRepository;
import io.indcloud.repository.ServerlessFunctionRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.functions.FunctionExecutionException;
import io.indcloud.service.functions.FunctionExecutionResult;
import io.indcloud.service.functions.FunctionExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service for executing serverless functions and managing execution logs.
 */
@Service
public class FunctionExecutionService {

    private static final Logger logger = LoggerFactory.getLogger(FunctionExecutionService.class);

    private final ServerlessFunctionRepository functionRepository;
    private final FunctionExecutionRepository executionRepository;
    private final List<FunctionExecutor> executors;
    private final SecurityUtils securityUtils;
    private final FunctionRateLimitService rateLimitService;

    public FunctionExecutionService(
        ServerlessFunctionRepository functionRepository,
        FunctionExecutionRepository executionRepository,
        List<FunctionExecutor> executors,
        SecurityUtils securityUtils,
        FunctionRateLimitService rateLimitService
    ) {
        this.functionRepository = functionRepository;
        this.executionRepository = executionRepository;
        this.executors = executors;
        this.securityUtils = securityUtils;
        this.rateLimitService = rateLimitService;
    }

    /**
     * Execute a function synchronously and return the result.
     */
    @Transactional
    public FunctionExecutionResponse executeFunction(Long functionId, JsonNode input) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findById(functionId)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + functionId));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + functionId);
        }

        if (!function.getEnabled()) {
            throw new RuntimeException("Function is disabled: " + function.getName());
        }

        // Check rate limit quota
        if (!rateLimitService.checkAndIncrementQuota(userOrg)) {
            logger.warn("Rate limit exceeded for organization {} attempting to execute function {}",
                userOrg.getId(), function.getName());
            throw new RuntimeException("Rate limit exceeded. Please try again later.");
        }

        // Create execution record
        FunctionExecution execution = new FunctionExecution(function, input);
        execution = executionRepository.save(execution);

        // Find executor for runtime
        FunctionExecutor executor = executors.stream()
            .filter(e -> e.supports(function.getRuntime()))
            .findFirst()
            .orElseThrow(() -> new RuntimeException("No executor found for runtime: " + function.getRuntime()));

        try {
            logger.info("Executing function: {} (id={})", function.getName(), functionId);

            // Execute function
            FunctionExecutionResult result = executor.execute(function, input);

            // Update execution record
            if (result.isSuccess()) {
                execution.complete(FunctionExecutionStatus.SUCCESS, result.getOutput());
            } else {
                execution.fail(result.getErrorMessage(), result.getErrorStack());
            }
            execution.setMemoryUsedMb(result.getMemoryUsedMb());

            executionRepository.save(execution);

            logger.info("Function execution completed: {} (duration={}ms, status={})",
                function.getName(), execution.getDurationMs(), execution.getStatus());

            return FunctionExecutionResponse.fromEntity(execution);

        } catch (FunctionExecutionException e) {
            logger.error("Function execution failed: {}", e.getMessage(), e);
            execution.fail(e.getMessage(), e.getErrorStack());
            executionRepository.save(execution);
            throw new RuntimeException("Function execution failed: " + e.getMessage(), e);
        }
    }

    /**
     * Execute a function asynchronously (fire and forget).
     */
    @Async
    @Transactional
    public void executeFunctionAsync(Long functionId, JsonNode input, FunctionTrigger trigger) {
        try {
            ServerlessFunction function = functionRepository.findById(functionId)
                .orElseThrow(() -> new RuntimeException("Function not found with id: " + functionId));

            if (!function.getEnabled()) {
                logger.warn("Skipping disabled function: {}", function.getName());
                return;
            }

            // Check rate limit quota
            if (!rateLimitService.checkAndIncrementQuota(function.getOrganization())) {
                logger.warn("Rate limit exceeded for organization {} attempting to execute function {} (async)",
                    function.getOrganization().getId(), function.getName());
                return; // Silently skip execution if rate limited (don't fail triggers)
            }

            // Create execution record
            FunctionExecution execution = new FunctionExecution(function, input);
            execution.setTrigger(trigger);
            execution = executionRepository.save(execution);

            // Find executor
            FunctionExecutor executor = executors.stream()
                .filter(e -> e.supports(function.getRuntime()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("No executor found for runtime: " + function.getRuntime()));

            logger.info("Executing function asynchronously: {} (id={})", function.getName(), functionId);

            // Execute
            FunctionExecutionResult result = executor.execute(function, input);

            // Update execution record
            if (result.isSuccess()) {
                execution.complete(FunctionExecutionStatus.SUCCESS, result.getOutput());
            } else {
                execution.fail(result.getErrorMessage(), result.getErrorStack());
            }
            execution.setMemoryUsedMb(result.getMemoryUsedMb());

            executionRepository.save(execution);

            logger.info("Async function execution completed: {} (duration={}ms, status={})",
                function.getName(), execution.getDurationMs(), execution.getStatus());

        } catch (Exception e) {
            logger.error("Async function execution failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Get execution history for a function.
     */
    @Transactional(readOnly = true)
    public Page<FunctionExecutionResponse> getExecutionHistory(Long functionId, Pageable pageable) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        ServerlessFunction function = functionRepository.findById(functionId)
            .orElseThrow(() -> new RuntimeException("Function not found with id: " + functionId));

        // Verify function belongs to user's organization
        if (!function.getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to function: " + functionId);
        }

        return executionRepository.findByFunctionOrderByStartedAtDesc(function, pageable)
            .map(FunctionExecutionResponse::fromEntityWithoutDetails);
    }

    /**
     * Get detailed execution information.
     */
    @Transactional(readOnly = true)
    public FunctionExecutionResponse getExecution(Long executionId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        FunctionExecution execution = executionRepository.findById(executionId)
            .orElseThrow(() -> new RuntimeException("Execution not found with id: " + executionId));

        // Verify execution's function belongs to user's organization
        if (!execution.getFunction().getOrganization().getId().equals(userOrg.getId())) {
            throw new RuntimeException("Access denied to execution: " + executionId);
        }

        return FunctionExecutionResponse.fromEntity(execution);
    }
}
