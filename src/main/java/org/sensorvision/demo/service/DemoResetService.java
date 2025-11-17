package org.sensorvision.demo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.demo.cache.RollingTelemetryCache;
import org.sensorvision.demo.config.DemoModeProperties;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service for resetting demo mode data.
 *
 * Provides a one-click reset functionality to prepare for client demos:
 * 1. Clears telemetry records from database
 * 2. Clears alerts from database
 * 3. Clears rolling cache
 *
 * Note: Does NOT delete devices, allowing token reuse and faster reset.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
public class DemoResetService {

    private final DemoModeProperties properties;
    private final RollingTelemetryCache cache;
    private final OrganizationRepository organizationRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final AlertRepository alertRepository;

    /**
     * Reset all demo data to a clean state.
     *
     * This operation:
     * - Deletes all telemetry records for the demo organization
     * - Deletes all alerts for the demo organization
     * - Clears the in-memory cache
     *
     * Devices are preserved to maintain API tokens.
     *
     * @return Summary of deletion counts
     */
    @Transactional
    public DemoResetResult reset() {
        log.info("🔄 Starting demo mode reset...");

        Organization demoOrg = organizationRepository
                .findByName(properties.getOrganizationName())
                .orElse(null);

        if (demoOrg == null) {
            log.warn("Demo organization not found: {}", properties.getOrganizationName());
            return new DemoResetResult(0, 0, 0, "Demo organization not found");
        }

        // 1. Clear telemetry records
        int telemetryDeleted = clearDemoTelemetry(demoOrg.getId());
        log.info("   Deleted {} telemetry records", telemetryDeleted);

        // 2. Clear alerts
        int alertsDeleted = clearDemoAlerts(demoOrg.getId());
        log.info("   Deleted {} alerts", alertsDeleted);

        // 3. Clear cache
        cache.clearAll();
        log.info("   Cleared in-memory cache");

        log.info("✅ Demo mode reset complete");

        return new DemoResetResult(
                telemetryDeleted,
                alertsDeleted,
                properties.getDeviceCount(),
                "Demo reset successful"
        );
    }

    /**
     * Delete all telemetry records for the demo organization
     */
    private int clearDemoTelemetry(Long organizationId) {
        return telemetryRecordRepository.deleteByOrganizationId(organizationId);
    }

    /**
     * Delete all alerts for devices in the demo organization
     */
    private int clearDemoAlerts(Long organizationId) {
        return alertRepository.deleteByDeviceOrganizationId(organizationId);
    }

    /**
     * Result DTO for demo reset operation
     */
    public record DemoResetResult(
            int telemetryRecordsDeleted,
            int alertsDeleted,
            int devicesRetained,
            String message
    ) {}
}
