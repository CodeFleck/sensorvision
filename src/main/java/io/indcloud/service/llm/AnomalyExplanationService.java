package io.indcloud.service.llm;

import io.indcloud.config.LLMConfigurationProperties;
import io.indcloud.dto.llm.AnomalyExplanationDto;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.model.*;
import io.indcloud.repository.MLAnomalyRepository;
import io.indcloud.repository.VariableValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Service for generating AI-powered explanations of detected anomalies.
 * Uses LLM to analyze anomaly data and provide actionable insights.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AnomalyExplanationService {

    private final LLMServiceRouter llmRouter;
    private final MLAnomalyRepository anomalyRepository;
    private final VariableValueRepository variableValueRepository;
    private final LLMConfigurationProperties config;

    private static final String SYSTEM_PROMPT = """
        You are an expert IoT systems analyst specializing in industrial monitoring and anomaly detection.
        Your role is to analyze anomaly data from sensors and devices, explain what might be causing the anomaly,
        and provide actionable recommendations.

        When analyzing anomalies, consider:
        1. The type of device and what it monitors
        2. The specific variables showing anomalous behavior
        3. Historical patterns and whether this is a sudden or gradual change
        4. Potential root causes (equipment failure, environmental factors, sensor issues, etc.)
        5. The severity and urgency of the situation

        Provide your response in a clear, structured format:
        - Start with a brief summary of the anomaly
        - Explain the likely causes
        - Suggest immediate actions to take
        - Recommend preventive measures

        Keep your response concise but comprehensive. Use technical terms appropriately but explain them if needed.
        """;

    /**
     * Generate an AI explanation for a specific anomaly.
     * Uses proper reactive patterns - blocking DB calls are wrapped with subscribeOn(boundedElastic).
     *
     * @param anomalyId The ID of the anomaly to explain
     * @param organization The organization requesting the explanation
     * @param user The user requesting the explanation (for usage tracking)
     * @return The explanation response
     */
    public Mono<AnomalyExplanationDto> explainAnomaly(UUID anomalyId, Organization organization, User user) {
        return Mono.fromCallable(() -> {
                    // Blocking DB call - wrapped in Mono.fromCallable
                    MLAnomaly anomaly = anomalyRepository.findById(anomalyId)
                            .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + anomalyId));

                    // Verify organization access
                    if (!anomaly.getOrganization().getId().equals(organization.getId())) {
                        throw new SecurityException("Access denied to anomaly from different organization");
                    }

                    return anomaly;
                })
                .subscribeOn(Schedulers.boundedElastic()) // Execute blocking call on bounded elastic scheduler
                .flatMap(anomaly -> {
                    // Build context about the anomaly
                    String context = buildAnomalyContext(anomaly);

                    // Build the LLM request
                    LLMRequest request = LLMRequest.builder()
                            .featureType(LLMFeatureType.ANOMALY_EXPLANATION)
                            .systemPrompt(SYSTEM_PROMPT)
                            .userMessage(context)
                            .maxTokens(config.getDefaults().getExplanationMaxTokens())
                            .temperature(config.getDefaults().getExplanationTemperature())
                            .referenceType("anomaly")
                            .referenceId(anomalyId)
                            .build();

                    // Send to LLM and map response
                    return llmRouter.complete(request, organization, user)
                            .map(response -> buildExplanationDto(anomaly, response));
                })
                .onErrorResume(e -> {
                    log.error("Failed to explain anomaly {}: {}", anomalyId, e.getMessage(), e);
                    return Mono.just(AnomalyExplanationDto.builder()
                            .anomalyId(anomalyId)
                            .success(false)
                            .errorMessage("An error occurred while generating the explanation. Please try again.")
                            .generatedAt(Instant.now())
                            .build());
                });
    }

    /**
     * Generate explanations for multiple anomalies in batch.
     * Uses Flux for better backpressure handling and limits concurrency.
     */
    public Mono<List<AnomalyExplanationDto>> explainAnomalies(List<UUID> anomalyIds,
                                                               Organization organization,
                                                               User user) {
        return Flux.fromIterable(anomalyIds)
                .flatMap(id -> explainAnomaly(id, organization, user),
                        config.getBatchLimits().getBatchConcurrency())
                .collectList();
    }

    private String buildAnomalyContext(MLAnomaly anomaly) {
        StringBuilder context = new StringBuilder();

        // Device information
        Device device = anomaly.getDevice();
        context.append("## Device Information\n");
        context.append("- Device Name: ").append(device.getName()).append("\n");
        context.append("- Device ID: ").append(device.getExternalId()).append("\n");
        if (device.getDescription() != null) {
            context.append("- Description: ").append(device.getDescription()).append("\n");
        }
        if (device.getDeviceType() != null) {
            context.append("- Device Type: ").append(device.getDeviceType().getName()).append("\n");
            if (device.getDeviceType().getTemplateCategory() != null) {
                context.append("- Category: ").append(device.getDeviceType().getTemplateCategory()).append("\n");
            }
        }
        context.append("\n");

        // Anomaly details
        context.append("## Anomaly Details\n");
        context.append("- Anomaly Score: ").append(String.format("%.2f", anomaly.getAnomalyScore())).append("\n");
        context.append("- Severity: ").append(anomaly.getSeverity()).append("\n");
        context.append("- Anomaly Type: ").append(anomaly.getAnomalyType()).append("\n");
        context.append("- Detected At: ").append(anomaly.getDetectedAt()).append("\n");
        context.append("- Status: ").append(anomaly.getStatus()).append("\n");
        context.append("\n");

        // Affected variables (they are stored as List<String>)
        if (anomaly.getAffectedVariables() != null && !anomaly.getAffectedVariables().isEmpty()) {
            context.append("## Affected Variables\n");
            for (String variableName : anomaly.getAffectedVariables()) {
                context.append("- ").append(variableName).append("\n");
            }
            context.append("\n");
        }

        // Instructions for LLM
        context.append("## Analysis Request\n");
        context.append("Please analyze this anomaly and provide:\n");
        context.append("1. A clear explanation of what this anomaly means\n");
        context.append("2. Possible root causes\n");
        context.append("3. Recommended immediate actions\n");
        context.append("4. Suggestions for preventing similar issues\n");

        return context.toString();
    }

    private AnomalyExplanationDto buildExplanationDto(MLAnomaly anomaly, LLMResponse response) {
        return AnomalyExplanationDto.builder()
                .anomalyId(anomaly.getId())
                .deviceId(anomaly.getDevice().getId())
                .deviceName(anomaly.getDevice().getName())
                .anomalyScore(anomaly.getAnomalyScore() != null ? anomaly.getAnomalyScore().doubleValue() : null)
                .severity(anomaly.getSeverity().name())
                .explanation(response.isSuccess() ? response.getContent() : null)
                .success(response.isSuccess())
                .errorMessage(response.getErrorMessage())
                .provider(response.getProvider() != null ? response.getProvider().name() : null)
                .modelId(response.getModelId())
                .tokensUsed(response.getTotalTokens())
                .latencyMs(response.getLatencyMs())
                .generatedAt(Instant.now())
                .build();
    }
}
