package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.indcloud.model.PluginProvider;
import io.indcloud.model.PluginType;

public record CreateDataPluginRequest(
        @NotBlank(message = "Name is required")
        String name,

        String description,

        @NotNull(message = "Plugin type is required")
        PluginType pluginType,

        @NotNull(message = "Provider is required")
        PluginProvider provider,

        Boolean enabled,

        @NotNull(message = "Configuration is required")
        JsonNode configuration
) {
}
