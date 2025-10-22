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
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Integration tests for EmailNotificationService.
 * Tests email sending functionality with mocked JavaMailSender.
 */
@ExtendWith(MockitoExtension.class)
class EmailNotificationServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @Mock
    private EmailTemplateService templateService;

    @InjectMocks
    private EmailNotificationService emailNotificationService;

    @Captor
    private ArgumentCaptor<MimeMessage> messageCaptor;

    private MimeMessage mimeMessage;
    private User testUser;
    private Device testDevice;
    private Alert testAlert;
    private Rule testRule;

    @BeforeEach
    void setUp() {
        // Create a test MimeMessage
        mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Set up service configuration
        ReflectionTestUtils.setField(emailNotificationService, "enabled", true);
        ReflectionTestUtils.setField(emailNotificationService, "fromAddress", "noreply@sensorvision.com");

        // Create test data
        testUser = createTestUser();
        testDevice = createTestDevice();
        testRule = createTestRule();
        testAlert = createTestAlert();

        // Mock template service
        when(templateService.renderAlertTemplate(any(), any(), any(), any(), any(), any()))
            .thenReturn("<html><body>Alert Email</body></html>");
        when(templateService.renderPasswordResetTemplate(any()))
            .thenReturn("<html><body>Reset Email</body></html>");
    }

    @Test
    void testSendAlertEmail_Success() {
        // When
        emailNotificationService.sendAlertEmail(testUser, testAlert);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_RendersTemplateCorrectly() {
        // When
        emailNotificationService.sendAlertEmail(testUser, testAlert);

        // Then
        verify(templateService).renderAlertTemplate(
            eq("Test Device"),
            eq("100.5"),
            eq("CRITICAL"),
            eq("Test Rule"),
            any(String.class),
            eq("http://localhost:8080")
        );
    }

    @Test
    void testSendAlertEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "enabled", false);

        // When
        emailNotificationService.sendAlertEmail(testUser, testAlert);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_Success() {
        // Given
        String resetToken = "test-reset-token-12345";

        // When
        emailNotificationService.sendPasswordResetEmail(testUser, resetToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
        verify(templateService).renderPasswordResetTemplate(contains(resetToken));
    }

    @Test
    void testSendPasswordResetEmail_ContainsResetLink() {
        // Given
        String resetToken = "test-reset-token-12345";

        // When
        emailNotificationService.sendPasswordResetEmail(testUser, resetToken);

        // Then
        verify(templateService).renderPasswordResetTemplate(
            argThat(link -> link.contains("reset-password") && link.contains(resetToken))
        );
    }

    @Test
    void testSendVerificationEmail_Success() {
        // Given
        String verificationToken = "verification-token-67890";

        // When
        emailNotificationService.sendVerificationEmail(testUser, verificationToken);

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendReportEmail_WithAttachment_Success() {
        // Given
        byte[] reportData = "Report Content".getBytes();
        String reportName = "monthly-report.pdf";

        // When
        emailNotificationService.sendReportEmail(
            testUser.getEmail(),
            "Monthly Report",
            "Here is your monthly report",
            reportData,
            reportName
        );

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendIssueReportEmail_WithScreenshot_Success() {
        // Given
        String issueDescription = "Bug in dashboard";
        byte[] screenshot = new byte[]{1, 2, 3, 4, 5};

        // When
        emailNotificationService.sendIssueReportEmail(
            testUser.getEmail(),
            issueDescription,
            screenshot
        );

        // Then
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_HandlesNullAlertGracefully() {
        // When
        emailNotificationService.sendAlertEmail(testUser, null);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_HandlesNullUserGracefully() {
        // When
        emailNotificationService.sendAlertEmail(null, testAlert);

        // Then
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_HandlesMailSenderException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        // When/Then - Should not throw exception
        emailNotificationService.sendPasswordResetEmail(testUser, "token");

        // Verify it was attempted
        verify(mailSender).send(any(MimeMessage.class));
    }

    // Helper methods to create test data

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        return user;
    }

    private Device createTestDevice() {
        Device device = new Device();
        device.setId(1L);
        device.setDeviceId("device-001");
        device.setName("Test Device");
        return device;
    }

    private Rule createTestRule() {
        Rule rule = new Rule();
        rule.setId(UUID.randomUUID());
        rule.setName("Test Rule");
        rule.setVariable("temperature");
        rule.setOperator(RuleOperator.GT);
        rule.setThreshold(new BigDecimal("75.0"));
        return rule;
    }

    private Alert createTestAlert() {
        Alert alert = new Alert();
        alert.setId(1L);
        alert.setDevice(testDevice);
        alert.setRule(testRule);
        alert.setTriggeredValue(100.5);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setMessage("Temperature exceeded threshold");
        return alert;
    }
}
