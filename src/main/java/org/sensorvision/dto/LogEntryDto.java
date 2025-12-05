package org.sensorvision.dto;

/**
 * DTO representing a log entry for WebSocket streaming.
 *
 * @param source    the log source ("backend", "mosquitto", "postgres")
 * @param level     the log level ("DEBUG", "INFO", "WARN", "ERROR", "FATAL")
 * @param timestamp the timestamp of the log entry
 * @param message   the sanitized log message
 * @param logger    the logger name (optional, may be null)
 */
public record LogEntryDto(
        String source,
        String level,
        String timestamp,
        String message,
        String logger
) {
    /**
     * Creates a LogEntryDto with the given source and raw message,
     * extracting level and timestamp if possible.
     */
    public static LogEntryDto fromRawLog(String source, String rawLine) {
        // Default values
        String level = "INFO";
        String timestamp = java.time.Instant.now().toString();
        String message = rawLine;
        String logger = null;

        // Try to parse standard log format: timestamp [thread] LEVEL logger - message
        // Example: 2024-01-01 12:00:00.000 [main] INFO org.sensorvision.App - Starting...
        if (rawLine != null && !rawLine.isEmpty()) {
            // Check for common log levels in the line
            if (rawLine.contains(" ERROR ") || rawLine.contains("[ERROR]")) {
                level = "ERROR";
            } else if (rawLine.contains(" WARN ") || rawLine.contains("[WARN]") || rawLine.contains("[WARNING]")) {
                level = "WARN";
            } else if (rawLine.contains(" DEBUG ") || rawLine.contains("[DEBUG]")) {
                level = "DEBUG";
            } else if (rawLine.contains(" FATAL ") || rawLine.contains("[FATAL]")) {
                level = "FATAL";
            } else if (rawLine.contains(" INFO ") || rawLine.contains("[INFO]")) {
                level = "INFO";
            } else if (rawLine.contains(" TRACE ") || rawLine.contains("[TRACE]")) {
                level = "DEBUG"; // Map TRACE to DEBUG for UI simplicity
            }

            // Try to extract timestamp from beginning of line
            // Common patterns: "2024-01-01 12:00:00" or "2024-01-01T12:00:00"
            if (rawLine.length() > 19) {
                String potentialTimestamp = rawLine.substring(0, 23).trim();
                if (potentialTimestamp.matches("\\d{4}-\\d{2}-\\d{2}[T ]\\d{2}:\\d{2}:\\d{2}.*")) {
                    timestamp = potentialTimestamp;
                }
            }
        }

        return new LogEntryDto(source, level, timestamp, message, logger);
    }

    /**
     * Creates a LogEntryDto for MQTT/Mosquitto logs.
     * Mosquitto log format: timestamp : level : message
     */
    public static LogEntryDto fromMosquittoLog(String rawLine) {
        String level = "INFO";
        String timestamp = java.time.Instant.now().toString();
        String message = rawLine;

        if (rawLine != null && rawLine.contains(" : ")) {
            String[] parts = rawLine.split(" : ", 3);
            if (parts.length >= 2) {
                // First part might be timestamp
                timestamp = parts[0].trim();
                // Check for level indicators
                String levelPart = parts.length >= 3 ? parts[1].trim().toUpperCase() : "";
                if (levelPart.contains("ERROR")) {
                    level = "ERROR";
                } else if (levelPart.contains("WARN")) {
                    level = "WARN";
                } else if (levelPart.contains("DEBUG")) {
                    level = "DEBUG";
                }
                message = parts.length >= 3 ? parts[2] : parts[1];
            }
        }

        return new LogEntryDto("mosquitto", level, timestamp, message, null);
    }

    /**
     * Creates a LogEntryDto for PostgreSQL logs.
     * PostgreSQL log format varies but commonly: timestamp [PID] LOG/ERROR/WARNING: message
     */
    public static LogEntryDto fromPostgresLog(String rawLine) {
        String level = "INFO";
        String timestamp = java.time.Instant.now().toString();
        String message = rawLine;

        if (rawLine != null) {
            if (rawLine.contains("ERROR:") || rawLine.contains("FATAL:") || rawLine.contains("PANIC:")) {
                level = "ERROR";
            } else if (rawLine.contains("WARNING:")) {
                level = "WARN";
            } else if (rawLine.contains("DEBUG:")) {
                level = "DEBUG";
            } else if (rawLine.contains("LOG:")) {
                level = "INFO";
            }

            // Try to extract timestamp from PostgreSQL format
            if (rawLine.length() > 23 && rawLine.matches("\\d{4}-\\d{2}-\\d{2}.*")) {
                timestamp = rawLine.substring(0, 23).trim();
            }
        }

        return new LogEntryDto("postgres", level, timestamp, message, null);
    }
}
