package io.indcloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.model.*;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

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
    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        // Create a test MimeMessage
        mimeMessage = new MimeMessage(Session.getDefaultInstance(new Properties()));
        lenient().when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        // Set up service configuration
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", true);
        ReflectionTestUtils.setField(emailNotificationService, "fromEmail", "noreply@indcloud.com");

        // Create test data
        testOrganization = createTestOrganization();
        testUser = createTestUser();
        testDevice = createTestDevice();
        testRule = createTestRule();
        testAlert = createTestAlert();

        // Mock template service responses with valid HTML content
        lenient().when(templateService.generateAlertNotificationEmail(any(Alert.class)))
                .thenReturn("<html><body><h1>Alert Notification</h1><p>Test alert email body</p></body></html>");
        lenient().when(templateService.generatePasswordResetEmail(anyString()))
                .thenReturn("<html><body><h1>Password Reset</h1><p>Test password reset email body</p></body></html>");
    }

    @Test
    void testSendAlertEmail_Success() {
        // When
        boolean result = emailNotificationService.sendAlertEmail(testUser, testAlert, testUser.getEmail());

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);

        // When
        boolean result = emailNotificationService.sendAlertEmail(testUser, testAlert, testUser.getEmail());

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_Success() {
        // Given
        String resetToken = "test-reset-token-12345";

        // When
        boolean result = emailNotificationService.sendPasswordResetEmail(testUser.getEmail(), resetToken);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);
        String resetToken = "test-reset-token-12345";

        // When
        boolean result = emailNotificationService.sendPasswordResetEmail(testUser.getEmail(), resetToken);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendVerificationEmail_Success() {
        // Given
        String verificationToken = "verification-token-67890";

        // When
        boolean result = emailNotificationService.sendVerificationEmail(testUser.getEmail(), verificationToken);

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendVerificationEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);
        String verificationToken = "verification-token-67890";

        // When
        boolean result = emailNotificationService.sendVerificationEmail(testUser.getEmail(), verificationToken);

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendReportEmail_WithAttachment_Success() {
        // Given
        byte[] reportData = "Report Content".getBytes();
        String fileName = "monthly-report.pdf";
        ReportExecution execution = createTestReportExecution();

        // When
        boolean result = emailNotificationService.sendReportEmail(
            testUser.getEmail(),
            "Monthly Report",
            "Here is your monthly report",
            fileName,
            reportData,
            execution
        );

        // Then
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendReportEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);
        byte[] reportData = "Report Content".getBytes();
        String fileName = "monthly-report.pdf";
        ReportExecution execution = createTestReportExecution();

        // When
        boolean result = emailNotificationService.sendReportEmail(
            testUser.getEmail(),
            "Monthly Report",
            "Here is your monthly report",
            fileName,
            reportData,
            execution
        );

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendIssueReportEmail_WithScreenshot_Success() {
        // Given
        IssueSubmission issue = createTestIssue();

        // Debug - check if issue is properly set up
        System.out.println("Issue severity: " + issue.getSeverity());
        System.out.println("Issue screenshot data length: " + (issue.getScreenshotData() != null ? issue.getScreenshotData().length : "null"));

        // When
        boolean result = emailNotificationService.sendIssueReportEmail(
            issue,
            "support@indcloud.com"
        );

        // Then
        System.out.println("Result: " + result);
        assertThat(result).isTrue();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendIssueReportEmail_WhenDisabled_DoesNotSend() {
        // Given
        ReflectionTestUtils.setField(emailNotificationService, "emailEnabled", false);
        IssueSubmission issue = createTestIssue();

        // When
        boolean result = emailNotificationService.sendIssueReportEmail(
            issue,
            "support@indcloud.com"
        );

        // Then
        assertThat(result).isFalse();
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_HandlesNullAlertGracefully() {
        // When
        boolean result = emailNotificationService.sendAlertEmail(testUser, null, testUser.getEmail());

        // Then - Should handle gracefully and not throw
        verify(mailSender, never()).send(any(MimeMessage.class));
    }

    @Test
    void testSendAlertEmail_HandlesMailSenderException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailNotificationService.sendAlertEmail(testUser, testAlert, testUser.getEmail());

        // Then - Should catch exception and return false
        assertThat(result).isFalse();
        verify(mailSender).send(any(MimeMessage.class));
    }

    @Test
    void testSendPasswordResetEmail_HandlesMailSenderException() {
        // Given
        doThrow(new RuntimeException("Mail server error")).when(mailSender).send(any(MimeMessage.class));

        // When
        boolean result = emailNotificationService.sendPasswordResetEmail(testUser.getEmail(), "token");

        // Then - Should catch exception and return false
        assertThat(result).isFalse();
        verify(mailSender).send(any(MimeMessage.class));
    }

    // Helper methods to create test data

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
        rule.setName("Test Rule");
        rule.setVariable("temperature");
        rule.setOperator(RuleOperator.GT);
        rule.setThreshold(new BigDecimal("75.0"));
        rule.setOrganization(testOrganization);
        return rule;
    }

    private Alert createTestAlert() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setDevice(testDevice);
        alert.setRule(testRule);
        alert.setTriggeredValue(new BigDecimal("100.5"));
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setTriggeredAt(LocalDateTime.now());
        alert.setMessage("Temperature exceeded threshold");
        return alert;
    }

    private ReportExecution createTestReportExecution() {
        ReportExecution execution = new ReportExecution();
        execution.setId(1L);
        execution.setStatus(ReportExecution.ExecutionStatus.COMPLETED);
        execution.setStartedAt(LocalDateTime.now());
        execution.setCompletedAt(LocalDateTime.now().plusMinutes(5));
        execution.setRecordCount(1000);
        execution.setFileSizeBytes(51200L); // 50 KB
        return execution;
    }

    private IssueSubmission createTestIssue() {
        IssueSubmission issue = new IssueSubmission();
        issue.setId(1L);
        issue.setTitle("Test Issue");
        issue.setDescription("This is a test issue description");
        issue.setCategory(IssueCategory.BUG);
        issue.setSeverity(IssueSeverity.MEDIUM);
        issue.setStatus(IssueStatus.SUBMITTED);
        issue.setUser(testUser);
        issue.setOrganization(testOrganization);
        // Set createdAt manually for unit test using reflection (normally set automatically by AuditableEntity)
        ReflectionTestUtils.setField(issue, "createdAt", java.time.Instant.now());
        issue.setPageUrl("http://localhost:3001/dashboard");
        issue.setBrowserInfo("Chrome 120.0");
        issue.setScreenResolution("1920x1080");
        issue.setUserAgent("Mozilla/5.0...");
        issue.setScreenshotData(new byte[]{1, 2, 3, 4, 5});
        issue.setScreenshotFilename("screenshot.png");
        return issue;
    }
}
