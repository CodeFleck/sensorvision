package io.indcloud.controller;

import io.indcloud.config.LLMConfigurationProperties;
import io.indcloud.dto.llm.AnomalyExplanationDto;
import io.indcloud.dto.llm.LLMUsageStatsDto;
import io.indcloud.dto.llm.NaturalLanguageQueryDto;
import io.indcloud.dto.llm.ReportGenerationDto;
import io.indcloud.dto.llm.RootCauseAnalysisDto;
import io.indcloud.model.User;
import io.indcloud.service.llm.AnomalyExplanationService;
import io.indcloud.service.llm.LLMUsageService;
import io.indcloud.service.llm.NaturalLanguageQueryService;
import io.indcloud.service.llm.PromptSanitizer;
import io.indcloud.service.llm.ReportGenerationService;
import io.indcloud.service.llm.RootCauseAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for LLM-powered features.
 * Provides endpoints for anomaly explanations, natural language queries,
 * report generation, and usage statistics.
 */
@RestController
@RequestMapping("/api/v1/llm")
@RequiredArgsConstructor
@Slf4j
public class LLMController {

    private final AnomalyExplanationService anomalyExplanationService;
    private final NaturalLanguageQueryService naturalLanguageQueryService;
    private final ReportGenerationService reportGenerationService;
    private final RootCauseAnalysisService rootCauseAnalysisService;
    private final LLMUsageService llmUsageService;
    private final LLMConfigurationProperties config;
    private final PromptSanitizer promptSanitizer;

