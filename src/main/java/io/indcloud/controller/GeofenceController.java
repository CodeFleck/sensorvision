package io.indcloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.*;
import io.indcloud.service.GeofenceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/geofences")
@RequiredArgsConstructor
@Tag(name = "Geofence Management", description = "APIs for managing geofences and location-based alerts")
@PreAuthorize("isAuthenticated()")
public class GeofenceController {

    private final GeofenceService geofenceService;

    @GetMapping
    @Operation(summary = "Get all geofences", description = "Retrieve all geofences for the organization")
    public ResponseEntity<List<GeofenceResponse>> getAllGeofences() {
        return ResponseEntity.ok(geofenceService.getAllGeofences());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get geofence by ID", description = "Retrieve a specific geofence by its ID")
    public ResponseEntity<GeofenceResponse> getGeofence(@PathVariable Long id) {
        return ResponseEntity.ok(geofenceService.getGeofence(id));
    }

    @PostMapping
    @Operation(summary = "Create geofence", description = "Create a new geofence")
    public ResponseEntity<GeofenceResponse> createGeofence(@Valid @RequestBody GeofenceRequest request) {
        GeofenceResponse response = geofenceService.createGeofence(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update geofence", description = "Update an existing geofence")
    public ResponseEntity<GeofenceResponse> updateGeofence(
            @PathVariable Long id,
            @Valid @RequestBody GeofenceRequest request) {
        return ResponseEntity.ok(geofenceService.updateGeofence(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete geofence", description = "Delete a geofence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteGeofence(@PathVariable Long id) {
        geofenceService.deleteGeofence(id);
    }

    @GetMapping("/{id}/assignments")
    @Operation(summary = "Get geofence assignments", description = "Get all device assignments for a geofence")
    public ResponseEntity<List<GeofenceAssignmentResponse>> getGeofenceAssignments(@PathVariable Long id) {
        return ResponseEntity.ok(geofenceService.getGeofenceAssignments(id));
    }

    @PostMapping("/{id}/assignments")
    @Operation(summary = "Assign device to geofence", description = "Assign a device to a geofence for monitoring")
    public ResponseEntity<GeofenceAssignmentResponse> assignDeviceToGeofence(
            @PathVariable Long id,
            @Valid @RequestBody GeofenceAssignmentRequest request) {
        GeofenceAssignmentResponse response = geofenceService.assignDeviceToGeofence(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{geofenceId}/assignments/{deviceId}")
    @Operation(summary = "Unassign device from geofence", description = "Remove a device assignment from a geofence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassignDeviceFromGeofence(
            @PathVariable Long geofenceId,
            @PathVariable UUID deviceId) {
        geofenceService.unassignDeviceFromGeofence(geofenceId, deviceId);
    }
}
