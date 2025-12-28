package io.indcloud.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.BulkDeviceOperationRequest;
import io.indcloud.dto.BulkDeviceOperationResponse;
import io.indcloud.dto.DeviceCreateRequest;
import io.indcloud.dto.DeviceResponse;
import io.indcloud.dto.DeviceUpdateRequest;
import io.indcloud.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceResponse> getDevices(
            @RequestParam(required = false) String tagName,
            @RequestParam(required = false) Long groupId) {
        return deviceService.getAllDevices(tagName, groupId);
    }

    @GetMapping("/{externalId}")
    public DeviceResponse getDevice(@PathVariable String externalId) {
        return deviceService.getDevice(externalId);
    }

    @PostMapping
    public ResponseEntity<DeviceResponse> createDevice(@Valid @RequestBody DeviceCreateRequest request) {
        DeviceResponse response = deviceService.createDevice(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{externalId}")
    public DeviceResponse updateDevice(@PathVariable String externalId,
            @Valid @RequestBody DeviceUpdateRequest request) {
        return deviceService.updateDevice(externalId, request);
    }

    @DeleteMapping("/{externalId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDevice(@PathVariable String externalId) {
        deviceService.deleteDevice(externalId);
    }

    @PostMapping("/{externalId}/rotate-token")
    public ResponseEntity<TokenRotationResponse> rotateDeviceToken(@PathVariable String externalId) {
        String newToken = deviceService.rotateDeviceToken(externalId);
        return ResponseEntity.ok(new TokenRotationResponse(externalId, newToken));
    }

    @PostMapping("/bulk")
    public ResponseEntity<BulkDeviceOperationResponse> performBulkOperation(
            @Valid @RequestBody BulkDeviceOperationRequest request) {
        BulkDeviceOperationResponse response = deviceService.performBulkOperation(request);
        return ResponseEntity.ok(response);
    }

    // DTO for token rotation response
    public record TokenRotationResponse(String deviceExternalId, String apiToken) {
    }
}
