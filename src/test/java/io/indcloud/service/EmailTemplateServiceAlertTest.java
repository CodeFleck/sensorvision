package io.indcloud.service;

import io.indcloud.model.*;
import io.indcloud.repository.EmailTemplateRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
@DisplayName("EmailTemplateService Alert Email Tests")
class EmailTemplateServiceAlertTest {

    @Mock
    private EmailTemplateRepository emailTemplateRepository;

    private EmailTemplateService emailTemplateService;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        emailTemplateService = new EmailTemplateService(emailTemplateRepository, objectMapper);
    }

    private Alert createTestAlert(String ruleName, String deviceName, String deviceExternalId,
            AlertSeverity severity, String message, String variable, RuleOperator operator,
            BigDecimal threshold, BigDecimal triggeredValue) {

        Organization org = new Organization();
        org.setId(1L);
        org.setName("Test Organization");

        Device device = new Device();
        device.setId(UUID.randomUUID());
        device.setExternalId(deviceExternalId);
        device.setName(deviceName);
        device.setOrganization(org);

        Rule rule = Rule.builder()
            .id(UUID.randomUUID())
            .name(ruleName)
            .device(device)
            .variable(variable)
            .operator(operator)
            .threshold(threshold)
            .enabled(true)
            .organization(org)
            .build();

        return Alert.builder()
            .id(UUID.randomUUID())
            .rule(rule)
            .device(device)
            .message(message)
            .severity(severity)
            .triggeredValue(triggeredValue)
            .triggeredAt(LocalDateTime.now())
            .acknowledged(false)
            .build();
    }

    @Nested
    @DisplayName("generateAlertNotificationEmail(Alert) Tests")
    class GenerateAlertFromAlertObjectTests {

        @Test
        @DisplayName("Should extract device name from Alert object")
        void shouldExtractDeviceNameFromAlert() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("smart-meter-001")
                .doesNotContain("Unknown Device");
        }

        @Test
        @DisplayName("Should extract device external ID from Alert object")
        void shouldExtractDeviceExternalIdFromAlert() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("sm-001");
        }

        @Test
        @DisplayName("Should extract rule name from Alert object")
        void shouldExtractRuleNameFromAlert() {
            Alert alert = createTestAlert(
                "Critical Temperature Alert",
                "temp-sensor-001",
                "ts-001",
                AlertSeverity.HIGH,
                "Temperature exceeded safe limit",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("85"),
                new BigDecimal("92.3")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("Critical Temperature Alert")
                .doesNotContain("System Alert");
        }

        @Test
        @DisplayName("Should include triggered value in email")
        void shouldIncludeTriggeredValueInEmail() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("175.5");
        }

        @Test
        @DisplayName("Should include operator symbol in email")
        void shouldIncludeOperatorSymbolInEmail() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("&lt;"); // HTML-escaped '<'
        }

        @Test
        @DisplayName("Should include threshold value in email")
        void shouldIncludeThresholdValueInEmail() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("200");
        }

        @Test
        @DisplayName("Should include variable name in email")
        void shouldIncludeVariableNameInEmail() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Voltage dropped below threshold",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("voltage");
        }

        @Test
        @DisplayName("Should include alert message in email")
        void shouldIncludeAlertMessageInEmail() {
            String expectedMessage = "Voltage dropped below threshold";
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                expectedMessage,
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("175.5")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains(expectedMessage);
        }

        @Test
        @DisplayName("Should include severity badge in email")
        void shouldIncludeSeverityBadgeInEmail() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.CRITICAL,
                "Voltage dropped critically low",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("50")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("CRITICAL");
        }

        @Test
        @DisplayName("Should use correct color for CRITICAL severity")
        void shouldUseCorrectColorForCriticalSeverity() {
            Alert alert = createTestAlert(
                "Low Voltage Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.CRITICAL,
                "Critical alert",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("50")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("#991b1b"); // Critical red color
        }

        @Test
        @DisplayName("Should use correct color for HIGH severity")
        void shouldUseCorrectColorForHighSeverity() {
            Alert alert = createTestAlert(
                "High Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.HIGH,
                "High severity alert",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("100")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("#c2410c"); // High orange color
        }

        @Test
        @DisplayName("Should use correct color for MEDIUM severity")
        void shouldUseCorrectColorForMediumSeverity() {
            Alert alert = createTestAlert(
                "Medium Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.MEDIUM,
                "Medium severity alert",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("150")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("#a16207"); // Medium amber color
        }

        @Test
        @DisplayName("Should use correct color for LOW severity")
        void shouldUseCorrectColorForLowSeverity() {
            Alert alert = createTestAlert(
                "Low Alert",
                "smart-meter-001",
                "sm-001",
                AlertSeverity.LOW,
                "Low severity alert",
                "voltage",
                RuleOperator.LT,
                new BigDecimal("200"),
                new BigDecimal("190")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("#166534"); // Low green color
        }

        @Test
        @DisplayName("Should include SensorVision branding")
        void shouldIncludeSensorVisionBranding() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("SensorVision")
                .contains("Industrial Cloud");
        }

        @Test
        @DisplayName("Should include View Alert Details button with correct link")
        void shouldIncludeViewAlertDetailsButton() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("View Alert Details")
                .contains("https://indcloud.io/alerts");
        }

        @Test
        @DisplayName("Should include notification preferences link")
        void shouldIncludeNotificationPreferencesLink() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml).contains("Manage notification preferences");
        }

        @Test
        @DisplayName("Should generate valid HTML document")
        void shouldGenerateValidHtmlDocument() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("<!DOCTYPE html>")
                .contains("<html")
                .contains("</html>")
                .contains("<body")
                .contains("</body>");
        }
    }

    @Nested
    @DisplayName("generateAlertNotificationEmail(Object) Fallback Tests")
    class GenerateAlertFallbackTests {

        @Test
        @DisplayName("Should use fallback for null object")
        void shouldUseFallbackForNullObject() {
            String emailHtml = emailTemplateService.generateAlertNotificationEmail((Object) null);

            assertThat(emailHtml)
                .contains("System Alert")
                .contains("Unknown Device");
        }

        @Test
        @DisplayName("Should use fallback for non-Alert object")
        void shouldUseFallbackForNonAlertObject() {
            String notAnAlert = "This is a string, not an Alert";

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(notAnAlert);

            assertThat(emailHtml)
                .contains("System Alert")
                .contains("Unknown Device");
        }
    }

    @Nested
    @DisplayName("XSS Prevention Tests")
    class XssPreventionTests {

        @Test
        @DisplayName("Should escape HTML in device name")
        void shouldEscapeHtmlInDeviceName() {
            Alert alert = createTestAlert(
                "Test Alert",
                "<script>alert('xss')</script>",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
        }

        @Test
        @DisplayName("Should escape HTML in rule name")
        void shouldEscapeHtmlInRuleName() {
            Alert alert = createTestAlert(
                "<img src=x onerror=alert('xss')>",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .doesNotContain("<img")
                .contains("&lt;img");
        }

        @Test
        @DisplayName("Should escape HTML in alert message")
        void shouldEscapeHtmlInAlertMessage() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                "<script>document.cookie</script>",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .doesNotContain("<script>document.cookie</script>")
                .contains("&lt;script&gt;");
        }
    }

    @Nested
    @DisplayName("Null Safety Tests")
    class NullSafetyTests {

        @Test
        @DisplayName("Should handle null rule gracefully")
        void shouldHandleNullRuleGracefully() {
            Organization org = new Organization();
            org.setId(1L);

            Device device = new Device();
            device.setId(UUID.randomUUID());
            device.setExternalId("d-001");
            device.setName("device-001");
            device.setOrganization(org);

            Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(null) // Null rule
                .device(device)
                .message("Test message")
                .severity(AlertSeverity.LOW)
                .triggeredValue(new BigDecimal("105"))
                .triggeredAt(LocalDateTime.now())
                .acknowledged(false)
                .build();

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("Unknown Rule")
                .contains("device-001");
        }

        @Test
        @DisplayName("Should handle null device gracefully")
        void shouldHandleNullDeviceGracefully() {
            Organization org = new Organization();
            org.setId(1L);

            Device ruleDevice = new Device();
            ruleDevice.setId(UUID.randomUUID());
            ruleDevice.setOrganization(org);

            Rule rule = Rule.builder()
                .id(UUID.randomUUID())
                .name("Test Rule")
                .device(ruleDevice)
                .variable("temperature")
                .operator(RuleOperator.GT)
                .threshold(new BigDecimal("100"))
                .enabled(true)
                .organization(org)
                .build();

            Alert alert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(rule)
                .device(null) // Null device
                .message("Test message")
                .severity(AlertSeverity.LOW)
                .triggeredValue(new BigDecimal("105"))
                .triggeredAt(LocalDateTime.now())
                .acknowledged(false)
                .build();

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("Unknown Device")
                .contains("Test Rule");
        }

        @Test
        @DisplayName("Should handle null message gracefully")
        void shouldHandleNullMessageGracefully() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW,
                null, // Null message
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            // Override the message to null after creation
            alert.setMessage(null);

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            assertThat(emailHtml)
                .contains("An alert was triggered")
                .contains("device-001");
        }

        @Test
        @DisplayName("Should handle null severity gracefully")
        void shouldHandleNullSeverityGracefully() {
            Alert alert = createTestAlert(
                "Test Alert",
                "device-001",
                "d-001",
                AlertSeverity.LOW, // Create with valid severity first
                "Test message",
                "temperature",
                RuleOperator.GT,
                new BigDecimal("100"),
                new BigDecimal("105")
            );

            alert.setSeverity(null);

            String emailHtml = emailTemplateService.generateAlertNotificationEmail(alert);

            // When severity is null, code defaults to "MEDIUM" string, which uses amber color
            assertThat(emailHtml).contains("#a16207");
        }
    }
}
