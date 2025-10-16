package org.sensorvision.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "report_executions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "scheduled_report_id", nullable = false)
    private ScheduledReport scheduledReport;

    @Column(nullable = false)
    private LocalDateTime startedAt;

    private LocalDateTime completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExecutionStatus status;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    private Integer recordCount;

    private Long fileSizeBytes;

    @Column(columnDefinition = "TEXT")
    private String fileUrl;

    public enum ExecutionStatus {
        RUNNING,
        COMPLETED,
        FAILED
    }
}
