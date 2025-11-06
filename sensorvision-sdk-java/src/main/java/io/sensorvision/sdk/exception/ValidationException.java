package io.sensorvision.sdk.exception;

/**
 * Thrown when input validation fails.
 */
public class ValidationException extends SensorVisionException {
    public ValidationException(String message) {
        super(message);
    }
}
