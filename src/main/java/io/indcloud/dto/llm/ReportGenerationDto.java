package io.indcloud.dto.llm;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * DTO for AI-generated report requests and responses.
 */
@Data
@Builder
public class ReportGenerationDto {

    /**
     * Unique ID for this report.
     */
    private UUID reportId;

    /**
     * The type of report to generate.
     */
    private ReportType reportType;

    /**
     * The report title (generated or provided).
     */
    private String title;

    /**
     * Executive summary of the report.
     */
    private String executiveSummary;

    /**
     * The full report content in markdown format.
     */
    private String content;

    /**
     * Key findings from the analysis.
     */
    private List<String> keyFindings;

    /**
     * Recommendations based on the analysis.
     */
    private List<String> recommendations;

    /**
     * Period covered by this report.
     */
    private Instant periodStart;
    private Instant periodEnd;

    /**
     * Devices included in this report.
     */
    private List<UUID> deviceIds;

    /**
     * Whether the report generation was successful.
     */
    private boolean success;

    /**
     * Error message if generation failed.
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
     * When the report was generated.
     */
    private Instant generatedAt;

    /**
     * Types of reports that can be generated.
     */
    public enum ReportType {
        /**
         * Daily operational summary.
         */
        DAILY_SUMMARY,

        /**
         * Weekly performance review.
         */
        WEEKLY_REVIEW,

        /**
         * Monthly performance and trend analysis.
         */
        MONTHLY_ANALYSIS,

        /**
         * Anomaly summary and analysis.
         */
        ANOMALY_REPORT,

        /**
         * Device health and status report.
         */
        DEVICE_HEALTH,

        /**
         * Energy consumption analysis.
         */
        ENERGY_ANALYSIS,

        /**
         * Custom report based on user prompt.
         */
        CUSTOM
    }

    /**
     * Request DTO for report generation.
     */
    @Data
    @Builder
    public static class Request {
        private ReportType reportType;
        private List<UUID> deviceIds;
        private Instant periodStart;
        private Instant periodEnd;
        /**
         * Custom prompt for CUSTOM report type.
         */
        private String customPrompt;
    }
}
