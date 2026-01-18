package io.indcloud.dto.llm;

import io.indcloud.model.LLMProvider;
import lombok.Builder;
import lombok.Data;

/**
 * Response DTO from LLM API calls.
 */
@Data
@Builder
public class LLMResponse {

    /**
     * The provider that handled this request.
     */
    private LLMProvider provider;

    /**
     * The model ID used (e.g., "claude-3-5-sonnet-20241022").
     */
    private String modelId;

    /**
     * The generated response content.
     */
    private String content;

    /**
     * Number of input tokens used.
     */
    private Integer inputTokens;

    /**
     * Number of output tokens generated.
     */
    private Integer outputTokens;

    /**
     * Total tokens (input + output).
     */
    private Integer totalTokens;

    /**
     * Estimated cost in USD cents.
     */
    private Integer estimatedCostCents;

    /**
     * Request latency in milliseconds.
     */
    private Integer latencyMs;

    /**
     * Whether the request was successful.
     */
    private boolean success;

    /**
     * Error message if request failed.
     */
    private String errorMessage;

    /**
     * Stop reason from the API (e.g., "end_turn", "max_tokens").
     */
    private String stopReason;

    /**
     * Create a successful response.
     */
    public static LLMResponse success(LLMProvider provider, String modelId, String content,
                                       int inputTokens, int outputTokens, int latencyMs) {
        return LLMResponse.builder()
                .provider(provider)
                .modelId(modelId)
                .content(content)
                .inputTokens(inputTokens)
                .outputTokens(outputTokens)
                .totalTokens(inputTokens + outputTokens)
                .latencyMs(latencyMs)
                .success(true)
                .build();
    }

    /**
     * Create a failed response.
     */
    public static LLMResponse failure(LLMProvider provider, String errorMessage) {
        return LLMResponse.builder()
                .provider(provider)
                .success(false)
                .errorMessage(errorMessage)
                .build();
    }
}
