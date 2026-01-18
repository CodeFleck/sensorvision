package io.indcloud.service.llm;

import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.model.LLMFeatureType;
import io.indcloud.model.LLMProvider;
import io.indcloud.model.LLMUsage;
import io.indcloud.model.Organization;
import io.indcloud.model.User;
import io.indcloud.repository.LLMUsageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Routes LLM requests to the appropriate provider and tracks usage.
 * Handles provider selection, fallback, and usage logging.
 */
@Service
@Slf4j
public class LLMServiceRouter {

    private final Map<LLMProvider, LLMService> providers;
    private final LLMUsageRepository usageRepository;
    private final LLMProvider defaultProvider;

    public LLMServiceRouter(
            List<LLMService> llmServices,
            LLMUsageRepository usageRepository,
            @Value("${llm.default-provider:CLAUDE}") String defaultProviderName) {

        this.providers = llmServices.stream()
                .collect(Collectors.toMap(LLMService::getProvider, Function.identity()));

        this.usageRepository = usageRepository;

        // Set default provider
        LLMProvider configuredDefault;
        try {
            configuredDefault = LLMProvider.valueOf(defaultProviderName.toUpperCase());
        } catch (IllegalArgumentException e) {
            log.warn("Invalid default provider '{}', falling back to CLAUDE", defaultProviderName);
            configuredDefault = LLMProvider.CLAUDE;
        }
        this.defaultProvider = configuredDefault;

        log.info("LLM Service Router initialized with {} providers, default: {}",
                providers.size(), defaultProvider);
        providers.forEach((provider, service) ->
                log.info("  - {}: {}", provider, service.isAvailable() ? "available" : "not configured"));
    }

    /**
     * Get the list of available (configured) providers.
     */
    public List<LLMProvider> getAvailableProviders() {
        return providers.entrySet().stream()
                .filter(e -> e.getValue().isAvailable())
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * Check if any provider is available.
     */
    public boolean isAnyProviderAvailable() {
        return providers.values().stream().anyMatch(LLMService::isAvailable);
    }

    /**
     * Send an LLM request using the specified or default provider.
     * Tracks usage for billing.
     *
     * @param request The LLM request
     * @param organization The organization making the request
     * @param user The user making the request (can be null for system requests)
     * @return The LLM response
     */
    public Mono<LLMResponse> complete(LLMRequest request, Organization organization, User user) {
        LLMProvider provider = request.getProvider() != null ? request.getProvider() : defaultProvider;

        LLMService service = providers.get(provider);
        if (service == null || !service.isAvailable()) {
            // Try fallback to any available provider
            Optional<LLMService> fallback = providers.values().stream()
                    .filter(LLMService::isAvailable)
                    .findFirst();

            if (fallback.isEmpty()) {
                return Mono.just(LLMResponse.failure(provider, "No LLM provider is configured"));
            }

            log.info("Provider {} not available, falling back to {}", provider, fallback.get().getProvider());
            service = fallback.get();
            provider = service.getProvider();
        }

        final LLMProvider finalProvider = provider;
        final LLMService finalService = service;

        return finalService.complete(request)
                .doOnNext(response -> trackUsage(response, request, organization, user, finalProvider));
    }

    /**
     * Send an LLM request with automatic provider selection.
     */
    public Mono<LLMResponse> complete(LLMRequest request, Organization organization) {
        return complete(request, organization, null);
    }

    /**
     * Get a specific provider service.
     */
    public Optional<LLMService> getProvider(LLMProvider provider) {
        LLMService service = providers.get(provider);
        return service != null && service.isAvailable() ? Optional.of(service) : Optional.empty();
    }

    /**
     * Get the default provider.
     */
    public LLMProvider getDefaultProvider() {
        return defaultProvider;
    }

    private void trackUsage(LLMResponse response, LLMRequest request,
                           Organization organization, User user, LLMProvider provider) {
        try {
            LLMUsage usage = LLMUsage.builder()
                    .organization(organization)
                    .user(user)
                    .provider(provider)
                    .modelId(response.getModelId() != null ? response.getModelId() :
                            providers.get(provider).getDefaultModelId())
                    .featureType(request.getFeatureType() != null ?
                            request.getFeatureType() : LLMFeatureType.ANOMALY_EXPLANATION)
                    .inputTokens(response.getInputTokens() != null ? response.getInputTokens() : 0)
                    .outputTokens(response.getOutputTokens() != null ? response.getOutputTokens() : 0)
                    .totalTokens(response.getTotalTokens() != null ? response.getTotalTokens() : 0)
                    .estimatedCostCents(response.getEstimatedCostCents())
                    .latencyMs(response.getLatencyMs())
                    .success(response.isSuccess())
                    .errorMessage(response.getErrorMessage())
                    .referenceType(request.getReferenceType())
                    .referenceId(request.getReferenceId())
                    .build();

            usageRepository.save(usage);

            log.debug("Tracked LLM usage: org={}, provider={}, tokens={}, cost={}¢",
                    organization.getId(), provider, usage.getTotalTokens(), usage.getEstimatedCostCents());

        } catch (Exception e) {
            log.error("Failed to track LLM usage: {}", e.getMessage());
            // Don't fail the request just because tracking failed
        }
    }
}
