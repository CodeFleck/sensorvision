package org.sensorvision.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record DeviceLocationResponse(
        UUID deviceId,
        String deviceName,
        BigDecimal latitude,
        BigDecimal longitude,
        BigDecimal altitude,
        LocalDateTime locationUpdatedAt,
        String status
) {
}
