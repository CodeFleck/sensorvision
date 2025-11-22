package org.sensorvision.dto;

import jakarta.validation.constraints.NotBlank;

public record DeviceUpdateRequest(
                @NotBlank(message = "name is required") String name,
                String description,
                Boolean active,
                String location,
                String sensorType,
                String firmwareVersion,
                java.util.List<String> tags,
                java.util.List<Long> groupIds) {
}
