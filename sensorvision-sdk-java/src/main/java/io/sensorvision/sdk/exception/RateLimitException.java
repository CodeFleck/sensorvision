package io.sensorvision.sdk.exception;

/**
 * Thrown when rate limit is exceeded.
 */
public class RateLimitException extends SensorVisionException {
    public RateLimitException(String message) {
        super(message);
    }
}
