package org.sensorvision.service;

import com.twilio.exception.TwilioException;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.*;
import org.sensorvision.repository.OrganizationSmsSettingsRepository;
import org.sensorvision.repository.SmsDeliveryLogRepository;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SmsNotificationServiceTest {

    @Mock
    private SmsDeliveryLogRepository smsDeliveryLogRepository;

    @Mock
    private OrganizationSmsSettingsRepository smsSettingsRepository;

    @InjectMocks
    private SmsNotificationService smsNotificationService;

    private Alert testAlert;
    private OrganizationSmsSettings testSettings;
    private Organization testOrganization;
    private Device testDevice;
    private Rule testRule;

    @BeforeEach
    void setUp() {
        // Enable SMS for testing
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", true);
        ReflectionTestUtils.setField(smsNotificationService, "fromNumber", "+15551234567");
        ReflectionTestUtils.setField(smsNotificationService, "costPerMessage", new BigDecimal("0.0075"));

        // Setup test data
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Org");

        testDevice = new Device();
        testDevice.setId(UUID.randomUUID());
        testDevice.setName("Test Device");
        testDevice.setOrganization(testOrganization);

        testRule = new Rule();
        testRule.setId(UUID.randomUUID());
        testRule.setName("Test Rule");
        testRule.setDevice(testDevice);

        testAlert = new Alert();
        testAlert.setId(UUID.randomUUID());
        testAlert.setRule(testRule);
        testAlert.setDevice(testDevice);
        testAlert.setMessage("Temperature critical");
        testAlert.setSeverity(AlertSeverity.CRITICAL);

        testSettings = OrganizationSmsSettings.builder()
            .id(UUID.randomUUID())
            .organization(testOrganization)
            .enabled(true)
            .dailyLimit(100)
            .monthlyBudget(new BigDecimal("50.00"))
            .currentMonthCount(0)
            .currentMonthCost(BigDecimal.ZERO)
            .alertOnBudgetThreshold(true)
            .budgetThresholdPercentage(80)
            .build();
    }

    @Test
    void testSendSms_SmsDisabled_ReturnsNull() {
        // Given
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", false);

        // When
        SmsDeliveryLog result = smsNotificationService.sendSms(
            testAlert, "+15559876543", "Test message"
        );

        // Then
        assertNull(result);
        verify(smsDeliveryLogRepository, never()).save(any());
    }

    @Test
    void testSendSms_OrganizationSmsNotEnabled_ReturnsNull() {
        // Given
        testSettings.setEnabled(false);
        when(smsSettingsRepository.findByOrganizationId(1L))
            .thenReturn(Optional.of(testSettings));

        // When
        SmsDeliveryLog result = smsNotificationService.sendSms(
            testAlert, "+15559876543", "Test message"
        );

        // Then
        assertNull(result);
        verify(smsDeliveryLogRepository, never()).save(any());
    }

    @Test
    void testSendSms_DailyLimitExceeded_ReturnsFailed() {
        // Given
        testSettings.setCurrentMonthCount(100); // At limit
        when(smsSettingsRepository.findByOrganizationId(1L))
            .thenReturn(Optional.of(testSettings));

        SmsDeliveryLog failedLog = SmsDeliveryLog.builder()
            .alert(testAlert)
            .phoneNumber("+15559876543")
            .messageBody("Test message")
            .status("FAILED")
            .errorCode("DAILY_LIMIT_EXCEEDED")
            .build();

        when(smsDeliveryLogRepository.save(any(SmsDeliveryLog.class)))
            .thenReturn(failedLog);

        // When
        SmsDeliveryLog result = smsNotificationService.sendSms(
            testAlert, "+15559876543", "Test message"
        );

        // Then
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals("DAILY_LIMIT_EXCEEDED", result.getErrorCode());
        verify(smsSettingsRepository, never()).save(any());
    }

    @Test
    void testSendSms_BudgetExceeded_ReturnsFailed() {
        // Given
        testSettings.setCurrentMonthCost(new BigDecimal("50.00")); // At budget limit
        when(smsSettingsRepository.findByOrganizationId(1L))
            .thenReturn(Optional.of(testSettings));

        SmsDeliveryLog failedLog = SmsDeliveryLog.builder()
            .alert(testAlert)
            .phoneNumber("+15559876543")
            .messageBody("Test message")
            .status("FAILED")
            .errorCode("BUDGET_EXCEEDED")
            .build();

        when(smsDeliveryLogRepository.save(any(SmsDeliveryLog.class)))
            .thenReturn(failedLog);

        // When
        SmsDeliveryLog result = smsNotificationService.sendSms(
            testAlert, "+15559876543", "Test message"
        );

        // Then
        assertNotNull(result);
        assertEquals("FAILED", result.getStatus());
        assertEquals("BUDGET_EXCEEDED", result.getErrorCode());
        verify(smsSettingsRepository, never()).save(any());
    }

    @Test
    void testFormatAlertMessage_ShortMessage_NoTruncation() {
        // When
        String formatted = smsNotificationService.formatAlertMessage(testAlert);

        // Then
        assertNotNull(formatted);
        assertTrue(formatted.contains("CRITICAL"));
        assertTrue(formatted.contains("Test Device"));
        assertTrue(formatted.contains("Test Rule"));
        assertTrue(formatted.contains("Temperature critical"));
        assertTrue(formatted.length() <= 160);
    }

    @Test
    void testFormatAlertMessage_LongMessage_Truncated() {
        // Given
        String longMessage = "Temperature has exceeded critical threshold by a significant margin and requires immediate attention from operations team to prevent equipment damage and production downtime";
        testAlert.setMessage(longMessage);

        // When
        String formatted = smsNotificationService.formatAlertMessage(testAlert);

        // Then
        assertNotNull(formatted);
        assertTrue(formatted.length() <= 160, "Message should be truncated to 160 chars");
        assertTrue(formatted.contains("CRITICAL"));
        assertTrue(formatted.contains("Test Device"));
    }

    @Test
    void testIsSmsEnabled_WhenEnabled_ReturnsTrue() {
        // Given
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", true);

        // When
        boolean result = smsNotificationService.isSmsEnabled();

        // Then
        assertTrue(result);
    }

    @Test
    void testIsSmsEnabled_WhenDisabled_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", false);

        // When
        boolean result = smsNotificationService.isSmsEnabled();

        // Then
        assertFalse(result);
    }

    @Test
    void testIsSmsEnabledForOrganization_WhenEnabled_ReturnsTrue() {
        // Given
        when(smsSettingsRepository.existsByOrganizationIdAndEnabledTrue(1L))
            .thenReturn(true);

        // When
        boolean result = smsNotificationService.isSmsEnabledForOrganization(1L);

        // Then
        assertTrue(result);
    }

    @Test
    void testIsSmsEnabledForOrganization_WhenGloballyDisabled_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", false);

        // When
        boolean result = smsNotificationService.isSmsEnabledForOrganization(1L);

        // Then
        assertFalse(result);
        verify(smsSettingsRepository, never()).existsByOrganizationIdAndEnabledTrue(any());
    }

    @Test
    void testGetOrganizationSettings_Found_ReturnsSettings() {
        // Given
        when(smsSettingsRepository.findByOrganizationId(1L))
            .thenReturn(Optional.of(testSettings));

        // When
        OrganizationSmsSettings result = smsNotificationService.getOrganizationSettings(1L);

        // Then
        assertNotNull(result);
        assertEquals(testSettings.getId(), result.getId());
        assertEquals(true, result.getEnabled());
    }

    @Test
    void testGetOrganizationSettings_NotFound_ReturnsNull() {
        // Given
        when(smsSettingsRepository.findByOrganizationId(1L))
            .thenReturn(Optional.empty());

        // When
        OrganizationSmsSettings result = smsNotificationService.getOrganizationSettings(1L);

        // Then
        assertNull(result);
    }

    @Test
    void testSendVerificationSms_SmsDisabled_ReturnsFalse() {
        // Given
        ReflectionTestUtils.setField(smsNotificationService, "smsEnabled", false);

        // When
        boolean result = smsNotificationService.sendVerificationSms(
            "+15559876543",
            "Your verification code is: 123456"
        );

        // Then
        assertFalse(result);
    }
}
