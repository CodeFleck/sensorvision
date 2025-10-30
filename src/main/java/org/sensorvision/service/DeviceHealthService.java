package org.sensorvision.service;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Service for calculating and maintaining device health scores (0-100).
 *
 * Health score is calculated based on:
 * - Uptime/Connectivity: 40% weight (based on last seen time)
 * - Alert Status: 30% weight (number of unacknowledged alerts)
 * - Data Quality: 20% weight (telemetry consistency)
 * - Status: 10% weight (device operational status)
 */
@Service
@Slf4j
public class DeviceHealthService {

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    // Health score weights
    private static final double UPTIME_WEIGHT = 0.40;
    private static final double ALERT_WEIGHT = 0.30;
    private static final double DATA_QUALITY_WEIGHT = 0.20;
    private static final double STATUS_WEIGHT = 0.10;

    // Thresholds
    private static final int OFFLINE_THRESHOLD_MINUTES = 30;
    private static final int CRITICAL_ALERT_THRESHOLD = 5;
    private static final int WARNING_ALERT_THRESHOLD = 2;

    /**
     * Calculate health score for a specific device
     */
    @Transactional
    public int calculateDeviceHealthScore(Device device) {
        double uptimeScore = calculateUptimeScore(device);
        double alertScore = calculateAlertScore(device);
        double dataQualityScore = calculateDataQualityScore(device);
        double statusScore = calculateStatusScore(device);

        // Weighted average
        double totalScore = (uptimeScore * UPTIME_WEIGHT) +
                           (alertScore * ALERT_WEIGHT) +
                           (dataQualityScore * DATA_QUALITY_WEIGHT) +
                           (statusScore * STATUS_WEIGHT);

        int healthScore = (int) Math.round(totalScore);

        // Update device health score
        device.setHealthScore(healthScore);
        device.setLastHealthCheckAt(LocalDateTime.now());
        deviceRepository.save(device);

        log.debug("Device {} health score calculated: {} (uptime: {}, alerts: {}, data: {}, status: {})",
                device.getExternalId(), healthScore, uptimeScore, alertScore, dataQualityScore, statusScore);

        return healthScore;
    }

    /**
     * Calculate uptime score (0-100) based on last seen time
     * 100 = seen within last 5 minutes
     * 0 = not seen for over 30 minutes
     */
    private double calculateUptimeScore(Device device) {
        if (device.getLastSeenAt() == null) {
            return 0.0;
        }

        long minutesSinceLastSeen = Duration.between(device.getLastSeenAt(), Instant.now()).toMinutes();

        if (minutesSinceLastSeen <= 5) {
            return 100.0;
        } else if (minutesSinceLastSeen >= OFFLINE_THRESHOLD_MINUTES) {
            return 0.0;
        } else {
            // Linear decay from 100 at 5 min to 0 at 30 min
            return 100.0 - ((minutesSinceLastSeen - 5.0) / (OFFLINE_THRESHOLD_MINUTES - 5.0)) * 100.0;
        }
    }

    /**
     * Calculate alert score (0-100) based on unacknowledged alerts
     * 100 = no alerts
     * 50 = moderate alerts
     * 0 = critical number of alerts
     */
    private double calculateAlertScore(Device device) {
        long unacknowledgedAlerts = alertRepository.countByDeviceIdAndAcknowledgedFalse(device.getId());

        if (unacknowledgedAlerts == 0) {
            return 100.0;
        } else if (unacknowledgedAlerts <= WARNING_ALERT_THRESHOLD) {
            return 80.0;
        } else if (unacknowledgedAlerts <= CRITICAL_ALERT_THRESHOLD) {
            return 40.0;
        } else {
            // More than critical threshold
            return Math.max(0.0, 40.0 - ((unacknowledgedAlerts - CRITICAL_ALERT_THRESHOLD) * 5.0));
        }
    }

    /**
     * Calculate data quality score (0-100) based on telemetry consistency
     * 100 = consistent telemetry data
     * 0 = no telemetry data or highly inconsistent
     */
    private double calculateDataQualityScore(Device device) {
        // Count telemetry records in last 24 hours
        Instant oneDayAgo = Instant.now().minus(1, ChronoUnit.DAYS);
        long telemetryCount = telemetryRecordRepository.countByDeviceIdAndTimestampAfter(
                device.getId(), oneDayAgo);

        if (telemetryCount == 0) {
            return 0.0;
        } else if (telemetryCount >= 288) {
            // 288 records = every 5 minutes for 24 hours
            return 100.0;
        } else {
            // Linear scale based on expected telemetry frequency
            return (telemetryCount / 288.0) * 100.0;
        }
    }

    /**
     * Calculate status score (0-100) based on device operational status
     */
    private double calculateStatusScore(Device device) {
        return switch (device.getStatus()) {
            case ONLINE -> 100.0;
            case OFFLINE -> 0.0;
            case UNKNOWN -> 50.0;
        };
    }

    /**
     * Calculate health scores for all devices in an organization
     */
    @Transactional
    public void calculateAllHealthScores(Long organizationId) {
        List<Device> devices = deviceRepository.findByOrganizationId(organizationId);
        int updatedCount = 0;

        for (Device device : devices) {
            try {
                calculateDeviceHealthScore(device);
                updatedCount++;
            } catch (Exception e) {
                log.error("Failed to calculate health score for device {}: {}",
                        device.getExternalId(), e.getMessage());
            }
        }

        log.info("Updated health scores for {} / {} devices in organization {}",
                updatedCount, devices.size(), organizationId);
    }

    /**
     * Scheduled task to recalculate health scores for all devices
     * Runs every 5 minutes
     */
    @Scheduled(fixedDelay = 300000) // 5 minutes
    @Transactional
    public void scheduledHealthScoreUpdate() {
        log.info("Starting scheduled health score update");
        List<Device> allDevices = deviceRepository.findAll();
        int updatedCount = 0;

        for (Device device : allDevices) {
            try {
                calculateDeviceHealthScore(device);
                updatedCount++;
            } catch (Exception e) {
                log.error("Failed to calculate health score for device {}: {}",
                        device.getExternalId(), e.getMessage());
            }
        }

        log.info("Scheduled health score update completed: {} devices updated", updatedCount);
    }

    /**
     * Get health status category based on score
     */
    public String getHealthStatus(int healthScore) {
        if (healthScore >= 80) {
            return "EXCELLENT";
        } else if (healthScore >= 60) {
            return "GOOD";
        } else if (healthScore >= 40) {
            return "FAIR";
        } else if (healthScore >= 20) {
            return "POOR";
        } else {
            return "CRITICAL";
        }
    }
}
