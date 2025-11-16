package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;

public record PluginInstallRequest(
        JsonNode configuration
) {
}
