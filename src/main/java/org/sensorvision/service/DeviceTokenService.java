package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Device;
import org.sensorvision.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

/**
 * Service for managing device API tokens.
 * Handles token generation, validation, and rotation for device-based authentication.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceTokenService {

    private final DeviceRepository deviceRepository;

    /**
     * Generate a new unique API token (UUID format)
     *
     * @return A new UUID token as a string
     */
    public String generateToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Validate if a token exists and is associated with a device
     *
     * @param token The API token to validate
     * @return true if the token is valid, false otherwise
     */
    public boolean validateToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        try {
            return deviceRepository.findByApiToken(token).isPresent();
        } catch (Exception e) {
            log.error("Error validating token: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get device by API token
     *
     * @param token The API token
     * @return Optional containing the device if found
     */
    public Optional<Device> getDeviceByToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return Optional.empty();
        }

        return deviceRepository.findByApiToken(token);
    }

    /**
     * Generate and assign a new token to a device
     *
     * @param device The device to assign the token to
     * @return The generated token
     */
    @Transactional
    public String assignTokenToDevice(Device device) {
        String token = generateToken();
        device.setApiToken(token);
        device.setTokenCreatedAt(LocalDateTime.now());
        deviceRepository.save(device);

        log.info("Generated new API token for device: {}", device.getExternalId());
        return token;
    }

    /**
     * Rotate (regenerate) the API token for a device
     *
     * @param deviceId The UUID of the device
     * @return The new token
     * @throws IllegalArgumentException if device not found
     */
    @Transactional
    public String rotateToken(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        String oldToken = device.getApiToken();
        String newToken = generateToken();

        device.setApiToken(newToken);
        device.setTokenCreatedAt(LocalDateTime.now());
        deviceRepository.save(device);

        log.info("Rotated API token for device: {} (old token: {}...)",
                device.getExternalId(),
                oldToken != null ? oldToken.substring(0, 8) : "none");

        return newToken;
    }

    /**
     * Revoke (remove) the API token from a device
     *
     * @param deviceId The UUID of the device
     * @throws IllegalArgumentException if device not found
     */
    @Transactional
    public void revokeToken(UUID deviceId) {
        Device device = deviceRepository.findById(deviceId)
                .orElseThrow(() -> new IllegalArgumentException("Device not found: " + deviceId));

        device.setApiToken(null);
        device.setTokenCreatedAt(null);
        device.setTokenLastUsedAt(null);
        deviceRepository.save(device);

        log.info("Revoked API token for device: {}", device.getExternalId());
    }

    /**
     * Update the last used timestamp for a token
     *
     * @param token The API token that was used
     */
    @Transactional
    public void updateTokenLastUsed(String token) {
        deviceRepository.findByApiToken(token).ifPresent(device -> {
            device.setTokenLastUsedAt(LocalDateTime.now());
            deviceRepository.save(device);
        });
    }

    /**
     * Check if a string looks like a device token (UUID format)
     *
     * @param token The token to check
     * @return true if it matches UUID pattern
     */
    public boolean isDeviceToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }

        // UUID format: 8-4-4-4-12 hex digits
        // Example: 550e8400-e29b-41d4-a716-446655440000
        String uuidPattern = "^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$";
        return token.matches(uuidPattern);
    }
}
