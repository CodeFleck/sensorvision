package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceUpdateRequest(
        @NotBlank(message = "name is required") String name,
        String location,
        String sensorType,
        String firmwareVersion
) {
}
