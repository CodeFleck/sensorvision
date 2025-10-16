package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.EventResponse;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.security.UserPrincipal;
import org.sensorvision.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;
    private final OrganizationRepository organizationRepository;

    /**
     * Get events with optional filters
     */
    @GetMapping
    public ResponseEntity<Page<EventResponse>> getEvents(
            Authentication authentication,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        Organization organization = getCurrentUserOrganization(authentication);
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
            Authentication authentication,
            @RequestParam(defaultValue = "24") int hours
    ) {
        Organization organization = getCurrentUserOrganization(authentication);
        java.util.List<Event> events = eventService.getRecentEvents(organization, hours);
        java.util.List<EventResponse> response = events.stream()
                .map(EventResponse::fromEvent)
                .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get event statistics by type
     */
    @GetMapping("/statistics/by-type")
    public ResponseEntity<Map<String, Long>> getEventStatisticsByType(
            Authentication authentication,
            @RequestParam(defaultValue = "24") int hours
    ) {
        Organization organization = getCurrentUserOrganization(authentication);
        Map<String, Long> statistics = eventService.getEventStatisticsByType(organization, hours);
        return ResponseEntity.ok(statistics);
    }

    /**
     * Get event statistics by severity
     */
    @GetMapping("/statistics/by-severity")
    public ResponseEntity<Map<String, Long>> getEventStatisticsBySeverity(
            Authentication authentication,
            @RequestParam(defaultValue = "24") int hours
    ) {
        Organization organization = getCurrentUserOrganization(authentication);
        Map<String, Long> statistics = eventService.getEventStatisticsBySeverity(organization, hours);
        return ResponseEntity.ok(statistics);
    }

    private Organization getCurrentUserOrganization(Authentication authentication) {
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        User user = userRepository.findById(userPrincipal.getId())
                .orElseThrow(() -> new RuntimeException("User not found"));
        return user.getOrganization();
    }
}
