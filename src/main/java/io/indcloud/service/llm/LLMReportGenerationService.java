package io.indcloud.service.llm;

import io.indcloud.config.LLMConfigurationProperties;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.dto.llm.ReportGenerationDto;
import io.indcloud.model.*;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.MLAnomalyRepository;
import io.indcloud.repository.VariableRepository;
import io.indcloud.repository.VariableValueRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for generating AI-powered reports about IoT device data.
 * Supports various report types including daily summaries, weekly reviews, anomaly reports, etc.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMReportGenerationService {

    private final LLMServiceRouter llmRouter;
    private final DeviceRepository deviceRepository;
    private final VariableRepository variableRepository;
    private final VariableValueRepository variableValueRepository;
    private final MLAnomalyRepository anomalyRepository;
    private final LLMConfigurationProperties config;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
            .withZone(ZoneOffset.UTC);

    /**
     * Internal class to hold report generation context through the reactive chain.
     */
    private record ReportContext(
            ReportGenerationDto.Request request,
            List<Device> devices,
            Instant periodStart,
            Instant periodEnd,
            String reportContext
    ) {}

    private static final String SYSTEM_PROMPT = """
        You are a professional IoT systems analyst tasked with generating comprehensive reports.
        Your reports should be:
        1. Professional and well-structured
        2. Data-driven with specific metrics and values
        3. Actionable with clear recommendations
        4. Formatted in clean markdown

        Structure your reports as follows:
        # [Report Title]

        ## Executive Summary
        [2-3 sentence overview]

        ## Key Findings
        - Finding 1
        - Finding 2
        - Finding 3

        ## Detailed Analysis
        [Analysis sections based on the data]

        ## Recommendations
        1. Recommendation 1
        2. Recommendation 2
        3. Recommendation 3

        Use tables, bullet points, and clear headings. Include specific numbers and percentages.
        If you notice concerning trends or anomalies, highlight them clearly.
        """;

    /**
     * Generate an AI-powered report.
     * Uses proper reactive patterns - blocking DB calls are wrapped with subscribeOn(boundedElastic).
     *
     * @param request The report request
     * @param organization The organization
     * @param user The user requesting the report
     * @return The generated report
     */
    public Mono<ReportGenerationDto> generateReport(
            ReportGenerationDto.Request request,
            Organization organization,
            User user) {

        log.info("Generating {} report for org {}", request.getReportType(), organization.getId());

        return Mono.fromCallable(() -> {
                    // All blocking database operations wrapped in fromCallable
                    // Set default time range based on report type
                    Instant periodStart = request.getPeriodStart();
                    Instant periodEnd = request.getPeriodEnd() != null ? request.getPeriodEnd() : Instant.now();

                    if (periodStart == null) {
                        periodStart = switch (request.getReportType()) {
                            case DAILY_SUMMARY -> Instant.now().minus(1, ChronoUnit.DAYS);
                            case WEEKLY_REVIEW -> Instant.now().minus(7, ChronoUnit.DAYS);
                            case MONTHLY_ANALYSIS -> Instant.now().minus(30, ChronoUnit.DAYS);
                            default -> Instant.now().minus(7, ChronoUnit.DAYS);
                        };
                    }

                    // Fetch relevant devices - blocking operation
                    List<Device> devices;
                    if (request.getDeviceIds() != null && !request.getDeviceIds().isEmpty()) {
                        devices = request.getDeviceIds().stream()
                                .map(id -> deviceRepository.findById(id).orElse(null))
                                .filter(d -> d != null && d.getOrganization().getId().equals(organization.getId()))
                                .collect(Collectors.toList());
                    } else {
                        devices = deviceRepository.findByOrganizationId(organization.getId())
                                .stream()
                                .limit(config.getQueryLimits().getMaxDevicesForReport())
                                .collect(Collectors.toList());
                    }

                    if (devices.isEmpty()) {
                        return null; // Will be handled below
                    }

                    // Build the report context - contains blocking DB calls
                    String reportContext = buildReportContext(
                            request.getReportType(),
                            devices,
                            periodStart,
                            periodEnd,
                            organization,
                            request.getCustomPrompt());

                    return new ReportContext(request, devices, periodStart, periodEnd, reportContext);
                })
                .subscribeOn(Schedulers.boundedElastic()) // Execute blocking calls on bounded elastic scheduler
                .flatMap(context -> {
                    if (context == null) {
                        return Mono.just(ReportGenerationDto.builder()
                                .reportType(request.getReportType())
                                .success(false)
                                .errorMessage("No devices found for this organization")
                                .generatedAt(Instant.now())
                                .build());
                    }

                    // Build LLM request
                    LLMRequest llmRequest = LLMRequest.builder()
                            .featureType(LLMFeatureType.REPORT_GENERATION)
                            .systemPrompt(SYSTEM_PROMPT)
                            .userMessage(context.reportContext())
                            .maxTokens(config.getDefaults().getReportMaxTokens())
                            .temperature(config.getDefaults().getReportTemperature())
                            .referenceType("report")
                            .build();

                    // Generate the report
                    return llmRouter.complete(llmRequest, organization, user)
                            .map(response -> buildReportResponse(
                                    context.request(),
                                    response,
                                    context.devices(),
                                    context.periodStart(),
                                    context.periodEnd()));
                })
                .onErrorResume(e -> {
                    log.error("Failed to generate report: {}", e.getMessage(), e);
                    return Mono.just(ReportGenerationDto.builder()
                            .reportType(request.getReportType())
                            .success(false)
                            .errorMessage("An error occurred while generating the report. Please try again.")
                            .generatedAt(Instant.now())
                            .build());
                });
    }

    private String buildReportContext(
            ReportGenerationDto.ReportType reportType,
            List<Device> devices,
            Instant periodStart,
            Instant periodEnd,
            Organization organization,
            String customPrompt) {

        StringBuilder context = new StringBuilder();

        // Report metadata
        context.append("## Report Request\n");
        context.append("- Report Type: ").append(reportType.name().replace("_", " ")).append("\n");
        context.append("- Organization: ").append(organization.getName()).append("\n");
        context.append("- Period: ").append(DATE_FORMATTER.format(periodStart))
                .append(" to ").append(DATE_FORMATTER.format(periodEnd)).append(" UTC\n");
        context.append("- Number of Devices: ").append(devices.size()).append("\n\n");

        if (customPrompt != null && !customPrompt.isEmpty()) {
            context.append("Custom Instructions: ").append(customPrompt).append("\n\n");
        }

        // Device overview
        context.append("## Device Overview\n\n");
        Map<String, Long> statusCounts = devices.stream()
                .collect(Collectors.groupingBy(
                        d -> d.getStatus() != null ? d.getStatus().name() : "UNKNOWN",
                        Collectors.counting()));
        context.append("Device Status Summary:\n");
        statusCounts.forEach((status, count) ->
                context.append("- ").append(status).append(": ").append(count).append("\n"));
        context.append("\n");

        // Per-device statistics (using batch queries to avoid N+1)
        context.append("## Device Data Summary\n\n");

        // Batch fetch: Get all variables for limited devices in ONE query
        List<Device> limitedDevices = devices.stream()
                .limit(config.getQueryLimits().getMaxDevicesDetailed()).toList();
        List<UUID> deviceIds = limitedDevices.stream().map(Device::getId).collect(Collectors.toList());
        List<Variable> allVariables = variableRepository.findByDeviceIds(deviceIds);

        // Group variables by device ID
        Map<UUID, List<Variable>> variablesByDevice = allVariables.stream()
                .collect(Collectors.groupingBy(v -> v.getDevice().getId()));

        // Batch fetch: Get statistics for all variables in ONE query
        List<Long> variableIds = allVariables.stream().map(Variable::getId).collect(Collectors.toList());
        Map<Long, Object[]> statsMap = new HashMap<>();
        if (!variableIds.isEmpty()) {
            List<Object[]> batchStats = variableValueRepository.calculateBatchStatistics(variableIds, periodStart, periodEnd);
            for (Object[] row : batchStats) {
                Long varId = (Long) row[0];
                statsMap.put(varId, row);
            }
        }

        // Build context using pre-fetched data (no more DB queries in loop)
        for (Device device : limitedDevices) {
            context.append("### ").append(device.getName()).append("\n");
            if (device.getDeviceType() != null) {
                context.append("- Type: ").append(device.getDeviceType().getName()).append("\n");
            }
            context.append("- Status: ").append(device.getStatus()).append("\n");

            // Use pre-fetched variables
            List<Variable> variables = variablesByDevice.getOrDefault(device.getId(), Collections.emptyList());
            if (!variables.isEmpty()) {
                context.append("- Variables:\n");
                for (Variable var : variables.stream()
                        .limit(config.getQueryLimits().getMaxVariablesPerDevice()).toList()) {
                    Object[] stats = statsMap.get(var.getId());
                    Double avg = stats != null ? (Double) stats[1] : null;
                    Double min = stats != null ? (Double) stats[2] : null;
                    Double max = stats != null ? (Double) stats[3] : null;
                    Long count = stats != null ? (Long) stats[4] : 0L;

                    context.append("  - ").append(var.getName());
                    if (var.getUnit() != null) {
                        context.append(" (").append(var.getUnit()).append(")");
                    }
                    context.append(": ");

                    if (count != null && count > 0) {
                        context.append(String.format("Avg=%.2f, Min=%.2f, Max=%.2f, Points=%d",
                                avg != null ? avg : 0,
                                min != null ? min : 0,
                                max != null ? max : 0,
                                count));
                    } else {
                        context.append("No data in period");
                    }
                    context.append("\n");
                }
            }
            context.append("\n");
        }

        // Anomaly information for anomaly reports (using batch query to avoid N+1)
        if (reportType == ReportGenerationDto.ReportType.ANOMALY_REPORT) {
            context.append("## Anomaly Summary\n\n");

            // Batch fetch: Get all anomalies for all devices in ONE query
            List<Device> anomalyDevices = devices.stream().limit(10).toList();
            List<UUID> anomalyDeviceIds = anomalyDevices.stream().map(Device::getId).collect(Collectors.toList());
            List<MLAnomaly> allAnomalies = anomalyRepository.findByDeviceIdsAndTimeRange(
                    anomalyDeviceIds, periodStart, periodEnd);

            // Group anomalies by device ID
            Map<UUID, List<MLAnomaly>> anomaliesByDevice = allAnomalies.stream()
                    .collect(Collectors.groupingBy(a -> a.getDevice().getId()));

            // Build context using pre-fetched data (no more DB queries in loop)
            for (Device device : anomalyDevices) {
                List<MLAnomaly> anomalies = anomaliesByDevice.getOrDefault(device.getId(), Collections.emptyList());
                if (!anomalies.isEmpty()) {
                    context.append("### ").append(device.getName()).append(" - Anomalies\n");
                    Map<String, Long> severityCounts = anomalies.stream()
                            .collect(Collectors.groupingBy(
                                    a -> a.getSeverity() != null ? a.getSeverity().name() : "UNKNOWN",
                                    Collectors.counting()));
                    severityCounts.forEach((severity, count) ->
                            context.append("- ").append(severity).append(": ").append(count).append("\n"));

                    // Show recent anomalies (already sorted by detectedAt DESC from the query)
                    context.append("Recent anomalies:\n");
                    anomalies.stream().limit(5).forEach(a ->
                            context.append("  - ").append(DATE_FORMATTER.format(a.getDetectedAt()))
                                    .append(": Score=").append(String.format("%.2f", a.getAnomalyScore()))
                                    .append(", Type=").append(a.getAnomalyType())
                                    .append("\n"));
                    context.append("\n");
                }
            }
        }

        // Add specific prompts based on report type
        context.append("## Generation Instructions\n");
        context.append(switch (reportType) {
            case DAILY_SUMMARY -> """
                Generate a concise daily operational summary including:
                - Overall system health status
                - Notable events or readings
                - Any devices requiring attention
                - Key metrics compared to typical values
                """;
            case WEEKLY_REVIEW -> """
                Generate a weekly performance review including:
                - Week-over-week trends
                - Performance highlights and concerns
                - Maintenance recommendations
                - Energy efficiency observations
                """;
            case MONTHLY_ANALYSIS -> """
                Generate a comprehensive monthly analysis including:
                - Long-term trends and patterns
                - Month-over-month comparisons
                - Strategic recommendations
                - Capacity and planning insights
                """;
            case ANOMALY_REPORT -> """
                Generate an anomaly analysis report including:
                - Summary of detected anomalies by severity
                - Root cause hypotheses
                - Impact assessment
                - Remediation recommendations
                """;
            case DEVICE_HEALTH -> """
                Generate a device health report including:
                - Overall fleet health status
                - Devices requiring maintenance
                - Reliability metrics
                - Lifecycle recommendations
                """;
            case ENERGY_ANALYSIS -> """
                Generate an energy analysis report including:
                - Total energy consumption
                - Peak usage patterns
                - Efficiency metrics
                - Cost optimization recommendations
                """;
            case CUSTOM -> customPrompt != null ? customPrompt :
                "Generate a comprehensive report based on the provided data.";
        });

        return context.toString();
    }

    private ReportGenerationDto buildReportResponse(
            ReportGenerationDto.Request request,
            LLMResponse response,
            List<Device> devices,
            Instant periodStart,
            Instant periodEnd) {

        // Parse key findings and recommendations from the response
        List<String> keyFindings = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        String executiveSummary = "";

        if (response.isSuccess() && response.getContent() != null) {
            String content = response.getContent();

            // Extract executive summary
            int summaryStart = content.indexOf("## Executive Summary");
            int summaryEnd = content.indexOf("##", summaryStart + 20);
            if (summaryStart >= 0 && summaryEnd > summaryStart) {
                executiveSummary = content.substring(summaryStart + 20, summaryEnd).trim();
            }

            // Extract key findings (simple line-based extraction)
            int findingsStart = content.indexOf("## Key Findings");
            if (findingsStart >= 0) {
                int findingsEnd = content.indexOf("##", findingsStart + 15);
                if (findingsEnd < 0) findingsEnd = content.length();
                String findingsSection = content.substring(findingsStart + 15, findingsEnd);
                keyFindings = Arrays.stream(findingsSection.split("\n"))
                        .map(String::trim)
                        .filter(line -> line.startsWith("-") || line.startsWith("*"))
                        .map(line -> line.substring(1).trim())
                        .filter(line -> !line.isEmpty())
                        .limit(10)
                        .collect(Collectors.toList());
            }

            // Extract recommendations
            int recsStart = content.indexOf("## Recommendations");
            if (recsStart >= 0) {
                int recsEnd = content.indexOf("##", recsStart + 18);
                if (recsEnd < 0) recsEnd = content.length();
                String recsSection = content.substring(recsStart + 18, recsEnd);
                recommendations = Arrays.stream(recsSection.split("\n"))
                        .map(String::trim)
                        .filter(line -> line.matches("^[0-9]+\\..*") || line.startsWith("-"))
                        .map(line -> line.replaceFirst("^[0-9]+\\.\\s*", "").replaceFirst("^-\\s*", ""))
                        .filter(line -> !line.isEmpty())
                        .limit(10)
                        .collect(Collectors.toList());
            }
        }

        return ReportGenerationDto.builder()
                .reportId(UUID.randomUUID())
                .reportType(request.getReportType())
                .title(generateTitle(request.getReportType(), periodStart, periodEnd))
                .executiveSummary(executiveSummary)
                .content(response.isSuccess() ? response.getContent() : null)
                .keyFindings(keyFindings)
                .recommendations(recommendations)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .deviceIds(devices.stream().map(Device::getId).collect(Collectors.toList()))
                .success(response.isSuccess())
                .errorMessage(response.getErrorMessage())
                .provider(response.getProvider() != null ? response.getProvider().name() : null)
                .modelId(response.getModelId())
                .tokensUsed(response.getTotalTokens())
                .latencyMs(response.getLatencyMs())
                .generatedAt(Instant.now())
                .build();
    }

    private String generateTitle(ReportGenerationDto.ReportType reportType, Instant start, Instant end) {
        String dateRange = DATE_FORMATTER.format(start).substring(0, 10) + " to " +
                DATE_FORMATTER.format(end).substring(0, 10);
        return switch (reportType) {
            case DAILY_SUMMARY -> "Daily Operations Summary - " + dateRange;
            case WEEKLY_REVIEW -> "Weekly Performance Review - " + dateRange;
            case MONTHLY_ANALYSIS -> "Monthly Analysis Report - " + dateRange;
            case ANOMALY_REPORT -> "Anomaly Analysis Report - " + dateRange;
            case DEVICE_HEALTH -> "Device Health Report - " + dateRange;
            case ENERGY_ANALYSIS -> "Energy Analysis Report - " + dateRange;
            case CUSTOM -> "Custom Report - " + dateRange;
        };
    }
}
