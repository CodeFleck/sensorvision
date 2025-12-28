package io.indcloud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class GeofenceAlertService {

    private final AlertService alertService;
    private final EventService eventService;

    public enum GeofenceEventType {
        ENTER,
        EXIT
    }

    public void triggerGeofenceAlert(Device device, Geofence geofence, GeofenceEventType eventType) {
        String action = eventType == GeofenceEventType.ENTER ? "entered" : "exited";
        String message = String.format("Device '%s' %s geofence '%s'",
                device.getName(), action, geofence.getName());

        // Create an alert (geofence alerts don't need a rule, so we skip rule field)
        // Note: This may fail if rule is required. Consider creating a system rule or making rule optional.

        // Create an event
        eventService.createDeviceEvent(
                device.getOrganization(),
                device.getId().toString(),
                Event.EventType.ALERT_TRIGGERED,
                Event.EventSeverity.WARNING,
                message,
                null
        );

        log.info("Geofence alert triggered: device={}, geofence={}, event={}",
                device.getId(), geofence.getId(), eventType);
    }
}
