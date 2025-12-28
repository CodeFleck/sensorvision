package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "scheduled_reports")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExportFormat exportFormat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ScheduleFrequency scheduleFrequency;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    // Email recipients (comma-separated)
    @Column(columnDefinition = "TEXT")
    private String emailRecipients;

    // Report parameters stored as JSON
    @Column(columnDefinition = "TEXT")
    private String reportParameters;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    private LocalDateTime lastRunAt;

    private LocalDateTime nextRunAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public enum ReportType {
        TELEMETRY_DATA,
        DEVICE_STATUS,
        ALERT_SUMMARY,
        ANALYTICS_SUMMARY
    }

    public enum ExportFormat {
        CSV,
        JSON,
        EXCEL
    }

    public enum ScheduleFrequency {
        DAILY,
        WEEKLY,
        MONTHLY
    }
}
