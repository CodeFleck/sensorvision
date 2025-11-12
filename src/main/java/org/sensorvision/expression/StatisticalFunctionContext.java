package org.sensorvision.expression;

import lombok.Builder;
import lombok.Getter;
import org.sensorvision.repository.TelemetryRecordRepository;

import java.time.Instant;

/**
 * Context object passed to statistical functions to enable time-series queries.
 * Contains necessary information to query historical telemetry data.
 */
@Getter
@Builder
public class StatisticalFunctionContext {

    /**
     * Device external ID for querying device-specific telemetry.
     */
    private final String deviceExternalId;

    /**
     * Current timestamp (reference point for time windows).
     */
    private final Instant currentTimestamp;

    /**
     * Repository for querying historical telemetry data.
     */
    private final TelemetryRecordRepository telemetryRepository;

    /**
     * Check if context is available (not null).
     * Statistical functions should fail gracefully if context is not available.
     */
    public boolean isAvailable() {
        return deviceExternalId != null && currentTimestamp != null && telemetryRepository != null;
    }
}
