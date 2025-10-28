package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.DashboardResponse;
import org.sensorvision.dto.DashboardTemplateResponse;
import org.sensorvision.dto.InstantiateDashboardTemplateRequest;
import org.sensorvision.model.DashboardTemplateCategory;
import org.sensorvision.service.DashboardTemplateService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for Dashboard Templates
 */
@RestController
@RequestMapping("/api/v1/dashboard-templates")
@RequiredArgsConstructor
@Tag(name = "Dashboard Templates", description = "Pre-built dashboard templates for quick setup")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardTemplateController {

    private final DashboardTemplateService templateService;

    /**
     * Get all available dashboard templates
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get all dashboard templates", description = "Returns list of all available dashboard templates")
    public ResponseEntity<List<DashboardTemplateResponse>> getAllTemplates() {
        List<DashboardTemplateResponse> templates = templateService.getAllTemplates();
        return ResponseEntity.ok(templates);
    }

    /**
     * Get templates by category
     */
    @GetMapping("/category/{category}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get templates by category", description = "Returns templates filtered by category")
    public ResponseEntity<List<DashboardTemplateResponse>> getTemplatesByCategory(
            @PathVariable DashboardTemplateCategory category) {
        List<DashboardTemplateResponse> templates = templateService.getTemplatesByCategory(category);
        return ResponseEntity.ok(templates);
    }

    /**
     * Get a specific template with full configuration
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get template details", description = "Returns full template configuration")
    public ResponseEntity<DashboardTemplateResponse> getTemplate(@PathVariable Long id) {
        DashboardTemplateResponse template = templateService.getTemplate(id);
        return ResponseEntity.ok(template);
    }

    /**
     * Instantiate a dashboard from a template
     */
    @PostMapping("/{id}/instantiate")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create dashboard from template", description = "Instantiates a new dashboard from a template")
    public ResponseEntity<DashboardResponse> instantiateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody InstantiateDashboardTemplateRequest request) {
        DashboardResponse dashboard = templateService.instantiateTemplate(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dashboard);
    }

    /**
     * Delete a template (non-system templates only)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete template", description = "Delete a user-created template (system templates cannot be deleted)")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTemplate(@PathVariable Long id) {
        templateService.deleteTemplate(id);
    }
}
