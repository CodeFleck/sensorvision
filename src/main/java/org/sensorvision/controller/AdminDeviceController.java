package org.sensorvision.controller;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.ApiResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.TrashLog;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/devices")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDeviceController {

    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;
    private final TrashService trashService;

    public AdminDeviceController(DeviceRepository deviceRepository, SecurityUtils securityUtils,
                                 TrashService trashService) {
        this.deviceRepository = deviceRepository;
        this.securityUtils = securityUtils;
        this.trashService = trashService;
    }

    @GetMapping
    public ResponseEntity<List<AdminDeviceDto>> getAllDevices() {
        // Only return non-deleted devices
        List<Device> devices = deviceRepository.findAllActive();
        List<AdminDeviceDto> deviceDtos = devices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    @GetMapping("/{deviceId}")
    public ResponseEntity<AdminDeviceDto> getDevice(@PathVariable UUID deviceId) {
        Device device = deviceRepository.findByIdWithOrganization(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));
        return ResponseEntity.ok(convertToDto(device));
    }

    @GetMapping("/external/{externalId}")
    public ResponseEntity<AdminDeviceDto> getDeviceByExternalId(@PathVariable String externalId) {
        Device device = deviceRepository.findByExternalIdWithOrganization(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));
        return ResponseEntity.ok(convertToDto(device));
    }

    @PutMapping("/{deviceId}/enable")
    public ResponseEntity<ApiResponse<AdminDeviceDto>> enableDevice(@PathVariable UUID deviceId) {
        Device device = deviceRepository.findByIdWithOrganization(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        device.setActive(true);
        device.setStatus(DeviceStatus.ONLINE);
        deviceRepository.save(device);

        log.info("ADMIN_ACTION: Admin {} enabled device {} (id: {})",
                securityUtils.getCurrentUser().getUsername(),
                device.getExternalId(),
                deviceId);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(device),
                "Device enabled successfully"));
    }

    @PutMapping("/{deviceId}/disable")
    public ResponseEntity<ApiResponse<AdminDeviceDto>> disableDevice(@PathVariable UUID deviceId) {
        Device device = deviceRepository.findByIdWithOrganization(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        device.setActive(false);
        device.setStatus(DeviceStatus.OFFLINE);
        deviceRepository.save(device);

        log.info("ADMIN_ACTION: Admin {} disabled device {} (id: {})",
                securityUtils.getCurrentUser().getUsername(),
                device.getExternalId(),
                deviceId);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(device),
                "Device disabled successfully"));
    }

    @PutMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<AdminDeviceDto>> updateDevice(
            @PathVariable UUID deviceId,
            @RequestBody DeviceUpdateRequest request) {
        Device device = deviceRepository.findByIdWithOrganization(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        if (request.getName() != null) {
            device.setName(request.getName());
        }
        if (request.getDescription() != null) {
            device.setDescription(request.getDescription());
        }
        if (request.getLocation() != null) {
            device.setLocation(request.getLocation());
        }
        if (request.getSensorType() != null) {
            device.setSensorType(request.getSensorType());
        }
        if (request.getFirmwareVersion() != null) {
            device.setFirmwareVersion(request.getFirmwareVersion());
        }

        deviceRepository.save(device);

        log.info("ADMIN_ACTION: Admin {} updated device {} (id: {})",
                securityUtils.getCurrentUser().getUsername(),
                device.getExternalId(),
                deviceId);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(device),
                "Device updated successfully"));
    }

    @DeleteMapping("/{deviceId}")
    public ResponseEntity<ApiResponse<SoftDeleteResponse>> deleteDevice(
            @PathVariable UUID deviceId,
            @RequestParam(required = false) String reason) {
        Device device = deviceRepository.findByIdWithOrganization(deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found with id: " + deviceId));

        // Don't allow deleting already deleted devices
        if (device.isDeleted()) {
            throw new BadRequestException("Device is already deleted");
        }

        // Use soft delete instead of hard delete
        TrashLog trashLog = trashService.softDeleteDevice(deviceId, reason);

        SoftDeleteResponse response = new SoftDeleteResponse(
                trashLog.getId(),
                trashLog.getEntityType(),
                trashLog.getEntityName(),
                trashLog.getExpiresAt().toString(),
                trashLog.getDaysRemaining()
        );

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "Device moved to trash. You can restore it within " + TrashLog.RETENTION_DAYS + " days."));
    }

    /**
     * Response for soft delete operations, including undo info.
     */
    public record SoftDeleteResponse(
            Long trashId,
            String entityType,
            String entityName,
            String expiresAt,
            long daysRemaining
    ) {}

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<AdminDeviceDto>> getDevicesByOrganization(@PathVariable Long organizationId) {
        List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
        List<AdminDeviceDto> deviceDtos = devices.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(deviceDtos);
    }

    @GetMapping("/stats")
    public ResponseEntity<DeviceStatsDto> getDeviceStats() {
        List<Device> allDevices = deviceRepository.findAll();

        long totalDevices = allDevices.size();
        long activeDevices = allDevices.stream().filter(Device::getActive).count();
        long inactiveDevices = totalDevices - activeDevices;
        long onlineDevices = allDevices.stream()
                .filter(d -> d.getStatus() == DeviceStatus.ONLINE)
                .count();
        long offlineDevices = allDevices.stream()
                .filter(d -> d.getStatus() == DeviceStatus.OFFLINE)
                .count();

        return ResponseEntity.ok(new DeviceStatsDto(
                totalDevices,
                activeDevices,
                inactiveDevices,
                onlineDevices,
                offlineDevices
        ));
    }

    private AdminDeviceDto convertToDto(Device device) {
        AdminDeviceDto dto = new AdminDeviceDto();
        dto.setId(device.getId());
        dto.setExternalId(device.getExternalId());
        dto.setName(device.getName());
        dto.setDescription(device.getDescription());
        dto.setActive(device.getActive());
        dto.setLocation(device.getLocation());
        dto.setSensorType(device.getSensorType());
        dto.setFirmwareVersion(device.getFirmwareVersion());
        dto.setStatus(device.getStatus() != null ? device.getStatus().name() : "UNKNOWN");
        dto.setLastSeenAt(device.getLastSeenAt());
        dto.setHealthScore(device.getHealthScore());
        dto.setLastHealthCheckAt(device.getLastHealthCheckAt());
        dto.setHasApiToken(device.getApiToken() != null && !device.getApiToken().isEmpty());
        dto.setTokenCreatedAt(device.getTokenCreatedAt());
        dto.setTokenLastUsedAt(device.getTokenLastUsedAt());

        if (device.getOrganization() != null) {
            dto.setOrganizationId(device.getOrganization().getId());
            dto.setOrganizationName(device.getOrganization().getName());
        }

        dto.setCreatedAt(device.getCreatedAt() != null
                ? LocalDateTime.ofInstant(device.getCreatedAt(), ZoneId.systemDefault())
                : null);
        dto.setUpdatedAt(device.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(device.getUpdatedAt(), ZoneId.systemDefault())
                : null);

        return dto;
    }

    // Inner DTOs
    public static class AdminDeviceDto {
        private UUID id;
        private String externalId;
        private String name;
        private String description;
        private Boolean active;
        private String location;
        private String sensorType;
        private String firmwareVersion;
        private String status;
        private Instant lastSeenAt;
        private Integer healthScore;
        private LocalDateTime lastHealthCheckAt;
        private Long organizationId;
        private String organizationName;
        private Boolean hasApiToken;
        private LocalDateTime tokenCreatedAt;
        private LocalDateTime tokenLastUsedAt;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        // Getters and Setters
        public UUID getId() { return id; }
        public void setId(UUID id) { this.id = id; }
        public String getExternalId() { return externalId; }
        public void setExternalId(String externalId) { this.externalId = externalId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Boolean getActive() { return active; }
        public void setActive(Boolean active) { this.active = active; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getSensorType() { return sensorType; }
        public void setSensorType(String sensorType) { this.sensorType = sensorType; }
        public String getFirmwareVersion() { return firmwareVersion; }
        public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public Instant getLastSeenAt() { return lastSeenAt; }
        public void setLastSeenAt(Instant lastSeenAt) { this.lastSeenAt = lastSeenAt; }
        public Integer getHealthScore() { return healthScore; }
        public void setHealthScore(Integer healthScore) { this.healthScore = healthScore; }
        public LocalDateTime getLastHealthCheckAt() { return lastHealthCheckAt; }
        public void setLastHealthCheckAt(LocalDateTime lastHealthCheckAt) { this.lastHealthCheckAt = lastHealthCheckAt; }
        public Long getOrganizationId() { return organizationId; }
        public void setOrganizationId(Long organizationId) { this.organizationId = organizationId; }
        public String getOrganizationName() { return organizationName; }
        public void setOrganizationName(String organizationName) { this.organizationName = organizationName; }
        public Boolean getHasApiToken() { return hasApiToken; }
        public void setHasApiToken(Boolean hasApiToken) { this.hasApiToken = hasApiToken; }
        public LocalDateTime getTokenCreatedAt() { return tokenCreatedAt; }
        public void setTokenCreatedAt(LocalDateTime tokenCreatedAt) { this.tokenCreatedAt = tokenCreatedAt; }
        public LocalDateTime getTokenLastUsedAt() { return tokenLastUsedAt; }
        public void setTokenLastUsedAt(LocalDateTime tokenLastUsedAt) { this.tokenLastUsedAt = tokenLastUsedAt; }
        public LocalDateTime getCreatedAt() { return createdAt; }
        public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
        public LocalDateTime getUpdatedAt() { return updatedAt; }
        public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    }

    public static class DeviceUpdateRequest {
        private String name;
        private String description;
        private String location;
        private String sensorType;
        private String firmwareVersion;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public String getLocation() { return location; }
        public void setLocation(String location) { this.location = location; }
        public String getSensorType() { return sensorType; }
        public void setSensorType(String sensorType) { this.sensorType = sensorType; }
        public String getFirmwareVersion() { return firmwareVersion; }
        public void setFirmwareVersion(String firmwareVersion) { this.firmwareVersion = firmwareVersion; }
    }

    public record DeviceStatsDto(
            long totalDevices,
            long activeDevices,
            long inactiveDevices,
            long onlineDevices,
            long offlineDevices
    ) {}
}
