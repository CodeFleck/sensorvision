package org.sensorvision.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceCreateRequest;
import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.dto.DeviceUpdateRequest;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;
    private final EventService eventService;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        return deviceRepository.findByOrganization(userOrg).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String externalId) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        return toResponse(device);
    }

    public DeviceResponse createDevice(DeviceCreateRequest request) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();

        deviceRepository.findByExternalId(request.externalId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Device already exists: " + request.externalId());
                });

        Device device = Device.builder()
                .externalId(request.externalId())
                .name(request.name())
                .location(request.location())
                .sensorType(request.sensorType())
                .firmwareVersion(request.firmwareVersion())
                .status(DeviceStatus.UNKNOWN)
                .organization(userOrg)  // Set organization from current user
                .build();

        // Generate API token for new device (returns raw token)
        String rawToken = generateApiToken(device);

        Device saved = deviceRepository.save(device);

        // Emit device created event
        if (saved.getOrganization() != null) {
            eventService.emitDeviceLifecycleEvent(
                saved.getOrganization(),
                saved.getExternalId(),
                saved.getName(),
                Event.EventType.DEVICE_CREATED
            );
        }

        log.info("Device created with API token: {} (token starts with: {}..., stored as hash)",
                saved.getExternalId(),
                rawToken != null ? rawToken.substring(0, 8) : "none");

        return toResponse(saved);
    }

    public DeviceResponse updateDevice(String externalId, DeviceUpdateRequest request) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        device.setName(request.name());
        device.setLocation(request.location());
        device.setSensorType(request.sensorType());
        device.setFirmwareVersion(request.firmwareVersion());
        Device updated = deviceRepository.save(device);

        // Emit device updated event
        if (updated.getOrganization() != null) {
            eventService.emitDeviceLifecycleEvent(
                updated.getOrganization(),
                updated.getExternalId(),
                updated.getName(),
                Event.EventType.DEVICE_UPDATED
            );
        }

        return toResponse(updated);
    }

    public void deleteDevice(String externalId) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        // Emit device deleted event before deleting
        if (device.getOrganization() != null) {
            eventService.emitDeviceLifecycleEvent(
                device.getOrganization(),
                device.getExternalId(),
                device.getName(),
                Event.EventType.DEVICE_DELETED
            );
        }

        deviceRepository.delete(device);
    }

    /**
     * Generate or rotate API token for a device
     * Returns the raw (unhashed) token to be shown to the user ONCE
     */
    public String rotateDeviceToken(String externalId) {
        Organization userOrg = SecurityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        String rawToken = generateApiToken(device);
        Device saved = deviceRepository.save(device);

        log.info("API token rotated for device: {} (showing first 8 chars of hash: {}...)",
                externalId,
                saved.getApiToken() != null ? saved.getApiToken().substring(0, 8) : "none");

        // Emit token rotation event
        if (saved.getOrganization() != null) {
            eventService.createEvent(
                    saved.getOrganization(),
                    Event.EventType.DEVICE_UPDATED,
                    Event.EventSeverity.INFO,
                    "Device API Token Rotated",
                    String.format("API token rotated for device %s (%s)", saved.getName(), saved.getExternalId())
            );
        }

        // Return the raw token - this is the only time it will be visible
        return rawToken;
    }

    /**
     * Validate device API token and update last used timestamp
     * Tokens are hashed at rest, so we must check all devices and verify with BCrypt
     */
    @Transactional
    public Device authenticateDeviceByToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return null;
        }

        // We cannot query by token directly since it's hashed
        // We need a more efficient approach - find all devices with non-null tokens
        // In production, consider using a token prefix or separate token table
        List<Device> devicesWithTokens = deviceRepository.findAllByApiTokenIsNotNull();

        for (Device device : devicesWithTokens) {
            if (passwordEncoder.matches(rawToken, device.getApiToken())) {
                device.setTokenLastUsedAt(LocalDateTime.now());
                deviceRepository.save(device);
                log.debug("Device authenticated via token: {}", device.getExternalId());
                return device;
            }
        }

        log.debug("No device found matching provided token");
        return null;
    }

    /**
     * Get device by API token (read-only, no timestamp update)
     * Note: This is inefficient with hashed tokens, prefer authenticateDeviceByToken
     * @deprecated Use authenticateDeviceByToken instead
     */
    @Deprecated
    @Transactional(readOnly = true)
    public Device getDeviceByToken(String rawToken) {
        if (rawToken == null || rawToken.isEmpty()) {
            return null;
        }

        List<Device> devicesWithTokens = deviceRepository.findAllByApiTokenIsNotNull();
        for (Device device : devicesWithTokens) {
            if (passwordEncoder.matches(rawToken, device.getApiToken())) {
                return device;
            }
        }
        return null;
    }

    /**
     * Generate a new API token for a device
     * Returns the raw token (to be shown to user once), stores hashed version
     */
    private String generateApiToken(Device device) {
        // Generate a secure random token
        String rawToken = UUID.randomUUID().toString().replace("-", "");

        // Hash the token using BCrypt before storing
        String hashedToken = passwordEncoder.encode(rawToken);

        device.setApiToken(hashedToken);
        device.setTokenCreatedAt(LocalDateTime.now());
        device.setTokenLastUsedAt(null);

        log.debug("Generated API token for device {}, hash starts with: {}...",
                device.getExternalId(),
                hashedToken.substring(0, Math.min(10, hashedToken.length())));

        // Return raw token so it can be displayed to user
        return rawToken;
    }

    private DeviceResponse toResponse(Device device) {
        return new DeviceResponse(
                device.getExternalId(),
                device.getName(),
                device.getLocation(),
                device.getSensorType(),
                device.getFirmwareVersion(),
                device.getStatus(),
                device.getLastSeenAt()
        );
    }
}
