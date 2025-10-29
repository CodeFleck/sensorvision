package org.sensorvision.plugin;

import org.sensorvision.model.DataPlugin;
import org.sensorvision.mqtt.TelemetryPayload;

import java.util.List;

/**
 * Core interface for all data plugin processors.
 * Each plugin type implements this interface to transform raw data into TelemetryPayload format.
 */
public interface DataPluginProcessor {

    /**
     * Process raw data and transform it into one or more telemetry payloads.
     *
     * @param plugin The plugin configuration
     * @param rawData Raw input data (format depends on plugin type)
     * @return List of telemetry payloads extracted from the raw data
     * @throws PluginProcessingException if processing fails
     */
    List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException;

    /**
     * Validate plugin configuration.
     *
     * @param plugin The plugin configuration to validate
     * @return Validation result with errors if any
     */
    PluginValidationResult validateConfiguration(DataPlugin plugin);

    /**
     * Get the plugin type this processor supports.
     *
     * @return The plugin type
     */
    org.sensorvision.model.PluginProvider getSupportedProvider();
}
