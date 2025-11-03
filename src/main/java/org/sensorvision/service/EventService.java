package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.EventRepository;
import org.sensorvision.service.triggers.DeviceEventFunctionTriggerHandler;
import org.sensorvision.service.triggers.TriggerContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final DeviceEventFunctionTriggerHandler deviceEventTriggerHandler;
    private final ObjectMapper objectMapper;

    /**
     * Create an event with full details
     */
    @Transactional
    public Event createEvent(Event event) {
        log.debug("Creating event: type={}, severity={}, title={}",
                event.getEventType(), event.getSeverity(), event.getTitle());
        return eventRepository.save(event);
    }

    /**
     * Create a simple event with minimal details
     */
    @Transactional
    public Event createEvent(
            Organization organization,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String title,
            String description
    ) {
        Event event = Event.builder()
                .organization(organization)
                .eventType(eventType)
                .severity(severity)
                .title(title)
                .description(description)
                .build();
        return createEvent(event);
    }

    /**
     * Create event with user context
     */
    @Transactional
    public Event createUserEvent(
            Organization organization,
            User user,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String title,
            String description
    ) {
        Event event = Event.builder()
                .organization(organization)
                .user(user)
                .eventType(eventType)
                .severity(severity)
                .title(title)
                .description(description)
                .build();
        return createEvent(event);
    }

    /**
     * Create event with device context
     */
    @Transactional
    public Event createDeviceEvent(
            Organization organization,
            String deviceId,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String title,
            String description
    ) {
        Event event = Event.builder()
                .organization(organization)
                .deviceId(deviceId)
                .eventType(eventType)
                .severity(severity)
                .title(title)
                .description(description)
                .entityType("Device")
                .entityId(deviceId)
                .build();
        return createEvent(event);
    }

    /**
     * Create event with entity context and metadata
     */
    @Transactional
    public Event createEntityEvent(
            Organization organization,
            String entityType,
            String entityId,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String title,
            String description,
            Map<String, Object> metadata
    ) {
        Event event = Event.builder()
                .organization(organization)
                .entityType(entityType)
                .entityId(entityId)
                .eventType(eventType)
                .severity(severity)
                .title(title)
                .description(description)
                .metadata(metadata)
                .build();
        return createEvent(event);
    }

    /**
     * Get all events for an organization with pagination
     */
    @Transactional(readOnly = true)
    public Page<Event> getEvents(Organization organization, Pageable pageable) {
        return eventRepository.findByOrganizationOrderByCreatedAtDesc(organization, pageable);
    }

    /**
     * Get events with filters
     */
    @Transactional(readOnly = true)
    public Page<Event> getEventsWithFilters(
            Organization organization,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String deviceId,
            String entityType,
            LocalDateTime startTime,
            LocalDateTime endTime,
            Pageable pageable
    ) {
        return eventRepository.findByFilters(
                organization, eventType, severity, deviceId, entityType, startTime, endTime, pageable
        );
    }

    /**
     * Get recent events (last N hours)
     */
    @Transactional(readOnly = true)
    public List<Event> getRecentEvents(Organization organization, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        return eventRepository.findRecentEvents(organization, since);
    }

    /**
     * Get event statistics by type
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getEventStatisticsByType(Organization organization, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Object[]> results = eventRepository.countEventsByType(organization, since);

        Map<String, Long> statistics = new HashMap<>();
        for (Object[] result : results) {
            Event.EventType type = (Event.EventType) result[0];
            Long count = (Long) result[1];
            statistics.put(type.name(), count);
        }
        return statistics;
    }

    /**
     * Get event statistics by severity
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getEventStatisticsBySeverity(Organization organization, int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        List<Object[]> results = eventRepository.countEventsBySeverity(organization, since);

        Map<String, Long> statistics = new HashMap<>();
        for (Object[] result : results) {
            Event.EventSeverity severity = (Event.EventSeverity) result[0];
            Long count = (Long) result[1];
            statistics.put(severity.name(), count);
        }
        return statistics;
    }

    /**
     * Delete old events based on retention policy (e.g., older than 90 days)
     */
    @Transactional
    public void deleteOldEvents(int retentionDays) {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(retentionDays);
        log.info("Deleting events older than {}", cutoffDate);
        eventRepository.deleteByCreatedAtBefore(cutoffDate);
    }

    /**
     * Helper method to emit device lifecycle events
     */
    @Transactional
    public void emitDeviceLifecycleEvent(
            Organization organization,
            String deviceId,
            String deviceName,
            Event.EventType eventType
    ) {
        String title = switch (eventType) {
            case DEVICE_CREATED -> String.format("Device '%s' created", deviceName);
            case DEVICE_UPDATED -> String.format("Device '%s' updated", deviceName);
            case DEVICE_DELETED -> String.format("Device '%s' deleted", deviceName);
            case DEVICE_CONNECTED -> String.format("Device '%s' connected", deviceName);
            case DEVICE_DISCONNECTED -> String.format("Device '%s' disconnected", deviceName);
            case DEVICE_OFFLINE -> String.format("Device '%s' is offline", deviceName);
            default -> String.format("Device '%s' event", deviceName);
        };

        Event.EventSeverity severity = eventType == Event.EventType.DEVICE_OFFLINE
                ? Event.EventSeverity.WARNING
                : Event.EventSeverity.INFO;

        createDeviceEvent(organization, deviceId, eventType, severity, title, null);

        // Trigger device event-based serverless functions
        try {
            triggerDeviceEventFunctions(deviceId, deviceName, eventType);
        } catch (Exception e) {
            log.error("Error triggering device event functions for {}: {}",
                deviceId, e.getMessage(), e);
            // Don't fail the event creation if function triggers fail
        }
    }

    /**
     * Trigger device event-based serverless functions.
     */
    private void triggerDeviceEventFunctions(String deviceId, String deviceName, Event.EventType eventType) {
        // Build event data
        ObjectNode eventData = objectMapper.createObjectNode();
        ObjectNode deviceInfo = objectMapper.createObjectNode();
        deviceInfo.put("externalId", deviceId);
        deviceInfo.put("name", deviceName);
        eventData.set("device", deviceInfo);

        // Map Event.EventType to function event type string
        String functionEventType = switch (eventType) {
            case DEVICE_CREATED -> "device.created";
            case DEVICE_UPDATED -> "device.updated";
            case DEVICE_DELETED -> "device.deleted";
            case DEVICE_CONNECTED -> "device.connected";
            case DEVICE_DISCONNECTED -> "device.disconnected";
            case DEVICE_OFFLINE -> "device.offline";
            default -> "device.event";
        };

        // Build trigger context
        TriggerContext context = TriggerContext.builder()
            .eventType(functionEventType)
            .eventSource("device-lifecycle")
            .timestamp(System.currentTimeMillis())
            .deviceId(deviceId)
            .build();

        // Handle the event (will match against configured triggers)
        deviceEventTriggerHandler.handleEvent(eventData, context);
    }

    /**
     * Helper method to emit rule events
     */
    @Transactional
    public void emitRuleEvent(
            Organization organization,
            java.util.UUID ruleId,
            String ruleName,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String description
    ) {
        String title = switch (eventType) {
            case RULE_CREATED -> String.format("Rule '%s' created", ruleName);
            case RULE_UPDATED -> String.format("Rule '%s' updated", ruleName);
            case RULE_DELETED -> String.format("Rule '%s' deleted", ruleName);
            case RULE_TRIGGERED -> String.format("Rule '%s' triggered", ruleName);
            default -> String.format("Rule '%s' event", ruleName);
        };

        createEntityEvent(
                organization,
                "Rule",
                ruleId.toString(),
                eventType,
                severity,
                title,
                description,
                null
        );
    }

    /**
     * Helper method to emit alert events
     */
    @Transactional
    public void emitAlertEvent(
            Organization organization,
            java.util.UUID alertId,
            String deviceId,
            Event.EventType eventType,
            Event.EventSeverity severity,
            String description
    ) {
        Event event = Event.builder()
                .organization(organization)
                .deviceId(deviceId)
                .entityType("Alert")
                .entityId(alertId.toString())
                .eventType(eventType)
                .severity(severity)
                .title(eventType.name().replace("_", " "))
                .description(description)
                .build();
        createEvent(event);
    }
}
