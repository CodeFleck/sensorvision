package io.indcloud.dto.ml;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchInferenceResponseDto {
    private UUID modelId;
    private int totalDevices;
    private int processedDevices;
    private List<InferenceResponseDto> predictions;
    private List<Map<String, Object>> errors;
    private long processingTimeMs;
}
