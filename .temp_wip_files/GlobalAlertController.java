package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.GlobalAlertResponse;
import org.sensorvision.security.CurrentUser;
import org.sensorvision.security.UserPrincipal;
import org.sensorvision.service.GlobalAlertService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

/**
 * REST API controller for global alerts management
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/global-alerts")
@RequiredArgsConstructor
public class GlobalAlertController {

    private final GlobalAlertService globalAlertService;

    /**
     * Get all global alerts for the current organization
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<GlobalAlertResponse>> getGlobalAlerts(
            @RequestParam(required = false, defaultValue = "false") boolean unacknowledgedOnly,
            @PageableDefault(size = 20, sort = "triggeredAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @CurrentUser UserPrincipal currentUser) {

        log.debug("Fetching global alerts for organization {}", currentUser.getOrganizationId());

        Page<GlobalAlertResponse> alerts = unacknowledgedOnly ?
                globalAlertService.getUnacknowledgedAlerts(currentUser.getOrganizationId(), pageable) :
                globalAlertService.getGlobalAlerts(currentUser.getOrganizationId(), pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Get a specific global alert by ID
     */
    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> getGlobalAlert(
            @PathVariable UUID alertId,
            @CurrentUser UserPrincipal currentUser) {

        log.debug("Fetching global alert: {}", alertId);

        GlobalAlertResponse alert = globalAlertService.getGlobalAlert(
                alertId, currentUser.getOrganizationId());

        return ResponseEntity.ok(alert);
    }

    /**
     * Acknowledge a global alert
     */
    @PostMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> acknowledgeAlert(
            @PathVariable UUID alertId,
            @CurrentUser UserPrincipal currentUser) {

        log.info("Acknowledging global alert: {} by user {}", alertId, currentUser.getId());

        GlobalAlertResponse response = globalAlertService.acknowledgeAlert(
                alertId, currentUser.getId(), currentUser.getOrganizationId());

        return ResponseEntity.ok(response);
    }

    /**
     * Resolve a global alert
     */
    @PostMapping("/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> resolveAlert(
            @PathVariable UUID alertId,
            @RequestBody(required = false) Map<String, String> requestBody,
            @CurrentUser UserPrincipal currentUser) {

        log.info("Resolving global alert: {} by user {}", alertId, currentUser.getId());

        String resolutionNote = requestBody != null ? requestBody.get("resolutionNote") : null;

        GlobalAlertResponse response = globalAlertService.resolveAlert(
                alertId, currentUser.getId(), resolutionNote, currentUser.getOrganizationId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get alert statistics for the current organization
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Long>> getAlertStats(
            @CurrentUser UserPrincipal currentUser) {

        log.debug("Fetching alert stats for organization {}", currentUser.getOrganizationId());

        long unacknowledged = globalAlertService.countUnacknowledgedAlerts(
                currentUser.getOrganizationId());
        long unresolved = globalAlertService.countUnresolvedAlerts(
                currentUser.getOrganizationId());

        return ResponseEntity.ok(Map.of(
                "unacknowledged", unacknowledged,
                "unresolved", unresolved
        ));
    }
}
