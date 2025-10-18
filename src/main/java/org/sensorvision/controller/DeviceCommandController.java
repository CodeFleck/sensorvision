package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.MqttPublishService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * REST API for sending commands to devices via MQTT
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}/commands")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "mqtt", name = "enabled", havingValue = "true", matchIfMissing = true)
public class DeviceCommandController {

    private final MqttPublishService mqttPublishService;
    private final DeviceRepository deviceRepository;

    /**
     * Send a command to a device
     * POST /api/v1/devices/{deviceId}/commands
     * Body: { "command": "toggle", "payload": { "target": "relay1" } }
     */
    @PostMapping
    public ResponseEntity<CommandResponse> sendCommand(
            @PathVariable String deviceId,
            @Valid @RequestBody CommandRequest request) {

        log.info("Sending command '{}' to device: {}", request.command(), deviceId);

        try {
            // Verify device ownership before sending command
            verifyDeviceOwnership(deviceId);

            mqttPublishService.publishCommand(deviceId, request.command(), request.payload());
            return ResponseEntity.ok(new CommandResponse(true, "Command sent successfully"));
        } catch (AccessDeniedException e) {
            log.warn("Access denied to device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommandResponse(false, "Access denied to device"));
        } catch (Exception e) {
            log.error("Failed to send command to device: {}", deviceId, e);
            return ResponseEntity.internalServerError()
                    .body(new CommandResponse(false, "Failed to send command: " + e.getMessage()));
        }
    }

    /**
     * Send a toggle command
     * POST /api/v1/devices/{deviceId}/commands/toggle
     * Body: { "target": "relay1" }
     */
    @PostMapping("/toggle")
    public ResponseEntity<CommandResponse> toggleCommand(
            @PathVariable String deviceId,
            @RequestBody Map<String, String> body) {

        String target = body.getOrDefault("target", "default");

        log.info("Sending toggle command for '{}' to device: {}", target, deviceId);

        try {
            // Verify device ownership before sending command
            verifyDeviceOwnership(deviceId);

            mqttPublishService.publishToggleCommand(deviceId, target);
            return ResponseEntity.ok(new CommandResponse(true, "Toggle command sent"));
        } catch (AccessDeniedException e) {
            log.warn("Access denied to device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommandResponse(false, "Access denied to device"));
        } catch (Exception e) {
            log.error("Failed to send toggle command", e);
            return ResponseEntity.internalServerError()
                    .body(new CommandResponse(false, "Failed: " + e.getMessage()));
        }
    }

    /**
     * Set a value on the device
     * POST /api/v1/devices/{deviceId}/commands/set-value
     * Body: { "variable": "led_brightness", "value": 75 }
     */
    @PostMapping("/set-value")
    public ResponseEntity<CommandResponse> setValueCommand(
            @PathVariable String deviceId,
            @RequestBody Map<String, Object> body) {

        String variable = (String) body.get("variable");
        Object value = body.get("value");

        log.info("Setting {} = {} on device: {}", variable, value, deviceId);

        try {
            // Verify device ownership before sending command
            verifyDeviceOwnership(deviceId);

            mqttPublishService.publishValueCommand(deviceId, variable, value);
            return ResponseEntity.ok(new CommandResponse(true, "Value set command sent"));
        } catch (AccessDeniedException e) {
            log.warn("Access denied to device: {}", deviceId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new CommandResponse(false, "Access denied to device"));
        } catch (Exception e) {
            log.error("Failed to send set value command", e);
            return ResponseEntity.internalServerError()
                    .body(new CommandResponse(false, "Failed: " + e.getMessage()));
        }
    }

    /**
     * Verify that the device belongs to the current user's organization
     */
    private void verifyDeviceOwnership(String deviceId) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }
    }

    // DTOs
    public record CommandRequest(
            String command,
            Object payload
    ) {}

    public record CommandResponse(
            boolean success,
            String message
    ) {}
}
