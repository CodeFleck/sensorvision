package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceTagRequest;
import org.sensorvision.dto.DeviceTagResponse;
import org.sensorvision.service.DeviceTagService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/device-tags")
@RequiredArgsConstructor
@Tag(name = "Device Tags", description = "Device tag management endpoints")
@SecurityRequirement(name = "Bearer Authentication")
public class DeviceTagController {

    private final DeviceTagService deviceTagService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get all device tags", description = "Returns all device tags for the current user's organization")
    public ResponseEntity<List<DeviceTagResponse>> getAllTags() {
        log.debug("REST request to get all device tags");
        List<DeviceTagResponse> tags = deviceTagService.getAllTags();
        return ResponseEntity.ok(tags);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get device tag by ID", description = "Returns a single device tag with its associated devices")
    public ResponseEntity<DeviceTagResponse> getTag(@PathVariable Long id) {
        log.debug("REST request to get device tag: {}", id);
        DeviceTagResponse tag = deviceTagService.getTag(id);
        return ResponseEntity.ok(tag);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Create device tag", description = "Creates a new device tag in the current user's organization")
    public ResponseEntity<DeviceTagResponse> createTag(@Valid @RequestBody DeviceTagRequest request) {
        log.debug("REST request to create device tag: {}", request.name());
        DeviceTagResponse created = deviceTagService.createTag(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Update device tag", description = "Updates an existing device tag's name and color")
    public ResponseEntity<DeviceTagResponse> updateTag(
            @PathVariable Long id,
            @Valid @RequestBody DeviceTagRequest request) {
        log.debug("REST request to update device tag: {}", id);
        DeviceTagResponse updated = deviceTagService.updateTag(id, request);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete device tag", description = "Deletes a device tag (admin only)")
    public ResponseEntity<Void> deleteTag(@PathVariable Long id) {
        log.debug("REST request to delete device tag: {}", id);
        deviceTagService.deleteTag(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/devices/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Add tag to device", description = "Adds a tag to a device")
    public ResponseEntity<DeviceTagResponse> addTagToDevice(
            @PathVariable Long id,
            @PathVariable String deviceId) {
        log.debug("REST request to add tag {} to device {}", id, deviceId);
        DeviceTagResponse updated = deviceTagService.addTagToDevice(id, deviceId);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{id}/devices/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    @Operation(summary = "Remove tag from device", description = "Removes a tag from a device")
    public ResponseEntity<DeviceTagResponse> removeTagFromDevice(
            @PathVariable Long id,
            @PathVariable String deviceId) {
        log.debug("REST request to remove tag {} from device {}", id, deviceId);
        DeviceTagResponse updated = deviceTagService.removeTagFromDevice(id, deviceId);
        return ResponseEntity.ok(updated);
    }

    @GetMapping("/by-device/{deviceId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER', 'VIEWER')")
    @Operation(summary = "Get tags by device", description = "Returns all tags assigned to a specific device")
    public ResponseEntity<List<DeviceTagResponse>> getTagsByDevice(@PathVariable String deviceId) {
        log.debug("REST request to get tags for device: {}", deviceId);
        List<DeviceTagResponse> tags = deviceTagService.getTagsByDevice(deviceId);
        return ResponseEntity.ok(tags);
    }
}
