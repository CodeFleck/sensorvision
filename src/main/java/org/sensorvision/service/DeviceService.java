package org.sensorvision.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.BulkDeviceOperationRequest;
import org.sensorvision.dto.BulkDeviceOperationResponse;
import org.sensorvision.dto.DeviceCreateRequest;
import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.dto.DeviceUpdateRequest;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Organization;
import org.sensorvision.model.DeviceTag;
import org.sensorvision.model.DeviceGroup;
import org.sensorvision.dto.DeviceTagDto;
import org.sensorvision.dto.DeviceGroupDto;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.DeviceTagRepository;
import org.sensorvision.repository.DeviceGroupRepository;
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
    private final DeviceTagRepository deviceTagRepository;
    private final DeviceGroupRepository deviceGroupRepository;
    private final EventService eventService;
    private final PasswordEncoder passwordEncoder;
    private final DeviceTokenService deviceTokenService;
    private final SecurityUtils securityUtils;
    private final DeviceHealthService deviceHealthService;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices(String tagName, Long groupId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        List<Device> devices;

        if (tagName != null && !tagName.isBlank()) {
            devices = deviceRepository.findByOrganizationAndTagName(userOrg, tagName);
        } else if (groupId != null) {
            devices = deviceRepository.findByOrganizationAndGroupId(userOrg, groupId);
        } else {
            devices = deviceRepository.findByOrganization(userOrg);
        }

        return devices.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String externalId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        return toResponse(device);
    }

    public DeviceResponse createDevice(DeviceCreateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        deviceRepository.findByExternalId(request.externalId())
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Device already exists: " + request.externalId());
                });

        Device device = Device.builder()
                .externalId(request.externalId())
                .name(request.name())
                .description(request.description())
                .active(request.active() != null ? request.active() : true)
                .location(request.location())
                .sensorType(request.sensorType())
                .firmwareVersion(request.firmwareVersion())
                .status(DeviceStatus.UNKNOWN)
                .organization(userOrg) // Set organization from current user
                .build();

        // Handle Tags
        if (request.tags() != null) {
            Set<DeviceTag> tags = new HashSet<>();
            for (String tagName : request.tags()) {
                DeviceTag tag = deviceTagRepository.findByOrganizationIdAndName(userOrg.getId(), tagName)
                        .orElseGet(() -> deviceTagRepository.save(
                                DeviceTag.builder()
                                        .organization(userOrg)
                                        .name(tagName)
                                        .color("#FF5733") // Default color
                                        .build()));
                tags.add(tag);
            }
            device.setTags(tags);
        }

        Device saved = deviceRepository.save(device);

        // Handle Groups (must be done after save because DeviceGroup owns the
        // relationship)
        if (request.groupIds() != null) {
            for (Long groupId : request.groupIds()) {
                DeviceGroup group = deviceGroupRepository.findById(groupId)
                        .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

                if (!group.getOrganization().getId().equals(userOrg.getId())) {
                    throw new AccessDeniedException("Access denied to group: " + groupId);
                }

                group.getDevices().add(saved);
                deviceGroupRepository.save(group);
            }
        }

        // Generate API token for new device using DeviceTokenService
        String apiToken = deviceTokenService.assignTokenToDevice(saved);

        // Emit device created event
        if (saved.getOrganization() != null) {
            eventService.emitDeviceLifecycleEvent(
                    saved.getOrganization(),
                    saved.getExternalId(),
                    saved.getName(),
                    Event.EventType.DEVICE_CREATED);
        }

        log.info("Device created with API token: {} (token: {}...)",
                saved.getExternalId(),
                apiToken != null ? apiToken.substring(0, 8) : "none");

        return toResponse(saved);
    }

    public DeviceResponse updateDevice(String externalId, DeviceUpdateRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        device.setName(request.name());
        device.setDescription(request.description());
        if (request.active() != null) {
            device.setActive(request.active());
        }
        device.setLocation(request.location());
        device.setSensorType(request.sensorType());
        device.setFirmwareVersion(request.firmwareVersion());

        // Update Tags
        if (request.tags() != null) {
            Set<DeviceTag> tags = new HashSet<>();
            for (String tagName : request.tags()) {
                DeviceTag tag = deviceTagRepository.findByOrganizationIdAndName(userOrg.getId(), tagName)
                        .orElseGet(() -> deviceTagRepository.save(
                                DeviceTag.builder()
                                        .organization(userOrg)
                                        .name(tagName)
                                        .color("#FF5733")
                                        .build()));
                tags.add(tag);
            }
            device.setTags(tags);
        }

        Device updated = deviceRepository.save(device);

        // Update Groups
        if (request.groupIds() != null) {
            // Remove from existing groups
            // Note: This is inefficient for large datasets but acceptable for single device
            // update
            List<DeviceGroup> currentGroups = new ArrayList<>(device.getGroups());
            for (DeviceGroup group : currentGroups) {
                if (!request.groupIds().contains(group.getId())) {
                    group.getDevices().remove(device);
                    deviceGroupRepository.save(group);
                }
            }

            // Add to new groups
            for (Long groupId : request.groupIds()) {
                DeviceGroup group = deviceGroupRepository.findById(groupId)
                        .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

                if (!group.getOrganization().getId().equals(userOrg.getId())) {
                    throw new AccessDeniedException("Access denied to group: " + groupId);
                }

                if (!group.getDevices().contains(device)) {
                    group.getDevices().add(device);
                    deviceGroupRepository.save(group);
                }
            }
        }

        // Emit device updated event
        if (updated.getOrganization() != null) {
            eventService.emitDeviceLifecycleEvent(
                    updated.getOrganization(),
                    updated.getExternalId(),
                    updated.getName(),
                    Event.EventType.DEVICE_UPDATED);
        }

        return toResponse(updated);
    }

    public void deleteDevice(String externalId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
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
                    Event.EventType.DEVICE_DELETED);
        }

        deviceRepository.delete(device);
    }

    /**
     * Generate or rotate API token for a device
     * Returns the raw token to be shown to the user
     */
    public String rotateDeviceToken(String externalId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        String newToken = deviceTokenService.rotateToken(device.getId());

        log.info("API token rotated for device: {} (token: {}...)",
                externalId,
                newToken != null ? newToken.substring(0, 8) : "none");

        // Emit token rotation event
        if (device.getOrganization() != null) {
            eventService.createEvent(
                    device.getOrganization(),
                    Event.EventType.DEVICE_UPDATED,
                    Event.EventSeverity.INFO,
                    "Device API Token Rotated",
                    String.format("API token rotated for device %s (%s)", device.getName(), device.getExternalId()));
        }

        // Return the raw token - this is the only time it will be visible
        return newToken;
    }

    /**
     * Get device by external ID (convenience method for auto-provisioning)
     */
    public Device getOrCreateDevice(String externalId, Organization organization) {
        return deviceRepository.findByExternalId(externalId)
                .orElseGet(() -> {
                    log.info("Auto-creating device: {} for organization: {}",
                            externalId, organization.getName());

                    Device device = Device.builder()
                            .externalId(externalId)
                            .name(externalId) // Use externalId as default name
                            .active(true)
                            .status(DeviceStatus.UNKNOWN)
                            .organization(organization)
                            .build();

                    Device saved = deviceRepository.save(device);

                    // Generate API token for auto-created device
                    deviceTokenService.assignTokenToDevice(saved);

                    // Emit device created event
                    eventService.emitDeviceLifecycleEvent(
                            organization,
                            saved.getExternalId(),
                            saved.getName(),
                            Event.EventType.DEVICE_CREATED);

                    return saved;
                });
    }

    /**
     * Perform bulk operations on multiple devices
     */
    public BulkDeviceOperationResponse performBulkOperation(BulkDeviceOperationRequest request) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();

        List<String> successfulIds = new ArrayList<>();
        Map<String, String> failures = new HashMap<>();

        for (String deviceId : request.getDeviceIds()) {
            try {
                Device device = deviceRepository.findByExternalId(deviceId)
                        .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + deviceId));

                // Verify device belongs to user's organization
                if (!device.getOrganization().getId().equals(userOrg.getId())) {
                    failures.put(deviceId, "Access denied");
                    continue;
                }

                // Perform the requested operation
                switch (request.getOperation()) {
                    case DELETE:
                        deviceRepository.delete(device);
                        eventService.emitDeviceLifecycleEvent(
                                userOrg, deviceId, device.getName(), Event.EventType.DEVICE_DELETED);
                        break;

                    case ENABLE:
                        device.setStatus(DeviceStatus.ONLINE);
                        deviceRepository.save(device);
                        break;

                    case DISABLE:
                        device.setStatus(DeviceStatus.OFFLINE);
                        deviceRepository.save(device);
                        break;

                    case UPDATE_STATUS:
                        if (request.getParameters() != null && request.getParameters().containsKey("status")) {
                            String statusStr = (String) request.getParameters().get("status");
                            device.setStatus(DeviceStatus.valueOf(statusStr));
                            deviceRepository.save(device);
                        }
                        break;

                    case ASSIGN_TAGS:
                        if (request.getParameters() != null && request.getParameters().containsKey("tags")) {
                            List<String> tags = (List<String>) request.getParameters().get("tags");
                            for (String tagName : tags) {
                                DeviceTag tag = deviceTagRepository
                                        .findByOrganizationIdAndName(userOrg.getId(), tagName)
                                        .orElseGet(() -> deviceTagRepository.save(
                                                DeviceTag.builder().organization(userOrg).name(tagName).color("#FF5733")
                                                        .build()));
                                device.getTags().add(tag);
                            }
                            deviceRepository.save(device);
                        }
                        break;

                    case REMOVE_TAGS:
                        if (request.getParameters() != null && request.getParameters().containsKey("tags")) {
                            List<String> tagsToRemove = (List<String>) request.getParameters().get("tags");
                            device.getTags().removeIf(t -> tagsToRemove.contains(t.getName()));
                            deviceRepository.save(device);
                        }
                        break;

                    case ASSIGN_GROUP:
                        if (request.getParameters() != null && request.getParameters().containsKey("groupId")) {
                            // Handle both Integer and Long (JSON numbers might be Integer)
                            Number groupIdNum = (Number) request.getParameters().get("groupId");
                            Long groupId = groupIdNum.longValue();

                            DeviceGroup group = deviceGroupRepository.findById(groupId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

                            if (!group.getOrganization().getId().equals(userOrg.getId())) {
                                throw new AccessDeniedException("Access denied to group: " + groupId);
                            }

                            group.getDevices().add(device);
                            deviceGroupRepository.save(group);
                        }
                        break;

                    case REMOVE_FROM_GROUP:
                        if (request.getParameters() != null && request.getParameters().containsKey("groupId")) {
                            Number groupIdNum = (Number) request.getParameters().get("groupId");
                            Long groupId = groupIdNum.longValue();

                            DeviceGroup group = deviceGroupRepository.findById(groupId)
                                    .orElseThrow(() -> new ResourceNotFoundException("Group not found: " + groupId));

                            if (!group.getOrganization().getId().equals(userOrg.getId())) {
                                throw new AccessDeniedException("Access denied to group: " + groupId);
                            }

                            group.getDevices().remove(device);
                            deviceGroupRepository.save(group);
                        }
                        break;

                    default:
                        failures.put(deviceId, "Unsupported operation: " + request.getOperation());
                        continue;
                }

                successfulIds.add(deviceId);

            } catch (Exception e) {
                log.error("Failed to perform bulk operation on device {}: {}", deviceId, e.getMessage());
                failures.put(deviceId, e.getMessage());
            }
        }

        String message = String.format("Bulk operation completed: %d succeeded, %d failed",
                successfulIds.size(), failures.size());

        return BulkDeviceOperationResponse.builder()
                .totalRequested(request.getDeviceIds().size())
                .successCount(successfulIds.size())
                .failureCount(failures.size())
                .successfulDeviceIds(successfulIds)
                .failedDevices(failures)
                .message(message)
                .build();
    }

    private DeviceResponse toResponse(Device device) {
        Integer healthScore = device.getHealthScore() != null ? device.getHealthScore() : 100;
        String healthStatus = deviceHealthService.getHealthStatus(healthScore);

        return new DeviceResponse(
                device.getExternalId(),
                device.getName(),
                device.getDescription(),
                device.getActive(),
                device.getLocation(),
                device.getSensorType(),
                device.getFirmwareVersion(),
                device.getStatus(),
                device.getLastSeenAt(),
                healthScore,
                healthStatus,
                device.getLastHealthCheckAt(),
                device.getTags().stream()
                        .map(t -> new DeviceTagDto(t.getId(), t.getName(), t.getColor()))
                        .collect(Collectors.toList()),
                device.getGroups().stream()
                        .map(g -> new DeviceGroupDto(g.getId(), g.getName(), g.getDescription()))
                        .collect(Collectors.toList()));
    }
}
