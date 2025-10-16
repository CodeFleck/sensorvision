package org.sensorvision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sensorvision.model.ScheduledReport;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReportResponse {

    private Long id;
    private String name;
    private String description;
    private ScheduledReport.ReportType reportType;
    private ScheduledReport.ExportFormat exportFormat;
    private ScheduledReport.ScheduleFrequency scheduleFrequency;
    private Boolean enabled;
    private String emailRecipients;
    private String reportParameters;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime lastRunAt;
    private LocalDateTime nextRunAt;
    private String createdByUsername;

    public static ScheduledReportResponse fromEntity(ScheduledReport report) {
        return ScheduledReportResponse.builder()
                .id(report.getId())
                .name(report.getName())
                .description(report.getDescription())
                .reportType(report.getReportType())
                .exportFormat(report.getExportFormat())
                .scheduleFrequency(report.getScheduleFrequency())
                .enabled(report.getEnabled())
                .emailRecipients(report.getEmailRecipients())
                .reportParameters(report.getReportParameters())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .lastRunAt(report.getLastRunAt())
                .nextRunAt(report.getNextRunAt())
                .createdByUsername(report.getCreatedBy().getUsername())
                .build();
    }
}
