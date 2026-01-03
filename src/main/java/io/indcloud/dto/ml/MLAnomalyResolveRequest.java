package io.indcloud.dto.ml;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for resolving an ML anomaly.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MLAnomalyResolveRequest {

    @Size(max = 2000, message = "Resolution note must not exceed 2000 characters")
    private String resolutionNote;
}
