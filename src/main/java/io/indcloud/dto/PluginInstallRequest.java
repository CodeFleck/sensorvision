package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PluginInstallRequest(
        JsonNode configuration
) {
}
