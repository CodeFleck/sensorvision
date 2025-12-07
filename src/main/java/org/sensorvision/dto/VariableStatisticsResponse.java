package org.sensorvision.dto;

import org.sensorvision.service.DynamicVariableService.VariableStatistics;

import java.time.Instant;

/**
 * Response DTO for variable statistics/aggregations.
 */
public record VariableStatisticsResponse(
        Long variableId,
        String variableName,
        Instant startTime,
        Instant endTime,
        Double average,
        Double min,
        Double max,
        Double sum,
        long count
) {
    /**
     * Create from VariableStatistics.
     */
    public static VariableStatisticsResponse from(
            Long variableId,
            String variableName,
            Instant startTime,
            Instant endTime,
            VariableStatistics stats) {
        return new VariableStatisticsResponse(
                variableId,
                variableName,
                startTime,
                endTime,
                stats.average(),
                stats.min(),
                stats.max(),
                stats.sum(),
                stats.count()
        );
    }
}
