package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.plugin.BaseWebhookPlugin;
import io.indcloud.plugin.PluginProcessingException;
import io.indcloud.plugin.PluginValidationResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Sigfox Webhook Plugin.
 * Handles webhook callbacks from Sigfox backend.
 *
 * Sigfox sends callbacks in this format:
 * {
 *   "device": "AABBCC",
 *   "time": 1234567890,
 *   "data": "0102030405060708",
 *   "seqNumber": 123,
 *   "station": "1234",
 *   "avgSnr": "15.50",
 *   "lat": "48.8566",
 *   "lng": "2.3522",
 *   "rssi": "-120.00",
 *   "snr": "15.50",
 *   "duplicate": false
 * }
 *
 * Configuration format:
 * {
 *   "deviceIdPrefix": "",        // Optional prefix for device IDs
 *   "deviceIdSuffix": "",        // Optional suffix for device IDs
 *   "includeMetadata": true,     // Include Sigfox metadata (RSSI, SNR, location)
 *   "dataParser": "custom",      // "custom" or "json" - how to parse the data field
 *   "customVariables": [         // For custom binary parsing
 *     {
 *       "name": "temperature",
 *       "byteOffset": 0,
 *       "dataType": "INT16",
 *       "scale": 0.1
 *     },
 *     {
 *       "name": "humidity",
 *       "byteOffset": 2,
 *       "dataType": "UINT8",
 *       "scale": 1.0
 *     }
 *   ]
 * }
 *
 * Data field parsing modes:
 * - "custom": Parse binary data using customVariables configuration
 * - "json": Expect JSON-encoded string in the data field (less common)
 */
@Slf4j
@Component
public class SigfoxPlugin extends BaseWebhookPlugin {

    @Override
    protected String extractDeviceId(JsonNode payload, JsonNode config) throws PluginProcessingException {
        // Sigfox device ID is in "device" field
        JsonNode deviceNode = payload.path("device");

        if (deviceNode.isMissingNode() || deviceNode.isNull()) {
            throw new PluginProcessingException("Missing Sigfox device ID in 'device' field");
        }

        String sigfoxDeviceId = deviceNode.asText();

        // Apply prefix/suffix if configured
        if (config.has("deviceIdPrefix") && !config.get("deviceIdPrefix").isNull()) {
            sigfoxDeviceId = config.get("deviceIdPrefix").asText() + sigfoxDeviceId;
        }
        if (config.has("deviceIdSuffix") && !config.get("deviceIdSuffix").isNull()) {
            sigfoxDeviceId = sigfoxDeviceId + config.get("deviceIdSuffix").asText();
        }

        return sigfoxDeviceId;
    }

    @Override
    protected Instant extractTimestamp(JsonNode payload, JsonNode config) {
        // Sigfox timestamp is Unix epoch in seconds
        JsonNode timeNode = payload.path("time");

        if (!timeNode.isMissingNode() && !timeNode.isNull()) {
            try {
                long epochSeconds = timeNode.asLong();
                return Instant.ofEpochSecond(epochSeconds);
            } catch (Exception e) {
                log.warn("Failed to parse Sigfox timestamp, using current time: {}", e.getMessage());
            }
        }

        return Instant.now();
    }

    @Override
    protected Map<String, BigDecimal> extractVariables(JsonNode payload, JsonNode config) throws PluginProcessingException {
        Map<String, BigDecimal> variables = new HashMap<>();

        // Get data parsing mode
        String dataParser = config.has("dataParser") ? config.get("dataParser").asText("custom") : "custom";

        JsonNode dataNode = payload.path("data");
        if (dataNode.isMissingNode() || dataNode.isNull()) {
            throw new PluginProcessingException("Missing 'data' field in Sigfox payload");
        }

        String hexData = dataNode.asText();

        if ("json".equalsIgnoreCase(dataParser)) {
            // Parse JSON-encoded data (uncommon for Sigfox)
            variables.putAll(parseJsonData(hexData));
        } else {
            // Parse custom binary data using configuration
            variables.putAll(parseCustomBinaryData(hexData, config));
        }

        if (variables.isEmpty()) {
            throw new PluginProcessingException("No variables extracted from Sigfox data payload");
        }

        return variables;
    }

