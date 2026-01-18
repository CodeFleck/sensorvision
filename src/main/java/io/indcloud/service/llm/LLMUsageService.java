package io.indcloud.service.llm;

import io.indcloud.dto.llm.LLMUsageStatsDto;
import io.indcloud.model.LLMFeatureType;
import io.indcloud.model.LLMProvider;
import io.indcloud.model.LLMUsage;
import io.indcloud.repository.LLMUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for LLM usage tracking, billing, and analytics.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LLMUsageService {

    private final LLMUsageRepository usageRepository;

    /**
     * Get usage history for an organization.
     */
    public Page<LLMUsage> getUsageHistory(Long organizationId, Pageable pageable) {
        return usageRepository.findByOrganizationIdOrderByCreatedAtDesc(organizationId, pageable);
    }

    /**
     * Get usage history for a user.
     */
    public Page<LLMUsage> getUserUsageHistory(Long userId, Pageable pageable) {
        return usageRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    /**
     * Get comprehensive usage statistics for an organization.
     */
    public LLMUsageStatsDto getUsageStats(Long organizationId, int daysBack) {
        Instant since = Instant.now().minus(daysBack, ChronoUnit.DAYS);

        Long totalTokens = usageRepository.getTotalTokensForOrganization(organizationId, since);
        Long totalCostCents = usageRepository.getTotalCostCentsForOrganization(organizationId, since);
        Long successfulRequests = usageRepository.countSuccessfulRequests(organizationId, since);
        Long failedRequests = usageRepository.countFailedRequests(organizationId, since);
        Double avgLatency = usageRepository.getAverageLatency(organizationId, since);

        // Get breakdown by provider
        List<Object[]> providerBreakdown = usageRepository.getUsageByProvider(organizationId, since);
        Map<String, LLMUsageStatsDto.ProviderStats> byProvider = providerBreakdown.stream()
                .collect(Collectors.toMap(
                        row -> ((LLMProvider) row[0]).name(),
                        row -> LLMUsageStatsDto.ProviderStats.builder()
                                .provider(((LLMProvider) row[0]).name())
                                .totalTokens(((Number) row[1]).longValue())
                                .totalCostCents(((Number) row[2]).longValue())
                                .requestCount(((Number) row[3]).longValue())
                                .build()
                ));

        // Get breakdown by feature
        List<Object[]> featureBreakdown = usageRepository.getUsageByFeature(organizationId, since);
        Map<String, LLMUsageStatsDto.FeatureStats> byFeature = featureBreakdown.stream()
                .collect(Collectors.toMap(
                        row -> ((LLMFeatureType) row[0]).name(),
                        row -> LLMUsageStatsDto.FeatureStats.builder()
                                .featureType(((LLMFeatureType) row[0]).name())
                                .totalTokens(((Number) row[1]).longValue())
                                .totalCostCents(((Number) row[2]).longValue())
                                .requestCount(((Number) row[3]).longValue())
                                .build()
                ));

        // Get daily usage for chart
        List<Object[]> dailyData = usageRepository.getDailyUsage(organizationId, since);
        List<LLMUsageStatsDto.DailyUsage> dailyUsage = dailyData.stream()
                .map(row -> LLMUsageStatsDto.DailyUsage.builder()
                        .date(row[0].toString())
                        .totalTokens(((Number) row[1]).longValue())
                        .totalCostCents(((Number) row[2]).longValue())
                        .requestCount(((Number) row[3]).longValue())
                        .build())
                .collect(Collectors.toList());

        return LLMUsageStatsDto.builder()
                .periodDays(daysBack)
                .totalTokens(totalTokens)
                .totalCostCents(totalCostCents)
                .totalCostDollars(totalCostCents / 100.0)
                .successfulRequests(successfulRequests)
                .failedRequests(failedRequests)
                .totalRequests(successfulRequests + failedRequests)
                .successRate(successfulRequests + failedRequests > 0 ?
                        (double) successfulRequests / (successfulRequests + failedRequests) * 100 : 100.0)
                .averageLatencyMs(avgLatency != null ? avgLatency.intValue() : 0)
                .byProvider(byProvider)
                .byFeature(byFeature)
                .dailyUsage(dailyUsage)
                .build();
    }

    /**
     * Check if an organization has exceeded their token quota.
     * This is a placeholder for subscription-based limits.
     *
     * @param organizationId The organization to check
     * @param monthlyQuota The monthly token quota (0 = unlimited)
     * @return true if quota exceeded, false otherwise
     */
    public boolean isQuotaExceeded(Long organizationId, long monthlyQuota) {
        if (monthlyQuota <= 0) {
            return false; // Unlimited
        }

        Instant startOfMonth = Instant.now().truncatedTo(ChronoUnit.DAYS)
                .minus(Instant.now().atZone(java.time.ZoneOffset.UTC).getDayOfMonth() - 1, ChronoUnit.DAYS);

        Long usedTokens = usageRepository.getTotalTokensForOrganization(organizationId, startOfMonth);
        return usedTokens >= monthlyQuota;
    }

    /**
     * Get remaining tokens for an organization this month.
     *
     * @param organizationId The organization
     * @param monthlyQuota The monthly token quota
     * @return Remaining tokens, or -1 for unlimited
     */
    public long getRemainingTokens(Long organizationId, long monthlyQuota) {
        if (monthlyQuota <= 0) {
            return -1; // Unlimited
        }

        Instant startOfMonth = Instant.now().truncatedTo(ChronoUnit.DAYS)
                .minus(Instant.now().atZone(java.time.ZoneOffset.UTC).getDayOfMonth() - 1, ChronoUnit.DAYS);

        Long usedTokens = usageRepository.getTotalTokensForOrganization(organizationId, startOfMonth);
        return Math.max(0, monthlyQuota - usedTokens);
    }
}
