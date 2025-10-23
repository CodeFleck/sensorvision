package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.DeviceLocationRequest;
import org.sensorvision.dto.DeviceLocationResponse;
import org.sensorvision.dto.LocationHistoryResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.*;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.GeofenceAssignmentRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class LocationTrackingService {

    private final DeviceRepository deviceRepository;
    private final TelemetryRecordRepository telemetryRepository;
    private final GeofenceAssignmentRepository assignmentRepository;
    private final GeofenceAlertService geofenceAlertService;
    private final EventService eventService;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<DeviceLocationResponse> getAllDeviceLocations() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return deviceRepository.findByOrganizationId(org.getId()).stream()
                .filter(device -> device.getLatitude() != null && device.getLongitude() != null)
                .map(this::toLocationResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public DeviceLocationResponse getDeviceLocation(UUID deviceId) {
        Device device = findDeviceById(deviceId);
        return toLocationResponse(device);
    }

    public DeviceLocationResponse updateDeviceLocation(UUID deviceId, DeviceLocationRequest request) {
        Device device = findDeviceById(deviceId);

        // Store previous location for geofence checking
        var previousLat = device.getLatitude();
        var previousLon = device.getLongitude();

        // Update device location
        device.setLatitude(request.latitude());
        device.setLongitude(request.longitude());
        device.setAltitude(request.altitude());
        device.setLocationUpdatedAt(LocalDateTime.now());

        Device updated = deviceRepository.save(device);

        // Check geofence alerts
        checkGeofenceAlerts(device, previousLat, previousLon);

        // Emit event
        eventService.createDeviceEvent(
                device.getOrganization(),
                device.getId().toString(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Location updated for device '" + device.getName() + "'",
                null
        );

        log.info("Updated location for device: {}", deviceId);
        return toLocationResponse(updated);
    }

    @Transactional(readOnly = true)
    public LocationHistoryResponse getLocationHistory(UUID deviceId, LocalDateTime startTime,
                                                      LocalDateTime endTime, Integer limit) {
        Device device = findDeviceById(deviceId);

        if (limit == null || limit <= 0) {
            limit = 1000; // Default limit
        }

        Pageable pageable = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "timestamp"));

        List<TelemetryRecord> records;
        if (startTime != null && endTime != null) {
            records = telemetryRepository.findByDeviceIdAndTimestampBetween(
                    deviceId, startTime, endTime, pageable
            ).getContent();
        } else {
            records = telemetryRepository.findByDeviceId(deviceId, pageable).getContent();
        }

        List<LocationHistoryResponse.LocationPoint> locationPoints = records.stream()
                .filter(record -> record.getLatitude() != null && record.getLongitude() != null)
                .map(record -> new LocationHistoryResponse.LocationPoint(
                        record.getLatitude(),
                        record.getLongitude(),
                        record.getAltitude(),
                        LocalDateTime.ofInstant(record.getTimestamp(), java.time.ZoneId.systemDefault())
                ))
                .collect(Collectors.toList());

        return new LocationHistoryResponse(
                device.getId(),
                device.getName(),
                locationPoints
        );
    }

    private void checkGeofenceAlerts(Device device, java.math.BigDecimal previousLat, java.math.BigDecimal previousLon) {
        List<GeofenceAssignment> assignments = assignmentRepository.findActiveAssignmentsByDeviceId(device.getId());

        for (GeofenceAssignment assignment : assignments) {
            Geofence geofence = assignment.getGeofence();

            boolean wasInside = false;
            if (previousLat != null && previousLon != null) {
                wasInside = geofence.containsPoint(previousLat, previousLon);
            }

            boolean isInside = geofence.containsPoint(device.getLatitude(), device.getLongitude());

            // Check for entry
            if (!wasInside && isInside && assignment.getAlertOnEnter()) {
                geofenceAlertService.triggerGeofenceAlert(
                        device, geofence, GeofenceAlertService.GeofenceEventType.ENTER
                );
                log.info("Device {} entered geofence {}", device.getId(), geofence.getId());
            }

            // Check for exit
            if (wasInside && !isInside && assignment.getAlertOnExit()) {
                geofenceAlertService.triggerGeofenceAlert(
                        device, geofence, GeofenceAlertService.GeofenceEventType.EXIT
                );
                log.info("Device {} exited geofence {}", device.getId(), geofence.getId());
            }
        }
    }

    private Device findDeviceById(UUID id) {
        Organization org = securityUtils.getCurrentUserOrganization();
        Device device = deviceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Device not found: " + id));

        // Verify organization access
        if (!device.getOrganization().getId().equals(org.getId())) {
            throw new ResourceNotFoundException("Device not found: " + id);
        }

        return device;
    }

    private DeviceLocationResponse toLocationResponse(Device device) {
        return new DeviceLocationResponse(
                device.getId(),
                device.getName(),
                device.getLatitude(),
                device.getLongitude(),
                device.getAltitude(),
                device.getLocationUpdatedAt(),
                device.getStatus().name()
        );
    }
}
