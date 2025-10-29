package org.sensorvision.controller;

import jakarta.validation.Valid;
import org.sensorvision.dto.*;
import org.sensorvision.service.FunctionExecutionService;
import org.sensorvision.service.ServerlessFunctionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    public ServerlessFunctionController(
        ServerlessFunctionService functionService,
        FunctionExecutionService executionService
    ) {
        this.functionService = functionService;
        this.executionService = executionService;
    }

    /**
     * Get all serverless functions for current organization.
     */
    @GetMapping
    public List<ServerlessFunctionResponse> getAllFunctions() {
        return functionService.getAllFunctions();
    }

    /**
     * Get a specific function by ID.
     */
    @GetMapping("/{id}")
    public ServerlessFunctionResponse getFunction(@PathVariable Long id) {
        return functionService.getFunction(id);
    }

    /**
     * Create a new serverless function.
     */
    @PostMapping
    public ResponseEntity<ServerlessFunctionResponse> createFunction(
        @Valid @RequestBody ServerlessFunctionRequest request
    ) {
        ServerlessFunctionResponse response = functionService.createFunction(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update an existing serverless function.
     */
    @PutMapping("/{id}")
    public ServerlessFunctionResponse updateFunction(
        @PathVariable Long id,
        @Valid @RequestBody ServerlessFunctionRequest request
    ) {
        return functionService.updateFunction(id, request);
    }

    /**
     * Delete a serverless function.
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteFunction(@PathVariable Long id) {
        functionService.deleteFunction(id);
    }

    /**
     * Add a trigger to a function.
     */
    @PostMapping("/{id}/triggers")
    public ServerlessFunctionResponse addTrigger(
        @PathVariable Long id,
        @Valid @RequestBody FunctionTriggerRequest request
    ) {
        return functionService.addTrigger(id, request);
    }

    /**
     * Remove a trigger from a function.
     */
    @DeleteMapping("/{functionId}/triggers/{triggerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeTrigger(
        @PathVariable Long functionId,
        @PathVariable Long triggerId
    ) {
        functionService.removeTrigger(functionId, triggerId);
    }

    /**
     * Execute a function manually (for testing).
     */
    @PostMapping("/{id}/invoke")
    public FunctionExecutionResponse invokeFunction(
        @PathVariable Long id,
        @RequestBody FunctionInvokeRequest request
    ) {
        return executionService.executeFunction(id, request.input());
    }

    /**
     * Get execution history for a function.
     */
    @GetMapping("/{id}/executions")
    public Page<FunctionExecutionResponse> getExecutionHistory(
        @PathVariable Long id,
        Pageable pageable
    ) {
        return executionService.getExecutionHistory(id, pageable);
    }

    /**
     * Get details of a specific execution.
     */
    @GetMapping("/executions/{executionId}")
    public FunctionExecutionResponse getExecution(@PathVariable Long executionId) {
        return executionService.getExecution(executionId);
    }
}
