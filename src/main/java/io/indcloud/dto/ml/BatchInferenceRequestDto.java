package io.indcloud.dto.ml;

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
    private Long organizationId;
    private UUID modelId;
    private List<UUID> deviceIds;
    private int timeRangeHours;
}
