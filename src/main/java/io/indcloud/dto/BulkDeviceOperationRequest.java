package io.indcloud.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
public class BulkDeviceOperationRequest {

    @NotEmpty(message = "Device IDs list cannot be empty")
    private List<String> deviceIds;

    @NotNull(message = "Operation type is required")
    private OperationType operation;

    // Optional parameters for specific operations
    private Map<String, Object> parameters;

    public enum OperationType {
        DELETE,
        ENABLE,
        DISABLE,
        ASSIGN_TAGS,
        REMOVE_TAGS,
        ASSIGN_GROUP,
        REMOVE_FROM_GROUP,
        UPDATE_STATUS
    }
}
