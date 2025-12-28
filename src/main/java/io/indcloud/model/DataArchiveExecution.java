package io.indcloud.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "data_archive_executions")
@Getter
@Setter
public class DataArchiveExecution {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "policy_id", nullable = false)
    private DataRetentionPolicy policy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ArchiveExecutionStatus status = ArchiveExecutionStatus.RUNNING;

    @Column(name = "archive_from_date", nullable = false)
    private Instant archiveFromDate;

    @Column(name = "archive_to_date", nullable = false)
    private Instant archiveToDate;

    @Column(name = "records_archived", nullable = false)
    private Integer recordsArchived = 0;

    @Column(name = "archive_file_path", columnDefinition = "TEXT")
    private String archiveFilePath;

    @Column(name = "archive_size_bytes", nullable = false)
    private Long archiveSizeBytes = 0L;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "stack_trace", columnDefinition = "TEXT")
    private String stackTrace;

    @PrePersist
    protected void onCreate() {
        if (startedAt == null) {
            startedAt = Instant.now();
        }
    }

    public Long getDurationMs() {
        if (startedAt == null) {
            return null;
        }
        Instant end = completedAt != null ? completedAt : Instant.now();
        return java.time.Duration.between(startedAt, end).toMillis();
    }
}
