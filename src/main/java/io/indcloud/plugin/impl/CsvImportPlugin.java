package io.indcloud.plugin.impl;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.PluginProvider;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.plugin.DataPluginProcessor;
import io.indcloud.plugin.PluginProcessingException;
import io.indcloud.plugin.PluginValidationResult;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

/**
 * CSV File Import Plugin.
 * Imports historical telemetry data from CSV files.
 *
 * Expected CSV format (with header row):
 * deviceId,timestamp,temperature,humidity,voltage
 * device-001,2024-01-01T12:00:00Z,25.5,60.0,220.1
 * device-002,2024-01-01T12:01:00Z,26.0,58.5,219.8
 *
 * Configuration:
 * {
 *   "deviceIdColumn": "deviceId",  // column name for device ID
 *   "timestampColumn": "timestamp", // column name for timestamp
 *   "timestampFormat": "ISO",  // ISO, EPOCH_SECONDS, EPOCH_MILLIS, or custom pattern
 *   "variableColumns": ["temperature", "humidity", "voltage"], // columns to import as variables
 *   "skipHeader": true  // whether to skip first row
 * }
 */
@Component
public class CsvImportPlugin implements DataPluginProcessor {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ISO_DATE_TIME;

    @Override
    public List<TelemetryPayload> process(DataPlugin plugin, Object rawData) throws PluginProcessingException {
        if (!(rawData instanceof String)) {
            throw new PluginProcessingException("Expected CSV string data");
        }

        String csvContent = (String) rawData;
        JsonNode config = plugin.getConfiguration();

        List<TelemetryPayload> payloads = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            boolean isFirstLine = true;
            String[] headers = null;

            boolean skipHeader = config.path("skipHeader").asBoolean(true);

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                String[] values = parseCsvLine(line);

                if (isFirstLine) {
                    isFirstLine = false;
                    headers = values;
                    if (skipHeader) {
                        continue; // Skip header row
                    }
                }

                if (headers == null) {
                    throw new PluginProcessingException("CSV file must have a header row");
                }

                // Parse the row into a TelemetryPayload
                TelemetryPayload payload = parseRow(headers, values, config);
                if (payload != null) {
                    payloads.add(payload);
                }
            }

        } catch (Exception e) {
            throw new PluginProcessingException("Failed to parse CSV data", e);
        }

        return payloads;
    }

    private TelemetryPayload parseRow(String[] headers, String[] values, JsonNode config) throws PluginProcessingException {
        if (headers.length != values.length) {
            throw new PluginProcessingException(
                    String.format("Row has %d values but header has %d columns", values.length, headers.length)
            );
        }

        // Create a map of column name to value
        Map<String, String> row = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            row.put(headers[i].trim(), values[i].trim());
        }

        // Extract device ID
        String deviceIdColumn = config.path("deviceIdColumn").asText("deviceId");
        String deviceId = row.get(deviceIdColumn);
        if (deviceId == null || deviceId.isEmpty()) {
            throw new PluginProcessingException("Missing device ID in row (column: " + deviceIdColumn + ")");
        }

        // Extract timestamp
        String timestampColumn = config.path("timestampColumn").asText("timestamp");
        String timestampStr = row.get(timestampColumn);
        Instant timestamp = parseTimestamp(timestampStr, config);

        // Extract variables
        Map<String, BigDecimal> variables = new HashMap<>();
        JsonNode variableColumnsNode = config.path("variableColumns");

        if (variableColumnsNode.isArray()) {
            variableColumnsNode.forEach(columnNode -> {
                String columnName = columnNode.asText();
                String valueStr = row.get(columnName);
                if (valueStr != null && !valueStr.isEmpty()) {
                    try {
                        BigDecimal value = new BigDecimal(valueStr);
                        variables.put(columnName, value);
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            });
        } else {
            // If no variable columns specified, import all numeric columns except deviceId and timestamp
            for (Map.Entry<String, String> entry : row.entrySet()) {
                String columnName = entry.getKey();
                if (!columnName.equals(deviceIdColumn) && !columnName.equals(timestampColumn)) {
                    try {
                        BigDecimal value = new BigDecimal(entry.getValue());
                        variables.put(columnName, value);
                    } catch (NumberFormatException e) {
                        // Skip non-numeric values
                    }
                }
            }
        }

        return new TelemetryPayload(deviceId, timestamp, variables, Map.of());
    }

    private Instant parseTimestamp(String timestampStr, JsonNode config) throws PluginProcessingException {
        if (timestampStr == null || timestampStr.isEmpty()) {
            return Instant.now();
        }

        String format = config.path("timestampFormat").asText("ISO");

        try {
            switch (format) {
                case "ISO":
                    return Instant.parse(timestampStr);
                case "EPOCH_SECONDS":
                    return Instant.ofEpochSecond(Long.parseLong(timestampStr));
                case "EPOCH_MILLIS":
                    return Instant.ofEpochMilli(Long.parseLong(timestampStr));
                default:
                    // Custom format
                    DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                    return LocalDateTime.parse(timestampStr, formatter).toInstant(ZoneOffset.UTC);
            }
        } catch (DateTimeParseException | NumberFormatException e) {
            throw new PluginProcessingException("Failed to parse timestamp: " + timestampStr, e);
        }
    }

    private String[] parseCsvLine(String line) {
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                result.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }

        result.add(current.toString());

        return result.toArray(new String[0]);
    }

    @Override
    public PluginValidationResult validateConfiguration(DataPlugin plugin) {
        JsonNode config = plugin.getConfiguration();

        // Validate required fields
        if (!config.has("deviceIdColumn")) {
            return PluginValidationResult.invalid("Missing required configuration: deviceIdColumn");
        }

        if (!config.has("timestampColumn")) {
            return PluginValidationResult.invalid("Missing required configuration: timestampColumn");
        }

        return PluginValidationResult.valid();
    }

    @Override
    public PluginProvider getSupportedProvider() {
        return PluginProvider.CSV_FILE;
    }
}
