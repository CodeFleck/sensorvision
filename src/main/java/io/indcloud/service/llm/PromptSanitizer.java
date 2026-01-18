package io.indcloud.service.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Utility for sanitizing user inputs before sending to LLM to prevent prompt injection attacks.
 * Detects and removes potentially malicious patterns while preserving legitimate queries.
 */
@Component
@Slf4j
public class PromptSanitizer {

    /**
     * Patterns that may indicate prompt injection attempts.
     * These are detected but not necessarily blocked - we log them and sanitize.
     */
    private static final List<Pattern> SUSPICIOUS_PATTERNS = List.of(
            // Instruction override attempts
            Pattern.compile("(?i)\\bignore\\s+(all\\s+|the\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)\\b"),
            Pattern.compile("(?i)\\bdisregard\\s+(all\\s+|the\\s+)?(previous|above|prior)\\s+(instructions?|prompts?|rules?)\\b"),
            Pattern.compile("(?i)\\bforget\\s+(everything|all)\\s+(above|before|previously)\\b"),

            // Role/persona hijacking
            Pattern.compile("(?i)\\byou\\s+are\\s+(now|actually|really)\\s+a?\\b"),
            Pattern.compile("(?i)\\bact\\s+(as|like)\\s+(if\\s+you\\s+were|a|an)\\b"),
            Pattern.compile("(?i)\\bpretend\\s+(to\\s+be|you\\s+are)\\b"),
            Pattern.compile("(?i)\\byour\\s+new\\s+(role|persona|identity)\\b"),

            // System prompt extraction attempts
            Pattern.compile("(?i)\\brepeat\\s+(your|the)\\s+(system\\s+)?(prompt|instructions?)\\b"),
            Pattern.compile("(?i)\\bshow\\s+(me\\s+)?(your|the)\\s+(system\\s+)?(prompt|instructions?)\\b"),
            Pattern.compile("(?i)\\bwhat\\s+(are|were)\\s+your\\s+(original\\s+)?(instructions?|prompt)\\b"),
            Pattern.compile("(?i)\\bprint\\s+(your|the)\\s+(system\\s+)?(prompt|instructions?)\\b"),

            // Jailbreak/DAN patterns
            Pattern.compile("(?i)\\b(DAN|do\\s+anything\\s+now)\\b"),
            Pattern.compile("(?i)\\bjailbreak\\b"),
            Pattern.compile("(?i)\\bdeveloper\\s+mode\\b"),
            Pattern.compile("(?i)\\badmin\\s+mode\\b"),

            // Delimiter injection
            Pattern.compile("\\[\\[\\["),
            Pattern.compile("\\]\\]\\]"),
            Pattern.compile("<<<"),
            Pattern.compile(">>>"),
            Pattern.compile("\\{\\{\\{"),
            Pattern.compile("\\}\\}\\}")
    );

    /**
     * Characters/sequences to escape to prevent delimiter confusion.
     */
    private static final String[][] ESCAPE_SEQUENCES = {
            {"```", "` ` `"},
            {"###", "# # #"},
            {"---", "- - -"},
            {"***", "* * *"},
            {"===", "= = ="}
    };

    /**
     * Maximum allowed length for any single input field to prevent token exhaustion.
     */
    private static final int MAX_INPUT_LENGTH = 10000;

    /**
     * Sanitize user input for use in LLM prompts.
     * Returns a sanitized version that's safe to include in prompts.
     *
     * @param input The raw user input
     * @param fieldName Name of the field for logging purposes
     * @return Sanitized input
     */
    public String sanitize(String input, String fieldName) {
        if (input == null) {
            return null;
        }

        // Trim and check length
        String sanitized = input.trim();
        if (sanitized.isEmpty()) {
            return sanitized;
        }

        // Check for suspicious patterns first (for logging/monitoring)
        boolean suspicious = detectSuspiciousPatterns(sanitized, fieldName);
        if (suspicious) {
            log.warn("Suspicious input detected in field '{}' - applying sanitization", fieldName);
        }

        // Truncate if too long
        if (sanitized.length() > MAX_INPUT_LENGTH) {
            log.warn("Input truncated from {} to {} characters for field '{}'",
                    sanitized.length(), MAX_INPUT_LENGTH, fieldName);
            sanitized = sanitized.substring(0, MAX_INPUT_LENGTH) + "...";
        }

        // Escape delimiter sequences that could confuse the LLM
        for (String[] escape : ESCAPE_SEQUENCES) {
            sanitized = sanitized.replace(escape[0], escape[1]);
        }

        // Remove null bytes and other control characters (except newlines and tabs)
        sanitized = sanitized.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]", "");

        // Normalize whitespace (collapse multiple spaces but keep newlines)
        sanitized = sanitized.replaceAll("[ \\t]+", " ");
        sanitized = sanitized.replaceAll("\\n{3,}", "\n\n");

        return sanitized;
    }

    /**
     * Sanitize a query that will be sent directly to the LLM.
     * More aggressive sanitization for direct user queries.
     *
     * @param query The user's natural language query
     * @return Sanitized query
     */
    public String sanitizeQuery(String query) {
        if (query == null) {
            return null;
        }

        String sanitized = sanitize(query, "query");

        // For queries, also wrap user input to make boundaries clear
        // This helps the LLM distinguish between user content and instructions
        return sanitized;
    }

    /**
     * Sanitize a custom prompt provided by the user.
     * Most aggressive sanitization since this could attempt to override system behavior.
     *
     * @param customPrompt The user's custom prompt
     * @return Sanitized custom prompt
     */
    public String sanitizeCustomPrompt(String customPrompt) {
        if (customPrompt == null) {
            return null;
        }

        String sanitized = sanitize(customPrompt, "customPrompt");

        // For custom prompts, remove patterns that look like they're trying to change roles
        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            sanitized = pattern.matcher(sanitized).replaceAll("[redacted]");
        }

        return sanitized;
    }

    /**
     * Detect if the input contains suspicious patterns that may indicate prompt injection.
     * This is for monitoring/alerting purposes.
     *
     * @param input The input to check
     * @param fieldName Name of the field for logging
     * @return true if suspicious patterns were detected
     */
    public boolean detectSuspiciousPatterns(String input, String fieldName) {
        if (input == null || input.isEmpty()) {
            return false;
        }

        for (Pattern pattern : SUSPICIOUS_PATTERNS) {
            if (pattern.matcher(input).find()) {
                log.warn("Suspicious pattern detected in '{}': pattern={}", fieldName, pattern.pattern());
                return true;
            }
        }
        return false;
    }

    /**
     * Validate that the input is suitable for LLM processing.
     * Throws exception if validation fails.
     *
     * @param input The input to validate
     * @param fieldName Name of the field
     * @param maxLength Maximum allowed length
     * @throws IllegalArgumentException if validation fails
     */
    public void validate(String input, String fieldName, int maxLength) {
        if (input == null) {
            return;
        }

        if (input.length() > maxLength) {
            throw new IllegalArgumentException(
                    String.format("%s exceeds maximum length of %d characters", fieldName, maxLength));
        }

        // Check for binary content
        if (containsBinaryContent(input)) {
            throw new IllegalArgumentException(fieldName + " contains invalid characters");
        }
    }

    /**
     * Check if the input appears to contain binary/non-text content.
     */
    private boolean containsBinaryContent(String input) {
        for (char c : input.toCharArray()) {
            if (c < 32 && c != '\n' && c != '\r' && c != '\t') {
                return true;
            }
        }
        return false;
    }
}
