package io.indcloud.sdk.exception;

/**
 * Thrown when rate limit is exceeded.
 */
public class RateLimitException extends IndCloudException {
    public RateLimitException(String message) {
        super(message);
    }
}