    /**
     * Generate an AI-powered explanation for a specific anomaly.
     * Uses POST because this operation has side effects (consumes LLM tokens, affects billing).
     *
     * @param anomalyId The ID of the anomaly to explain
     * @param user The authenticated user
     * @return The anomaly explanation
     */
    @PostMapping("/anomalies/{anomalyId}/explain")
    public Mono<ResponseEntity<AnomalyExplanationDto>> explainAnomaly(
            @PathVariable UUID anomalyId,
            @AuthenticationPrincipal User user) {

        log.info("Generating explanation for anomaly {} by user {}", anomalyId, user.getUsername());

        return anomalyExplanationService.explainAnomaly(anomalyId, user.getOrganization(), user)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Anomaly not found: {}", anomalyId);
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("Access denied to anomaly {} for user {}", anomalyId, user.getUsername());
                    return Mono.just(ResponseEntity.status(403).build());
                })
                .onErrorResume(e -> {
                    log.error("Error explaining anomaly {}: {}", anomalyId, e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(AnomalyExplanationDto.builder()
                                    .anomalyId(anomalyId)
                                    .success(false)
                                    .errorMessage("An error occurred while generating the explanation. Please try again.")
                                    .build()));
                });
    }

    /**
     * Get AI-generated explanations for multiple anomalies.
     *
     * @param anomalyIds List of anomaly IDs to explain
     * @param user The authenticated user
     * @return List of anomaly explanations
     */
    @PostMapping("/anomalies/explain-batch")
    public Mono<ResponseEntity<List<AnomalyExplanationDto>>> explainAnomalies(
            @RequestBody List<UUID> anomalyIds,
            @AuthenticationPrincipal User user) {

        log.info("Generating explanations for {} anomalies by user {}", anomalyIds.size(), user.getUsername());

        int maxBatch = config.getBatchLimits().getMaxBatchAnomalies();
        if (anomalyIds.size() > maxBatch) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(List.of(AnomalyExplanationDto.builder()
                            .success(false)
                            .errorMessage("Maximum " + maxBatch + " anomalies per batch request")
                            .build())));
        }

        return anomalyExplanationService.explainAnomalies(anomalyIds, user.getOrganization(), user)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error explaining anomalies batch: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError().build());
                });
    }

    /**
     * Process a natural language query about IoT data.
     *
     * @param request The query request containing the question and optional filters
     * @param user The authenticated user
     * @return The query response with AI-generated answer
     */
    @PostMapping("/query")
    public Mono<ResponseEntity<NaturalLanguageQueryDto>> processQuery(
            @RequestBody NaturalLanguageQueryDto.Request request,
            @AuthenticationPrincipal User user) {

        log.info("Processing NL query for user {}: {}", user.getUsername(), request.getQuery());

        if (request.getQuery() == null || request.getQuery().trim().isEmpty()) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(NaturalLanguageQueryDto.builder()
                            .success(false)
                            .errorMessage("Query cannot be empty")
                            .build()));
        }

        int maxQueryLength = config.getValidation().getMaxQueryLength();
        if (request.getQuery().length() > maxQueryLength) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(NaturalLanguageQueryDto.builder()
                            .query(request.getQuery().substring(0, 100) + "...")
                            .success(false)
                            .errorMessage("Query exceeds maximum length of " + maxQueryLength + " characters")
                            .build()));
        }

        // Sanitize the query to prevent prompt injection
        String sanitizedQuery = promptSanitizer.sanitizeQuery(request.getQuery());
        NaturalLanguageQueryDto.Request sanitizedRequest = NaturalLanguageQueryDto.Request.builder()
                .query(sanitizedQuery)
                .deviceIds(request.getDeviceIds())
                .fromTime(request.getFromTime())
                .toTime(request.getToTime())
                .build();

        return naturalLanguageQueryService.processQuery(sanitizedRequest, user.getOrganization(), user)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error processing NL query: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(NaturalLanguageQueryDto.builder()
                                    .query(request.getQuery())
                                    .success(false)
                                    .errorMessage("An error occurred while processing your query. Please try again.")
                                    .build()));
                });
    }

    /**
     * Generate an AI-powered report.
     *
     * @param request The report generation request
     * @param user The authenticated user
     * @return The generated report
     */
    @PostMapping("/reports/generate")
    public Mono<ResponseEntity<ReportGenerationDto>> generateReport(
            @RequestBody ReportGenerationDto.Request request,
            @AuthenticationPrincipal User user) {

        log.info("Generating {} report for user {}", request.getReportType(), user.getUsername());

        if (request.getReportType() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ReportGenerationDto.builder()
                            .success(false)
                            .errorMessage("Report type is required")
                            .build()));
        }

        if (request.getReportType() == ReportGenerationDto.ReportType.CUSTOM
                && (request.getCustomPrompt() == null || request.getCustomPrompt().trim().isEmpty())) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ReportGenerationDto.builder()
                            .reportType(request.getReportType())
                            .success(false)
                            .errorMessage("Custom prompt is required for CUSTOM report type")
                            .build()));
        }

        // Validate custom prompt length
        int maxCustomPromptLength = config.getValidation().getMaxCustomPromptLength();
        if (request.getCustomPrompt() != null && request.getCustomPrompt().length() > maxCustomPromptLength) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(ReportGenerationDto.builder()
                            .reportType(request.getReportType())
                            .success(false)
                            .errorMessage("Custom prompt exceeds maximum length of " + maxCustomPromptLength + " characters")
                            .build()));
        }

        // Sanitize custom prompt if present to prevent prompt injection
        ReportGenerationDto.Request sanitizedRequest = request;
        if (request.getCustomPrompt() != null) {
            String sanitizedPrompt = promptSanitizer.sanitizeCustomPrompt(request.getCustomPrompt());
            sanitizedRequest = ReportGenerationDto.Request.builder()
                    .reportType(request.getReportType())
                    .deviceIds(request.getDeviceIds())
                    .periodStart(request.getPeriodStart())
                    .periodEnd(request.getPeriodEnd())
                    .customPrompt(sanitizedPrompt)
                    .build();
        }

        return reportGenerationService.generateReport(sanitizedRequest, user.getOrganization(), user)
                .map(ResponseEntity::ok)
                .onErrorResume(e -> {
                    log.error("Error generating report: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ReportGenerationDto.builder()
                                    .reportType(request.getReportType())
                                    .success(false)
                                    .errorMessage("An error occurred while generating the report. Please try again.")
                                    .build()));
                });
    }

    /**
     * Get available report types.
     *
     * @return List of available report types with descriptions
     */
    @GetMapping("/reports/types")
    public ResponseEntity<List<ReportTypeInfo>> getReportTypes() {
        return ResponseEntity.ok(List.of(
                new ReportTypeInfo(ReportGenerationDto.ReportType.DAILY_SUMMARY,
                        "Daily Summary", "Concise daily operational summary"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.WEEKLY_REVIEW,
                        "Weekly Review", "Weekly performance review with trends"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.MONTHLY_ANALYSIS,
                        "Monthly Analysis", "Comprehensive monthly analysis with strategic insights"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.ANOMALY_REPORT,
                        "Anomaly Report", "Analysis of detected anomalies and root causes"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.DEVICE_HEALTH,
                        "Device Health", "Fleet health status and maintenance recommendations"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.ENERGY_ANALYSIS,
                        "Energy Analysis", "Energy consumption patterns and optimization"),
                new ReportTypeInfo(ReportGenerationDto.ReportType.CUSTOM,
                        "Custom Report", "Custom report based on your specific requirements")
        ));
    }

    /**
     * Report type information DTO.
     */
    public record ReportTypeInfo(
            ReportGenerationDto.ReportType type,
            String name,
            String description
    ) {}

    /**
     * Perform root cause analysis on an alert or anomaly.
     *
     * @param request The analysis request
     * @param user The authenticated user
     * @return The root cause analysis
     */
    @PostMapping("/root-cause/analyze")
    public Mono<ResponseEntity<RootCauseAnalysisDto>> analyzeRootCause(
            @RequestBody RootCauseAnalysisDto.Request request,
            @AuthenticationPrincipal User user) {

        log.info("Performing root cause analysis for {} {} by user {}",
                request.getSourceType(), request.getSourceId(), user.getUsername());

        if (request.getSourceId() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RootCauseAnalysisDto.builder()
                            .success(false)
                            .errorMessage("Source ID is required")
                            .build()));
        }

        if (request.getSourceType() == null) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RootCauseAnalysisDto.builder()
                            .sourceId(request.getSourceId())
                            .success(false)
                            .errorMessage("Source type is required")
                            .build()));
        }

        // Validate additional context length
        int maxAdditionalContextLength = config.getValidation().getMaxAdditionalContextLength();
        if (request.getAdditionalContext() != null && request.getAdditionalContext().length() > maxAdditionalContextLength) {
            return Mono.just(ResponseEntity.badRequest()
                    .body(RootCauseAnalysisDto.builder()
                            .sourceId(request.getSourceId())
                            .sourceType(request.getSourceType())
                            .success(false)
                            .errorMessage("Additional context exceeds maximum length of " + maxAdditionalContextLength + " characters")
                            .build()));
        }

        // Sanitize additional context if present to prevent prompt injection
        RootCauseAnalysisDto.Request sanitizedRequest = request;
        if (request.getAdditionalContext() != null) {
            String sanitizedContext = promptSanitizer.sanitize(
                    request.getAdditionalContext(), "additionalContext");
            sanitizedRequest = RootCauseAnalysisDto.Request.builder()
                    .sourceId(request.getSourceId())
                    .sourceType(request.getSourceType())
                    .lookbackHours(request.getLookbackHours())
                    .additionalContext(sanitizedContext)
                    .build();
        }

        return rootCauseAnalysisService.analyzeRootCause(sanitizedRequest, user.getOrganization(), user)
                .map(ResponseEntity::ok)
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.warn("Source not found: {} {}", request.getSourceType(), request.getSourceId());
                    return Mono.just(ResponseEntity.notFound().build());
                })
                .onErrorResume(SecurityException.class, e -> {
                    log.warn("Access denied to {} {} for user {}",
                            request.getSourceType(), request.getSourceId(), user.getUsername());
                    return Mono.just(ResponseEntity.status(403).build());
                })
                .onErrorResume(e -> {
                    log.error("Error performing root cause analysis: {}", e.getMessage(), e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(RootCauseAnalysisDto.builder()
                                    .sourceId(request.getSourceId())
                                    .sourceType(request.getSourceType())
                                    .success(false)
                                    .errorMessage("An error occurred while performing root cause analysis. Please try again.")
                                    .build()));
                });
    }

    /**
     * Get LLM usage statistics for the current organization.
     *
     * @param daysBack Number of days to look back (default 30)
     * @param user The authenticated user
     * @return Usage statistics
     */
    @GetMapping("/usage/stats")
    public ResponseEntity<LLMUsageStatsDto> getUsageStats(
            @RequestParam(defaultValue = "30") int daysBack,
            @AuthenticationPrincipal User user) {

        log.info("Getting LLM usage stats for org {} ({} days)", user.getOrganization().getId(), daysBack);

        if (daysBack < 1 || daysBack > 365) {
            daysBack = 30;
        }

        LLMUsageStatsDto stats = llmUsageService.getUsageStats(user.getOrganization().getId(), daysBack);
        return ResponseEntity.ok(stats);
    }

    /**
     * Check remaining token quota for the current organization.
     *
     * @param user The authenticated user
     * @return Remaining tokens or -1 for unlimited
     */
    @GetMapping("/usage/remaining")
    public ResponseEntity<RemainingQuotaResponse> getRemainingQuota(
            @AuthenticationPrincipal User user) {

        // TODO: Get monthly quota from subscription/organization settings
        // For now, use a default quota (0 = unlimited for early access)
        long monthlyQuota = 0; // Unlimited during beta

        long remaining = llmUsageService.getRemainingTokens(user.getOrganization().getId(), monthlyQuota);
        boolean unlimited = remaining == -1;

        return ResponseEntity.ok(new RemainingQuotaResponse(
                remaining,
                unlimited,
                monthlyQuota
        ));
    }

    /**
     * Response DTO for remaining quota endpoint.
     */
    public record RemainingQuotaResponse(
            long remainingTokens,
            boolean unlimited,
            long monthlyQuota
    ) {}
}
