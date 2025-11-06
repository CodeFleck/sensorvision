package io.sensorvision.sdk.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from telemetry data ingestion.
 */
public class IngestionResponse {
    @JsonProperty("message")
    private String message;

    @JsonProperty("deviceId")
    private String deviceId;

    @JsonProperty("timestamp")
    private String timestamp;

    public IngestionResponse() {
    }

    public IngestionResponse(String message, String deviceId, String timestamp) {
        this.message = message;
        this.deviceId = deviceId;
        this.timestamp = timestamp;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "IngestionResponse{" +
                "message='" + message + '\'' +
                ", deviceId='" + deviceId + '\'' +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
