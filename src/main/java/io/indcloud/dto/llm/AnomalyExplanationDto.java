package io.indcloud.dto.llm;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.UUID;

/**
 * DTO for anomaly explanation responses from the LLM service.
 */
@Data
@Builder
public class AnomalyExplanationDto {

    /**
     * The ID of the anomaly that was explained.
     */
    private UUID anomalyId;

    /**
     * The ID of the device associated with the anomaly.
     */
    private UUID deviceId;

    /**
     * The name of the device for display purposes.
     */
    private String deviceName;

    /**
     * The anomaly score (0-1 scale, higher = more anomalous).
     */
    private Double anomalyScore;

    /**
     * The severity level of the anomaly (INFO, WARNING, CRITICAL).
     */
    private String severity;

    /**
     * The AI-generated explanation of the anomaly.
     */
    private String explanation;

    /**
     * Whether the LLM request was successful.
     */
    private boolean success;

    /**
     * Error message if the request failed.
     */
    private String errorMessage;

    /**
     * The LLM provider used (CLAUDE, OPENAI, GEMINI).
     */
    private String provider;

    /**
     * The specific model ID used for the response.
     */
    private String modelId;

    /**
     * Total tokens used for this request.
     */
    private Integer tokensUsed;

    /**
     * Response latency in milliseconds.
     */
    private Integer latencyMs;

    /**
     * When the explanation was generated.
     */
    private Instant generatedAt;
}
