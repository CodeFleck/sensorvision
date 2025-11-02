package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.ArchiveExecutionStatus;
import org.sensorvision.model.ArchiveStorageType;

import java.time.Instant;

public record DataRetentionPolicyDto(
    Long id,
    Boolean enabled,
    Integer retentionDays,
    Boolean archiveEnabled,
    ArchiveStorageType archiveStorageType,
    JsonNode archiveStorageConfig,
    String archiveScheduleCron,
    Instant lastArchiveRun,
    ArchiveExecutionStatus lastArchiveStatus,
    String lastArchiveError,
    Long totalRecordsArchived,
    Long totalArchiveSizeBytes,
    Instant createdAt,
    Instant updatedAt
) {}
