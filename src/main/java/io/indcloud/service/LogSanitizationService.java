package io.indcloud.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Service for sanitizing log entries by redacting sensitive information
 * such as passwords, tokens, API keys, and connection strings.
 */
@Slf4j
@Service
public class LogSanitizationService {

    private static final String REDACTED = "[REDACTED]";

    // Pre-compiled regex patterns for sensitive data
    private static final List<SanitizationPattern> PATTERNS = List.of(
            // JWT tokens (three Base64 segments separated by dots)
            new SanitizationPattern(
                    Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+"),
                    "[JWT_REDACTED]"
            ),
            // Bearer tokens in headers
            new SanitizationPattern(
                    Pattern.compile("(Bearer\\s+)[A-Za-z0-9_.-]{20,}", Pattern.CASE_INSENSITIVE),
                    "$1[TOKEN_REDACTED]"
            ),
            // Password parameters in various formats
            new SanitizationPattern(
                    Pattern.compile("(password|passwd|pwd|secret)([=:]\\s*)[^\\s,;\"'\\]\\}]+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // API keys and tokens
            new SanitizationPattern(
                    Pattern.compile("(api[_-]?key|apikey|api_token|access[_-]?token|auth[_-]?token)([=:]\\s*)[^\\s,;\"'\\]\\}]+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // Device tokens (UUID format after specific keys)
            new SanitizationPattern(
                    Pattern.compile("(device[_-]?token|X-Device-Token|X-API-Key)([=:\\s]+)[a-fA-F0-9-]{32,}", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // Database connection strings with password
            new SanitizationPattern(
                    Pattern.compile("(jdbc:[^\\s]*password=)[^&\\s]+", Pattern.CASE_INSENSITIVE),
                    "$1" + REDACTED
            ),
            // Spring datasource password
            new SanitizationPattern(
                    Pattern.compile("(spring\\.datasource\\.password\\s*=\\s*)[^\\s]+", Pattern.CASE_INSENSITIVE),
                    "$1" + REDACTED
            ),
            // MQTT password
            new SanitizationPattern(
                    Pattern.compile("(MQTT_PASSWORD|mqtt\\.password)([=:]\\s*)[^\\s,;\"']+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // OAuth client secrets
            new SanitizationPattern(
                    Pattern.compile("(client[_-]?secret|GOOGLE_CLIENT_SECRET|GITHUB_CLIENT_SECRET)([=:]\\s*)[^\\s,;\"']+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // Twilio credentials
            new SanitizationPattern(
                    Pattern.compile("(TWILIO_AUTH_TOKEN|TWILIO_ACCOUNT_SID)([=:]\\s*)[^\\s,;\"']+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // SMTP password
            new SanitizationPattern(
                    Pattern.compile("(SMTP_PASSWORD|mail\\.password)([=:]\\s*)[^\\s,;\"']+", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // BCrypt password hashes
            new SanitizationPattern(
                    Pattern.compile("\\$2[aby]?\\$[0-9]{2}\\$[./A-Za-z0-9]{53}"),
                    "[BCRYPT_HASH_REDACTED]"
            ),
            // Generic encryption/private keys
            new SanitizationPattern(
                    Pattern.compile("(encryption[_-]?key|private[_-]?key|signing[_-]?key)([=:]\\s*)[^\\s,;\"']{8,}", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // Authorization headers with credentials
            new SanitizationPattern(
                    Pattern.compile("(Authorization[=:\\s]+Basic\\s+)[A-Za-z0-9+/=]+", Pattern.CASE_INSENSITIVE),
                    "$1" + REDACTED
            ),
            // AWS credentials
            new SanitizationPattern(
                    Pattern.compile("(AWS_SECRET_ACCESS_KEY|aws_secret_access_key|AWS_ACCESS_KEY_ID|aws_access_key_id)([=:]\\s*)[A-Za-z0-9/+=]{16,}", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            ),
            // Private keys (PEM format markers)
            new SanitizationPattern(
                    Pattern.compile("-----BEGIN (RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----[\\s\\S]*?-----END (RSA |EC |DSA |OPENSSH |)PRIVATE KEY-----"),
                    "[PRIVATE_KEY_REDACTED]"
            ),
            // Generic secret/credential environment variables
            new SanitizationPattern(
                    Pattern.compile("(_SECRET|_TOKEN|_KEY|_CREDENTIAL|_PASSWORD)([=:]\\s*)[^\\s,;\"']{8,}", Pattern.CASE_INSENSITIVE),
                    "$1$2" + REDACTED
            )
    );

    /**
     * Sanitize a log line by redacting sensitive information.
     *
     * @param logLine the original log line
     * @return the sanitized log line with sensitive data replaced
     */
    public String sanitize(String logLine) {
        if (logLine == null || logLine.isEmpty()) {
            return logLine;
        }

        String result = logLine;
        for (SanitizationPattern sp : PATTERNS) {
            result = sp.pattern.matcher(result).replaceAll(sp.replacement);
        }
        return result;
    }

    /**
     * Check if a log line contains potentially sensitive information.
     * Useful for quick filtering before applying full sanitization.
     *
     * @param logLine the log line to check
     * @return true if the line may contain sensitive data
     */
    public boolean containsSensitiveData(String logLine) {
        if (logLine == null || logLine.isEmpty()) {
            return false;
        }

        String lower = logLine.toLowerCase();
        return lower.contains("password") ||
                lower.contains("secret") ||
                lower.contains("token") ||
                lower.contains("api_key") ||
                lower.contains("apikey") ||
                lower.contains("bearer") ||
                lower.contains("authorization") ||
                lower.contains("jdbc:") ||
                logLine.contains("eyJ"); // JWT prefix
    }

    /**
     * Internal record for holding compiled patterns and their replacements.
     */
    private record SanitizationPattern(Pattern pattern, String replacement) {
        SanitizationPattern(Pattern pattern, String replacement) {
            this.pattern = pattern;
            this.replacement = replacement;
        }
    }
}
