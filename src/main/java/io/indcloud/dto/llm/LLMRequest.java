package io.indcloud.dto.llm;

import io.indcloud.model.LLMFeatureType;
import io.indcloud.model.LLMProvider;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for LLM API calls.
 */
@Data
@Builder
public class LLMRequest {

    /**
     * The LLM provider to use. If null, uses organization's default.
     */
    private LLMProvider provider;

    /**
     * The feature type being invoked.
     */
    private LLMFeatureType featureType;

    /**
     * System prompt to set the AI's behavior.
     */
    private String systemPrompt;

    /**
     * User message/query.
     */
    private String userMessage;

    /**
     * Conversation history for multi-turn conversations.
     */
    private List<Message> conversationHistory;

    /**
     * Additional context data to include in the prompt.
     */
    private Map<String, Object> context;

    /**
     * Maximum tokens for the response.
     */
    @Builder.Default
    private Integer maxTokens = 1024;

    /**
     * Temperature for response randomness (0.0 - 1.0).
     */
    @Builder.Default
    private Double temperature = 0.7;

    /**
     * Optional reference to the entity this request is about.
     */
    private String referenceType;
    private UUID referenceId;

    @Data
    @Builder
    public static class Message {
        private String role; // "user", "assistant", "system"
        private String content;
    }
}
