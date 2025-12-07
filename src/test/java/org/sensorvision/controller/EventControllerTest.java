package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.EventResponse;
import org.sensorvision.model.Event;
import org.sensorvision.model.Organization;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.EventService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for EventController.
 * Tests REST API endpoints for event management.
 * Validates that SecurityUtils is properly used for authentication (fixes #148).
 */
@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private EventController eventController;

    private Organization testOrganization;
    private Event testEvent;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testEvent = Event.builder()
                .id(1L)
                .eventType(Event.EventType.DEVICE_CREATED)
                .severity(Event.EventSeverity.INFO)
                .title("Test device created")
                .description("A test device was created")
                .entityType("Device")
                .entityId("test-device-001")
                .organization(testOrganization)
                .createdAt(LocalDateTime.now())
                .build();

        // Mock SecurityUtils - this is the key fix for #148
        // Previously, the controller tried to cast JWT to UserPrincipal directly
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
    }

    @Test
    void getEvents_shouldUseSecurityUtilsForOrganization() {
        // Given
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));
        when(eventService.getEventsWithFilters(
                eq(testOrganization),
                any(), any(), any(), any(), any(), any(), any()
        )).thenReturn(eventPage);

        // When
        ResponseEntity<Page<EventResponse>> response = eventController.getEvents(
                null, null, null, null, null, null, 0, 50
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getContent()).hasSize(1);

        // Verify SecurityUtils was called (not direct JWT casting)
        verify(securityUtils).getCurrentUserOrganization();
        verify(eventService).getEventsWithFilters(
                eq(testOrganization),
                any(), any(), any(), any(), any(), any(), any()
        );
    }

    @Test
    void getEvents_withFilters_shouldPassFiltersToService() {
        // Given
        Page<Event> eventPage = new PageImpl<>(Collections.singletonList(testEvent));
        when(eventService.getEventsWithFilters(
                eq(testOrganization),
                eq(Event.EventType.DEVICE_CREATED),
                eq(Event.EventSeverity.INFO),
                eq("device-001"),
                eq("Device"),
                any(), any(), any()
        )).thenReturn(eventPage);

        // When
        ResponseEntity<Page<EventResponse>> response = eventController.getEvents(
                "DEVICE_CREATED", "INFO", "device-001", "Device", null, null, 0, 50
        );

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(eventService).getEventsWithFilters(
                eq(testOrganization),
                eq(Event.EventType.DEVICE_CREATED),
                eq(Event.EventSeverity.INFO),
                eq("device-001"),
                eq("Device"),
                any(), any(), any()
        );
    }

    @Test
    void getRecentEvents_shouldReturnEventsFromLastNHours() {
        // Given
        List<Event> events = Collections.singletonList(testEvent);
        when(eventService.getRecentEvents(testOrganization, 24)).thenReturn(events);

        // When
        ResponseEntity<List<EventResponse>> response = eventController.getRecentEvents(24);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        verify(securityUtils).getCurrentUserOrganization();
        verify(eventService).getRecentEvents(testOrganization, 24);
    }

    @Test
    void getEventStatisticsByType_shouldReturnStatistics() {
        // Given
        Map<String, Long> statistics = Map.of(
                "DEVICE_CREATED", 5L,
                "TELEMETRY_RECEIVED", 100L
        );
        when(eventService.getEventStatisticsByType(testOrganization, 24)).thenReturn(statistics);

        // When
        ResponseEntity<Map<String, Long>> response = eventController.getEventStatisticsByType(24);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("DEVICE_CREATED", 5L);

        verify(securityUtils).getCurrentUserOrganization();
    }

    @Test
    void getEventStatisticsBySeverity_shouldReturnStatistics() {
        // Given
        Map<String, Long> statistics = Map.of(
                "INFO", 80L,
                "WARNING", 15L,
                "ERROR", 5L
        );
        when(eventService.getEventStatisticsBySeverity(testOrganization, 24)).thenReturn(statistics);

        // When
        ResponseEntity<Map<String, Long>> response = eventController.getEventStatisticsBySeverity(24);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).containsEntry("INFO", 80L);

        verify(securityUtils).getCurrentUserOrganization();
    }
}
