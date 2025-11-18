package org.sensorvision.controller;

import org.sensorvision.dto.AdminDashboardStatsDto;
import org.sensorvision.dto.AdminDashboardStatsDto.*;
import org.sensorvision.model.Alert;
import org.sensorvision.model.Device;
import org.sensorvision.model.IssueStatus;
import org.sensorvision.model.IssueSubmission;
import org.sensorvision.model.User;
import org.sensorvision.repository.*;
import org.sensorvision.websocket.TelemetryWebSocketHandler;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final DeviceRepository deviceRepository;
    private final OrganizationRepository organizationRepository;
    private final AlertRepository alertRepository;
    private final IssueSubmissionRepository issueSubmissionRepository;
    private final TelemetryRecordRepository telemetryRecordRepository;
    private final TelemetryWebSocketHandler telemetryWebSocketHandler;

    public AdminDashboardController(
            UserRepository userRepository,
            DeviceRepository deviceRepository,
            OrganizationRepository organizationRepository,
            AlertRepository alertRepository,
            IssueSubmissionRepository issueSubmissionRepository,
            TelemetryRecordRepository telemetryRecordRepository,
            TelemetryWebSocketHandler telemetryWebSocketHandler) {
        this.userRepository = userRepository;
        this.deviceRepository = deviceRepository;
        this.organizationRepository = organizationRepository;
        this.alertRepository = alertRepository;
        this.issueSubmissionRepository = issueSubmissionRepository;
        this.telemetryRecordRepository = telemetryRecordRepository;
        this.telemetryWebSocketHandler = telemetryWebSocketHandler;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<AdminDashboardStatsDto> getDashboardStats() {
        AdminDashboardStatsDto stats = new AdminDashboardStatsDto();

        // System Overview
        long totalUsers = userRepository.count();
        long totalDevices = deviceRepository.count();
        long totalOrganizations = organizationRepository.count();
        long activeAlerts = alertRepository.countByAcknowledged(false);
        long pendingSupportTickets = issueSubmissionRepository.countByStatusIn(
            List.of(IssueStatus.SUBMITTED, IssueStatus.IN_REVIEW)
        );

        stats.setSystemOverview(new SystemOverview(
            totalUsers,
            totalDevices,
            totalOrganizations,
            activeAlerts,
            pendingSupportTickets
        ));

        // Recent Activities
        stats.setRecentActivities(getRecentActivities());

        // System Health
        stats.setSystemHealth(getSystemHealth());

        // Chart Data
        stats.setChartData(getChartData());

        return ResponseEntity.ok(stats);
    }

    private List<RecentActivity> getRecentActivities() {
        List<RecentActivity> activities = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ISO_INSTANT;

        // Get recent users (last 10)
        List<User> recentUsers = userRepository.findTop10ByOrderByCreatedAtDesc();
        for (User user : recentUsers) {
            activities.add(new RecentActivity(
                "USER_REGISTERED",
                "New user registered: " + user.getUsername(),
                user.getCreatedAt() != null ? formatter.format(user.getCreatedAt()) : Instant.now().toString(),
                user.getUsername(),
                user.getId()
            ));
        }

        // Get recent devices (last 10)
        List<Device> recentDevices = deviceRepository.findTop10ByOrderByCreatedAtDesc();
        for (Device device : recentDevices) {
            String username = device.getOrganization() != null ?
                device.getOrganization().getName() : "Unknown";
            activities.add(new RecentActivity(
                "DEVICE_ADDED",
                "New device added: " + device.getName(),
                device.getCreatedAt() != null ? formatter.format(device.getCreatedAt()) : Instant.now().toString(),
                username,
                device.getId() != null ? device.getId().hashCode() : 0L
            ));
        }

        // Get recent support tickets (last 10)
        List<IssueSubmission> recentTickets = issueSubmissionRepository.findTop10ByOrderByCreatedAtDesc();
        for (IssueSubmission ticket : recentTickets) {
            activities.add(new RecentActivity(
                "TICKET_CREATED",
                "Support ticket: " + ticket.getTitle(),
                ticket.getCreatedAt() != null ? formatter.format(ticket.getCreatedAt()) : Instant.now().toString(),
                ticket.getUser() != null ? ticket.getUser().getUsername() : "Anonymous",
                ticket.getId()
            ));
        }

        // Get recent alerts (last 10)
        List<Alert> recentAlerts = alertRepository.findTop10ByOrderByTriggeredAtDesc();
        for (Alert alert : recentAlerts) {
            String deviceName = alert.getDevice() != null ? alert.getDevice().getName() : "Unknown";
            activities.add(new RecentActivity(
                "ALERT_TRIGGERED",
                "Alert triggered on " + deviceName + ": " + alert.getMessage(),
                alert.getTriggeredAt() != null ? formatter.format(alert.getTriggeredAt()) : Instant.now().toString(),
                "System",
                alert.getId() != null ? alert.getId().hashCode() : 0L
            ));
        }

        // Sort by timestamp descending and limit to 20 most recent
        return activities.stream()
            .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
            .limit(20)
            .collect(Collectors.toList());
    }

    private SystemHealth getSystemHealth() {
        // Calculate data ingestion rate (approximate from recent records)
        long recentRecordCount = telemetryRecordRepository.countRecordsInLastMinute();
        double dataIngestionRate = recentRecordCount / 60.0; // per second

        // Estimate storage (this is a simplified calculation)
        long totalRecords = telemetryRecordRepository.count();
        long storageUsedMb = (totalRecords * 512) / (1024 * 1024); // Rough estimate: ~512 bytes per record

        // Get active WebSocket connections
        int activeConnections = telemetryWebSocketHandler.getConnectedSessionCount();

        // Determine system status
        String status = "HEALTHY";
        if (dataIngestionRate > 1000) {
            status = "DEGRADED"; // High load
        }
        if (activeConnections > 500) {
            status = "DEGRADED";
        }

        return new SystemHealth(
            Math.round(dataIngestionRate * 100.0) / 100.0,
            storageUsedMb,
            activeConnections,
            status
        );
    }

    private ChartData getChartData() {
        // User growth over last 30 days
        List<DataPoint> userGrowth = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = userRepository.countUsersCreatedOnDate(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
            userGrowth.add(new DataPoint(date.toString(), count));
        }

        // Device activity (devices with telemetry) over last 30 days
        List<DataPoint> deviceActivity = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = telemetryRecordRepository.countUniqueDevicesOnDate(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
            deviceActivity.add(new DataPoint(date.toString(), count));
        }

        // Support ticket volume over last 30 days
        List<DataPoint> ticketVolume = new ArrayList<>();
        for (int i = 29; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            long count = issueSubmissionRepository.countTicketsCreatedOnDate(
                date.atStartOfDay(ZoneId.systemDefault()).toInstant(),
                date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
            );
            ticketVolume.add(new DataPoint(date.toString(), count));
        }

        return new ChartData(userGrowth, deviceActivity, ticketVolume);
    }
}