    @Override
    protected Map<String, Object> extractMetadata(JsonNode payload, JsonNode config) {
        Map<String, Object> metadata = new HashMap<>();

        // Check if metadata should be included
        boolean includeMetadata = config.has("includeMetadata") && config.get("includeMetadata").asBoolean(true);
        if (!includeMetadata) {
            return metadata;
        }

        // Extract Sigfox-specific metadata
        JsonNode seqNumber = payload.path("seqNumber");
        if (!seqNumber.isMissingNode()) {
            metadata.put("sigfox_sequence", seqNumber.asInt());
        }

        JsonNode station = payload.path("station");
        if (!station.isMissingNode()) {
            metadata.put("sigfox_station", station.asText());
        }

        JsonNode avgSnr = payload.path("avgSnr");
        if (!avgSnr.isMissingNode()) {
            metadata.put("sigfox_avg_snr", avgSnr.asDouble());
        }

        JsonNode rssi = payload.path("rssi");
        if (!rssi.isMissingNode()) {
            metadata.put("sigfox_rssi", rssi.asDouble());
        }

        JsonNode snr = payload.path("snr");
        if (!snr.isMissingNode()) {
            metadata.put("sigfox_snr", snr.asDouble());
        }

        // Extract location if available
        JsonNode lat = payload.path("lat");
        JsonNode lng = payload.path("lng");
        if (!lat.isMissingNode() && !lng.isMissingNode()) {
            metadata.put("sigfox_latitude", lat.asDouble());
            metadata.put("sigfox_longitude", lng.asDouble());
        }

        JsonNode duplicate = payload.path("duplicate");
        if (!duplicate.isMissingNode()) {
            metadata.put("sigfox_duplicate", duplicate.asBoolean());
        }

        return metadata;
    }

    /**
     * Parse JSON-encoded data field
     */
    private Map<String, BigDecimal> parseJsonData(String hexData) throws PluginProcessingException {
        Map<String, BigDecimal> variables = new HashMap<>();

        try {
            // Decode hex to string
            byte[] bytes = hexStringToByteArray(hexData);
            String jsonString = new String(bytes);

            // Parse JSON
            JsonNode jsonNode = objectMapper.readTree(jsonString);

            // Extract numeric fields
            jsonNode.fields().forEachRemaining(entry -> {
                if (entry.getValue().isNumber()) {
                    variables.put(entry.getKey(), entry.getValue().decimalValue());
                }
            });

        } catch (Exception e) {
            throw new PluginProcessingException("Failed to parse JSON data: " + e.getMessage(), e);
        }

        return variables;
    }

    /**
     * Parse custom binary data using configuration
     */
    private Map<String, BigDecimal> parseCustomBinaryData(String hexData, JsonNode config) throws PluginProcessingException {
        Map<String, BigDecimal> variables = new HashMap<>();

        if (!config.has("customVariables") || !config.get("customVariables").isArray()) {
            throw new PluginProcessingException(
                "Custom data parser requires 'customVariables' array in configuration"
            );
        }

        byte[] dataBytes = hexStringToByteArray(hexData);

        JsonNode customVariables = config.get("customVariables");
        for (JsonNode varConfig : customVariables) {
            try {
                String name = varConfig.get("name").asText();
                int byteOffset = varConfig.get("byteOffset").asInt();
                String dataType = varConfig.has("dataType") ? varConfig.get("dataType").asText("UINT8") : "UINT8";
                double scale = varConfig.has("scale") ? varConfig.get("scale").asDouble(1.0) : 1.0;

                double rawValue = extractBinaryValue(dataBytes, byteOffset, dataType);
                BigDecimal scaledValue = BigDecimal.valueOf(rawValue * scale);

                variables.put(name, scaledValue);

            } catch (Exception e) {
                log.error("Failed to extract variable {}: {}",
                    varConfig.has("name") ? varConfig.get("name").asText() : "unknown",
                    e.getMessage());
                // Continue with other variables
            }
        }

        return variables;
    }

