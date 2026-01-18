package io.indcloud.service.llm;

import io.indcloud.config.LLMConfigurationProperties;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.dto.llm.RootCauseAnalysisDto;
import io.indcloud.model.*;
import io.indcloud.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for performing AI-powered root cause analysis on alerts and anomalies.
 * Analyzes historical data, related events, and patterns to identify likely root causes.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RootCauseAnalysisService {

    private final LLMServiceRouter llmRouter;
    private final AlertRepository alertRepository;
    private final MLAnomalyRepository anomalyRepository;
    private final DeviceRepository deviceRepository;
    private final VariableRepository variableRepository;
    private final VariableValueRepository variableValueRepository;
    private final LLMConfigurationProperties config;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            .withZone(ZoneOffset.UTC);

    /**
     * Internal class to hold analysis context through the reactive chain.
     */
    private record AnalysisContext(
            RootCauseAnalysisDto.Request request,
            String sourceContext,
            Device device
    ) {}

    private static final String SYSTEM_PROMPT = """
        You are an expert industrial systems engineer specializing in root cause analysis (RCA).
        Your task is to analyze IoT device data, alerts, and anomalies to identify the most likely root causes
        of issues and provide actionable recommendations.

        When performing root cause analysis:
        1. Consider multiple potential causes and rank them by likelihood
        2. Look for patterns in the data that indicate systemic issues
        3. Consider both technical and environmental factors
        4. Provide specific, actionable corrective actions
        5. Suggest preventive measures to avoid recurrence

        Structure your analysis as follows:

        ## Issue Summary
        Brief description of the problem

        ## Root Causes (ranked by likelihood)
        1. [Most likely cause] - XX% likelihood
           - Category: [Hardware/Software/Environmental/Operational/Configuration]
           - Evidence: [What data supports this conclusion]

        ## Contributing Factors
        - Factor 1
        - Factor 2

        ## Corrective Actions
        1. [IMMEDIATE] Action 1 - Expected outcome
        2. [SHORT-TERM] Action 2 - Expected outcome

        ## Preventive Measures
        - Measure 1
        - Measure 2

        ## Confidence Level
        State your confidence (0-100%) in this analysis.

        Be specific with timestamps and values.
        """;

    /**
     * Perform root cause analysis on an alert or anomaly.
     * Uses proper reactive patterns - blocking DB calls are wrapped with subscribeOn(boundedElastic).
     */
    public Mono<RootCauseAnalysisDto> analyzeRootCause(
            RootCauseAnalysisDto.Request request,
            Organization organization,
            User user) {

        log.info("Performing root cause analysis for {} {} in org {}",
                request.getSourceType(), request.getSourceId(), organization.getId());

        return Mono.fromCallable(() -> {
                    // All blocking database operations wrapped in fromCallable
                    int lookbackHours = request.getLookbackHours() != null
                            ? request.getLookbackHours()
                            : config.getDefaults().getRootCauseLookbackHours();

                    String sourceContext;
                    Device device;

                    if (request.getSourceType() == RootCauseAnalysisDto.SourceType.ANOMALY) {
                        MLAnomaly anomaly = anomalyRepository.findById(request.getSourceId())
                                .orElseThrow(() -> new IllegalArgumentException("Anomaly not found: " + request.getSourceId()));

                        if (!anomaly.getOrganization().getId().equals(organization.getId())) {
                            throw new SecurityException("Access denied to anomaly from different organization");
                        }

                        device = anomaly.getDevice();
                        sourceContext = buildAnomalyContext(anomaly, lookbackHours);
                    } else {
                        Alert alert = alertRepository.findById(request.getSourceId())
                                .orElseThrow(() -> new IllegalArgumentException("Alert not found: " + request.getSourceId()));

                        Device alertDevice = alert.getDevice();
                        if (alertDevice != null && !alertDevice.getOrganization().getId().equals(organization.getId())) {
                            throw new SecurityException("Access denied to alert from different organization");
                        }

                        device = alertDevice;
                        sourceContext = buildAlertContext(alert, lookbackHours);
                    }

                    if (request.getAdditionalContext() != null && !request.getAdditionalContext().isEmpty()) {
                        sourceContext += "\n## Additional Context from User\n" + request.getAdditionalContext() + "\n";
                    }

                    return new AnalysisContext(request, sourceContext, device);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Execute blocking calls on bounded elastic scheduler
                .flatMap(context -> {
                    LLMRequest llmRequest = LLMRequest.builder()
                            .featureType(LLMFeatureType.ROOT_CAUSE_ANALYSIS)
                            .systemPrompt(SYSTEM_PROMPT)
                            .userMessage(context.sourceContext())
                            .maxTokens(config.getDefaults().getRootCauseMaxTokens())
                            .temperature(config.getDefaults().getRootCauseTemperature())
                            .referenceType(context.request().getSourceType().name().toLowerCase())
                            .referenceId(context.request().getSourceId())
                            .build();

                    return llmRouter.complete(llmRequest, organization, user)
                            .map(response -> buildAnalysisResponse(context.request(), response, context.device()));
                })
                .onErrorResume(e -> {
                    log.error("Failed to perform root cause analysis for {} {}: {}",
                            request.getSourceType(), request.getSourceId(), e.getMessage(), e);
                    return Mono.just(RootCauseAnalysisDto.builder()
                            .sourceId(request.getSourceId())
                            .sourceType(request.getSourceType())
                            .success(false)
                            .errorMessage("An error occurred while performing root cause analysis. Please try again.")
                            .generatedAt(Instant.now())
                            .build());
                });
    }

    private String buildAnomalyContext(MLAnomaly anomaly, int lookbackHours) {
        StringBuilder context = new StringBuilder();
        Device device = anomaly.getDevice();

        context.append("## Anomaly Details\n");
        context.append("- Anomaly ID: ").append(anomaly.getId()).append("\n");
        context.append("- Detected At: ").append(DATE_FORMATTER.format(anomaly.getDetectedAt())).append(" UTC\n");
        context.append("- Anomaly Type: ").append(anomaly.getAnomalyType()).append("\n");
        context.append("- Severity: ").append(anomaly.getSeverity()).append("\n");
        context.append("- Anomaly Score: ").append(String.format("%.3f", anomaly.getAnomalyScore())).append("\n");
        context.append("- Status: ").append(anomaly.getStatus()).append("\n\n");

        if (anomaly.getAffectedVariables() != null && !anomaly.getAffectedVariables().isEmpty()) {
            context.append("### Affected Variables\n");
            for (String variableName : anomaly.getAffectedVariables()) {
                context.append("- ").append(variableName).append("\n");
            }
            context.append("\n");
        }

        appendDeviceContext(context, device, anomaly.getDetectedAt(), lookbackHours);
        appendRelatedAnomalies(context, device, anomaly.getDetectedAt(), lookbackHours, anomaly.getId());

        return context.toString();
    }

    private String buildAlertContext(Alert alert, int lookbackHours) {
        StringBuilder context = new StringBuilder();
        Device device = alert.getDevice();

        context.append("## Alert Details\n");
        context.append("- Alert ID: ").append(alert.getId()).append("\n");
        if (alert.getTriggeredAt() != null) {
            context.append("- Triggered At: ").append(alert.getTriggeredAt()).append("\n");
        }
        context.append("- Severity: ").append(alert.getSeverity()).append("\n");
        context.append("- Acknowledged: ").append(alert.getAcknowledged()).append("\n");
        if (alert.getMessage() != null) {
            context.append("- Message: ").append(alert.getMessage()).append("\n");
        }
        context.append("\n");

        if (alert.getRule() != null) {
            Rule rule = alert.getRule();
            context.append("### Associated Rule\n");
            context.append("- Rule Name: ").append(rule.getName()).append("\n");
            context.append("- Variable: ").append(rule.getVariable()).append("\n");
            context.append("- Operator: ").append(rule.getOperator()).append("\n");
            context.append("- Threshold: ").append(rule.getThreshold()).append("\n");
            if (alert.getTriggeredValue() != null) {
                context.append("- Actual Value: ").append(alert.getTriggeredValue()).append("\n");
            }
            context.append("\n");
        }

        if (device != null) {
            Instant eventTime = alert.getTriggeredAt() != null
                    ? alert.getTriggeredAt().toInstant(ZoneOffset.UTC)
                    : Instant.now();
            appendDeviceContext(context, device, eventTime, lookbackHours);
        }

        return context.toString();
    }

    private void appendDeviceContext(StringBuilder context, Device device, Instant eventTime, int lookbackHours) {
        context.append("## Device Information\n");
        context.append("- Device Name: ").append(device.getName()).append("\n");
        context.append("- Device ID: ").append(device.getExternalId()).append("\n");
        context.append("- Status: ").append(device.getStatus()).append("\n");
        if (device.getDeviceType() != null) {
            context.append("- Type: ").append(device.getDeviceType().getName()).append("\n");
            if (device.getDeviceType().getTemplateCategory() != null) {
                context.append("- Category: ").append(device.getDeviceType().getTemplateCategory()).append("\n");
            }
        }
        if (device.getDescription() != null) {
            context.append("- Description: ").append(device.getDescription()).append("\n");
        }
        context.append("\n");

        Instant startTime = eventTime.minus(lookbackHours, ChronoUnit.HOURS);

        context.append("## Historical Data (").append(lookbackHours).append(" hours before event)\n");

        // Get all variables for this device
        List<Variable> variables = variableRepository.findByDeviceId(device.getId());
        List<Variable> limitedVars = variables.stream()
                .limit(config.getQueryLimits().getMaxVariablesPerDevice()).toList();

        // Batch fetch: Get statistics for all variables in ONE query (avoids N*3 queries)
        if (!limitedVars.isEmpty()) {
            List<Long> variableIds = limitedVars.stream().map(Variable::getId).collect(Collectors.toList());
            Map<Long, Object[]> statsMap = new HashMap<>();
            List<Object[]> batchStats = variableValueRepository.calculateBatchStatistics(variableIds, startTime, eventTime);
            for (Object[] row : batchStats) {
                Long varId = (Long) row[0];
                statsMap.put(varId, row);
            }

            // Build context using pre-fetched statistics (no more DB queries in loop)
            for (Variable var : limitedVars) {
                Object[] stats = statsMap.get(var.getId());
                Double avg = stats != null ? (Double) stats[1] : null;
                Double min = stats != null ? (Double) stats[2] : null;
                Double max = stats != null ? (Double) stats[3] : null;

                context.append("### ").append(var.getName());
                if (var.getDisplayName() != null) {
                    context.append(" (").append(var.getDisplayName()).append(")");
                }
                if (var.getUnit() != null) {
                    context.append(" [").append(var.getUnit()).append("]");
                }
                context.append("\n");

                if (avg != null) {
                    context.append("- Average: ").append(String.format("%.2f", avg)).append("\n");
                    context.append("- Min: ").append(String.format("%.2f", min)).append("\n");
                    context.append("- Max: ").append(String.format("%.2f", max)).append("\n");
                }
                context.append("\n");
            }
        }
    }

    private void appendRelatedAnomalies(StringBuilder context, Device device, Instant eventTime,
                                         int lookbackHours, UUID excludeId) {
        Instant startTime = eventTime.minus(lookbackHours, ChronoUnit.HOURS);
        List<MLAnomaly> relatedAnomalies = anomalyRepository.findByDeviceAndTimeRange(
                device.getId(), startTime, eventTime);

        relatedAnomalies = relatedAnomalies.stream()
                .filter(a -> !a.getId().equals(excludeId))
                .limit(config.getQueryLimits().getMaxRelatedAnomalies())
                .collect(Collectors.toList());

        if (!relatedAnomalies.isEmpty()) {
            context.append("## Related Anomalies (").append(lookbackHours).append("h window)\n");
            for (MLAnomaly anomaly : relatedAnomalies) {
                context.append("- ").append(DATE_FORMATTER.format(anomaly.getDetectedAt()))
                        .append(": ").append(anomaly.getAnomalyType())
                        .append(" (Score: ").append(String.format("%.2f", anomaly.getAnomalyScore()))
                        .append(", Severity: ").append(anomaly.getSeverity()).append(")\n");
            }
            context.append("\n");
        }
    }

    private static final int DEFAULT_CONFIDENCE_LEVEL = 70;

    /**
     * Parse confidence level from LLM response content.
     * Looks for patterns like "85%" in the Confidence section.
     *
     * @param content The LLM response content
     * @return The parsed confidence level, or DEFAULT_CONFIDENCE_LEVEL if parsing fails
     */
    private int parseConfidenceLevel(String content) {
        int confStart = content.indexOf("## Confidence");
        if (confStart < 0) {
            log.debug("No Confidence section found in LLM response");
            return DEFAULT_CONFIDENCE_LEVEL;
        }

        String confSection = content.substring(confStart);
        int pctIdx = confSection.indexOf('%');
        if (pctIdx < 0) {
            log.debug("No percentage sign found in Confidence section");
            return DEFAULT_CONFIDENCE_LEVEL;
        }

        // Find the start of the number by walking backwards from the % sign
        int numStart = pctIdx - 1;
        while (numStart > 0 && Character.isDigit(confSection.charAt(numStart - 1))) {
            numStart--;
        }

        if (numStart >= pctIdx) {
            log.debug("No digits found before percentage sign in Confidence section");
            return DEFAULT_CONFIDENCE_LEVEL;
        }

        String numberStr = confSection.substring(numStart, pctIdx).trim();
        try {
            int parsedLevel = Integer.parseInt(numberStr);
            // Validate reasonable range
            if (parsedLevel < 0 || parsedLevel > 100) {
                log.warn("Parsed confidence level {} is out of valid range (0-100), using default", parsedLevel);
                return DEFAULT_CONFIDENCE_LEVEL;
            }
            return parsedLevel;
        } catch (NumberFormatException e) {
            log.debug("Failed to parse confidence level from '{}': {}", numberStr, e.getMessage());
            return DEFAULT_CONFIDENCE_LEVEL;
        }
    }

    private RootCauseAnalysisDto buildAnalysisResponse(
            RootCauseAnalysisDto.Request request,
            LLMResponse response,
            Device device) {

        List<RootCauseAnalysisDto.RootCause> rootCauses = new ArrayList<>();
        String issueSummary = "";
        int confidenceLevel = DEFAULT_CONFIDENCE_LEVEL;

        if (response.isSuccess() && response.getContent() != null) {
            String content = response.getContent();

            int summaryStart = content.indexOf("## Issue Summary");
            if (summaryStart >= 0) {
                int summaryEnd = content.indexOf("##", summaryStart + 16);
                if (summaryEnd > summaryStart) {
                    issueSummary = content.substring(summaryStart + 16, summaryEnd).trim();
                }
            }

            confidenceLevel = parseConfidenceLevel(content);
        }

        return RootCauseAnalysisDto.builder()
                .analysisId(UUID.randomUUID())
                .sourceId(request.getSourceId())
                .sourceType(request.getSourceType())
                .deviceId(device != null ? device.getId() : null)
                .deviceName(device != null ? device.getName() : null)
                .issueSummary(issueSummary)
                .rootCauses(rootCauses)
                .contributingFactors(new ArrayList<>())
                .correctiveActions(new ArrayList<>())
                .preventiveMeasures(new ArrayList<>())
                .fullAnalysis(response.isSuccess() ? response.getContent() : null)
                .confidenceLevel(confidenceLevel)
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
