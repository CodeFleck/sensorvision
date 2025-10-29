package org.sensorvision.dto;

import org.sensorvision.model.ArchiveExecutionStatus;

import java.time.Instant;

public record DataArchiveExecutionDto(
        Long id,
        Long policyId,
        Instant startedAt,
        Instant completedAt,
        ArchiveExecutionStatus status,
        Instant archiveFromDate,
        Instant archiveToDate,
        Integer recordsArchived,
        String archiveFilePath,
        Long archiveSizeBytes,
        Long durationMs,
        String errorMessage
) {}
