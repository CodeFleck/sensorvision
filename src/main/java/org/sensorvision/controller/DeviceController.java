package org.sensorvision.controller;

import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.BulkDeviceOperationRequest;
import org.sensorvision.dto.BulkDeviceOperationResponse;
import org.sensorvision.dto.DeviceCreateRequest;
import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.dto.DeviceUpdateRequest;
import org.sensorvision.service.DeviceService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/devices")
@RequiredArgsConstructor
public class DeviceController {

    private final DeviceService deviceService;

    @GetMapping
    public List<DeviceResponse> getDevices() {
        return deviceService.getAllDevices();
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
    public record TokenRotationResponse(String deviceExternalId, String apiToken) {}
}
