package io.indcloud.model;

/**
 * Supported LLM providers for AI-powered features.
 */
public enum LLMProvider {
    CLAUDE("claude", "Anthropic Claude"),
    OPENAI("openai", "OpenAI GPT"),
    GEMINI("gemini", "Google Gemini");

    private final String code;
    private final String displayName;

    LLMProvider(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static LLMProvider fromCode(String code) {
        for (LLMProvider provider : values()) {
            if (provider.code.equalsIgnoreCase(code)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("Unknown LLM provider: " + code);
    }
}
