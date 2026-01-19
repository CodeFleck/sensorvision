package io.indcloud.controller;

import lombok.RequiredArgsConstructor;
import io.indcloud.dto.EventResponse;
import io.indcloud.model.Event;
import io.indcloud.model.Organization;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final SecurityUtils securityUtils;

    /**
     * Get events with optional filters
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        Pageable pageable = PageRequest.of(page, size);

        Event.EventType eventTypeEnum = eventType != null ? Event.EventType.valueOf(eventType) : null;
        Event.EventSeverity severityEnum = severity != null ? Event.EventSeverity.valueOf(severity) : null;

        Page<Event> events = eventService.getEventsWithFilters(
                organization,
                eventTypeEnum,
                severityEnum,
                deviceId,
                entityType,
                startTime,
                endTime,
                pageable
        );

        Page<EventResponse> response = events.map(EventResponse::fromEvent);
        return ResponseEntity.ok(response);
    }

    /**
     * Get recent events (last N hours)
     */
    @GetMapping("/recent")
    public ResponseEntity<java.util.List<EventResponse>> getRecentEvents(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "50") int limit
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        java.util.List<Event> events = eventService.getRecentEvents(organization, hours);
        java.util.List<EventResponse> response = events.stream()
                .limit(limit)
                .map(EventResponse::fromEvent)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get event statistics by type
     */
    @GetMapping("/statistics/by-type")
    public ResponseEntity<Map<String, Long>> getEventStatisticsByType(
            @RequestParam(defaultValue = "24") int hours
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        Map<String, Long> statistics = eventService.getEventStatisticsByType(organization, hours);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get event statistics by severity
     */
    @GetMapping("/statistics/by-severity")
    public ResponseEntity<Map<String, Long>> getEventStatisticsBySeverity(
            @RequestParam(defaultValue = "24") int hours
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        Map<String, Long> statistics = eventService.getEventStatisticsBySeverity(organization, hours);
        return ResponseEntity.ok(statistics);
    }
}
