package io.indcloud.dto;

import io.indcloud.model.DeviceStatus;

import java.util.List;

public record DeviceExportRequest(
        DeviceStatus status,  // Optional: filter by status
        List<Long> groupIds,  // Optional: filter by device groups
        String format  // "excel" or "csv"
) {
}
