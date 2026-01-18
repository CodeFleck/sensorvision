package io.indcloud.dto.llm;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for natural language query requests and responses.
 */
@Data
@Builder
public class NaturalLanguageQueryDto {

    /**
     * The user's natural language question.
     */
    private String query;

    /**
     * Optional: Limit query to specific device IDs.
     */
    private List<UUID> deviceIds;

    /**
     * Optional: Time range start for the query.
     */
    private Instant fromTime;

    /**
     * Optional: Time range end for the query.
     */
    private Instant toTime;

    /**
     * The AI-generated response to the query.
     */
    private String response;

    /**
     * Data points that were used to generate the response.
     */
    private List<DataPoint> supportingData;

    /**
     * Whether the query was successful.
     */
    private boolean success;

    /**
     * Error message if the query failed.
     */
    private String errorMessage;

    /**
     * The LLM provider used.
     */
    private String provider;

    /**
     * The model ID used.
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
     * When the response was generated.
     */
    private Instant generatedAt;

    /**
     * Represents a data point used in the response.
     */
    @Data
    @Builder
    public static class DataPoint {
        private UUID deviceId;
        private String deviceName;
        private String variableName;
        private Object value;
        private String unit;
        private Instant timestamp;
    }

    /**
     * Request DTO for natural language queries.
     */
    @Data
    @Builder
    public static class Request {
        private String query;
        private List<UUID> deviceIds;
        private Instant fromTime;
        private Instant toTime;
    }
}
