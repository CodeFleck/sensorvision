package io.indcloud.sdk.exception;

/**
 * Thrown when server returns 5xx error.
 */
public class ServerException extends IndCloudException {
    public ServerException(String message) {
        super(message);
    }
}
