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
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.repository.RuleRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RuleEngineService.
 * Tests rule evaluation, alert triggering, severity determination, and cooldown logic.
 */
@ExtendWith(MockitoExtension.class)
class RuleEngineServiceTest {

    @Mock
    private RuleRepository ruleRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AlertService alertService;

    @Mock
    private EventService eventService;

    @InjectMocks
    private RuleEngineService ruleEngineService;

    @Captor
    private ArgumentCaptor<Alert> alertCaptor;

    private Device testDevice;
    private Organization testOrganization;
    private TelemetryRecord testTelemetry;
    private Rule testRule;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDevice = Device.builder()
                .externalId("test-device-001")
                .name("Test Device")
                .organization(testOrganization)
                .build();

        testTelemetry = TelemetryRecord.builder()
                .device(testDevice)
                .kwConsumption(new BigDecimal("100.5"))
                .voltage(new BigDecimal("220.0"))
                .current(new BigDecimal("5.5"))
                .powerFactor(new BigDecimal("0.95"))
                .frequency(new BigDecimal("60.0"))
                .timestamp(Instant.now())
                .build();

        testRule = Rule.builder()
                .id(UUID.randomUUID())
                .name("High Voltage Alert")
                .device(testDevice)
                .variable("voltage")
                .operator(RuleOperator.GT)
                .threshold(new BigDecimal("230.0"))
                .enabled(true)
                .organization(testOrganization)
                .build();
    }

    // ===== EVALUATE RULES TESTS =====

    @Test
    void evaluateRules_shouldEvaluateAllEnabledRules_whenRulesExist() {
        // Given
        Rule rule1 = testRule;
        Rule rule2 = Rule.builder()
                .id(UUID.randomUUID())
                .name("Low Current Alert")
                .device(testDevice)
                .variable("current")
                .operator(RuleOperator.LT)
                .threshold(new BigDecimal("1.0"))
                .enabled(true)
                .organization(testOrganization)
                .build();

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(rule1, rule2));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(ruleRepository).findByDeviceExternalIdAndEnabledTrue("test-device-001");
        // Neither rule should trigger an alert (voltage 220 < 230, current 5.5 > 1.0)
        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void evaluateRules_shouldSkipEvaluation_whenNoEnabledRules() {
        // Given
        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(Collections.emptyList());

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(ruleRepository).findByDeviceExternalIdAndEnabledTrue("test-device-001");
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ===== OPERATOR TESTS =====

    @Test
    void evaluateRules_shouldTriggerAlert_whenGT_conditionMet() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0")); // voltage 220 > 200
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();

        assertThat(alert.getRule()).isEqualTo(testRule);
        assertThat(alert.getDevice()).isEqualTo(testDevice);
        assertThat(alert.getTriggeredValue()).isEqualByComparingTo(new BigDecimal("220.0"));
        assertThat(alert.getMessage()).contains("voltage", ">", "200.0", "220.0");
        verify(alertService).sendAlertNotification(alert);
    }

    @Test
    void evaluateRules_shouldTriggerAlert_whenGTE_conditionMet() {
        // Given
        testRule.setOperator(RuleOperator.GTE);
        testRule.setThreshold(new BigDecimal("220.0")); // voltage 220 >= 220
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(any(Alert.class));
        verify(alertService).sendAlertNotification(any(Alert.class));
    }

    @Test
    void evaluateRules_shouldTriggerAlert_whenLT_conditionMet() {
        // Given
        testRule.setOperator(RuleOperator.LT);
        testRule.setThreshold(new BigDecimal("10.0")); // current 5.5 < 10.0
        testRule.setVariable("current");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getMessage()).contains("current", "<", "10.0");
    }

    @Test
    void evaluateRules_shouldTriggerAlert_whenLTE_conditionMet() {
        // Given
        testRule.setOperator(RuleOperator.LTE);
        testRule.setThreshold(new BigDecimal("5.5")); // current 5.5 <= 5.5
        testRule.setVariable("current");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(any(Alert.class));
        verify(alertService).sendAlertNotification(any(Alert.class));
    }

    @Test
    void evaluateRules_shouldTriggerAlert_whenEQ_conditionMet() {
        // Given
        testRule.setOperator(RuleOperator.EQ);
        testRule.setThreshold(new BigDecimal("60.0")); // frequency 60.0 = 60.0
        testRule.setVariable("frequency");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getMessage()).contains("frequency", "=", "60.0");
    }

    @Test
    void evaluateRules_shouldNotTriggerAlert_whenConditionNotMet() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("250.0")); // voltage 220 < 250, condition not met
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertService, never()).sendAlertNotification(any(Alert.class));
    }

    // ===== COOLDOWN TESTS =====

    @Test
    void evaluateRules_shouldNotTriggerAlert_whenRecentAlertExists() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0"));
        testRule.setVariable("voltage");

        Alert recentAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(testRule)
                .device(testDevice)
                .build();

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(eq(testRule.getId()), any(Instant.class)))
                .thenReturn(List.of(recentAlert));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).findRecentAlertsForRule(eq(testRule.getId()), any(Instant.class));
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertService, never()).sendAlertNotification(any(Alert.class));
    }

    @Test
    void evaluateRules_shouldTriggerAlert_whenRecentAlertIsOld() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0"));
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(eq(testRule.getId()), any(Instant.class)))
                .thenReturn(Collections.emptyList()); // No recent alerts within 5 minutes
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(any(Alert.class));
        verify(alertService).sendAlertNotification(any(Alert.class));
    }

    // ===== SEVERITY DETERMINATION TESTS =====

    @Test
    void evaluateRules_shouldSetCriticalSeverity_whenDeviationOver200Percent() {
        // Given: threshold 100, actual 301 => deviation 201% => CRITICAL
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("kwConsumption");

        testTelemetry.setKwConsumption(new BigDecimal("301.0"));

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.CRITICAL);
    }

    @Test
    void evaluateRules_shouldSetHighSeverity_whenDeviationOver100Percent() {
        // Given: threshold 100, actual 201 => deviation 101% => HIGH
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("kwConsumption");

        testTelemetry.setKwConsumption(new BigDecimal("201.0"));

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.HIGH);
    }

    @Test
    void evaluateRules_shouldSetMediumSeverity_whenDeviationOver50Percent() {
        // Given: threshold 100, actual 151 => deviation 51% => MEDIUM
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("kwConsumption");

        testTelemetry.setKwConsumption(new BigDecimal("151.0"));

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
    }

    @Test
    void evaluateRules_shouldSetLowSeverity_whenDeviationUnder50Percent() {
        // Given: threshold 100, actual 120 => deviation 20% => LOW
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("kwConsumption");

        testTelemetry.setKwConsumption(new BigDecimal("120.0"));

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.LOW);
    }

    @Test
    void evaluateRules_shouldSetMediumSeverity_whenThresholdIsZero() {
        // Given: threshold 0 (edge case)
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(BigDecimal.ZERO);
        testRule.setVariable("kwConsumption");

        testTelemetry.setKwConsumption(new BigDecimal("50.0"));

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert alert = alertCaptor.getValue();
        assertThat(alert.getSeverity()).isEqualTo(AlertSeverity.MEDIUM);
    }

    // ===== MISSING VARIABLE TESTS =====

    @Test
    void evaluateRules_shouldNotTriggerAlert_whenVariableNotInTelemetry() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("unknownVariable"); // Not in telemetry

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository, never()).save(any(Alert.class));
        verify(alertService, never()).sendAlertNotification(any(Alert.class));
    }

    @Test
    void evaluateRules_shouldHandleNullTelemetryValues() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("100.0"));
        testRule.setVariable("voltage");

        testTelemetry.setVoltage(null); // Null voltage

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then: Should use BigDecimal.ZERO for null values, so 0 < 100, condition not met
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ===== EVENT EMISSION TESTS =====

    @Test
    void evaluateRules_shouldEmitRuleTriggeredEvent_whenAlertCreated() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0"));
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(eventService).emitRuleEvent(
                eq(testOrganization),
                eq(testRule.getId()),
                eq(testRule.getName()),
                eq(Event.EventType.RULE_TRIGGERED),
                any(Event.EventSeverity.class),
                any(String.class)
        );
    }

    @Test
    void evaluateRules_shouldEmitAlertCreatedEvent_whenAlertCreated() {
        // Given
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0"));
        testRule.setVariable("voltage");

        Alert savedAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(testRule)
                .device(testDevice)
                .build();

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenReturn(savedAlert);

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(eventService).emitAlertEvent(
                eq(testOrganization),
                eq(savedAlert.getId()),
                eq(testDevice.getExternalId()),
                eq(Event.EventType.ALERT_CREATED),
                any(Event.EventSeverity.class),
                any(String.class)
        );
    }

    @Test
    void evaluateRules_shouldNotEmitEvents_whenOrganizationIsNull() {
        // Given
        testRule.setOrganization(null); // No organization
        testRule.setOperator(RuleOperator.GT);
        testRule.setThreshold(new BigDecimal("200.0"));
        testRule.setVariable("voltage");

        when(ruleRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(testRule));
        when(alertRepository.findRecentAlertsForRule(any(UUID.class), any(Instant.class)))
                .thenReturn(Collections.emptyList());
        when(alertRepository.save(any(Alert.class))).thenAnswer(i -> i.getArgument(0));

        // When
        ruleEngineService.evaluateRules(testTelemetry);

        // Then
        verify(alertRepository).save(any(Alert.class));
        verify(eventService, never()).emitRuleEvent(any(), any(), any(), any(), any(), any());
        verify(eventService, never()).emitAlertEvent(any(), any(), any(), any(), any(), any());
    }
}
