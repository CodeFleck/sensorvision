package org.sensorvision.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record LocationHistoryResponse(
        UUID deviceId,
        String deviceName,
        List<LocationPoint> locationHistory
) {
    public record LocationPoint(
            BigDecimal latitude,
            BigDecimal longitude,
            BigDecimal altitude,
            LocalDateTime timestamp
    ) {
    }
}
