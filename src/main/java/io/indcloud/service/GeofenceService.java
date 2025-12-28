package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.*;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.GeofenceAssignmentRepository;
import io.indcloud.repository.GeofenceRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeofenceService {

    private final GeofenceRepository geofenceRepository;
    private final GeofenceAssignmentRepository assignmentRepository;
    private final DeviceRepository deviceRepository;
    private final EventService eventService;
    private final ObjectMapper objectMapper;
    private final SecurityUtils securityUtils;

    @Transactional(readOnly = true)
    public List<GeofenceResponse> getAllGeofences() {
        Organization org = securityUtils.getCurrentUserOrganization();
        return geofenceRepository.findByOrganizationId(org.getId()).stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public GeofenceResponse getGeofence(Long id) {
        Geofence geofence = findGeofenceById(id);
        return toResponse(geofence);
    }

    public GeofenceResponse createGeofence(GeofenceRequest request) {
        Organization org = securityUtils.getCurrentUserOrganization();

        validateGeofenceRequest(request);

        Geofence geofence = Geofence.builder()
                .organization(org)
                .name(request.name())
                .description(request.description())
                .shape(request.shape())
                .centerLatitude(request.centerLatitude())
                .centerLongitude(request.centerLongitude())
                .radiusMeters(request.radiusMeters())
                .polygonCoordinates(request.polygonCoordinates())
                .enabled(request.enabled() != null ? request.enabled() : true)
                .build();

        Geofence saved = geofenceRepository.save(geofence);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_CREATED,
                Event.EventSeverity.INFO,
                "Geofence '" + saved.getName() + "' created",
                null
        );

        log.info("Created geofence: {} for organization: {}", saved.getId(), org.getId());
        return toResponse(saved);
    }

    public GeofenceResponse updateGeofence(Long id, GeofenceRequest request) {
        Geofence geofence = findGeofenceById(id);

        validateGeofenceRequest(request);

        geofence.setName(request.name());
        geofence.setDescription(request.description());
        geofence.setShape(request.shape());
        geofence.setCenterLatitude(request.centerLatitude());
        geofence.setCenterLongitude(request.centerLongitude());
        geofence.setRadiusMeters(request.radiusMeters());
        geofence.setPolygonCoordinates(request.polygonCoordinates());
        if (request.enabled() != null) {
            geofence.setEnabled(request.enabled());
        }

        Geofence updated = geofenceRepository.save(geofence);

        // Emit event
        eventService.createEvent(
                geofence.getOrganization(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Geofence '" + updated.getName() + "' updated",
                null
        );

        log.info("Updated geofence: {}", id);
        return toResponse(updated);
    }

    public void deleteGeofence(Long id) {
        Geofence geofence = findGeofenceById(id);
        String geofenceName = geofence.getName();
        Organization org = geofence.getOrganization();

        geofenceRepository.delete(geofence);

        // Emit event
        eventService.createEvent(
                org,
                Event.EventType.DEVICE_DELETED,
                Event.EventSeverity.INFO,
                "Geofence '" + geofenceName + "' deleted",
                null
        );

        log.info("Deleted geofence: {}", id);
    }

    @Transactional(readOnly = true)
    public List<GeofenceAssignmentResponse> getGeofenceAssignments(Long geofenceId) {
        Geofence geofence = findGeofenceById(geofenceId);
        return assignmentRepository.findByGeofenceId(geofenceId).stream()
                .map(this::toAssignmentResponse)
                .collect(Collectors.toList());
    }

    public GeofenceAssignmentResponse assignDeviceToGeofence(Long geofenceId, GeofenceAssignmentRequest request) {
        Geofence geofence = findGeofenceById(geofenceId);
        Device device = findDeviceById(request.deviceId());

        // Check if assignment already exists
        assignmentRepository.findByGeofenceIdAndDeviceId(geofenceId, request.deviceId())
                .ifPresent(existing -> {
                    throw new BadRequestException("Device is already assigned to this geofence");
                });

        GeofenceAssignment assignment = GeofenceAssignment.builder()
                .geofence(geofence)
                .device(device)
                .alertOnEnter(request.alertOnEnter() != null ? request.alertOnEnter() : true)
                .alertOnExit(request.alertOnExit() != null ? request.alertOnExit() : true)
                .build();

        GeofenceAssignment saved = assignmentRepository.save(assignment);

        // Check if device is currently inside geofence
        if (device.getLatitude() != null && device.getLongitude() != null) {
            boolean isInside = geofence.containsPoint(device.getLatitude(), device.getLongitude());
            saved.setDeviceCurrentlyInside(isInside);
        }

        // Emit event
        eventService.createDeviceEvent(
                geofence.getOrganization(),
                device.getId().toString(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device '" + device.getName() + "' assigned to geofence '" + geofence.getName() + "'",
                null
        );

        log.info("Assigned device {} to geofence {}", request.deviceId(), geofenceId);
        return toAssignmentResponse(saved);
    }

    public void unassignDeviceFromGeofence(Long geofenceId, UUID deviceId) {
        Geofence geofence = findGeofenceById(geofenceId);
        Device device = findDeviceById(deviceId);

        GeofenceAssignment assignment = assignmentRepository.findByGeofenceIdAndDeviceId(geofenceId, deviceId)
                .orElseThrow(() -> new ResourceNotFoundException("Assignment not found"));

        assignmentRepository.delete(assignment);

        // Emit event
        eventService.createDeviceEvent(
                geofence.getOrganization(),
                device.getId().toString(),
                Event.EventType.DEVICE_UPDATED,
                Event.EventSeverity.INFO,
                "Device '" + device.getName() + "' unassigned from geofence '" + geofence.getName() + "'",
                null
        );

        log.info("Unassigned device {} from geofence {}", deviceId, geofenceId);
    }

    private Geofence findGeofenceById(Long id) {
        Organization org = securityUtils.getCurrentUserOrganization();
        return geofenceRepository.findByIdAndOrganizationId(id, org.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Geofence not found: " + id));
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

    private void validateGeofenceRequest(GeofenceRequest request) {
        switch (request.shape()) {
            case CIRCLE:
                if (request.centerLatitude() == null || request.centerLongitude() == null || request.radiusMeters() == null) {
                    throw new BadRequestException("CIRCLE geofence requires centerLatitude, centerLongitude, and radiusMeters");
                }
                break;
            case POLYGON:
                if (request.polygonCoordinates() == null || !request.polygonCoordinates().isArray()) {
                    throw new BadRequestException("POLYGON geofence requires polygonCoordinates array");
                }
                if (request.polygonCoordinates().size() < 3) {
                    throw new BadRequestException("POLYGON geofence requires at least 3 coordinate points");
                }
                break;
            case RECTANGLE:
                if (request.polygonCoordinates() == null || !request.polygonCoordinates().isArray()) {
                    throw new BadRequestException("RECTANGLE geofence requires polygonCoordinates array");
                }
                if (request.polygonCoordinates().size() != 4) {
                    throw new BadRequestException("RECTANGLE geofence requires exactly 4 coordinate points");
                }
                break;
        }
    }

    private GeofenceResponse toResponse(Geofence geofence) {
        int assignedCount = assignmentRepository.findByGeofenceId(geofence.getId()).size();

        return new GeofenceResponse(
                geofence.getId(),
                geofence.getOrganization().getId(),
                geofence.getName(),
                geofence.getDescription(),
                geofence.getShape(),
                geofence.getCenterLatitude(),
                geofence.getCenterLongitude(),
                geofence.getRadiusMeters(),
                geofence.getPolygonCoordinates(),
                geofence.getEnabled(),
                geofence.getCreatedAt(),
                geofence.getUpdatedAt(),
                assignedCount
        );
    }

    private GeofenceAssignmentResponse toAssignmentResponse(GeofenceAssignment assignment) {
        return new GeofenceAssignmentResponse(
                assignment.getGeofence().getId(),
                assignment.getGeofence().getName(),
                assignment.getDevice().getId(),
                assignment.getDevice().getName(),
                assignment.getAlertOnEnter(),
                assignment.getAlertOnExit(),
                assignment.getAssignedAt(),
                assignment.getDeviceCurrentlyInside()
        );
    }
}
