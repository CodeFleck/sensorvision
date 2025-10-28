package org.sensorvision.dto;

import java.time.Instant;

/**
 * Lightweight projection for unread ticket counting
 * Only includes timestamps needed for comparison, avoiding loading screenshot blobs
 */
public interface IssueTimestampProjection {
    Long getId();
    Instant getUpdatedAt();
    Instant getLastViewedAt();
    Instant getCreatedAt();
}
