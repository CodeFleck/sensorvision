package org.sensorvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.sensorvision.model.PluginRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Service for validating and managing plugin configurations using JSON schemas
 */
@Service
public class PluginConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(PluginConfigurationService.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Validate plugin configuration against its schema
     */
    public ConfigurationValidationResult validateConfiguration(PluginRegistry plugin, JsonNode configuration) {
        if (plugin.getConfigSchema() == null) {
            // No schema defined, accept any configuration
            return ConfigurationValidationResult.valid();
        }

        List<String> errors = new ArrayList<>();
        JsonNode schema = plugin.getConfigSchema();

        // Get required fields
        if (schema.has("required") && schema.get("required").isArray()) {
            JsonNode required = schema.get("required");
            for (JsonNode fieldNode : required) {
                String field = fieldNode.asText();
                if (!configuration.has(field) || configuration.get(field).isNull()) {
                    errors.add("Missing required field: " + field);
                }
            }
        }

        // Validate field types
        if (schema.has("properties") && schema.get("properties").isObject()) {
            JsonNode properties = schema.get("properties");
            Iterator<String> fieldNames = properties.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                if (configuration.has(fieldName)) {
                    JsonNode fieldSchema = properties.get(fieldName);
                    JsonNode fieldValue = configuration.get(fieldName);

                    String expectedType = fieldSchema.has("type") ? fieldSchema.get("type").asText() : null;
                    if (expectedType != null) {
                        boolean typeValid = validateFieldType(fieldValue, expectedType);
                        if (!typeValid) {
                            errors.add("Field '" + fieldName + "' has invalid type. Expected: " + expectedType);
                        }
                    }

                    // Validate min/max for numbers
                    if ("number".equals(expectedType) || "integer".equals(expectedType)) {
                        if (fieldSchema.has("minimum")) {
                            double min = fieldSchema.get("minimum").asDouble();
                            if (fieldValue.asDouble() < min) {
                                errors.add("Field '" + fieldName + "' must be >= " + min);
                            }
                        }
                        if (fieldSchema.has("maximum")) {
                            double max = fieldSchema.get("maximum").asDouble();
                            if (fieldValue.asDouble() > max) {
                                errors.add("Field '" + fieldName + "' must be <= " + max);
                            }
                        }
                    }

                    // Validate string length
                    if ("string".equals(expectedType)) {
                        String stringValue = fieldValue.asText();
                        if (fieldSchema.has("minLength")) {
                            int minLength = fieldSchema.get("minLength").asInt();
                            if (stringValue.length() < minLength) {
                                errors.add("Field '" + fieldName + "' must be at least " + minLength + " characters");
                            }
                        }
                        if (fieldSchema.has("maxLength")) {
                            int maxLength = fieldSchema.get("maxLength").asInt();
                            if (stringValue.length() > maxLength) {
                                errors.add("Field '" + fieldName + "' must be at most " + maxLength + " characters");
                            }
                        }
                        if (fieldSchema.has("pattern")) {
                            String pattern = fieldSchema.get("pattern").asText();
                            if (!stringValue.matches(pattern)) {
                                errors.add("Field '" + fieldName + "' does not match required pattern");
                            }
                        }
                    }

                    // Validate enum values
                    if (fieldSchema.has("enum")) {
                        JsonNode enumValues = fieldSchema.get("enum");
                        boolean found = false;
                        for (JsonNode enumValue : enumValues) {
                            if (enumValue.equals(fieldValue)) {
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            errors.add("Field '" + fieldName + "' must be one of the allowed values");
                        }
                    }
                }
            }
        }

        if (errors.isEmpty()) {
            return ConfigurationValidationResult.valid();
        } else {
            return ConfigurationValidationResult.invalid(errors);
        }
    }

    /**
     * Validate field type matches expected type
     */
    private boolean validateFieldType(JsonNode value, String expectedType) {
        switch (expectedType) {
            case "string":
                return value.isTextual();
            case "number":
                return value.isNumber();
            case "integer":
                return value.isInt() || value.isLong();
            case "boolean":
                return value.isBoolean();
            case "object":
                return value.isObject();
            case "array":
                return value.isArray();
            default:
                return true; // Unknown type, accept it
        }
    }

    /**
     * Get default configuration for a plugin based on its schema
     */
    public JsonNode getDefaultConfiguration(PluginRegistry plugin) {
        if (plugin.getConfigSchema() == null) {
            return objectMapper.createObjectNode();
        }

        JsonNode schema = plugin.getConfigSchema();
        var defaultConfig = objectMapper.createObjectNode();

        if (schema.has("properties") && schema.get("properties").isObject()) {
            JsonNode properties = schema.get("properties");
            Iterator<String> fieldNames = properties.fieldNames();

            while (fieldNames.hasNext()) {
                String fieldName = fieldNames.next();
                JsonNode fieldSchema = properties.get(fieldName);

                if (fieldSchema.has("default")) {
                    defaultConfig.set(fieldName, fieldSchema.get("default"));
                }
            }
        }

        return defaultConfig;
    }

    /**
     * Configuration validation result
     */
    public static class ConfigurationValidationResult {
        private final boolean valid;
        private final List<String> errors;

        private ConfigurationValidationResult(boolean valid, List<String> errors) {
            this.valid = valid;
            this.errors = errors;
        }

        public static ConfigurationValidationResult valid() {
            return new ConfigurationValidationResult(true, new ArrayList<>());
        }

        public static ConfigurationValidationResult invalid(List<String> errors) {
            return new ConfigurationValidationResult(false, errors);
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }
    }
}
