package org.sensorvision.service;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.LatestTelemetryResponse;
import org.sensorvision.dto.TelemetryPointDto;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Device;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TelemetryService {

    private final TelemetryRecordRepository telemetryRecordRepository;
    private final DeviceRepository deviceRepository;

    @Transactional(readOnly = true)
    public List<TelemetryPointDto> queryTelemetry(String externalId, Instant from, Instant to) {
        if (from.isAfter(to)) {
            throw new IllegalArgumentException("Parameter 'from' must be before 'to'");
        }

        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

        return telemetryRecordRepository
                .findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(device.getExternalId(), from, to)
                .stream()
                .map(this::toTelemetryPoint)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TelemetryPointDto getLatest(String externalId) {
        Device device = deviceRepository.findByExternalId(externalId)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + externalId));

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

        return telemetryRecordRepository.findLatestForDevices(externalIds).stream()
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
