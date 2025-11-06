package io.sensorvision.sdk.exception;

/**
 * Base exception for all SensorVision SDK errors.
 */
public class SensorVisionException extends Exception {
    public SensorVisionException(String message) {
        super(message);
    }

    public SensorVisionException(String message, Throwable cause) {
        super(message, cause);
    }
}
