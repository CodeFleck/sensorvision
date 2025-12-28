package io.indcloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.VariableRequest;
import io.indcloud.dto.VariableResponse;
import io.indcloud.service.VariableService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/variables")
@RequiredArgsConstructor
@Tag(name = "Variables", description = "Variable metadata management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class VariableController {

    private final VariableService variableService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get all variables", description = "Returns all variable definitions for the current user's organization")
    public ResponseEntity<List<VariableResponse>> getAllVariables() {
        log.debug("REST request to get all variables");
        List<VariableResponse> variables = variableService.getAllVariables();
        return ResponseEntity.ok(variables);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get variable by ID", description = "Returns a single variable definition")
    public ResponseEntity<VariableResponse> getVariable(@PathVariable Long id) {
        log.debug("REST request to get variable: {}", id);
        VariableResponse variable = variableService.getVariable(id);
        return ResponseEntity.ok(variable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create variable", description = "Creates a new variable definition in the current user's organization")
    public ResponseEntity<VariableResponse> createVariable(@Valid @RequestBody VariableRequest request) {
        log.debug("REST request to create variable: {}", request.name());
        VariableResponse created = variableService.createVariable(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update variable", description = "Updates an existing variable definition")
    public ResponseEntity<VariableResponse> updateVariable(
            @PathVariable Long id,
            @Valid @RequestBody VariableRequest request) {
        log.debug("REST request to update variable: {}", id);
        VariableResponse updated = variableService.updateVariable(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete variable", description = "Deletes a variable definition (admin only, cannot delete system variables)")
    public ResponseEntity<Void> deleteVariable(@PathVariable Long id) {
        log.debug("REST request to delete variable: {}", id);
        variableService.deleteVariable(id);
        return ResponseEntity.noContent().build();
    }
}
