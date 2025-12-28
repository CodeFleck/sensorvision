package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.PluginProvider;
import io.indcloud.model.PluginType;

import java.time.Instant;

public record DataPluginDto(
        Long id,
        String name,
        String description,
        PluginType pluginType,
        PluginProvider provider,
        Boolean enabled,
        JsonNode configuration,
        String createdByUsername,
        Instant createdAt,
        Instant updatedAt
) {
}