    /**
     * Extract a value from binary data based on data type
     */
    private double extractBinaryValue(byte[] data, int offset, String dataType) throws PluginProcessingException {
        if (offset < 0 || offset >= data.length) {
            throw new PluginProcessingException("Byte offset " + offset + " is out of bounds (data length: " + data.length + ")");
        }

        switch (dataType.toUpperCase()) {
            case "UINT8":
                return data[offset] & 0xFF;

            case "INT8":
                return data[offset];

            case "UINT16":
                if (offset + 1 >= data.length) {
                    throw new PluginProcessingException("Not enough bytes for UINT16 at offset " + offset);
                }
                // Big-endian by default
                return ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);

            case "INT16":
                if (offset + 1 >= data.length) {
                    throw new PluginProcessingException("Not enough bytes for INT16 at offset " + offset);
                }
                // Big-endian, sign-extended
                int uint16 = ((data[offset] & 0xFF) << 8) | (data[offset + 1] & 0xFF);
                return (short) uint16;

            case "UINT32":
                if (offset + 3 >= data.length) {
                    throw new PluginProcessingException("Not enough bytes for UINT32 at offset " + offset);
                }
                // Big-endian
                long uint32 = ((long)(data[offset] & 0xFF) << 24) |
                              ((data[offset + 1] & 0xFF) << 16) |
                              ((data[offset + 2] & 0xFF) << 8) |
                              (data[offset + 3] & 0xFF);
                return uint32;

            case "INT32":
                if (offset + 3 >= data.length) {
                    throw new PluginProcessingException("Not enough bytes for INT32 at offset " + offset);
                }
                // Big-endian
                int int32 = ((data[offset] & 0xFF) << 24) |
                           ((data[offset + 1] & 0xFF) << 16) |
                           ((data[offset + 2] & 0xFF) << 8) |
                           (data[offset + 3] & 0xFF);
                return int32;

            case "FLOAT32":
                if (offset + 3 >= data.length) {
                    throw new PluginProcessingException("Not enough bytes for FLOAT32 at offset " + offset);
                }
                // Big-endian
                int bits = ((data[offset] & 0xFF) << 24) |
                          ((data[offset + 1] & 0xFF) << 16) |
                          ((data[offset + 2] & 0xFF) << 8) |
                          (data[offset + 3] & 0xFF);
                return Float.intBitsToFloat(bits);

            default:
                throw new PluginProcessingException("Unsupported data type: " + dataType);
        }
    }

    /**
     * Convert hex string to byte array
     */
    private byte[] hexStringToByteArray(String hexString) {
        // Remove any spaces or non-hex characters
        hexString = hexString.replaceAll("[^0-9A-Fa-f]", "");

        int len = hexString.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexString.charAt(i), 16) << 4)
                                + Character.digit(hexString.charAt(i + 1), 16));
        }

        return data;
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        // Base validation
        PluginValidationResult baseResult = super.validateConfiguration(plugin);
        if (!baseResult.isValid()) {
            return baseResult;
        }

        JsonNode config = plugin.getConfiguration();

        // Validate data parser mode
        if (config.has("dataParser")) {
            String parser = config.get("dataParser").asText();
            if (!parser.equals("custom") && !parser.equals("json")) {
                return PluginValidationResult.invalid("dataParser must be 'custom' or 'json'");
            }

            // If custom parser, validate customVariables
            if (parser.equals("custom")) {
                if (!config.has("customVariables") || !config.get("customVariables").isArray()) {
                    return PluginValidationResult.invalid("Custom parser requires 'customVariables' array");
                }

                JsonNode customVars = config.get("customVariables");
                if (customVars.size() == 0) {
                    return PluginValidationResult.invalid("At least one custom variable must be configured");
                }

                // Validate each variable configuration
                for (int i = 0; i < customVars.size(); i++) {
                    JsonNode varConfig = customVars.get(i);

                    if (!varConfig.has("name")) {
                        return PluginValidationResult.invalid("Variable " + i + " missing 'name' field");
                    }
                    if (!varConfig.has("byteOffset")) {
                        return PluginValidationResult.invalid("Variable " + i + " missing 'byteOffset' field");
                    }

                    int offset = varConfig.get("byteOffset").asInt();
                    if (offset < 0 || offset > 12) { // Sigfox max payload is 12 bytes
                        return PluginValidationResult.invalid("Variable " + i + " byteOffset must be 0-12");
                    }

                    if (varConfig.has("dataType")) {
                        String dataType = varConfig.get("dataType").asText();
                        if (!dataType.matches("UINT8|INT8|UINT16|INT16|UINT32|INT32|FLOAT32")) {
                            return PluginValidationResult.invalid("Variable " + i + " has unsupported dataType: " + dataType);
                        }
                    }
                }
            }
        }

        return PluginValidationResult.valid();
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.SIGFOX;
    }
}
