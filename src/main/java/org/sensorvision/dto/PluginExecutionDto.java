package org.sensorvision.dto;

import org.sensorvision.model.PluginExecutionStatus;

import java.time.Instant;

public record PluginExecutionDto(
        Long id,
        Long pluginId,
        String pluginName,
        Instant executedAt,
        PluginExecutionStatus status,
        Integer recordsProcessed,
        String errorMessage,
        Long durationMs
) {
}
