package org.sensorvision.service;

import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.DeviceCreateRequest;
import org.sensorvision.dto.DeviceResponse;
import org.sensorvision.dto.DeviceUpdateRequest;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.repository.DeviceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class DeviceService {

    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public List<DeviceResponse> getAllDevices() {
        return deviceRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceResponse getDevice(String externalId) {
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
        return toResponse(device);
    }

    public DeviceResponse createDevice(DeviceCreateRequest request) {
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
                .build();
        Device saved = deviceRepository.save(device);
        return toResponse(saved);
    }

    public DeviceResponse updateDevice(String externalId, DeviceUpdateRequest request) {
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
        device.setName(request.name());
        device.setLocation(request.location());
        device.setSensorType(request.sensorType());
        device.setFirmwareVersion(request.firmwareVersion());
        Device updated = deviceRepository.save(device);
        return toResponse(updated);
    }

    public void deleteDevice(String externalId) {
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
        deviceRepository.delete(device);
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
