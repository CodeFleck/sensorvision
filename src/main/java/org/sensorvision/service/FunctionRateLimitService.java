package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.FunctionExecutionQuota;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.FunctionExecutionQuotaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;

/**
 * Service for rate limiting function executions.
 * Enforces quota limits per organization across different time windows.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FunctionRateLimitService {

    private final FunctionExecutionQuotaRepository quotaRepository;

    /**
     * Check if execution is allowed and increment counters if so.
     * Returns true if execution is allowed, false if rate limit exceeded.
     */
    @Transactional
    public boolean checkAndIncrementQuota(Organization organization) {
        // Get or create quota for organization
        FunctionExecutionQuota quota = quotaRepository
            .findByOrganizationId(organization.getId())
            .orElseGet(() -> createDefaultQuota(organization));

        // Reset counters if periods have expired
        Instant now = Instant.now();
        resetExpiredCounters(quota, now);

        // Check all quota limits
        if (quota.getCurrentMinuteCount() >= quota.getExecutionsPerMinute()) {
            log.warn("Rate limit exceeded for organization {}: {} executions per minute",
                organization.getId(), quota.getExecutionsPerMinute());
            return false;
        }

        if (quota.getCurrentHourCount() >= quota.getExecutionsPerHour()) {
            log.warn("Rate limit exceeded for organization {}: {} executions per hour",
                organization.getId(), quota.getExecutionsPerHour());
            return false;
        }

        if (quota.getCurrentDayCount() >= quota.getExecutionsPerDay()) {
            log.warn("Rate limit exceeded for organization {}: {} executions per day",
                organization.getId(), quota.getExecutionsPerDay());
            return false;
        }

        if (quota.getCurrentMonthCount() >= quota.getExecutionsPerMonth()) {
            log.warn("Rate limit exceeded for organization {}: {} executions per month",
                organization.getId(), quota.getExecutionsPerMonth());
            return false;
        }

        // All checks passed, increment counters
        quota.setCurrentMinuteCount(quota.getCurrentMinuteCount() + 1);
        quota.setCurrentHourCount(quota.getCurrentHourCount() + 1);
        quota.setCurrentDayCount(quota.getCurrentDayCount() + 1);
        quota.setCurrentMonthCount(quota.getCurrentMonthCount() + 1);

        quotaRepository.save(quota);
        return true;
    }

    /**
     * Get current quota status for an organization.
     */
    @Transactional(readOnly = true)
    public QuotaStatus getQuotaStatus(Organization organization) {
        FunctionExecutionQuota quota = quotaRepository
            .findByOrganization(organization)
            .orElseGet(() -> createDefaultQuota(organization));

        Instant now = Instant.now();
        resetExpiredCounters(quota, now);

        return QuotaStatus.builder()
            .executionsPerMinute(quota.getExecutionsPerMinute())
            .executionsPerHour(quota.getExecutionsPerHour())
            .executionsPerDay(quota.getExecutionsPerDay())
            .executionsPerMonth(quota.getExecutionsPerMonth())
            .currentMinuteCount(quota.getCurrentMinuteCount())
            .currentHourCount(quota.getCurrentHourCount())
            .currentDayCount(quota.getCurrentDayCount())
            .currentMonthCount(quota.getCurrentMonthCount())
            .minuteResetAt(quota.getMinuteResetAt())
            .hourResetAt(quota.getHourResetAt())
            .dayResetAt(quota.getDayResetAt())
            .monthResetAt(quota.getMonthResetAt())
            .build();
    }

    /**
     * Update quota limits for an organization.
     */
    @Transactional
    public void updateQuotaLimits(
            Organization organization,
            Integer executionsPerMinute,
            Integer executionsPerHour,
            Integer executionsPerDay,
            Integer executionsPerMonth) {

        FunctionExecutionQuota quota = quotaRepository
            .findByOrganization(organization)
            .orElseGet(() -> createDefaultQuota(organization));

        if (executionsPerMinute != null) {
            quota.setExecutionsPerMinute(executionsPerMinute);
        }
        if (executionsPerHour != null) {
            quota.setExecutionsPerHour(executionsPerHour);
        }
        if (executionsPerDay != null) {
            quota.setExecutionsPerDay(executionsPerDay);
        }
        if (executionsPerMonth != null) {
            quota.setExecutionsPerMonth(executionsPerMonth);
        }

        quotaRepository.save(quota);
        log.info("Updated quota limits for organization {}", organization.getId());
    }

    /**
     * Reset expired counters based on current time.
     */
    private void resetExpiredCounters(FunctionExecutionQuota quota, Instant now) {
        // Reset minute counter if 1 minute has passed
        if (Duration.between(quota.getMinuteResetAt(), now).toMinutes() >= 1) {
            quota.setCurrentMinuteCount(0);
            quota.setMinuteResetAt(now);
        }

        // Reset hour counter if 1 hour has passed
        if (Duration.between(quota.getHourResetAt(), now).toHours() >= 1) {
            quota.setCurrentHourCount(0);
            quota.setHourResetAt(now);
        }

        // Reset day counter if 1 day has passed
        if (Duration.between(quota.getDayResetAt(), now).toDays() >= 1) {
            quota.setCurrentDayCount(0);
            quota.setDayResetAt(now);
        }

        // Reset month counter if 30 days have passed (approximate)
        if (Duration.between(quota.getMonthResetAt(), now).toDays() >= 30) {
            quota.setCurrentMonthCount(0);
            quota.setMonthResetAt(now);
        }
    }

    /**
     * Create default quota for organization.
     */
    private FunctionExecutionQuota createDefaultQuota(Organization organization) {
        FunctionExecutionQuota quota = new FunctionExecutionQuota();
        quota.setOrganization(organization);
        // Default limits are set in entity
        return quotaRepository.save(quota);
    }

    /**
     * Quota status DTO.
     */
    @lombok.Builder
    @lombok.Data
    public static class QuotaStatus {
        private Integer executionsPerMinute;
        private Integer executionsPerHour;
        private Integer executionsPerDay;
        private Integer executionsPerMonth;
        private Integer currentMinuteCount;
        private Integer currentHourCount;
        private Integer currentDayCount;
        private Integer currentMonthCount;
        private Instant minuteResetAt;
        private Instant hourResetAt;
        private Instant dayResetAt;
        private Instant monthResetAt;

        public int getRemainingMinute() {
            return Math.max(0, executionsPerMinute - currentMinuteCount);
        }

        public int getRemainingHour() {
            return Math.max(0, executionsPerHour - currentHourCount);
        }

        public int getRemainingDay() {
            return Math.max(0, executionsPerDay - currentDayCount);
        }

        public int getRemainingMonth() {
            return Math.max(0, executionsPerMonth - currentMonthCount);
        }
    }
}
