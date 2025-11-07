package org.sensorvision.expression;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

/**
 * Context information for expression evaluation.
 * Required for statistical functions that need device ID and timestamp.
 */
@Getter
@Builder
public class ExpressionContext {

    /**
     * Device external ID for historical queries
     */
    private final String deviceId;

    /**
     * Reference timestamp for time window calculations
     */
    private final Instant timestamp;

    /**
     * Organization ID for multi-tenant isolation (optional)
     */
    private final Long organizationId;
}
