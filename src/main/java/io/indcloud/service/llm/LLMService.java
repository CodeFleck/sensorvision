package io.indcloud.service.llm;

import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.model.LLMProvider;
import reactor.core.publisher.Mono;

/**
 * Abstract interface for LLM provider implementations.
 * Each provider (Claude, OpenAI, Gemini) implements this interface.
 */
public interface LLMService {

    /**
     * Get the provider this service handles.
     */
    LLMProvider getProvider();

    /**
     * Check if this provider is configured and available.
     */
    boolean isAvailable();

    /**
     * Send a completion request to the LLM.
     *
     * @param request The LLM request with prompt and parameters
     * @return The LLM response with generated content and usage stats
     */
    Mono<LLMResponse> complete(LLMRequest request);

    /**
     * Get the default model ID for this provider.
     */
    String getDefaultModelId();

    /**
     * Estimate the cost in cents for given token counts.
     *
     * @param inputTokens Number of input tokens
     * @param outputTokens Number of output tokens
     * @return Estimated cost in USD cents
     */
    int estimateCostCents(int inputTokens, int outputTokens);
}
