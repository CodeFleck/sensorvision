package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record WebhookTestRequest(
        String name,

        @NotBlank
        String url,

        @Pattern(regexp = "GET|POST|PUT|PATCH|DELETE")
        String httpMethod,

        JsonNode headers,

        String requestBody
) {}
