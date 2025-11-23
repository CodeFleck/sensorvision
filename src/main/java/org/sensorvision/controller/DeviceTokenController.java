package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceTokenResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.DeviceService;
import org.sensorvision.service.DeviceTokenService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST API controller for managing device API tokens.
 * <p>
 * Provides endpoints for generating, rotating, viewing, and revoking device tokens.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/devices/{deviceId}/token")
@RequiredArgsConstructor
public class DeviceTokenController {

    private final DeviceService deviceService;
    private final DeviceTokenService deviceTokenService;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;

    /**
     * Generate a new API token for a device that doesn't have one
     * POST /api/v1/devices/{deviceId}/token/generate
     *
     * @param deviceId The device external ID
     * @return The newly generated token (only time it will be visible)
     */
    @PostMapping("/generate")
    @Transactional
    public ResponseEntity<DeviceTokenResponse> generateToken(@PathVariable String deviceId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }

        // Check if device already has a token
        if (device.getApiToken() != null && !device.getApiToken().isEmpty()) {
            return ResponseEntity.badRequest().body(DeviceTokenResponse.error(
                    "Device already has a token. Use /rotate to generate a new one."
            ));
        }

        String newToken = deviceTokenService.assignTokenToDevice(device);

        log.info("Generated API token for device: {}", deviceId);

        return ResponseEntity.ok(DeviceTokenResponse.success(
                deviceId,
                newToken,
                "Token generated successfully. Save this token securely - it won't be shown again!",
                device.getTokenCreatedAt()
        ));
    }

    /**
     * Rotate (regenerate) the API token for a device
     * POST /api/v1/devices/{deviceId}/token/rotate
     *
     * @param deviceId The device external ID
     * @return The new token (invalidates the old one)
     */
    @PostMapping("/rotate")
    @Transactional
    public ResponseEntity<DeviceTokenResponse> rotateToken(@PathVariable String deviceId) {
        String newToken = deviceService.rotateDeviceToken(deviceId);

        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        log.info("Rotated API token for device: {}", deviceId);

        return ResponseEntity.ok(DeviceTokenResponse.success(
                deviceId,
                newToken,
                "Token rotated successfully. Update your device configuration with the new token.",
                device.getTokenCreatedAt()
        ));
    }

    /**
     * Get current token information (token is masked for security)
     * GET /api/v1/devices/{deviceId}/token
     *
     * @param deviceId The device external ID
     * @return Masked token information
     */
    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<DeviceTokenResponse> getTokenInfo(@PathVariable String deviceId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }

        if (device.getApiToken() == null || device.getApiToken().isEmpty()) {
            return ResponseEntity.ok(DeviceTokenResponse.noToken(deviceId));
        }

        // Mask the token for security (show first 8 chars only)
        String maskedToken = device.getApiToken().substring(0, 8) + "..." +
                device.getApiToken().substring(device.getApiToken().length() - 4);

        return ResponseEntity.ok(DeviceTokenResponse.masked(
                deviceId,
                maskedToken,
                device.getTokenCreatedAt(),
                device.getTokenLastUsedAt()
        ));
    }

    /**
     * Revoke (delete) the API token for a device
     * DELETE /api/v1/devices/{deviceId}/token
     *
     * @param deviceId The device external ID
     * @return Success message
     */
    @DeleteMapping
    @Transactional
    public ResponseEntity<DeviceTokenResponse> revokeToken(@PathVariable String deviceId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        Device device = deviceRepository.findByExternalId(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + deviceId);
        }

        deviceTokenService.revokeToken(device.getId());

        log.info("Revoked API token for device: {}", deviceId);

        return ResponseEntity.ok(DeviceTokenResponse.revoked(deviceId));
    }
}
