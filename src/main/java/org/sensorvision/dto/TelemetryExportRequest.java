package org.sensorvision.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record TelemetryExportRequest(
        UUID deviceId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        List<String> variableNames,  // Optional: filter specific variables
        String format  // "excel" or "csv"
) {
}
