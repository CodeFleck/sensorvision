package io.indcloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for LLM services.
 * All limits and settings can be overridden in application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "llm")
@Data
public class LLMConfigurationProperties {

    /**
     * Default settings for LLM requests.
     */
    private Defaults defaults = new Defaults();

    /**
     * Query limits for various LLM operations.
     */
    private QueryLimits queryLimits = new QueryLimits();

    /**
     * Batch processing limits.
     */
    private BatchLimits batchLimits = new BatchLimits();

    /**
     * Input validation limits.
     */
    private ValidationLimits validation = new ValidationLimits();

    @Data
    public static class Defaults {
        /**
         * Default max tokens for natural language queries.
         */
        private int queryMaxTokens = 1024;

        /**
         * Default max tokens for anomaly explanations.
         */
        private int explanationMaxTokens = 1024;

        /**
         * Default max tokens for reports.
         */
        private int reportMaxTokens = 2048;

        /**
         * Default max tokens for root cause analysis.
         */
        private int rootCauseMaxTokens = 2048;

        /**
         * Temperature for natural language queries (0.0-1.0).
         */
        private double queryTemperature = 0.5;

        /**
         * Temperature for anomaly explanations (0.0-1.0).
         */
        private double explanationTemperature = 0.3;

        /**
         * Temperature for reports (0.0-1.0).
         */
        private double reportTemperature = 0.4;

        /**
         * Temperature for root cause analysis (0.0-1.0).
         */
        private double rootCauseTemperature = 0.3;

        /**
         * Default lookback hours for root cause analysis.
         */
        private int rootCauseLookbackHours = 24;
    }

    @Data
    public static class QueryLimits {
        /**
         * Maximum devices to include in NL query context.
         */
        private int maxDevicesForQuery = 20;

        /**
         * Maximum devices to include in report context.
         */
        private int maxDevicesForReport = 30;

        /**
         * Maximum devices to show detailed info for in reports.
         */
        private int maxDevicesDetailed = 15;

        /**
         * Maximum variables per device in context.
         */
        private int maxVariablesPerDevice = 10;

        /**
         * Maximum related anomalies to include.
         */
        private int maxRelatedAnomalies = 10;

        /**
         * Maximum anomalies to show in report section.
         */
        private int maxAnomaliesInReport = 5;
    }

    @Data
    public static class BatchLimits {
        /**
         * Maximum anomalies in a single batch explain request.
         */
        private int maxBatchAnomalies = 10;

        /**
         * Maximum data points to return as supporting data.
         */
        private int maxSupportingDataPoints = 50;

        /**
         * Concurrency limit for batch anomaly explanations.
         */
        private int batchConcurrency = 3;
    }

    @Data
    public static class ValidationLimits {
        /**
         * Maximum length of a natural language query.
         */
        private int maxQueryLength = 1000;

        /**
         * Maximum length of custom prompt for reports.
         */
        private int maxCustomPromptLength = 2000;

        /**
         * Maximum length of additional context for root cause analysis.
         */
        private int maxAdditionalContextLength = 2000;
    }
}
