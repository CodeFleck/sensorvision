package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceVariableResponse;
import org.sensorvision.dto.VariableStatisticsResponse;
import org.sensorvision.dto.VariableValueResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.Variable;
import org.sensorvision.model.VariableValue;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.VariableRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.DynamicVariableService;
import org.sensorvision.service.DynamicVariableService.VariableStatistics;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for device-specific variables (EAV pattern).
 * Provides endpoints to query and manage dynamic variables per device.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}/variables")
@RequiredArgsConstructor
@Tag(name = "Device Variables", description = "Device-specific variable management (EAV pattern)")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceVariableController {

    private final DynamicVariableService dynamicVariableService;
    private final VariableRepository variableRepository;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get all variables for a device",
            description = "Returns all dynamic variables that have been auto-provisioned for this device")
    public ResponseEntity<List<DeviceVariableResponse>> getDeviceVariables(
            @PathVariable UUID deviceId) {
        log.debug("REST request to get variables for device: {}", deviceId);

        // Verify device access
        Device device = getDeviceWithAccessCheck(deviceId);

        List<Variable> variables = dynamicVariableService.getDeviceVariables(deviceId);
        List<DeviceVariableResponse> response = variables.stream()
                .map(DeviceVariableResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get latest values for all device variables",
            description = "Returns a map of variable names to their most recent values")
    public ResponseEntity<Map<String, BigDecimal>> getLatestValues(
            @PathVariable UUID deviceId) {
        log.debug("REST request to get latest values for device: {}", deviceId);

        // Verify device access
        Device device = getDeviceWithAccessCheck(deviceId);

        Map<String, BigDecimal> latestValues = dynamicVariableService.getLatestValues(deviceId);
        return ResponseEntity.ok(latestValues);
    }

    @GetMapping("/{variableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get a specific variable",
            description = "Returns details of a specific variable")
    public ResponseEntity<DeviceVariableResponse> getVariable(
            @PathVariable UUID deviceId,
            @PathVariable Long variableId) {
        log.debug("REST request to get variable {} for device {}", variableId, deviceId);

        Variable variable = getVariableWithAccessCheck(deviceId, variableId);
        return ResponseEntity.ok(DeviceVariableResponse.from(variable));
    }

    @GetMapping("/{variableId}/values")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get time-series values for a variable",
            description = "Returns historical values for a variable within a time range")
    public ResponseEntity<List<VariableValueResponse>> getVariableHistory(
            @PathVariable UUID deviceId,
            @PathVariable Long variableId,
            @Parameter(description = "Start time (ISO 8601 format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time (ISO 8601 format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        log.debug("REST request to get history for variable {} (device {})", variableId, deviceId);

        Variable variable = getVariableWithAccessCheck(deviceId, variableId);

        // Default to last 24 hours if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        List<VariableValue> values = dynamicVariableService.getVariableHistory(variableId, startTime, endTime);
        List<VariableValueResponse> response = values.stream()
                .map(VariableValueResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{variableId}/values/latest")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get latest N values for a variable",
            description = "Returns the most recent N values for a variable")
    public ResponseEntity<List<VariableValueResponse>> getLatestVariableValues(
            @PathVariable UUID deviceId,
            @PathVariable Long variableId,
            @Parameter(description = "Number of values to return (default: 100)")
            @RequestParam(defaultValue = "100") int count) {
        log.debug("REST request to get latest {} values for variable {} (device {})",
                count, variableId, deviceId);

        Variable variable = getVariableWithAccessCheck(deviceId, variableId);

        // Limit to reasonable maximum
        count = Math.min(count, 1000);

        List<VariableValue> values = dynamicVariableService.getLatestValues(variableId, count);
        List<VariableValueResponse> response = values.stream()
                .map(VariableValueResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{variableId}/statistics")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER', 'DEVELOPER')")
    @Operation(summary = "Get statistics for a variable",
            description = "Returns aggregated statistics (avg, min, max, sum, count) for a variable")
    public ResponseEntity<VariableStatisticsResponse> getVariableStatistics(
            @PathVariable UUID deviceId,
            @PathVariable Long variableId,
            @Parameter(description = "Start time (ISO 8601 format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
            @Parameter(description = "End time (ISO 8601 format)")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime) {
        log.debug("REST request to get statistics for variable {} (device {})", variableId, deviceId);

        Variable variable = getVariableWithAccessCheck(deviceId, variableId);

        // Default to last 24 hours if no time range specified
        if (startTime == null) {
            startTime = Instant.now().minus(24, ChronoUnit.HOURS);
        }
        if (endTime == null) {
            endTime = Instant.now();
        }

        VariableStatistics stats = dynamicVariableService.getStatistics(variableId, startTime, endTime);
        VariableStatisticsResponse response = VariableStatisticsResponse.from(
                variableId, variable.getName(), startTime, endTime, stats);

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{variableId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update variable metadata",
            description = "Update display name, unit, color, etc. for a device variable")
    public ResponseEntity<DeviceVariableResponse> updateVariable(
            @PathVariable UUID deviceId,
            @PathVariable Long variableId,
            @RequestBody UpdateVariableRequest request) {
        log.debug("REST request to update variable {} (device {})", variableId, deviceId);

        Variable variable = getVariableWithAccessCheck(deviceId, variableId);

        // Update allowed fields
        if (request.displayName() != null) {
            variable.setDisplayName(request.displayName());
        }
        if (request.description() != null) {
            variable.setDescription(request.description());
        }
        if (request.unit() != null) {
            variable.setUnit(request.unit());
        }
        if (request.icon() != null) {
            variable.setIcon(request.icon());
        }
        if (request.color() != null) {
            variable.setColor(request.color());
        }
        if (request.decimalPlaces() != null) {
            variable.setDecimalPlaces(request.decimalPlaces());
        }
        if (request.minValue() != null) {
            variable.setMinValue(request.minValue());
        }
        if (request.maxValue() != null) {
            variable.setMaxValue(request.maxValue());
        }

        Variable updated = variableRepository.save(variable);
        return ResponseEntity.ok(DeviceVariableResponse.from(updated));
    }

    /**
     * Get device and verify the current user has access.
     */
    private Device getDeviceWithAccessCheck(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        Long currentOrgId = securityUtils.getCurrentUserOrganization().getId();
        if (!device.getOrganization().getId().equals(currentOrgId)) {
            throw new ResourceNotFoundException("Device not found: " + deviceId);
        }

        return device;
    }

    /**
     * Get variable and verify it belongs to the device and user has access.
     */
    private Variable getVariableWithAccessCheck(UUID deviceId, Long variableId) {
        // First verify device access
        Device device = getDeviceWithAccessCheck(deviceId);

        Variable variable = variableRepository.findById(variableId)
                .orElseThrow(() -> new ResourceNotFoundException("Variable not found: " + variableId));

        // Verify variable belongs to this device
        if (variable.getDevice() == null || !variable.getDevice().getId().equals(deviceId)) {
            throw new ResourceNotFoundException("Variable not found: " + variableId);
        }

        return variable;
    }

    /**
     * Request DTO for updating variable metadata.
     */
    public record UpdateVariableRequest(
            String displayName,
            String description,
            String unit,
            String icon,
            String color,
            Integer decimalPlaces,
            Double minValue,
            Double maxValue
    ) {}
}
