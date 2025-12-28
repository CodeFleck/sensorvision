package io.indcloud.dto;

import io.indcloud.model.IssueStatus;

import java.time.Instant;

/**
 * Lightweight projection for unread ticket counting
 * Only includes timestamps and status needed for comparison, avoiding loading screenshot blobs
 */
public interface IssueTimestampProjection {
    Long getId();
    Instant getUpdatedAt();
    Instant getLastViewedAt();
    Instant getCreatedAt();
    Instant getLastPublicReplyAt();
    IssueStatus getStatus();
}
