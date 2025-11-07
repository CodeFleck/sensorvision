package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.*;
import org.sensorvision.repository.NotificationLogRepository;
import org.sensorvision.repository.UserNotificationPreferenceRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Integration tests for NotificationService.
 * Tests notification routing, preference filtering, and multi-channel delivery.
 */
@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private UserNotificationPreferenceRepository preferenceRepository;

    @Mock
    private NotificationLogRepository notificationLogRepository;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private SmsNotificationService smsService;

    @Mock
    private WebhookNotificationService webhookService;

    @Mock
    private SlackNotificationService slackService;

    @Mock
    private TeamsNotificationService teamsService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private NotificationService notificationService;

    @Captor
    private ArgumentCaptor<NotificationLog> notificationLogCaptor;

    private User testUser;
    private Organization testOrganization;
    private Device testDevice;
    private Rule testRule;
    private Alert criticalAlert;
    private Alert lowAlert;

    @BeforeEach
    void setUp() {
        testOrganization = createTestOrganization();
        testUser = createTestUser();
        testDevice = createTestDevice();
        testRule = createTestRule();
        criticalAlert = createCriticalAlert();
        lowAlert = createLowAlert();

        // Default mock behavior - email service returns true (lenient for tests that don't use it)
        lenient().when(emailService.sendAlertEmail(any(), any(), anyString())).thenReturn(true);
        lenient().when(smsService.sendSms(any(), anyString(), anyString())).thenReturn(null);
        lenient().when(webhookService.sendAlertWebhook(any(), any(), anyString())).thenReturn(true);
    }

    @Test
    void testSendAlertNotifications_CreatesEvent() {
        // When
        notificationService.sendAlertNotifications(criticalAlert);

        // Then
        verify(eventService).createEvent(
                eq(testOrganization),
                eq(Event.EventType.ALERT_CREATED),
                eq(Event.EventSeverity.CRITICAL),
                contains("Alert:"),
                contains("device")
        );
    }

    @Test
    void testSendAlertNotifications_SendsToWebhookServices() {
        // When
        notificationService.sendAlertNotifications(criticalAlert);

        // Then
        verify(slackService).sendAlertNotification(criticalAlert);
        verify(teamsService).sendAlertNotification(criticalAlert);
    }

    @Test
    void testSendAlertNotifications_HandlesNullOrganizationGracefully() {
        // Given - Device with no organization
        Device deviceNoOrg = createTestDevice();
        deviceNoOrg.setOrganization(null);
        Alert alertNoOrg = createCriticalAlert();
        alertNoOrg.setDevice(deviceNoOrg);

        // When
        notificationService.sendAlertNotifications(alertNoOrg);

        // Then - Should not throw, and should not call webhook services
        verify(eventService, never()).createEvent(any(), any(), any(), any(), any());
        verify(slackService, never()).sendAlertNotification(any());
        verify(teamsService, never()).sendAlertNotification(any());
    }

    @Test
    void testSendNotificationToUser_WithEnabledEmailPreference_SendsEmail() {
        // Given
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(emailService).sendAlertEmail(eq(testUser), eq(criticalAlert), anyString());
        verify(notificationLogRepository).save(any(NotificationLog.class));
    }

    @Test
    void testSendNotificationToUser_WithNoPreferences_DoesNotSend() {
        // Given
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.emptyList());

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(emailService, never()).sendAlertEmail(any(), any(), anyString());
    }

    @Test
    void testSendNotificationToUser_FiltersBySeverityThreshold() {
        // Given - User wants only HIGH and CRITICAL alerts
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        emailPref.setMinSeverity(AlertSeverity.HIGH);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When - Send LOW alert
        notificationService.sendNotificationToUser(testUser, lowAlert);

        // Then - Should NOT send
        verify(emailService, never()).sendAlertEmail(any(), any(), anyString());
    }

    @Test
    void testSendNotificationToUser_AllowsAlertsAboveSeverityThreshold() {
        // Given - User wants only HIGH and CRITICAL alerts
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        emailPref.setMinSeverity(AlertSeverity.HIGH);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When - Send CRITICAL alert
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then - Should send
        verify(emailService).sendAlertEmail(eq(testUser), eq(criticalAlert), anyString());
    }

    @Test
    void testSendNotificationToUser_SkipsImmediateWhenDigestConfigured() {
        // Given - User prefers digest, not immediate
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        emailPref.setImmediate(false);
        emailPref.setDigestIntervalMinutes(60);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then - Should NOT send immediately
        verify(emailService, never()).sendAlertEmail(any(), any(), anyString());
    }

    @Test
    void testSendNotificationToUser_SendsToMultipleChannels() {
        // Given - User has multiple channels enabled
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        UserNotificationPreference smsPref = createSmsPreference(testUser, true);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Arrays.asList(emailPref, smsPref));

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then - Should send to both channels
        verify(emailService).sendAlertEmail(eq(testUser), eq(criticalAlert), anyString());
        verify(smsService).sendSms(eq(criticalAlert), eq("+1234567890"), anyString());
        verify(notificationLogRepository, times(2)).save(any(NotificationLog.class));
    }

    @Test
    void testSendNotificationViaChannel_LogsSuccessfulNotification() {
        // Given
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));
        when(emailService.sendAlertEmail(any(), any(), anyString())).thenReturn(true);

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        NotificationLog log = notificationLogCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(NotificationLog.NotificationStatus.SENT);
        assertThat(log.getSentAt()).isNotNull();
        assertThat(log.getErrorMessage()).isNull();
    }

    @Test
    void testSendNotificationViaChannel_LogsFailedNotification() {
        // Given
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));
        when(emailService.sendAlertEmail(any(), any(), anyString())).thenReturn(false);

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        NotificationLog log = notificationLogCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(NotificationLog.NotificationStatus.FAILED);
        assertThat(log.getSentAt()).isNull();
    }

    @Test
    void testSendNotificationViaChannel_HandlesExceptionGracefully() {
        // Given
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));
        when(emailService.sendAlertEmail(any(), any(), anyString()))
                .thenThrow(new RuntimeException("SMTP error"));

        // When - Should not throw exception
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(notificationLogRepository).save(notificationLogCaptor.capture());
        NotificationLog log = notificationLogCaptor.getValue();
        assertThat(log.getStatus()).isEqualTo(NotificationLog.NotificationStatus.FAILED);
        assertThat(log.getErrorMessage()).contains("SMTP error");
    }

    @Test
    void testSavePreference_CreatesNewPreference() {
        // Given
        UserNotificationPreference newPref = createEmailPreference(testUser, true);
        when(preferenceRepository.findByUserAndChannel(testUser, NotificationChannel.EMAIL))
                .thenReturn(Optional.empty());
        when(preferenceRepository.save(any())).thenReturn(newPref);

        // When
        UserNotificationPreference result = notificationService.savePreference(newPref);

        // Then
        verify(preferenceRepository).save(newPref);
        assertThat(result).isEqualTo(newPref);
    }

    @Test
    void testSavePreference_UpdatesExistingPreference() {
        // Given
        UserNotificationPreference existing = createEmailPreference(testUser, true);
        existing.setMinSeverity(AlertSeverity.LOW);

        UserNotificationPreference updated = createEmailPreference(testUser, true);
        updated.setMinSeverity(AlertSeverity.CRITICAL);

        when(preferenceRepository.findByUserAndChannel(testUser, NotificationChannel.EMAIL))
                .thenReturn(Optional.of(existing));
        when(preferenceRepository.save(any())).thenReturn(existing);

        // When
        notificationService.savePreference(updated);

        // Then
        verify(preferenceRepository).save(argThat(pref ->
                pref.getMinSeverity() == AlertSeverity.CRITICAL
        ));
    }

    @Test
    void testDeletePreference_CallsRepository() {
        // When
        notificationService.deletePreference(testUser, NotificationChannel.EMAIL);

        // Then
        verify(preferenceRepository).deleteByUserAndChannel(testUser, NotificationChannel.EMAIL);
    }

    @Test
    void testGetUserPreferences_ReturnsAllPreferences() {
        // Given
        List<UserNotificationPreference> preferences = Arrays.asList(
                createEmailPreference(testUser, true),
                createSmsPreference(testUser, true)
        );
        when(preferenceRepository.findByUser(testUser)).thenReturn(preferences);

        // When
        List<UserNotificationPreference> result = notificationService.getUserPreferences(testUser);

        // Then
        assertThat(result).hasSize(2);
        assertThat(result).containsExactlyElementsOf(preferences);
    }

    @Test
    void testSendNotificationToUser_UsesCustomDestination() {
        // Given - User has custom email destination
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        emailPref.setDestination("custom@example.com");
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(emailService).sendAlertEmail(testUser, criticalAlert, "custom@example.com");
    }

    @Test
    void testSendNotificationToUser_FallsBackToUserEmailWhenNoDestination() {
        // Given - User has no custom email destination
        UserNotificationPreference emailPref = createEmailPreference(testUser, true);
        emailPref.setDestination(null);
        when(preferenceRepository.findByUserAndEnabledTrue(testUser))
                .thenReturn(Collections.singletonList(emailPref));

        // When
        notificationService.sendNotificationToUser(testUser, criticalAlert);

        // Then
        verify(emailService).sendAlertEmail(testUser, criticalAlert, testUser.getEmail());
    }

    // Helper methods

    private Organization createTestOrganization() {
        Organization org = new Organization();
        org.setId(1L);
        org.setName("Test Organization");
        return org;
    }

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        user.setOrganization(testOrganization);
        return user;
    }

    private Device createTestDevice() {
        Device device = new Device();
        device.setId(UUID.randomUUID());
        device.setExternalId("device-001");
        device.setName("Test Device");
        device.setOrganization(testOrganization);
        return device;
    }

    private Rule createTestRule() {
        Rule rule = new Rule();
        rule.setId(UUID.randomUUID());
        rule.setName("Temperature Alert");
        rule.setVariable("temperature");
        rule.setOperator(RuleOperator.GT);
        rule.setThreshold(new BigDecimal("75.0"));
        return rule;
    }

    private Alert createCriticalAlert() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setDevice(testDevice);
        alert.setRule(testRule);
        alert.setTriggeredValue(new BigDecimal("100.5"));
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setMessage("Critical temperature exceeded");
        return alert;
    }

    private Alert createLowAlert() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setDevice(testDevice);
        alert.setRule(testRule);
        alert.setTriggeredValue(new BigDecimal("76.0"));
        alert.setSeverity(AlertSeverity.LOW);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setMessage("Temperature slightly elevated");
        return alert;
    }

    private UserNotificationPreference createEmailPreference(User user, boolean enabled) {
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setUser(user);
        pref.setChannel(NotificationChannel.EMAIL);
        pref.setEnabled(enabled);
        pref.setMinSeverity(AlertSeverity.LOW);
        pref.setImmediate(true);
        return pref;
    }

    private UserNotificationPreference createSmsPreference(User user, boolean enabled) {
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setUser(user);
        pref.setChannel(NotificationChannel.SMS);
        pref.setEnabled(enabled);
        pref.setDestination("+1234567890");
        pref.setMinSeverity(AlertSeverity.LOW);
        pref.setImmediate(true);
        return pref;
    }
}
