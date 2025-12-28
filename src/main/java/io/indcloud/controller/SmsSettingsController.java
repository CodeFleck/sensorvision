package io.indcloud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.ApiResponse;
import io.indcloud.dto.SmsSettingsRequest;
import io.indcloud.dto.SmsSettingsResponse;
import io.indcloud.model.Organization;
import io.indcloud.model.OrganizationSmsSettings;
import io.indcloud.repository.OrganizationSmsSettingsRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * REST controller for managing organization SMS settings
 */
@RestController
@RequestMapping("/api/v1/sms-settings")
@RequiredArgsConstructor
@Slf4j
public class SmsSettingsController {

    private final OrganizationSmsSettingsRepository smsSettingsRepository;
    private final SecurityUtils securityUtils;

    /**
     * Get SMS settings for current organization
     */
    @GetMapping
    public ResponseEntity<SmsSettingsResponse> getSmsSettings() {
        Organization org = securityUtils.getCurrentUserOrganization();

        OrganizationSmsSettings settings = smsSettingsRepository
            .findByOrganizationId(org.getId())
            .orElseGet(() -> createDefaultSettings(org));

        return ResponseEntity.ok(toResponse(settings));
    }

    /**
     * Update SMS settings (admin only)
     */
    @PutMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SmsSettingsResponse>> updateSmsSettings(
            @Valid @RequestBody SmsSettingsRequest request) {
        Organization org = securityUtils.getCurrentUserOrganization();

        OrganizationSmsSettings settings = smsSettingsRepository
            .findByOrganizationId(org.getId())
            .orElseGet(() -> createDefaultSettings(org));

        // Update settings
        settings.setEnabled(request.enabled());
        settings.setDailyLimit(request.dailyLimit());
        settings.setMonthlyBudget(request.monthlyBudget());
        settings.setAlertOnBudgetThreshold(request.alertOnBudgetThreshold());
        settings.setBudgetThresholdPercentage(request.budgetThresholdPercentage());

        settings = smsSettingsRepository.save(settings);

        log.info("SMS settings updated for organization {}: enabled={}, dailyLimit={}, monthlyBudget={}",
            org.getId(), settings.getEnabled(), settings.getDailyLimit(), settings.getMonthlyBudget());

        return ResponseEntity.ok(ApiResponse.success(
            toResponse(settings),
            "SMS settings updated successfully"
        ));
    }

    /**
     * Reset monthly SMS counters (admin only)
     */
    @PostMapping("/reset-monthly-counters")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SmsSettingsResponse>> resetMonthlyCounters() {
        Organization org = securityUtils.getCurrentUserOrganization();

        OrganizationSmsSettings settings = smsSettingsRepository
            .findByOrganizationId(org.getId())
            .orElseThrow(() -> new IllegalArgumentException("SMS settings not found"));

        settings.setCurrentMonthCount(0);
        settings.setCurrentMonthCost(java.math.BigDecimal.ZERO);

        settings = smsSettingsRepository.save(settings);

        log.info("Monthly SMS counters reset for organization {}", org.getId());

        return ResponseEntity.ok(ApiResponse.success(
            toResponse(settings),
            "Monthly counters reset successfully"
        ));
    }

    /**
     * Create default settings for organization
     */
    private OrganizationSmsSettings createDefaultSettings(Organization org) {
        OrganizationSmsSettings settings = OrganizationSmsSettings.builder()
            .organization(org)
            .enabled(false)
            .dailyLimit(100)
            .monthlyBudget(new java.math.BigDecimal("50.00"))
            .currentMonthCount(0)
            .currentMonthCost(java.math.BigDecimal.ZERO)
            .alertOnBudgetThreshold(true)
            .budgetThresholdPercentage(80)
            .build();

        return smsSettingsRepository.save(settings);
    }

    /**
     * Convert entity to response DTO
     */
    private SmsSettingsResponse toResponse(OrganizationSmsSettings settings) {
        return new SmsSettingsResponse(
            settings.getId(),
            settings.getEnabled(),
            settings.getDailyLimit(),
            settings.getMonthlyBudget(),
            settings.getCurrentMonthCount(),
            settings.getCurrentMonthCost(),
            settings.getCurrentDayCount(),
            settings.getLastResetDate(),
            settings.getAlertOnBudgetThreshold(),
            settings.getBudgetThresholdPercentage(),
            settings.getCreatedAt(),
            settings.getUpdatedAt()
        );
    }
}
