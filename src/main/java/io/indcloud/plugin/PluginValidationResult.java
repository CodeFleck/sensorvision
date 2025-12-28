package io.indcloud.plugin;

import java.util.ArrayList;
import java.util.List;

/**
 * Result of plugin configuration validation
 */
public class PluginValidationResult {

    private final boolean valid;
    private final List<String> errors;

    private PluginValidationResult(boolean valid, List<String> errors) {
        this.valid = valid;
        this.errors = errors;
    }

    public static PluginValidationResult valid() {
        return new PluginValidationResult(true, new ArrayList<>());
    }

    public static PluginValidationResult invalid(List<String> errors) {
        return new PluginValidationResult(false, errors);
    }

    public static PluginValidationResult invalid(String error) {
        List<String> errors = new ArrayList<>();
        errors.add(error);
        return new PluginValidationResult(false, errors);
    }

    public boolean isValid() {
        return valid;
    }

    public List<String> getErrors() {
        return errors;
    }
}
