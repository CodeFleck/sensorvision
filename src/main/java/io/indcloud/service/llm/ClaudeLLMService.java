package io.indcloud.service.llm;

import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.model.LLMProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Claude (Anthropic) LLM service implementation.
 * Uses the Anthropic Messages API.
 */
@Service
@Slf4j
public class ClaudeLLMService implements LLMService {

    private static final String API_BASE_URL = "https://api.anthropic.com";
    private static final String API_VERSION = "2023-06-01";
    private static final String DEFAULT_MODEL = "claude-sonnet-4-20250514";

    // Pricing per 1M tokens (in cents) - Claude 3.5 Sonnet pricing
    private static final double INPUT_PRICE_PER_M = 300;  // $3.00 per 1M input tokens
    private static final double OUTPUT_PRICE_PER_M = 1500; // $15.00 per 1M output tokens

    private final WebClient webClient;
    private final String apiKey;
    private final boolean enabled;

    public ClaudeLLMService(
            @Value("${llm.claude.api-key:}") String apiKey,
            @Value("${llm.claude.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();

        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("x-api-key", apiKey)
                .defaultHeader("anthropic-version", API_VERSION)
                .build();

        if (this.enabled) {
            log.info("Claude LLM service initialized with model: {}", DEFAULT_MODEL);
        } else {
            log.info("Claude LLM service is disabled (no API key configured)");
        }
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.CLAUDE;
    }

    @Override
    public boolean isAvailable() {
        return enabled;
    }

    @Override
    public String getDefaultModelId() {
        return DEFAULT_MODEL;
    }

    @Override
    public Mono<LLMResponse> complete(LLMRequest request) {
        if (!isAvailable()) {
            return Mono.just(LLMResponse.failure(LLMProvider.CLAUDE, "Claude API is not configured"));
        }

        long startTime = System.currentTimeMillis();

        Map<String, Object> body = buildRequestBody(request);

        return webClient.post()
                .uri("/v1/messages")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .map(response -> parseResponse(response, startTime))
                .onErrorResume(e -> {
                    log.error("Claude API error: {}", e.getMessage());
                    return Mono.just(LLMResponse.failure(LLMProvider.CLAUDE, e.getMessage()));
                });
    }

    @Override
    public int estimateCostCents(int inputTokens, int outputTokens) {
        double inputCost = (inputTokens / 1_000_000.0) * INPUT_PRICE_PER_M;
        double outputCost = (outputTokens / 1_000_000.0) * OUTPUT_PRICE_PER_M;
        return (int) Math.ceil((inputCost + outputCost));
    }

    private Map<String, Object> buildRequestBody(LLMRequest request) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", DEFAULT_MODEL);
        body.put("max_tokens", request.getMaxTokens() != null ? request.getMaxTokens() : 1024);

        // Add system prompt if provided
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            body.put("system", request.getSystemPrompt());
        }

        // Build messages array
        List<Map<String, String>> messages = new ArrayList<>();

        // Add conversation history if present
        if (request.getConversationHistory() != null) {
            for (LLMRequest.Message msg : request.getConversationHistory()) {
                messages.add(Map.of(
                        "role", msg.getRole(),
                        "content", msg.getContent()
                ));
            }
        }

        // Add the current user message
        messages.add(Map.of(
                "role", "user",
                "content", request.getUserMessage()
        ));

        body.put("messages", messages);

        // Add temperature if provided
        if (request.getTemperature() != null) {
            body.put("temperature", request.getTemperature());
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseResponse(Map<String, Object> response, long startTime) {
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        try {
            // Extract content from response
            List<Map<String, Object>> contentBlocks = (List<Map<String, Object>>) response.get("content");
            String content = "";
            if (contentBlocks != null && !contentBlocks.isEmpty()) {
                content = (String) contentBlocks.get(0).get("text");
            }

            // Extract usage
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            int inputTokens = usage != null ? ((Number) usage.get("input_tokens")).intValue() : 0;
            int outputTokens = usage != null ? ((Number) usage.get("output_tokens")).intValue() : 0;

            String stopReason = (String) response.get("stop_reason");
            String modelId = (String) response.get("model");

            LLMResponse llmResponse = LLMResponse.success(
                    LLMProvider.CLAUDE,
                    modelId != null ? modelId : DEFAULT_MODEL,
                    content,
                    inputTokens,
                    outputTokens,
                    latencyMs
            );
            llmResponse.setStopReason(stopReason);
            llmResponse.setEstimatedCostCents(estimateCostCents(inputTokens, outputTokens));

            return llmResponse;

        } catch (Exception e) {
            log.error("Error parsing Claude response: {}", e.getMessage());
            return LLMResponse.failure(LLMProvider.CLAUDE, "Failed to parse response: " + e.getMessage());
        }
    }
}
