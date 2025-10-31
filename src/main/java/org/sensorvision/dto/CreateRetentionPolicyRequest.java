package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.sensorvision.model.ArchiveStorageType;

public record CreateRetentionPolicyRequest(
    @NotNull Boolean enabled,
    @NotNull @Min(1) Integer retentionDays,
    @NotNull Boolean archiveEnabled,
    @NotNull ArchiveStorageType archiveStorageType,
    JsonNode archiveStorageConfig,
    String archiveScheduleCron
) {}
