package io.indcloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.DeviceGroupRequest;
import io.indcloud.dto.DeviceGroupResponse;
import io.indcloud.service.DeviceGroupService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/device-groups")
@RequiredArgsConstructor
@Tag(name = "Device Groups", description = "Device group management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceGroupController {

    private final DeviceGroupService deviceGroupService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get all device groups", description = "Returns all device groups for the current user's organization")
    public ResponseEntity<List<DeviceGroupResponse>> getAllGroups() {
        log.debug("REST request to get all device groups");
        List<DeviceGroupResponse> groups = deviceGroupService.getAllGroups();
        return ResponseEntity.ok(groups);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get device group by ID", description = "Returns a single device group with its members")
    public ResponseEntity<DeviceGroupResponse> getGroup(@PathVariable Long id) {
        log.debug("REST request to get device group: {}", id);
        DeviceGroupResponse group = deviceGroupService.getGroup(id);
        return ResponseEntity.ok(group);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create device group", description = "Creates a new device group in the current user's organization")
    public ResponseEntity<DeviceGroupResponse> createGroup(@Valid @RequestBody DeviceGroupRequest request) {
        log.debug("REST request to create device group: {}", request.name());
        DeviceGroupResponse created = deviceGroupService.createGroup(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update device group", description = "Updates an existing device group's name and description")
    public ResponseEntity<DeviceGroupResponse> updateGroup(
            @PathVariable Long id,
            @Valid @RequestBody DeviceGroupRequest request) {
        log.debug("REST request to update device group: {}", id);
        DeviceGroupResponse updated = deviceGroupService.updateGroup(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device group", description = "Deletes a device group (admin only)")
    public ResponseEntity<Void> deleteGroup(@PathVariable Long id) {
        log.debug("REST request to delete device group: {}", id);
        deviceGroupService.deleteGroup(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devices/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Add device to group", description = "Adds a device to a device group")
    public ResponseEntity<DeviceGroupResponse> addDeviceToGroup(
            @PathVariable Long id,
            @PathVariable String deviceId) {
        log.debug("REST request to add device {} to group {}", deviceId, id);
        DeviceGroupResponse updated = deviceGroupService.addDeviceToGroup(id, deviceId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/devices/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Remove device from group", description = "Removes a device from a device group")
    public ResponseEntity<DeviceGroupResponse> removeDeviceFromGroup(
            @PathVariable Long id,
            @PathVariable String deviceId) {
        log.debug("REST request to remove device {} from group {}", deviceId, id);
        DeviceGroupResponse updated = deviceGroupService.removeDeviceFromGroup(id, deviceId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/by-device/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get groups by device", description = "Returns all device groups containing a specific device")
    public ResponseEntity<List<DeviceGroupResponse>> getGroupsByDevice(@PathVariable String deviceId) {
        log.debug("REST request to get groups for device: {}", deviceId);
        List<DeviceGroupResponse> groups = deviceGroupService.getGroupsByDevice(deviceId);
        return ResponseEntity.ok(groups);
    }
}
