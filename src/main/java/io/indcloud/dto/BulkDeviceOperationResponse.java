package io.indcloud.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkDeviceOperationResponse {
    private int totalRequested;
    private int successCount;
    private int failureCount;
    private List<String> successfulDeviceIds;
    private Map<String, String> failedDevices; // deviceId -> error message
    private String message;
}
