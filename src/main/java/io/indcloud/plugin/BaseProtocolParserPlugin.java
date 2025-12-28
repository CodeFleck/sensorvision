package io.indcloud.plugin;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.DataPlugin;

/**
 * Base class for protocol parser plugins.
 * Protocol parsers transform binary or custom format data into JSON.
 */
public abstract class BaseProtocolParserPlugin implements DataPluginProcessor {

    /**
     * Parse raw bytes into a JSON structure
     *
     * @param rawData Raw bytes to parse
     * @param config Plugin configuration
     * @return Parsed JSON data
     * @throws PluginProcessingException if parsing fails
     */
    protected abstract JsonNode parseToJson(byte[] rawData, JsonNode config) throws PluginProcessingException;

    /**
     * Get required configuration fields for this parser
     *
     * @return Array of required field names
     */
    protected abstract String[] getRequiredConfigFields();

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        JsonNode config = plugin.getConfiguration();

        for (String field : getRequiredConfigFields()) {
            if (!config.has(field) || config.get(field).isNull()) {
                return PluginValidationResult.invalid("Missing required configuration field: " + field);
            }
        }

        return PluginValidationResult.valid();
    }
}
