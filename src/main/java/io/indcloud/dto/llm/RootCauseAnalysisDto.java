package io.indcloud.dto.llm;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for root cause analysis requests and responses.
 */
@Data
@Builder
public class RootCauseAnalysisDto {

    /**
     * Unique ID for this analysis.
     */
    private UUID analysisId;

    /**
     * The alert or anomaly being analyzed.
     */
    private UUID sourceId;

    /**
     * Type of source being analyzed.
     */
    private SourceType sourceType;

    /**
     * Device associated with the issue.
     */
    private UUID deviceId;
    private String deviceName;

    /**
     * Brief summary of the issue.
     */
    private String issueSummary;

    /**
     * The identified root cause(s) ranked by likelihood.
     */
    private List<RootCause> rootCauses;

    /**
     * Contributing factors that may have led to the issue.
     */
    private List<String> contributingFactors;

    /**
     * Timeline of events leading to the issue.
     */
    private List<TimelineEvent> timeline;

    /**
     * Recommended corrective actions.
     */
    private List<CorrectiveAction> correctiveActions;

    /**
     * Recommended preventive measures.
     */
    private List<String> preventiveMeasures;

    /**
     * The full analysis content in markdown.
     */
    private String fullAnalysis;

    /**
     * Confidence level of the analysis (0-100).
     */
    private Integer confidenceLevel;

    /**
     * Whether the analysis was successful.
     */
    private boolean success;

    /**
     * Error message if analysis failed.
     */
    private String errorMessage;

    /**
     * LLM provider used.
     */
    private String provider;

    /**
     * Model ID used.
     */
    private String modelId;

    /**
     * Total tokens used.
     */
    private Integer tokensUsed;

    /**
     * Response latency in milliseconds.
     */
    private Integer latencyMs;

    /**
     * When the analysis was generated.
     */
    private Instant generatedAt;

    /**
     * Source types for root cause analysis.
     */
    public enum SourceType {
        ALERT,
        ANOMALY,
        DEVICE_FAILURE,
        PERFORMANCE_DEGRADATION
    }

    /**
     * Represents a potential root cause.
     */
    @Data
    @Builder
    public static class RootCause {
        private String description;
        private int likelihoodPercent;
        private String category;
        private String evidence;
    }

    /**
     * Represents an event in the timeline.
     */
    @Data
    @Builder
    public static class TimelineEvent {
        private Instant timestamp;
        private String event;
        private String significance;
    }

    /**
     * Represents a corrective action.
     */
    @Data
    @Builder
    public static class CorrectiveAction {
        private int priority;
        private String action;
        private String expectedOutcome;
        private String urgency; // IMMEDIATE, SHORT_TERM, LONG_TERM
    }

    /**
     * Request DTO for root cause analysis.
     */
    @Data
    @Builder
    public static class Request {
        private UUID sourceId;
        private SourceType sourceType;
        /**
         * Optional: Additional context from the user.
         */
        private String additionalContext;
        /**
         * How far back to look for related events (hours).
         */
        private Integer lookbackHours;
    }
}
