package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.DeviceTagRequest;
import io.indcloud.dto.DeviceTagResponse;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Device;
import io.indcloud.model.DeviceTag;
import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.DeviceTagRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class DeviceTagService {

    private final DeviceTagRepository deviceTagRepository;
    private final DeviceRepository deviceRepository;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<DeviceTagResponse> getAllTags() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return deviceTagRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceTagResponse getTag(Long id) {
        DeviceTag tag = findTagById(id);
        return toResponse(tag);
    }

    public DeviceTagResponse createTag(DeviceTagRequest request) {
        Organization org = securityUtils.getCurrentUserOrganization();

        // Check for duplicate name within organization
        if (deviceTagRepository.existsByOrganizationIdAndName(org.getId(), request.name())) {
            throw new BadRequestException("Device tag with name '" + request.name() + "' already exists");
        }

        DeviceTag tag = DeviceTag.builder()
                .organization(org)
                .name(request.name())
                .color(request.color())
                .build();

        DeviceTag saved = deviceTagRepository.save(tag);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device tag '" + saved.getName() + "' created",
                null
        );

        log.info("Created device tag: {} for organization: {}", saved.getName(), org.getId());
        return toResponse(saved);
    }

    public DeviceTagResponse updateTag(Long id, DeviceTagRequest request) {
        DeviceTag tag = findTagById(id);

        // Check for duplicate name (excluding current tag)
        deviceTagRepository.findByOrganizationIdAndName(
                tag.getOrganization().getId(),
                request.name()
        ).ifPresent(existing -> {
            if (!existing.getId().equals(id)) {
                throw new BadRequestException("Device tag with name '" + request.name() + "' already exists");
            }
        });

        tag.setName(request.name());
        tag.setColor(request.color());

        DeviceTag updated = deviceTagRepository.save(tag);

        // Emit event
        eventService.createEvent(
                tag.getOrganization(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device tag '" + updated.getName() + "' updated",
                null
        );

        log.info("Updated device tag: {}", id);
        return toResponse(updated);
    }

    public void deleteTag(Long id) {
        DeviceTag tag = findTagById(id);
        String tagName = tag.getName();
        Organization org = tag.getOrganization();

        deviceTagRepository.delete(tag);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device tag '" + tagName + "' deleted",
                null
        );

        log.info("Deleted device tag: {}", id);
    }

    public DeviceTagResponse addTagToDevice(Long tagId, String deviceExternalId) {
        DeviceTag tag = findTagById(tagId);
        Device device = findDeviceByExternalId(deviceExternalId);

        // Verify device belongs to same organization
        if (!device.getOrganization().getId().equals(tag.getOrganization().getId())) {
            throw new BadRequestException("Device does not belong to the same organization");
        }

        device.getTags().add(tag);
        deviceRepository.save(device);

        log.info("Added tag {} to device {}", tagId, deviceExternalId);
        return toResponse(deviceTagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found")));
    }

    public DeviceTagResponse removeTagFromDevice(Long tagId, String deviceExternalId) {
        DeviceTag tag = findTagById(tagId);
        Device device = findDeviceByExternalId(deviceExternalId);

        device.getTags().remove(tag);
        deviceRepository.save(device);

        log.info("Removed tag {} from device {}", tagId, deviceExternalId);
        return toResponse(deviceTagRepository.findById(tagId)
                .orElseThrow(() -> new ResourceNotFoundException("Tag not found")));
    }

    @Transactional(readOnly = true)
    public List<DeviceTagResponse> getTagsByDevice(String deviceExternalId) {
        Device device = findDeviceByExternalId(deviceExternalId);
        return device.getTags().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private DeviceTag findTagById(Long id) {
        DeviceTag tag = deviceTagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device tag not found: " + id));

        // Verify organization access
        Organization currentOrg = securityUtils.getCurrentUserOrganization();
        if (!tag.getOrganization().getId().equals(currentOrg.getId())) {
            throw new ResourceNotFoundException("Device tag not found: " + id);
        }

        return tag;
    }

    private Device findDeviceByExternalId(String externalId) {
        return deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
    }

    private DeviceTagResponse toResponse(DeviceTag tag) {
        Set<String> deviceIds = tag.getDevices().stream()
                .map(Device::getExternalId)
                .collect(Collectors.toSet());

        return new DeviceTagResponse(
                tag.getId(),
                tag.getName(),
                tag.getColor(),
                tag.getDevices().size(),
                deviceIds,
                tag.getCreatedAt()
        );
    }
}
