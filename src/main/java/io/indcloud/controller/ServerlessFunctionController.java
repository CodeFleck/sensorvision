package io.indcloud.controller;

import jakarta.validation.Valid;
import io.indcloud.dto.*;
import io.indcloud.service.FunctionExecutionService;
import io.indcloud.service.ServerlessFunctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST API for managing serverless functions.
 */
@RestController
@RequestMapping("/api/v1/functions")
public class ServerlessFunctionController {

    private final ServerlessFunctionService functionService;
    private final FunctionExecutionService executionService;

    public ServerlessFunctionController(ServerlessFunctionService functionService,
            FunctionExecutionService executionService) {
        this.functionService = functionService;
        this.executionService = executionService;
    }

    /**
     * Get all serverless functions for current organization.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping
    public List<ServerlessFunctionResponse> getAllFunctions() {
        return functionService.getAllFunctions();
    }

    /**
     * Get a specific function by ID.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}")
    public ServerlessFunctionResponse getFunction(@PathVariable Long id) {
        return functionService.getFunction(id);
    }

    /**
     * Create a new serverless function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping
    public ResponseEntity<ServerlessFunctionResponse> createFunction(
            @Valid @RequestBody ServerlessFunctionRequest request) {
        ServerlessFunctionResponse response = functionService.createFunction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing serverless function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PutMapping("/{id}")
    public ServerlessFunctionResponse updateFunction(@PathVariable Long id,
            @Valid @RequestBody ServerlessFunctionRequest request) {
        return functionService.updateFunction(id, request);
    }

    /**
     * Delete a serverless function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFunction(@PathVariable Long id) {
        functionService.deleteFunction(id);
    }

    /**
     * Add a trigger to a function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{id}/triggers")
    public ServerlessFunctionResponse addTrigger(@PathVariable Long id,
            @Valid @RequestBody FunctionTriggerRequest request) {
        return functionService.addTrigger(id, request);
    }

    /**
     * Remove a trigger from a function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @DeleteMapping("/{functionId}/triggers/{triggerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTrigger(@PathVariable Long functionId,
            @PathVariable Long triggerId) {
        functionService.removeTrigger(functionId, triggerId);
    }

    /**
     * Execute a function manually (for testing).
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @PostMapping("/{id}/invoke")
    public FunctionExecutionResponse invokeFunction(@PathVariable Long id,
            @RequestBody FunctionInvokeRequest request) {
        return executionService.executeFunction(id, request.input());
    }

    /**
     * Get execution history for a function.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/{id}/executions")
    public Page<FunctionExecutionResponse> getExecutionHistory(@PathVariable Long id,
            Pageable pageable) {
        return executionService.getExecutionHistory(id, pageable);
    }

    /**
     * Get details of a specific execution.
     */
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @GetMapping("/executions/{executionId}")
    public FunctionExecutionResponse getExecution(@PathVariable Long executionId) {
        return executionService.getExecution(executionId);
    }
}
