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
 * OpenAI GPT LLM service implementation.
 * Uses the OpenAI Chat Completions API.
 */
@Service
@Slf4j
public class OpenAILLMService implements LLMService {

    private static final String API_BASE_URL = "https://api.openai.com";
    private static final String DEFAULT_MODEL = "gpt-4o";

    // Pricing per 1M tokens (in cents) - GPT-4o pricing
    private static final double INPUT_PRICE_PER_M = 250;  // $2.50 per 1M input tokens
    private static final double OUTPUT_PRICE_PER_M = 1000; // $10.00 per 1M output tokens

    private final WebClient webClient;
    private final String apiKey;
    private final boolean enabled;

    public OpenAILLMService(
            @Value("${llm.openai.api-key:}") String apiKey,
            @Value("${llm.openai.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();

        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .build();

        if (this.enabled) {
            log.info("OpenAI LLM service initialized with model: {}", DEFAULT_MODEL);
        } else {
            log.info("OpenAI LLM service is disabled (no API key configured)");
        }
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.OPENAI;
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
            return Mono.just(LLMResponse.failure(LLMProvider.OPENAI, "OpenAI API is not configured"));
        }

        long startTime = System.currentTimeMillis();

        Map<String, Object> body = buildRequestBody(request);

        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .map(response -> parseResponse(response, startTime))
                .onErrorResume(e -> {
                    log.error("OpenAI API error: {}", e.getMessage());
                    return Mono.just(LLMResponse.failure(LLMProvider.OPENAI, e.getMessage()));
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

        // Build messages array
        List<Map<String, String>> messages = new ArrayList<>();

        // Add system prompt if provided
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            messages.add(Map.of(
                    "role", "system",
                    "content", request.getSystemPrompt()
            ));
        }

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
            List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
            String content = "";
            String stopReason = null;
            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> choice = choices.get(0);
                Map<String, String> message = (Map<String, String>) choice.get("message");
                content = message.get("content");
                stopReason = (String) choice.get("finish_reason");
            }

            // Extract usage
            Map<String, Object> usage = (Map<String, Object>) response.get("usage");
            int inputTokens = usage != null ? ((Number) usage.get("prompt_tokens")).intValue() : 0;
            int outputTokens = usage != null ? ((Number) usage.get("completion_tokens")).intValue() : 0;

            String modelId = (String) response.get("model");

            LLMResponse llmResponse = LLMResponse.success(
                    LLMProvider.OPENAI,
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
            log.error("Error parsing OpenAI response: {}", e.getMessage());
            return LLMResponse.failure(LLMProvider.OPENAI, "Failed to parse response: " + e.getMessage());
        }
    }
}
