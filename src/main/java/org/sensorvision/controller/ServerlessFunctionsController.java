package org.sensorvision.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Stub controller for Serverless Functions API
 * TODO: Implement full CRUD operations and execution engine
 */
@RestController
@RequestMapping("/api/v1/functions")
@CrossOrigin(origins = "*", maxAge = 3600)
public class ServerlessFunctionsController {

    /**
     * Get all serverless functions for the current organization
     * Currently returns empty array - full implementation pending
     */
    @GetMapping
    public ResponseEntity<List<Object>> getAllFunctions() {
        // Return empty array to prevent frontend .map() error
        return ResponseEntity.ok(Collections.emptyList());
    }

    /**
     * Get function by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<Object> getFunction(@PathVariable Long id) {
        return ResponseEntity.notFound().build();
    }

    /**
     * Create new function
     */
    @PostMapping
    public ResponseEntity<Object> createFunction(@RequestBody Object request) {
        return ResponseEntity.status(501).body("Serverless Functions feature is not yet fully implemented");
    }

    /**
     * Update function
     */
    @PutMapping("/{id}")
    public ResponseEntity<Object> updateFunction(@PathVariable Long id, @RequestBody Object request) {
        return ResponseEntity.status(501).body("Serverless Functions feature is not yet fully implemented");
    }

    /**
     * Delete function
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFunction(@PathVariable Long id) {
        return ResponseEntity.status(501).build();
    }

    /**
     * Invoke function
     */
    @PostMapping("/{id}/invoke")
    public ResponseEntity<Object> invokeFunction(@PathVariable Long id, @RequestBody Object request) {
        return ResponseEntity.status(501).body("Serverless Functions feature is not yet fully implemented");
    }

    /**
     * Get function execution history
     */
    @GetMapping("/{id}/executions")
    public ResponseEntity<List<Object>> getExecutions(@PathVariable Long id) {
        return ResponseEntity.ok(Collections.emptyList());
    }
}
