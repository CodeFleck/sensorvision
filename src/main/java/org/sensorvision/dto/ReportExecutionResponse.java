package org.sensorvision.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.sensorvision.model.ReportExecution;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecutionResponse {

    private Long id;
    private Long scheduledReportId;
    private String scheduledReportName;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private ReportExecution.ExecutionStatus status;
    private String errorMessage;
    private Integer recordCount;
    private Long fileSizeBytes;
    private String fileUrl;

    public static ReportExecutionResponse fromEntity(ReportExecution execution) {
        return ReportExecutionResponse.builder()
                .id(execution.getId())
                .scheduledReportId(execution.getScheduledReport().getId())
                .scheduledReportName(execution.getScheduledReport().getName())
                .startedAt(execution.getStartedAt())
                .completedAt(execution.getCompletedAt())
                .status(execution.getStatus())
                .errorMessage(execution.getErrorMessage())
                .recordCount(execution.getRecordCount())
                .fileSizeBytes(execution.getFileSizeBytes())
                .fileUrl(execution.getFileUrl())
                .build();
    }
}
