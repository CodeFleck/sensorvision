package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.GlobalAlertResponse;

import org.sensorvision.security.SecurityUtils;
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
    private final SecurityUtils securityUtils;

    /**
     * Get all global alerts for the current organization
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Page<GlobalAlertResponse>> getGlobalAlerts(
            @RequestParam(required = false, defaultValue = "false") boolean unacknowledgedOnly,
            @PageableDefault(size = 20, sort = "triggeredAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.debug("Fetching global alerts for organization {}", organizationId);

        Page<GlobalAlertResponse> alerts = unacknowledgedOnly ?
                globalAlertService.getUnacknowledgedAlerts(organizationId, pageable) :
                globalAlertService.getGlobalAlerts(organizationId, pageable);

        return ResponseEntity.ok(alerts);
    }

    /**
     * Get a specific global alert by ID
     */
    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> getGlobalAlert(@PathVariable UUID alertId) {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.debug("Fetching global alert: {}", alertId);

        GlobalAlertResponse alert = globalAlertService.getGlobalAlert(alertId, organizationId);

        return ResponseEntity.ok(alert);
    }

    /**
     * Acknowledge a global alert
     */
    @PostMapping("/{alertId}/acknowledge")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> acknowledgeAlert(@PathVariable UUID alertId) {

        Long userId = securityUtils.getCurrentUserId();
        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Acknowledging global alert: {} by user {}", alertId, userId);

        GlobalAlertResponse response = globalAlertService.acknowledgeAlert(alertId, userId, organizationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Resolve a global alert
     */
    @PostMapping("/{alertId}/resolve")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<GlobalAlertResponse> resolveAlert(
            @PathVariable UUID alertId,
            @RequestBody(required = false) Map<String, String> requestBody) {

        Long userId = securityUtils.getCurrentUserId();
        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.info("Resolving global alert: {} by user {}", alertId, userId);

        String resolutionNote = requestBody != null ? requestBody.get("resolutionNote") : null;

        GlobalAlertResponse response = globalAlertService.resolveAlert(alertId, userId, resolutionNote, organizationId);

        return ResponseEntity.ok(response);
    }

    /**
     * Get alert statistics for the current organization
     */
    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN', 'USER')")
    public ResponseEntity<Map<String, Long>> getAlertStats() {

        Long organizationId = securityUtils.getCurrentUserOrganization().getId();
        log.debug("Fetching alert stats for organization {}", organizationId);

        long unacknowledged = globalAlertService.countUnacknowledgedAlerts(organizationId);
        long unresolved = globalAlertService.countUnresolvedAlerts(organizationId);

        return ResponseEntity.ok(Map.of(
                "unacknowledged", unacknowledged,
                "unresolved", unresolved
        ));
    }
}
