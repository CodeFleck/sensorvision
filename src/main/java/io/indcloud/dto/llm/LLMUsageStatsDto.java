package io.indcloud.dto.llm;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * DTO for LLM usage statistics and billing information.
 */
@Data
@Builder
public class LLMUsageStatsDto {

    /**
     * Period covered by these stats (in days).
     */
    private int periodDays;

    /**
     * Total tokens used in the period.
     */
    private Long totalTokens;

    /**
     * Total estimated cost in cents.
     */
    private Long totalCostCents;

    /**
     * Total estimated cost in dollars.
     */
    private Double totalCostDollars;

    /**
     * Number of successful API requests.
     */
    private Long successfulRequests;

    /**
     * Number of failed API requests.
     */
    private Long failedRequests;

    /**
     * Total number of requests.
     */
    private Long totalRequests;

    /**
     * Success rate as a percentage (0-100).
     */
    private Double successRate;

    /**
     * Average latency in milliseconds.
     */
    private Integer averageLatencyMs;

    /**
     * Usage breakdown by provider.
     */
    private Map<String, ProviderStats> byProvider;

    /**
     * Usage breakdown by feature type.
     */
    private Map<String, FeatureStats> byFeature;

    /**
     * Daily usage data for charts.
     */
    private List<DailyUsage> dailyUsage;

    @Data
    @Builder
    public static class ProviderStats {
        private String provider;
        private Long totalTokens;
        private Long totalCostCents;
        private Long requestCount;
    }

    @Data
    @Builder
    public static class FeatureStats {
        private String featureType;
        private Long totalTokens;
        private Long totalCostCents;
        private Long requestCount;
    }

    @Data
    @Builder
    public static class DailyUsage {
        private String date;
        private Long totalTokens;
        private Long totalCostCents;
        private Long requestCount;
    }
}
