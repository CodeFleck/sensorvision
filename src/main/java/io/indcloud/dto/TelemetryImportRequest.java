package io.indcloud.dto;

import java.time.Instant;
import java.util.Map;

public class TelemetryImportRequest {
    private String deviceId;
    private Instant timestamp;
    private Map<String, Double> variables;

    public TelemetryImportRequest() {
    }

    public TelemetryImportRequest(String deviceId, Instant timestamp, Map<String, Double> variables) {
        this.deviceId = deviceId;
        this.timestamp = timestamp;
        this.variables = variables;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public Map<String, Double> getVariables() {
        return variables;
    }

    public void setVariables(Map<String, Double> variables) {
        this.variables = variables;
    }
}
