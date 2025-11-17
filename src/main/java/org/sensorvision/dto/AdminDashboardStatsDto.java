package org.sensorvision.dto;

import java.util.List;

public class AdminDashboardStatsDto {
    private SystemOverview systemOverview;
    private List<RecentActivity> recentActivities;
    private SystemHealth systemHealth;
    private ChartData chartData;

    public static class SystemOverview {
        private long totalUsers;
        private long totalDevices;
        private long totalOrganizations;
        private long activeAlerts;
        private long pendingSupportTickets;

        public SystemOverview() {}

        public SystemOverview(long totalUsers, long totalDevices, long totalOrganizations,
                            long activeAlerts, long pendingSupportTickets) {
            this.totalUsers = totalUsers;
            this.totalDevices = totalDevices;
            this.totalOrganizations = totalOrganizations;
            this.activeAlerts = activeAlerts;
            this.pendingSupportTickets = pendingSupportTickets;
        }

        // Getters and setters
        public long getTotalUsers() { return totalUsers; }
        public void setTotalUsers(long totalUsers) { this.totalUsers = totalUsers; }

        public long getTotalDevices() { return totalDevices; }
        public void setTotalDevices(long totalDevices) { this.totalDevices = totalDevices; }

        public long getTotalOrganizations() { return totalOrganizations; }
        public void setTotalOrganizations(long totalOrganizations) { this.totalOrganizations = totalOrganizations; }

        public long getActiveAlerts() { return activeAlerts; }
        public void setActiveAlerts(long activeAlerts) { this.activeAlerts = activeAlerts; }

        public long getPendingSupportTickets() { return pendingSupportTickets; }
        public void setPendingSupportTickets(long pendingSupportTickets) { this.pendingSupportTickets = pendingSupportTickets; }
    }

    public static class RecentActivity {
        private String type; // USER_REGISTERED, TICKET_CREATED, ALERT_TRIGGERED, DEVICE_ADDED
        private String description;
        private String timestamp;
        private String username;
        private Long entityId;

        public RecentActivity() {}

        public RecentActivity(String type, String description, String timestamp, String username, Long entityId) {
            this.type = type;
            this.description = description;
            this.timestamp = timestamp;
            this.username = username;
            this.entityId = entityId;
        }

        // Getters and setters
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }

        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }

        public String getTimestamp() { return timestamp; }
        public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }

        public Long getEntityId() { return entityId; }
        public void setEntityId(Long entityId) { this.entityId = entityId; }
    }

    public static class SystemHealth {
        private double dataIngestionRate; // messages per second
        private long storageUsedMb;
        private int activeConnections;
        private String status; // HEALTHY, DEGRADED, ERROR

        public SystemHealth() {}

        public SystemHealth(double dataIngestionRate, long storageUsedMb, int activeConnections, String status) {
            this.dataIngestionRate = dataIngestionRate;
            this.storageUsedMb = storageUsedMb;
            this.activeConnections = activeConnections;
            this.status = status;
        }

        // Getters and setters
        public double getDataIngestionRate() { return dataIngestionRate; }
        public void setDataIngestionRate(double dataIngestionRate) { this.dataIngestionRate = dataIngestionRate; }

        public long getStorageUsedMb() { return storageUsedMb; }
        public void setStorageUsedMb(long storageUsedMb) { this.storageUsedMb = storageUsedMb; }

        public int getActiveConnections() { return activeConnections; }
        public void setActiveConnections(int activeConnections) { this.activeConnections = activeConnections; }

        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    public static class ChartData {
        private List<DataPoint> userGrowth;
        private List<DataPoint> deviceActivity;
        private List<DataPoint> ticketVolume;

        public ChartData() {}

        public ChartData(List<DataPoint> userGrowth, List<DataPoint> deviceActivity, List<DataPoint> ticketVolume) {
            this.userGrowth = userGrowth;
            this.deviceActivity = deviceActivity;
            this.ticketVolume = ticketVolume;
        }

        // Getters and setters
        public List<DataPoint> getUserGrowth() { return userGrowth; }
        public void setUserGrowth(List<DataPoint> userGrowth) { this.userGrowth = userGrowth; }

        public List<DataPoint> getDeviceActivity() { return deviceActivity; }
        public void setDeviceActivity(List<DataPoint> deviceActivity) { this.deviceActivity = deviceActivity; }

        public List<DataPoint> getTicketVolume() { return ticketVolume; }
        public void setTicketVolume(List<DataPoint> ticketVolume) { this.ticketVolume = ticketVolume; }
    }

    public static class DataPoint {
        private String label;
        private long value;

        public DataPoint() {}

        public DataPoint(String label, long value) {
            this.label = label;
            this.value = value;
        }

        // Getters and setters
        public String getLabel() { return label; }
        public void setLabel(String label) { this.label = label; }

        public long getValue() { return value; }
        public void setValue(long value) { this.value = value; }
    }

    // Main class getters and setters
    public SystemOverview getSystemOverview() { return systemOverview; }
    public void setSystemOverview(SystemOverview systemOverview) { this.systemOverview = systemOverview; }

    public List<RecentActivity> getRecentActivities() { return recentActivities; }
    public void setRecentActivities(List<RecentActivity> recentActivities) { this.recentActivities = recentActivities; }

    public SystemHealth getSystemHealth() { return systemHealth; }
    public void setSystemHealth(SystemHealth systemHealth) { this.systemHealth = systemHealth; }

    public ChartData getChartData() { return chartData; }
    public void setChartData(ChartData chartData) { this.chartData = chartData; }
}
