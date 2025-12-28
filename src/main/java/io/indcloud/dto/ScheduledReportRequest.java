package io.indcloud.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.indcloud.model.ScheduledReport;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ScheduledReportRequest {

    @NotBlank(message = "Report name is required")
    private String name;

    private String description;

    @NotNull(message = "Report type is required")
    private ScheduledReport.ReportType reportType;

    @NotNull(message = "Export format is required")
    private ScheduledReport.ExportFormat exportFormat;

    @NotNull(message = "Schedule frequency is required")
    private ScheduledReport.ScheduleFrequency scheduleFrequency;

    private Boolean enabled = true;

    private String emailRecipients;

    private String reportParameters;
}
