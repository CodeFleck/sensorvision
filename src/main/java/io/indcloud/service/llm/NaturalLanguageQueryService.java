package io.indcloud.service.llm;

import io.indcloud.config.LLMConfigurationProperties;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.dto.llm.NaturalLanguageQueryDto;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.VariableRepository;
import io.indcloud.repository.VariableValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for processing natural language queries about IoT device data.
 * Uses LLM to understand user intent and generate helpful responses based on actual data.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NaturalLanguageQueryService {

    private final LLMServiceRouter llmRouter;
    private final DeviceRepository deviceRepository;
    private final VariableRepository variableRepository;
    private final VariableValueRepository variableValueRepository;
    private final LLMConfigurationProperties config;

    private static final String SYSTEM_PROMPT = """
        You are an intelligent IoT data analyst assistant. You help users understand their device data
        by answering questions about sensor readings, device performance, and operational metrics.

        When answering questions:
        1. Use the actual data provided in the context to give accurate, factual responses
        2. If specific values are provided, reference them in your answer
        3. Provide insights and observations based on the data patterns
        4. If the data is insufficient to fully answer the question, say so clearly
        5. Use appropriate units when discussing values
        6. Format numbers clearly (e.g., "23.5°C" or "1,234 kWh")
        7. If you notice anomalies or concerning patterns, mention them

        Keep your responses concise but informative. Avoid speculation beyond what the data supports.
        """;

    /**
     * Internal class to hold query context data through the reactive chain.
     */
    private record QueryContext(
            NaturalLanguageQueryDto.Request request,
            String dataContext,
            List<NaturalLanguageQueryDto.DataPoint> supportingData
    ) {}

    /**
     * Process a natural language query about IoT data.
     * Uses proper reactive patterns - blocking DB calls are wrapped with subscribeOn(boundedElastic).
     *
     * @param request The query request
     * @param organization The organization making the query
     * @param user The user making the query
     * @return The query response with AI-generated answer
     */
    public Mono<NaturalLanguageQueryDto> processQuery(
            NaturalLanguageQueryDto.Request request,
            Organization organization,
            User user) {

        log.info("Processing NL query for org {}: {}", organization.getId(), request.getQuery());

        return Mono.fromCallable(() -> {
                    // All blocking database operations wrapped in fromCallable
                    // Set default time range if not provided (last 24 hours)
                    Instant fromTime = request.getFromTime() != null
                            ? request.getFromTime()
                            : Instant.now().minus(24, ChronoUnit.HOURS);
                    Instant toTime = request.getToTime() != null
                            ? request.getToTime()
                            : Instant.now();

                    // Fetch relevant devices
                    List<Device> devices = fetchDevices(request, organization);

                    if (devices.isEmpty()) {
                        return null; // Will be handled below
                    }

                    // Build data context
                    String dataContext = buildDataContext(devices, fromTime, toTime);
                    List<NaturalLanguageQueryDto.DataPoint> supportingData = extractSupportingData(devices, fromTime, toTime);

                    return new QueryContext(request, dataContext, supportingData);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Execute blocking calls on bounded elastic scheduler
                .flatMap(context -> {
                    if (context == null) {
                        return Mono.just(NaturalLanguageQueryDto.builder()
                                .query(request.getQuery())
                                .success(false)
                                .errorMessage("No devices found for this organization")
                                .generatedAt(Instant.now())
                                .build());
                    }

                    // Build the user message with context
                    String userMessage = buildUserMessage(request.getQuery(), context.dataContext());

                    // Build LLM request
                    LLMRequest llmRequest = LLMRequest.builder()
                            .featureType(LLMFeatureType.NATURAL_LANGUAGE_QUERY)
                            .systemPrompt(SYSTEM_PROMPT)
                            .userMessage(userMessage)
                            .maxTokens(config.getDefaults().getQueryMaxTokens())
                            .temperature(config.getDefaults().getQueryTemperature())
                            .referenceType("query")
                            .build();

                    // Send to LLM and build response
                    return llmRouter.complete(llmRequest, organization, user)
                            .map(response -> buildQueryResponse(request, response, context.supportingData()));
                })
                .onErrorResume(e -> {
                    log.error("Failed to process NL query: {}", e.getMessage(), e);
                    return Mono.just(NaturalLanguageQueryDto.builder()
                            .query(request.getQuery())
                            .success(false)
                            .errorMessage("An error occurred while processing your query. Please try again.")
                            .generatedAt(Instant.now())
                            .build());
                });
    }

    private List<Device> fetchDevices(NaturalLanguageQueryDto.Request request, Organization organization) {
        if (request.getDeviceIds() != null && !request.getDeviceIds().isEmpty()) {
            return request.getDeviceIds().stream()
                    .map(id -> deviceRepository.findById(id).orElse(null))
                    .filter(d -> d != null && d.getOrganization().getId().equals(organization.getId()))
                    .collect(Collectors.toList());
        } else {
            // Get all devices for the organization (limit to configured max)
            return deviceRepository.findByOrganizationId(organization.getId())
                    .stream()
                    .limit(config.getQueryLimits().getMaxDevicesForQuery())
                    .collect(Collectors.toList());
        }
    }

    /**
     * Statistics holder for variable data.
     */
    private record VariableStats(Double avg, Double min, Double max, Long count) {}

    private String buildDataContext(List<Device> devices, Instant fromTime, Instant toTime) {
        StringBuilder context = new StringBuilder();
        context.append("## Available Device Data\n\n");

        // Batch fetch: Get all variables for all devices in ONE query (avoids N+1)
        List<UUID> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        List<Variable> allVariables = variableRepository.findByDeviceIds(deviceIds);

        // Group variables by device ID for efficient lookup
        Map<UUID, List<Variable>> variablesByDevice = allVariables.stream()
                .collect(Collectors.groupingBy(v -> v.getDevice().getId()));

        // Batch fetch: Get statistics for all variables in ONE query (avoids N*4 queries)
        List<Long> variableIds = allVariables.stream().map(Variable::getId).collect(Collectors.toList());
        Map<Long, VariableStats> statsMap = new HashMap<>();
        if (!variableIds.isEmpty()) {
            List<Object[]> batchStats = variableValueRepository.calculateBatchStatistics(variableIds, fromTime, toTime);
            for (Object[] row : batchStats) {
                Long varId = (Long) row[0];
                Double avg = (Double) row[1];
                Double min = (Double) row[2];
                Double max = (Double) row[3];
                Long count = (Long) row[4];
                statsMap.put(varId, new VariableStats(avg, min, max, count));
            }
        }

        // Build context using pre-fetched data (no more DB queries in loop)
        for (Device device : devices) {
            context.append("### Device: ").append(device.getName());
            if (device.getDeviceType() != null) {
                context.append(" (").append(device.getDeviceType().getName()).append(")");
            }
            context.append("\n");
            context.append("- Device ID: ").append(device.getExternalId()).append("\n");
            context.append("- Status: ").append(device.getStatus()).append("\n");
            if (device.getDescription() != null) {
                context.append("- Description: ").append(device.getDescription()).append("\n");
            }

            // Use pre-fetched variables
            List<Variable> variables = variablesByDevice.getOrDefault(device.getId(), Collections.emptyList());
            if (!variables.isEmpty()) {
                context.append("- Variables:\n");
                for (Variable var : variables) {
                    context.append("  - ").append(var.getName());
                    if (var.getDisplayName() != null) {
                        context.append(" (").append(var.getDisplayName()).append(")");
                    }
                    if (var.getUnit() != null) {
                        context.append(" [").append(var.getUnit()).append("]");
                    }

                    // Get latest value (cached on Variable entity)
                    if (var.getLastValue() != null) {
                        context.append(": Current = ").append(formatValue(var.getLastValue().doubleValue()));
                        if (var.getUnit() != null) {
                            context.append(" ").append(var.getUnit());
                        }
                    }

                    // Use pre-fetched statistics
                    VariableStats stats = statsMap.get(var.getId());
                    if (stats != null && stats.avg() != null) {
                        context.append(" | Avg: ").append(formatValue(stats.avg()));
                        context.append(" | Min: ").append(formatValue(stats.min()));
                        context.append(" | Max: ").append(formatValue(stats.max()));
                    }
                    context.append("\n");
                }
            }
            context.append("\n");
        }

        return context.toString();
    }

    private List<NaturalLanguageQueryDto.DataPoint> extractSupportingData(
            List<Device> devices, Instant fromTime, Instant toTime) {
        List<NaturalLanguageQueryDto.DataPoint> dataPoints = new ArrayList<>();

        // Batch fetch: Get all variables for all devices in ONE query
        List<UUID> deviceIds = devices.stream().map(Device::getId).collect(Collectors.toList());
        List<Variable> allVariables = variableRepository.findByDeviceIds(deviceIds);

        // Create device lookup map
        Map<UUID, Device> deviceMap = devices.stream()
                .collect(Collectors.toMap(Device::getId, d -> d));

        // Batch fetch: Get latest values for all variables in ONE query
        List<Long> variableIds = allVariables.stream().map(Variable::getId).collect(Collectors.toList());
        Map<Long, VariableValue> latestValues = new HashMap<>();
        if (!variableIds.isEmpty()) {
            List<VariableValue> latestList = variableValueRepository.findLatestValuesByVariableIds(variableIds);
            for (VariableValue vv : latestList) {
                latestValues.put(vv.getVariable().getId(), vv);
            }
        }

        // Build data points using pre-fetched data (no more DB queries in loop)
        for (Variable var : allVariables) {
            if (dataPoints.size() >= config.getBatchLimits().getMaxSupportingDataPoints()) {
                break;
            }

            VariableValue latestValue = latestValues.get(var.getId());
            if (latestValue != null) {
                Device device = deviceMap.get(var.getDevice().getId());
                dataPoints.add(NaturalLanguageQueryDto.DataPoint.builder()
                        .deviceId(device.getId())
                        .deviceName(device.getName())
                        .variableName(var.getName())
                        .value(latestValue.getValue())
                        .unit(var.getUnit())
                        .timestamp(latestValue.getTimestamp())
                        .build());
            }
        }

        return dataPoints;
    }

    private String buildUserMessage(String query, String dataContext) {
        return String.format("""
            Here is the current IoT data context:

            %s

            User Question: %s

            Please analyze the data and provide a helpful answer to the user's question.
            """, dataContext, query);
    }

    private NaturalLanguageQueryDto buildQueryResponse(
            NaturalLanguageQueryDto.Request request,
            LLMResponse response,
            List<NaturalLanguageQueryDto.DataPoint> supportingData) {

        return NaturalLanguageQueryDto.builder()
                .query(request.getQuery())
                .deviceIds(request.getDeviceIds())
                .fromTime(request.getFromTime())
                .toTime(request.getToTime())
                .response(response.isSuccess() ? response.getContent() : null)
                .supportingData(supportingData)
                .success(response.isSuccess())
                .errorMessage(response.getErrorMessage())
                .provider(response.getProvider() != null ? response.getProvider().name() : null)
                .modelId(response.getModelId())
                .tokensUsed(response.getTotalTokens())
                .latencyMs(response.getLatencyMs())
                .generatedAt(Instant.now())
                .build();
    }

    private String formatValue(Double value) {
        if (value == null) return "N/A";
        if (value == value.longValue()) {
            return String.format("%d", value.longValue());
        }
        return String.format("%.2f", value);
    }
}
