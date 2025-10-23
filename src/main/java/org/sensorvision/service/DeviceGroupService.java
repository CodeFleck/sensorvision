package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceGroupRequest;
import org.sensorvision.dto.DeviceGroupResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceGroup;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.DeviceGroupRepository;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceGroupService {

    private final DeviceGroupRepository deviceGroupRepository;
    private final DeviceRepository deviceRepository;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<DeviceGroupResponse> getAllGroups() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return deviceGroupRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceGroupResponse getGroup(Long id) {
        DeviceGroup group = findGroupById(id);
        return toResponse(group);
    }

    public DeviceGroupResponse createGroup(DeviceGroupRequest request) {
        Organization org = securityUtils.getCurrentUserOrganization();

        // Check for duplicate name within organization
        if (deviceGroupRepository.existsByOrganizationIdAndName(org.getId(), request.name())) {
            throw new BadRequestException("Device group with name '" + request.name() + "' already exists");
        }

        DeviceGroup group = DeviceGroup.builder()
                .organization(org)
                .name(request.name())
                .description(request.description())
                .build();

        DeviceGroup saved = deviceGroupRepository.save(group);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_GROUP_CREATED,
                Event.EventSeverity.INFO,
                "Device group '" + saved.getName() + "' created",
                null
        );

        log.info("Created device group: {} for organization: {}", saved.getName(), org.getId());
        return toResponse(saved);
    }

    public DeviceGroupResponse updateGroup(Long id, DeviceGroupRequest request) {
        DeviceGroup group = findGroupById(id);

        // Check for duplicate name (excluding current group)
        deviceGroupRepository.findByOrganizationIdAndName(
                group.getOrganization().getId(),
                request.name()
        ).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Device group with name '" + request.name() + "' already exists");
            }
        });

        group.setName(request.name());
        group.setDescription(request.description());

        DeviceGroup updated = deviceGroupRepository.save(group);

        // Emit event
        eventService.createEvent(
                group.getOrganization(),
                Event.EventType.DEVICE_GROUP_UPDATED,
                Event.EventSeverity.INFO,
                "Device group '" + updated.getName() + "' updated",
                null
        );

        log.info("Updated device group: {}", id);
        return toResponse(updated);
    }

    public void deleteGroup(Long id) {
        DeviceGroup group = findGroupById(id);
        String groupName = group.getName();
        Organization org = group.getOrganization();

        deviceGroupRepository.delete(group);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_GROUP_DELETED,
                Event.EventSeverity.INFO,
                "Device group '" + groupName + "' deleted",
                null
        );

        log.info("Deleted device group: {}", id);
    }

    public DeviceGroupResponse addDeviceToGroup(Long groupId, String deviceExternalId) {
        DeviceGroup group = findGroupById(groupId);
        Device device = findDeviceByExternalId(deviceExternalId);

        // Verify device belongs to same organization
        if (!device.getOrganization().getId().equals(group.getOrganization().getId())) {
            throw new BadRequestException("Device does not belong to the same organization");
        }

        group.getDevices().add(device);
        DeviceGroup updated = deviceGroupRepository.save(group);

        log.info("Added device {} to group {}", deviceExternalId, groupId);
        return toResponse(updated);
    }

    public DeviceGroupResponse removeDeviceFromGroup(Long groupId, String deviceExternalId) {
        DeviceGroup group = findGroupById(groupId);
        Device device = findDeviceByExternalId(deviceExternalId);

        group.getDevices().remove(device);
        DeviceGroup updated = deviceGroupRepository.save(group);

        log.info("Removed device {} from group {}", deviceExternalId, groupId);
        return toResponse(updated);
    }

    @Transactional(readOnly = true)
    public List<DeviceGroupResponse> getGroupsByDevice(String deviceExternalId) {
        return deviceGroupRepository.findByDeviceExternalId(deviceExternalId).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DeviceGroup findGroupById(Long id) {
        DeviceGroup group = deviceGroupRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device group not found: " + id));

        // Verify organization access
        Organization currentOrg = securityUtils.getCurrentUserOrganization();
        if (!group.getOrganization().getId().equals(currentOrg.getId())) {
            throw new ResourceNotFoundException("Device group not found: " + id);
        }

        return group;
    }

    private Device findDeviceByExternalId(String externalId) {
        return deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
    }

    private DeviceGroupResponse toResponse(DeviceGroup group) {
        Set<String> deviceIds = group.getDevices().stream()
                .map(Device::getExternalId)
                .collect(Collectors.toSet());

        return new DeviceGroupResponse(
                group.getId(),
                group.getName(),
                group.getDescription(),
                group.getDevices().size(),
                deviceIds,
                group.getCreatedAt(),
                group.getUpdatedAt()
        );
    }
}
