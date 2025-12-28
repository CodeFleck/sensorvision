package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Request to manually invoke a serverless function (for testing/debugging).
 */
public record FunctionInvokeRequest(
    JsonNode input
) {
}
