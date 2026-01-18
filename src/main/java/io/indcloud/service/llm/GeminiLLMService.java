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
 * Google Gemini LLM service implementation.
 * Uses the Gemini API.
 */
@Service
@Slf4j
public class GeminiLLMService implements LLMService {

    private static final String API_BASE_URL = "https://generativelanguage.googleapis.com";
    private static final String DEFAULT_MODEL = "gemini-1.5-pro";

    // Pricing per 1M tokens (in cents) - Gemini 1.5 Pro pricing
    private static final double INPUT_PRICE_PER_M = 125;  // $1.25 per 1M input tokens
    private static final double OUTPUT_PRICE_PER_M = 500; // $5.00 per 1M output tokens

    private final WebClient webClient;
    private final String apiKey;
    private final boolean enabled;

    public GeminiLLMService(
            @Value("${llm.gemini.api-key:}") String apiKey,
            @Value("${llm.gemini.enabled:false}") boolean enabled) {
        this.apiKey = apiKey;
        this.enabled = enabled && apiKey != null && !apiKey.isBlank();

        this.webClient = WebClient.builder()
                .baseUrl(API_BASE_URL)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();

        if (this.enabled) {
            log.info("Gemini LLM service initialized with model: {}", DEFAULT_MODEL);
        } else {
            log.info("Gemini LLM service is disabled (no API key configured)");
        }
    }

    @Override
    public LLMProvider getProvider() {
        return LLMProvider.GEMINI;
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
            return Mono.just(LLMResponse.failure(LLMProvider.GEMINI, "Gemini API is not configured"));
        }

        long startTime = System.currentTimeMillis();

        Map<String, Object> body = buildRequestBody(request);

        String uri = String.format("/v1beta/models/%s:generateContent?key=%s", DEFAULT_MODEL, apiKey);

        return webClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Map.class)
                .timeout(Duration.ofSeconds(60))
                .map(response -> parseResponse(response, startTime))
                .onErrorResume(e -> {
                    log.error("Gemini API error: {}", e.getMessage());
                    return Mono.just(LLMResponse.failure(LLMProvider.GEMINI, e.getMessage()));
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

        // Build contents array
        List<Map<String, Object>> contents = new ArrayList<>();

        // Add system instruction if provided
        if (request.getSystemPrompt() != null && !request.getSystemPrompt().isBlank()) {
            body.put("systemInstruction", Map.of(
                    "parts", List.of(Map.of("text", request.getSystemPrompt()))
            ));
        }

        // Add conversation history if present
        if (request.getConversationHistory() != null) {
            for (LLMRequest.Message msg : request.getConversationHistory()) {
                String role = msg.getRole().equals("assistant") ? "model" : msg.getRole();
                contents.add(Map.of(
                        "role", role,
                        "parts", List.of(Map.of("text", msg.getContent()))
                ));
            }
        }

        // Add the current user message
        contents.add(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", request.getUserMessage()))
        ));

        body.put("contents", contents);

        // Add generation config
        Map<String, Object> generationConfig = new HashMap<>();
        if (request.getMaxTokens() != null) {
            generationConfig.put("maxOutputTokens", request.getMaxTokens());
        }
        if (request.getTemperature() != null) {
            generationConfig.put("temperature", request.getTemperature());
        }
        if (!generationConfig.isEmpty()) {
            body.put("generationConfig", generationConfig);
        }

        return body;
    }

    @SuppressWarnings("unchecked")
    private LLMResponse parseResponse(Map<String, Object> response, long startTime) {
        int latencyMs = (int) (System.currentTimeMillis() - startTime);

        try {
            // Extract content from response
            List<Map<String, Object>> candidates = (List<Map<String, Object>>) response.get("candidates");
            String content = "";
            String stopReason = null;
            if (candidates != null && !candidates.isEmpty()) {
                Map<String, Object> candidate = candidates.get(0);
                Map<String, Object> contentMap = (Map<String, Object>) candidate.get("content");
                List<Map<String, Object>> parts = (List<Map<String, Object>>) contentMap.get("parts");
                if (parts != null && !parts.isEmpty()) {
                    content = (String) parts.get(0).get("text");
                }
                stopReason = (String) candidate.get("finishReason");
            }

            // Extract usage metadata
            Map<String, Object> usageMetadata = (Map<String, Object>) response.get("usageMetadata");
            int inputTokens = usageMetadata != null ?
                    ((Number) usageMetadata.getOrDefault("promptTokenCount", 0)).intValue() : 0;
            int outputTokens = usageMetadata != null ?
                    ((Number) usageMetadata.getOrDefault("candidatesTokenCount", 0)).intValue() : 0;

            LLMResponse llmResponse = LLMResponse.success(
                    LLMProvider.GEMINI,
                    DEFAULT_MODEL,
                    content,
                    inputTokens,
                    outputTokens,
                    latencyMs
            );
            llmResponse.setStopReason(stopReason);
            llmResponse.setEstimatedCostCents(estimateCostCents(inputTokens, outputTokens));

            return llmResponse;

        } catch (Exception e) {
            log.error("Error parsing Gemini response: {}", e.getMessage());
            return LLMResponse.failure(LLMProvider.GEMINI, "Failed to parse response: " + e.getMessage());
        }
    }
}
