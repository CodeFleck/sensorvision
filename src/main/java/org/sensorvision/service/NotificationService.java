package org.sensorvision.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.*;
import org.sensorvision.repository.NotificationLogRepository;
import org.sensorvision.repository.UserNotificationPreferenceRepository;
import org.sensorvision.repository.UserPhoneNumberRepository;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final UserNotificationPreferenceRepository preferenceRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final WebhookNotificationService webhookService;
    private final SlackNotificationService slackService;
    private final TeamsNotificationService teamsService;
    private final EventService eventService;
    private final UserPhoneNumberRepository userPhoneNumberRepository;

    /**
     * Send notifications for an alert to all users with appropriate preferences
     */
    @Async
    @Transactional
    public void sendAlertNotifications(Alert alert) {
        log.info("Processing notifications for alert: {}", alert.getId());

        // Get device owner's organization - add null check
        Organization organization = alert.getDevice().getOrganization();
        if (organization == null) {
            log.warn("Cannot send notification for alert {} - device {} has no organization",
                    alert.getId(), alert.getDevice().getExternalId());
            return;
        }

        try {
            // Emit event for in-app notifications
            eventService.createEvent(
                    organization,
                    Event.EventType.ALERT_CREATED,
                    mapAlertSeverityToEventSeverity(alert.getSeverity()),
                    String.format("Alert: %s", alert.getMessage()),
                    String.format("Alert triggered for device %s", alert.getDevice().getName())
            );

            // Send email notifications to all users in the organization with email preferences enabled
            List<UserNotificationPreference> emailPreferences = preferenceRepository
                    .findByUserOrganizationAndChannelAndEnabledTrue(
                            organization,
                            NotificationChannel.EMAIL
                    );

            log.info("Found {} users with email notifications enabled for organization {}",
                    emailPreferences.size(), organization.getName());

            for (UserNotificationPreference pref : emailPreferences) {
                try {
                    sendNotificationToUser(pref.getUser(), alert);
                } catch (Exception e) {
                    log.error("Failed to send email notification to user: {}",
                            pref.getUser().getUsername(), e);
                    // Continue sending to other users even if one fails
                }
            }

            // Send webhook notifications (Slack, Teams)
            slackService.sendAlertNotification(alert);
            teamsService.sendAlertNotification(alert);

            // Send rule-based SMS notifications
            sendRuleBasedSmsNotifications(alert);

            log.info("Alert notification processing completed for alert: {}", alert.getId());
        } catch (Exception e) {
            log.error("Failed to process notifications for alert: {}", alert.getId(), e);
        }
    }

    /**
     * Send SMS notifications based on rule configuration
     */
    private void sendRuleBasedSmsNotifications(Alert alert) {
        Rule rule = alert.getRule();

        // Check if SMS is enabled for this rule
        if (rule.getSendSms() == null || !rule.getSendSms()) {
            log.debug("SMS notifications not enabled for rule: {}", rule.getId());
            return;
        }

        // Resolve phone numbers
        List<String> phoneNumbers = resolvePhoneNumbers(alert, rule);

        if (phoneNumbers.isEmpty()) {
            log.warn("No phone numbers found for SMS notification on alert: {}", alert.getId());
            return;
        }

        // Format and send SMS
        String message = smsService.formatAlertMessage(alert);
        smsService.sendSmsToMultiple(alert, phoneNumbers, message);

        log.info("Sent {} rule-based SMS notifications for alert: {}", phoneNumbers.size(), alert.getId());
    }

    /**
     * Resolve phone numbers from rule configuration
     */
    private List<String> resolvePhoneNumbers(Alert alert, Rule rule) {
        List<String> phoneNumbers = new ArrayList<>();

        if (rule.getSmsRecipients() == null || rule.getSmsRecipients().length == 0) {
            // No recipients configured - no SMS will be sent
            log.debug("No SMS recipients configured for rule: {}", rule.getId());
            return phoneNumbers;
        }

        // Use configured recipients
        for (String recipient : rule.getSmsRecipients()) {
            // For now, only support explicit E.164 phone numbers
            // Future enhancement: support "primary" or "all" by linking users to devices
            phoneNumbers.add(recipient);
        }

        return phoneNumbers;
    }

    /**
     * Send notification to a specific user through all enabled channels
     */
    @Transactional
    public void sendNotificationToUser(User user, Alert alert) {
        List<UserNotificationPreference> preferences = preferenceRepository.findByUserAndEnabledTrue(user);

        for (UserNotificationPreference pref : preferences) {
            // Check if alert severity meets minimum threshold
            if (alert.getSeverity().ordinal() < pref.getMinSeverity().ordinal()) {
                log.debug("Alert severity {} below threshold {} for user {}",
                        alert.getSeverity(), pref.getMinSeverity(), user.getUsername());
                continue;
            }

            // Skip if not immediate and digest is configured (digest sending would be handled separately)
            if (!pref.getImmediate() && pref.getDigestIntervalMinutes() != null) {
                log.debug("Skipping immediate notification - user prefers digest");
                continue;
            }

            sendNotificationViaChannel(user, alert, pref);
        }
    }

    private void sendNotificationViaChannel(User user, Alert alert, UserNotificationPreference preference) {
        boolean success = false;
        String errorMessage = null;

        try {
            success = switch (preference.getChannel()) {
                case EMAIL -> {
                    String email = preference.getDestination() != null
                            ? preference.getDestination()
                            : user.getEmail();
                    yield emailService.sendAlertEmail(user, alert, email);
                }
                case SMS -> {
                    if (preference.getDestination() == null) {
                        log.warn("SMS notification configured but no phone number provided for user: {}",
                                user.getUsername());
                        yield false;
                    }
                    String message = smsService.formatAlertMessage(alert);
                    SmsDeliveryLog deliveryLog = smsService.sendSms(alert, preference.getDestination(), message);
                    yield deliveryLog != null && !"FAILED".equals(deliveryLog.getStatus());
                }
                case WEBHOOK -> {
                    if (preference.getDestination() == null) {
                        log.warn("Webhook notification configured but no webhook URL provided for user: {}",
                                user.getUsername());
                        yield false;
                    }
                    yield webhookService.sendAlertWebhook(user, alert, preference.getDestination());
                }
                case IN_APP -> {
                    // IN_APP notifications are handled through events system
                    yield true;
                }
            };
        } catch (Exception e) {
            log.error("Error sending notification via {}", preference.getChannel(), e);
            errorMessage = e.getMessage();
        }

        // Log the notification attempt
        logNotification(user, alert, preference, success, errorMessage);
    }

    private void logNotification(User user, Alert alert, UserNotificationPreference preference,
                                  boolean success, String errorMessage) {
        NotificationLog.NotificationStatus status = success
                ? NotificationLog.NotificationStatus.SENT
                : NotificationLog.NotificationStatus.FAILED;

        NotificationLog log = NotificationLog.builder()
                .alert(alert)
                .user(user)
                .channel(preference.getChannel())
                .destination(preference.getDestination() != null ? preference.getDestination() : user.getEmail())
                .subject(String.format("Alert: %s", alert.getRule().getName()))
                .message(alert.getMessage())
                .status(status)
                .errorMessage(errorMessage)
                .build();

        if (success) {
            log.setSentAt(LocalDateTime.now());
        }

        notificationLogRepository.save(log);
    }

    /**
     * Get user's notification preferences
     */
    @Transactional(readOnly = true)
    public List<UserNotificationPreference> getUserPreferences(User user) {
        return preferenceRepository.findByUser(user);
    }

    /**
     * Save or update user notification preference
     */
    @Transactional
    public UserNotificationPreference savePreference(UserNotificationPreference preference) {
        // Check if preference already exists for this user and channel
        return preferenceRepository.findByUserAndChannel(preference.getUser(), preference.getChannel())
                .map(existing -> {
                    // Update existing preference
                    existing.setEnabled(preference.getEnabled());
                    existing.setDestination(preference.getDestination());
                    existing.setMinSeverity(preference.getMinSeverity());
                    existing.setImmediate(preference.getImmediate());
                    existing.setDigestIntervalMinutes(preference.getDigestIntervalMinutes());
                    return preferenceRepository.save(existing);
                })
                .orElseGet(() -> preferenceRepository.save(preference));
    }

    /**
     * Delete user notification preference
     */
    @Transactional
    public void deletePreference(User user, NotificationChannel channel) {
        preferenceRepository.deleteByUserAndChannel(user, channel);
    }

    private Event.EventSeverity mapAlertSeverityToEventSeverity(AlertSeverity alertSeverity) {
        return switch (alertSeverity) {
            case CRITICAL -> Event.EventSeverity.CRITICAL;
            case HIGH -> Event.EventSeverity.ERROR;
            case MEDIUM -> Event.EventSeverity.WARNING;
            case LOW -> Event.EventSeverity.INFO;
        };
    }
}
