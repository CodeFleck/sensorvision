package io.indcloud.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.LatestTelemetryResponse;
import io.indcloud.dto.TelemetryPointDto;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Device;
import io.indcloud.model.Organization;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TelemetryService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DeviceRepository deviceRepository;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> queryTelemetry(String externalId, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' must be before 'to'");
        }

        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        return telemetryRecordRepository
                .findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(device.getExternalId(), from, to)
                .stream()
                .map(this::toTelemetryPoint)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TelemetryPointDto getLatest(String externalId) {
        Organization userOrg = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        // Verify device belongs to user's organization
        if (!device.getOrganization().getId().equals(userOrg.getId())) {
            throw new AccessDeniedException("Access denied to device: " + externalId);
        }

        return telemetryRecordRepository
                .findByDeviceExternalIdOrderByTimestampDesc(device.getExternalId(), PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(this::toTelemetryPoint)
                .orElseThrow(() -> new ResourceNotFoundException("No telemetry found for device: " + externalId));
    }

    @Transactional(readOnly = true)
    public List<LatestTelemetryResponse> getLatest(Collection<String> externalIds) {
        if (externalIds == null || externalIds.isEmpty()) {
            return List.of();
        }

        Organization userOrg = securityUtils.getCurrentUserOrganization();

        // First, get all records
        List<TelemetryRecord> records = telemetryRecordRepository.findLatestForDevices(externalIds);

        // Filter to only include records from user's organization
        return records.stream()
                .filter(record -> record.getDevice().getOrganization().getId().equals(userOrg.getId()))
                .map(this::toLatestResponse)
                .collect(Collectors.toList());
    }

    private TelemetryPointDto toTelemetryPoint(TelemetryRecord record) {
        return new TelemetryPointDto(
                record.getDevice().getExternalId(),
                record.getTimestamp(),
                record.getKwConsumption() != null ? record.getKwConsumption().doubleValue() : null,
                record.getVoltage() != null ? record.getVoltage().doubleValue() : null,
                record.getCurrent() != null ? record.getCurrent().doubleValue() : null,
                record.getPowerFactor() != null ? record.getPowerFactor().doubleValue() : null,
                record.getFrequency() != null ? record.getFrequency().doubleValue() : null
        );
    }

    private LatestTelemetryResponse toLatestResponse(TelemetryRecord record) {
        return new LatestTelemetryResponse(
                record.getDevice().getExternalId(),
                record.getTimestamp(),
                record.getKwConsumption(),
                record.getVoltage(),
                record.getCurrent(),
                record.getPowerFactor(),
                record.getFrequency(),
                record.getMetadata()
        );
    }
}
