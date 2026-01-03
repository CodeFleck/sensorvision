package io.indcloud.dto.ml;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchInferenceRequestDto {

    @NotNull(message = "Organization ID is required")
    private Long organizationId;

    @NotNull(message = "Model ID is required")
    private UUID modelId;

    @NotEmpty(message = "Device IDs list cannot be empty")
    private List<UUID> deviceIds;

    @Min(value = 1, message = "Time range must be at least 1 hour")
    @Max(value = 168, message = "Time range cannot exceed 168 hours (7 days)")
    private int timeRangeHours;
}
