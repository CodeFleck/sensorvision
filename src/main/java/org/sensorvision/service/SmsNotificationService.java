package org.sensorvision.service;

import com.twilio.Twilio;
import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.OrganizationSmsSettings;
import org.sensorvision.model.SmsDeliveryLog;
import org.sensorvision.repository.OrganizationSmsSettingsRepository;
import org.sensorvision.repository.SmsDeliveryLogRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Service for sending SMS notifications via Twilio.
 * Handles SMS delivery, budget controls, and delivery tracking.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class SmsNotificationService {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.from:}")
    private String fromNumber;

    @Value("${notification.sms.twilio.account-sid:}")
    private String accountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String authToken;

    @Value("${notification.sms.cost-per-message:0.0075}")
    private BigDecimal costPerMessage;

    private final SmsDeliveryLogRepository smsDeliveryLogRepository;
    private final OrganizationSmsSettingsRepository smsSettingsRepository;

    @PostConstruct
    public void init() {
        if (smsEnabled) {
            if (accountSid == null || accountSid.isBlank() || authToken == null || authToken.isBlank()) {
                log.warn("SMS enabled but Twilio credentials not configured. SMS notifications will be disabled.");
                smsEnabled = false;
                return;
            }
            try {
                Twilio.init(accountSid, authToken);
                log.info("SMS notifications enabled via Twilio (from: {})", fromNumber);
            } catch (Exception e) {
                log.error("Failed to initialize Twilio: {}", e.getMessage(), e);
                smsEnabled = false;
            }
        } else {
            log.info("SMS notifications disabled");
        }
    }

    /**
     * Send SMS notification for an alert
     */
    @Transactional
    public SmsDeliveryLog sendSms(Alert alert, String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("SMS notifications disabled, skipping SMS for alert {}", alert.getId());
            return null;
        }

        // Get organization from alert
        Long organizationId = alert.getRule().getDevice().getOrganization().getId();

        // Check organization SMS settings
        OrganizationSmsSettings settings = smsSettingsRepository
            .findByOrganizationId(organizationId)
            .orElse(null);

        if (settings == null || !settings.getEnabled()) {
            log.warn("SMS not enabled for organization {}", organizationId);
            return null;
        }

        // Check daily limit
        if (settings.getCurrentMonthCount() >= settings.getDailyLimit()) {
            log.warn("Daily SMS limit reached for organization {}", organizationId);
            return createFailedLog(alert, phoneNumber, message, "DAILY_LIMIT_EXCEEDED", null);
        }

        // Check monthly budget
        if (settings.getCurrentMonthCost().compareTo(settings.getMonthlyBudget()) >= 0) {
            log.warn("Monthly SMS budget exceeded for organization {}", organizationId);
            return createFailedLog(alert, phoneNumber, message, "BUDGET_EXCEEDED", null);
        }

        try {
            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();

            // Log delivery
            SmsDeliveryLog deliveryLog = SmsDeliveryLog.builder()
                .alert(alert)
                .phoneNumber(phoneNumber)
                .messageBody(message)
                .twilioSid(twilioMessage.getSid())
                .status(twilioMessage.getStatus().name())
                .cost(costPerMessage)
                .sentAt(Instant.now())
                .build();

            deliveryLog = smsDeliveryLogRepository.save(deliveryLog);

            // Update organization SMS stats
            updateOrganizationStats(settings, costPerMessage);

            log.info("SMS sent to {} for alert {}: SID={}", phoneNumber, alert.getId(), twilioMessage.getSid());
            return deliveryLog;

        } catch (TwilioException e) {
            log.error("Failed to send SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return createFailedLog(alert, phoneNumber, message, e.getCode() != null ? String.valueOf(e.getCode()) : null, e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error sending SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return createFailedLog(alert, phoneNumber, message, "SYSTEM_ERROR", e.getMessage());
        }
    }

    /**
     * Send SMS to multiple recipients
     */
    @Transactional
    public List<SmsDeliveryLog> sendSmsToMultiple(Alert alert, List<String> phoneNumbers, String message) {
        return phoneNumbers.stream()
            .map(phone -> sendSms(alert, phone, message))
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }

    /**
     * Format alert message for SMS (160 char limit for single segment)
     */
    public String formatAlertMessage(Alert alert) {
        String deviceName = alert.getRule().getDevice().getName();
        String ruleName = alert.getRule().getName();
        String severity = alert.getSeverity().name();
        String message = alert.getMessage();

        // Target 160 characters to avoid multi-segment costs
        // Format: [SEVERITY] Device: Rule - Message
        String formatted = String.format(
            "[%s] %s: %s - %s",
            severity,
            deviceName,
            ruleName,
            message
        );

        // Truncate if too long
        if (formatted.length() > 160) {
            int maxMessageLength = 160 - severity.length() - deviceName.length() - ruleName.length() - 10; // 10 for formatting chars
            if (maxMessageLength > 3) {
                String truncatedMessage = message.length() > maxMessageLength
                    ? message.substring(0, maxMessageLength - 3) + "..."
                    : message;
                formatted = String.format(
                    "[%s] %s: %s - %s",
                    severity,
                    deviceName,
                    ruleName,
                    truncatedMessage
                );
            } else {
                // Extreme case: just show severity and device
                formatted = String.format("[%s] %s alert", severity, deviceName);
            }
        }

        return formatted;
    }

    /**
     * Create a failed delivery log entry
     */
    private SmsDeliveryLog createFailedLog(Alert alert, String phoneNumber, String message, String errorCode, String errorMessage) {
        SmsDeliveryLog deliveryLog = SmsDeliveryLog.builder()
            .alert(alert)
            .phoneNumber(phoneNumber)
            .messageBody(message)
            .status("FAILED")
            .errorCode(errorCode)
            .errorMessage(errorMessage)
            .sentAt(Instant.now())
            .build();
        return smsDeliveryLogRepository.save(deliveryLog);
    }

    /**
     * Update organization SMS statistics
     */
    private void updateOrganizationStats(OrganizationSmsSettings settings, BigDecimal cost) {
        settings.setCurrentMonthCount(settings.getCurrentMonthCount() + 1);
        settings.setCurrentMonthCost(settings.getCurrentMonthCost().add(cost));

        // Alert if approaching budget threshold
        if (settings.getAlertOnBudgetThreshold()) {
            BigDecimal threshold = settings.getMonthlyBudget()
                .multiply(new BigDecimal(settings.getBudgetThresholdPercentage()))
                .divide(new BigDecimal(100), 2, RoundingMode.HALF_UP);

            if (settings.getCurrentMonthCost().compareTo(threshold) >= 0) {
                log.warn("Organization {} has reached {}% of SMS budget ({}/{})",
                    settings.getOrganization().getId(),
                    settings.getBudgetThresholdPercentage(),
                    settings.getCurrentMonthCost(),
                    settings.getMonthlyBudget());
                // TODO: Send admin notification via email
            }
        }

        smsSettingsRepository.save(settings);
    }

    /**
     * Check if SMS is enabled globally
     */
    public boolean isSmsEnabled() {
        return smsEnabled;
    }

    /**
     * Check if SMS is enabled for a specific organization
     */
    public boolean isSmsEnabledForOrganization(Long organizationId) {
        if (!smsEnabled) {
            return false;
        }
        return smsSettingsRepository.existsByOrganizationIdAndEnabledTrue(organizationId);
    }

    /**
     * Get SMS settings for an organization
     */
    public OrganizationSmsSettings getOrganizationSettings(Long organizationId) {
        return smsSettingsRepository.findByOrganizationId(organizationId).orElse(null);
    }

    /**
     * Send verification SMS (bypasses organization budget checks)
     * Used for phone number verification via OTP
     */
    public boolean sendVerificationSms(String phoneNumber, String message) {
        if (!smsEnabled) {
            log.warn("SMS notifications disabled, cannot send verification SMS to {}", phoneNumber);
            return false;
        }

        try {
            // Send SMS via Twilio
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();

            log.info("Verification SMS sent to {}: SID={}", phoneNumber, twilioMessage.getSid());
            return true;

        } catch (TwilioException e) {
            log.error("Failed to send verification SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        } catch (Exception e) {
            log.error("Unexpected error sending verification SMS to {}: {}", phoneNumber, e.getMessage(), e);
            return false;
        }
    }
}
