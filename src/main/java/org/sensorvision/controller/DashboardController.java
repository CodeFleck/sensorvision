package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.*;
import org.sensorvision.model.Dashboard;
import org.sensorvision.model.DashboardPermission;
import org.sensorvision.service.DashboardService;
import org.sensorvision.service.DashboardSharingService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/dashboards")
@RequiredArgsConstructor
@Tag(name = "Dashboards", description = "Dashboard and widget management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DashboardController {

    private final DashboardService dashboardService;
    private final DashboardSharingService dashboardSharingService;

    /**
     * Get all dashboards (without widgets)
     */
    @GetMapping
    public List<DashboardResponse> getAllDashboards() {
        return dashboardService.getAllDashboards();
    }

    /**
     * Get a specific dashboard with all widgets
     */
    @GetMapping("/{id}")
    public DashboardResponse getDashboard(@PathVariable Long id) {
        return dashboardService.getDashboardById(id);
    }

    /**
     * Get the default dashboard
     */
    @GetMapping("/default")
    public DashboardResponse getDefaultDashboard() {
        return dashboardService.getDefaultDashboard();
    }

    /**
     * Create a new dashboard
     */
    @PostMapping
    public ResponseEntity<DashboardResponse> createDashboard(@Valid @RequestBody DashboardCreateRequest request) {
        DashboardResponse response = dashboardService.createDashboard(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a dashboard
     */
    @PutMapping("/{id}")
    public DashboardResponse updateDashboard(@PathVariable Long id,
                                            @Valid @RequestBody DashboardUpdateRequest request) {
        return dashboardService.updateDashboard(id, request);
    }

    /**
     * Delete a dashboard
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDashboard(@PathVariable Long id) {
        dashboardService.deleteDashboard(id);
    }

    /**
     * Get all widgets for a dashboard
     */
    @GetMapping("/{id}/widgets")
    public List<WidgetResponse> getDashboardWidgets(@PathVariable Long id) {
        return dashboardService.getWidgetsByDashboard(id);
    }

    /**
     * Add a widget to a dashboard
     */
    @PostMapping("/{id}/widgets")
    public ResponseEntity<WidgetResponse> addWidget(@PathVariable Long id,
                                                     @Valid @RequestBody WidgetCreateRequest request) {
        WidgetResponse response = dashboardService.addWidget(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a widget
     */
    @PutMapping("/{dashboardId}/widgets/{widgetId}")
    public WidgetResponse updateWidget(@PathVariable Long dashboardId,
                                       @PathVariable Long widgetId,
                                       @Valid @RequestBody WidgetUpdateRequest request) {
        return dashboardService.updateWidget(widgetId, request);
    }

    /**
     * Delete a widget
     */
    @DeleteMapping("/{dashboardId}/widgets/{widgetId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteWidget(@PathVariable Long dashboardId,
                            @PathVariable Long widgetId) {
        dashboardService.deleteWidget(widgetId);
    }

    /**
     * Update dashboard sharing settings (public/private)
     */
    @PatchMapping("/{id}/sharing")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update dashboard sharing", description = "Enable or disable public sharing for a dashboard")
    public ResponseEntity<Map<String, Object>> updateSharingSettings(
            @PathVariable Long id,
            @Valid @RequestBody DashboardSharingRequest request) {
        Dashboard dashboard = dashboardSharingService.updateSharingSettings(id, request);

        Map<String, Object> response = new HashMap<>();
        response.put("isPublic", dashboard.getIsPublic());
        response.put("publicShareToken", dashboard.getPublicShareToken());
        if (dashboard.getPublicShareToken() != null) {
            response.put("publicShareUrl", "/public/dashboard/" + dashboard.getPublicShareToken());
        }

        return ResponseEntity.ok(response);
    }

    /**
     * Get all permissions for a dashboard
     */
    @GetMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Get dashboard permissions", description = "Returns all user permissions for a dashboard")
    public ResponseEntity<List<DashboardPermissionResponse>> getPermissions(@PathVariable Long id) {
        List<DashboardPermissionResponse> permissions = dashboardSharingService.getPermissions(id);
        return ResponseEntity.ok(permissions);
    }

    /**
     * Add a permission to a dashboard
     */
    @PostMapping("/{id}/permissions")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Add dashboard permission", description = "Grant a user permission to access a dashboard")
    public ResponseEntity<DashboardPermissionResponse> addPermission(
            @PathVariable Long id,
            @Valid @RequestBody DashboardPermissionRequest request) {
        DashboardPermissionResponse response = dashboardSharingService.addPermission(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update a permission level
     */
    @PatchMapping("/{dashboardId}/permissions/{permissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update permission level", description = "Change a user's permission level for a dashboard")
    public ResponseEntity<DashboardPermissionResponse> updatePermission(
            @PathVariable Long dashboardId,
            @PathVariable Long permissionId,
            @RequestBody Map<String, String> request) {
        DashboardPermission.PermissionLevel permissionLevel =
                DashboardPermission.PermissionLevel.valueOf(request.get("permissionLevel"));
        DashboardPermissionResponse response = dashboardSharingService.updatePermission(
                dashboardId, permissionId, permissionLevel);
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke a permission
     */
    @DeleteMapping("/{dashboardId}/permissions/{permissionId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Revoke permission", description = "Remove a user's access to a dashboard")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revokePermission(@PathVariable Long dashboardId, @PathVariable Long permissionId) {
        dashboardSharingService.revokePermission(dashboardId, permissionId);
    }
}
