package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.DeviceLocationRequest;
import org.sensorvision.dto.DeviceLocationResponse;
import org.sensorvision.dto.LocationHistoryResponse;
import org.sensorvision.service.LocationTrackingService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/locations")
@RequiredArgsConstructor
@Tag(name = "Location Tracking", description = "APIs for device location tracking and history")
@PreAuthorize("isAuthenticated()")
public class LocationController {

    private final LocationTrackingService locationTrackingService;

    @GetMapping
    @Operation(summary = "Get all device locations", description = "Retrieve current locations of all devices")
    public ResponseEntity<List<DeviceLocationResponse>> getAllDeviceLocations() {
        return ResponseEntity.ok(locationTrackingService.getAllDeviceLocations());
    }

    @GetMapping("/devices/{deviceId}")
    @Operation(summary = "Get device location", description = "Retrieve current location of a specific device")
    public ResponseEntity<DeviceLocationResponse> getDeviceLocation(@PathVariable UUID deviceId) {
        return ResponseEntity.ok(locationTrackingService.getDeviceLocation(deviceId));
    }

    @PutMapping("/devices/{deviceId}")
    @Operation(summary = "Update device location", description = "Update the location of a device")
    public ResponseEntity<DeviceLocationResponse> updateDeviceLocation(
            @PathVariable UUID deviceId,
            @Valid @RequestBody DeviceLocationRequest request) {
        return ResponseEntity.ok(locationTrackingService.updateDeviceLocation(deviceId, request));
    }

    @GetMapping("/devices/{deviceId}/history")
    @Operation(summary = "Get location history", description = "Retrieve location history for a device")
    public ResponseEntity<LocationHistoryResponse> getLocationHistory(
            @PathVariable UUID deviceId,
            @Parameter(description = "Start time (ISO 8601 format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "End time (ISO 8601 format)")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @Parameter(description = "Maximum number of points to return")
            @RequestParam(required = false, defaultValue = "1000") Integer limit) {
        return ResponseEntity.ok(locationTrackingService.getLocationHistory(deviceId, startTime, endTime, limit));
    }
}
