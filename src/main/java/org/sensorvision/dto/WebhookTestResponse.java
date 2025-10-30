package org.sensorvision.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

public record WebhookTestResponse(
        Long id,
        String name,
        String url,
        String httpMethod,
        JsonNode headers,
        String requestBody,
        Integer statusCode,
        String responseBody,
        JsonNode responseHeaders,
        Long durationMs,
        String errorMessage,
        String createdBy,
        Instant createdAt
) {}
