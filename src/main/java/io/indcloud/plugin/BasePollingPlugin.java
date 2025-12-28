package io.indcloud.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.DataPlugin;
import io.indcloud.mqtt.TelemetryPayload;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Base class for polling plugins that actively fetch data from external sources.
 * Unlike webhook plugins that receive data passively, polling plugins:
 * - Connect to external devices/services
 * - Poll data on a schedule
 * - Transform data into SensorVision format
 *
 * Examples: Modbus TCP, SNMP, REST API polling, Database queries
 */
public abstract class BasePollingPlugin implements DataPluginProcessor {

    /**
     * Poll data from the external source.
     *
     * @param config Plugin configuration (connection details, polling settings)
     * @return Polling result containing device ID, timestamp, and variables
     * @throws PluginProcessingException if polling fails
     */
    public abstract PollingResult poll(JsonNode config) throws PluginProcessingException;

    /**
     * Get required configuration fields for this polling plugin
     *
     * @return Array of required field names
     */
    protected abstract String[] getRequiredConfigFields();

    /**
     * Get the default polling interval in seconds
     *
     * @return Default interval (e.g., 60 for 1 minute)
     */
    protected abstract int getDefaultPollingIntervalSeconds();

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        JsonNode config = plugin.getConfiguration();

        // Validate required fields
        for (String field : getRequiredConfigFields()) {
            if (!config.has(field) || config.get(field).isNull()) {
                return PluginValidationResult.invalid("Missing required configuration field: " + field);
            }
        }

        // Validate polling interval
        if (config.has("pollingIntervalSeconds")) {
            int interval = config.get("pollingIntervalSeconds").asInt();
            if (interval < 1) {
                return PluginValidationResult.invalid("Polling interval must be at least 1 second");
            }
            if (interval > 86400) {
                return PluginValidationResult.invalid("Polling interval cannot exceed 24 hours (86400 seconds)");
            }
        }

        return PluginValidationResult.valid();
    }

    @Override
    public List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException {
        throw new UnsupportedOperationException(
            "Polling plugins do not process webhooks. Use poll() method instead.");
    }

    /**
     * Result of a polling operation
     */
    public static class PollingResult {
        private final String deviceId;
        private final Instant timestamp;
        private final Map<String, BigDecimal> variables;

        public PollingResult(String deviceId, Instant timestamp, Map<String, BigDecimal> variables) {
            this.deviceId = deviceId;
            this.timestamp = timestamp;
            this.variables = variables;
        }

        public String getDeviceId() {
            return deviceId;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public Map<String, BigDecimal> getVariables() {
            return variables;
        }
    }
}
