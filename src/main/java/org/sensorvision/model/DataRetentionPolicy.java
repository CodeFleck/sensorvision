package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "data_retention_policies")
@Getter
@Setter
public class DataRetentionPolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "retention_days", nullable = false)
    private Integer retentionDays = 90;

    @Column(name = "archive_enabled", nullable = false)
    private Boolean archiveEnabled = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "archive_storage_type", nullable = false)
    private ArchiveStorageType archiveStorageType = ArchiveStorageType.LOCAL_FILE;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "archive_storage_config", columnDefinition = "jsonb")
    private JsonNode archiveStorageConfig;

    @Column(name = "archive_schedule_cron", nullable = false)
    private String archiveScheduleCron = "0 2 * * *"; // Daily at 2 AM

    @Column(name = "last_archive_run")
    private Instant lastArchiveRun;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_archive_status")
    private ArchiveExecutionStatus lastArchiveStatus;

    @Column(name = "last_archive_error", columnDefinition = "TEXT")
    private String lastArchiveError;

    @Column(name = "total_records_archived", nullable = false)
    private Long totalRecordsArchived = 0L;

    @Column(name = "total_archive_size_bytes", nullable = false)
    private Long totalArchiveSizeBytes = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
