package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.expression.ExpressionEvaluator;
import org.sensorvision.expression.FunctionRegistry;
import org.sensorvision.model.*;
import org.sensorvision.repository.SyntheticVariableRepository;
import org.sensorvision.repository.SyntheticVariableValueRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SyntheticVariableService.
 * Tests expression evaluation, variable substitution, arithmetic operations, and error handling.
 */
@ExtendWith(MockitoExtension.class)
class SyntheticVariableServiceTest {

    @Mock
    private SyntheticVariableRepository syntheticVariableRepository;

    @Mock
    private SyntheticVariableValueRepository syntheticVariableValueRepository;

    @Mock
    private org.sensorvision.repository.TelemetryRecordRepository telemetryRecordRepository;

    private ExpressionEvaluator expressionEvaluator;

    private SyntheticVariableService syntheticVariableService;

    @Captor
    private ArgumentCaptor<SyntheticVariableValue> valueCaptor;

    private Organization organization;
    private Device device;
    private TelemetryRecord telemetryRecord;
    private SyntheticVariable syntheticVariable;

    @BeforeEach
    void setUp() {
        // Initialize real ExpressionEvaluator with FunctionRegistry
        FunctionRegistry functionRegistry = new FunctionRegistry();
        expressionEvaluator = new ExpressionEvaluator(functionRegistry);

        // Initialize service with real evaluator and mocked repositories
        syntheticVariableService = new SyntheticVariableService(
                syntheticVariableRepository,
                syntheticVariableValueRepository,
                expressionEvaluator,
                telemetryRecordRepository
        );

        organization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        device = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .organization(organization)
                .build();

        telemetryRecord = TelemetryRecord.builder()
                .id(UUID.randomUUID())
                .device(device)
                .timestamp(Instant.now())
                .kwConsumption(new BigDecimal("100.0"))
                .voltage(new BigDecimal("220.0"))
                .current(new BigDecimal("5.0"))
                .powerFactor(new BigDecimal("0.95"))
                .frequency(new BigDecimal("60.0"))
                .build();

        syntheticVariable = SyntheticVariable.builder()
                .id(UUID.randomUUID())
                .name("apparentPower")
                .expression("voltage * current")
                .description("Apparent Power (VA)")
                .unit("VA")
                .enabled(true)
                .device(device)
                .build();
    }

    // ===== BASIC EXPRESSION EVALUATION TESTS =====

    @Test
    void calculateSyntheticVariables_shouldEvaluateSimpleMultiplication() {
        // Given
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // voltage (220) * current (5) = 1100
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("1100.0"));
        assertThat(savedValue.getSyntheticVariable()).isEqualTo(syntheticVariable);
        assertThat(savedValue.getTelemetryRecord()).isEqualTo(telemetryRecord);
        assertThat(savedValue.getTimestamp()).isEqualTo(telemetryRecord.getTimestamp());
    }

    @Test
    void calculateSyntheticVariables_shouldEvaluateAddition() {
        // Given
        syntheticVariable.setExpression("kwConsumption + voltage");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // kwConsumption (100) + voltage (220) = 320
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("320.0"));
    }

    @Test
    void calculateSyntheticVariables_shouldEvaluateSubtraction() {
        // Given
        syntheticVariable.setExpression("voltage - kwConsumption");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // voltage (220) - kwConsumption (100) = 120
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("120.0"));
    }

    @Test
    void calculateSyntheticVariables_shouldEvaluateDivision() {
        // Given
        syntheticVariable.setExpression("kwConsumption / current");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // kwConsumption (100) / current (5) = 20
        BigDecimal expected = new BigDecimal("100.0")
                .divide(new BigDecimal("5.0"), 10, RoundingMode.HALF_UP);
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(expected);
    }

    // ===== OPERATOR PRECEDENCE TESTS =====

    @Test
    void calculateSyntheticVariables_shouldRespectOperatorPrecedence() {
        // Given: voltage + current * kwConsumption
        // Should calculate as: voltage + (current * kwConsumption) = 220 + (5 * 100) = 720
        syntheticVariable.setExpression("voltage + current * kwConsumption");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("720.0"));
    }

    @Test
    void calculateSyntheticVariables_shouldHandleParentheses() {
        // Given: (voltage + current) * kwConsumption
        // Should calculate as: (220 + 5) * 100 = 225 * 100 = 22500
        syntheticVariable.setExpression("(voltage + current) * kwConsumption");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("22500.0"));
    }

    @Test
    void calculateSyntheticVariables_shouldHandleNestedParentheses() {
        // Given: ((voltage + current) * kwConsumption) / frequency
        // Should calculate as: ((220 + 5) * 100) / 60 = 22500 / 60 = 375
        syntheticVariable.setExpression("((voltage + current) * kwConsumption) / frequency");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        BigDecimal expected = new BigDecimal("22500.0")
                .divide(new BigDecimal("60.0"), 10, RoundingMode.HALF_UP);
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(expected);
    }

    // ===== MULTIPLE VARIABLES TESTS =====

    @Test
    void calculateSyntheticVariables_shouldCalculateMultipleSyntheticVariables() {
        // Given
        SyntheticVariable var1 = SyntheticVariable.builder()
                .id(UUID.randomUUID())
                .name("apparentPower")
                .expression("voltage * current")
                .enabled(true)
                .device(device)
                .build();

        SyntheticVariable var2 = SyntheticVariable.builder()
                .id(UUID.randomUUID())
                .name("realPower")
                .expression("voltage * current * powerFactor")
                .enabled(true)
                .device(device)
                .build();

        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(var1, var2));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, times(2)).save(valueCaptor.capture());
        List<SyntheticVariableValue> savedValues = valueCaptor.getAllValues();

        // apparentPower: voltage (220) * current (5) = 1100
        assertThat(savedValues.get(0).getCalculatedValue()).isEqualByComparingTo(new BigDecimal("1100.0"));

        // realPower: voltage (220) * current (5) * powerFactor (0.95) = 1045
        assertThat(savedValues.get(1).getCalculatedValue()).isEqualByComparingTo(new BigDecimal("1045.0"));
    }

    // ===== ERROR HANDLING TESTS =====

    @Test
    void calculateSyntheticVariables_shouldNotSaveValue_whenExpressionIsNull() {
        // Given
        syntheticVariable.setExpression(null);
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    @Test
    void calculateSyntheticVariables_shouldNotSaveValue_whenExpressionIsEmpty() {
        // Given
        syntheticVariable.setExpression("");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    @Test
    void calculateSyntheticVariables_shouldNotSaveValue_whenVariableNotFoundInTelemetry() {
        // Given: expression references a variable that doesn't exist
        syntheticVariable.setExpression("voltage * nonExistentVariable");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    @Test
    void calculateSyntheticVariables_shouldNotSaveValue_whenDivisionByZero() {
        // Given: create a telemetry record with zero current
        TelemetryRecord recordWithZero = TelemetryRecord.builder()
                .id(UUID.randomUUID())
                .device(device)
                .timestamp(Instant.now())
                .kwConsumption(new BigDecimal("100.0"))
                .voltage(new BigDecimal("220.0"))
                .current(BigDecimal.ZERO)
                .build();

        syntheticVariable.setExpression("voltage / current");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));

        // When
        syntheticVariableService.calculateSyntheticVariables(recordWithZero);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    @Test
    void calculateSyntheticVariables_shouldNotSaveValue_whenExpressionHasInvalidCharacters() {
        // Given
        syntheticVariable.setExpression("voltage * current & 100");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    @Test
    void calculateSyntheticVariables_shouldHandleMissingTelemetryValue() {
        // Given: telemetry record with null values
        TelemetryRecord recordWithNulls = TelemetryRecord.builder()
                .id(UUID.randomUUID())
                .device(device)
                .timestamp(Instant.now())
                .kwConsumption(new BigDecimal("100.0"))
                .voltage(null)  // null voltage
                .current(new BigDecimal("5.0"))
                .build();

        syntheticVariable.setExpression("voltage * current");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(recordWithNulls);

        // Then - should treat null as 0 and calculate: 0 * 5 = 0
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    // ===== EMPTY/NO DATA TESTS =====

    @Test
    void calculateSyntheticVariables_shouldDoNothing_whenNoEnabledVariables() {
        // Given
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(Collections.emptyList());

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository, never()).save(any(SyntheticVariableValue.class));
    }

    // ===== COMPLEX EXPRESSION TESTS =====

    @Test
    void calculateSyntheticVariables_shouldHandleComplexExpression() {
        // Given: Real power calculation with all variables
        // realPower = (voltage * current * powerFactor) - (kwConsumption / frequency)
        syntheticVariable.setExpression("(voltage * current * powerFactor) - (kwConsumption / frequency)");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // (220 * 5 * 0.95) - (100 / 60)
        // = 1045 - 1.666... ≈ 1043.33
        // Verify the result is approximately correct (within reasonable range)
        assertThat(savedValue.getCalculatedValue()).isNotNull();
        assertThat(savedValue.getCalculatedValue().doubleValue()).isBetween(1043.0, 1044.0);
    }

    @Test
    void calculateSyntheticVariables_shouldHandleDecimalNumbers() {
        // Given
        syntheticVariable.setExpression("voltage * powerFactor");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // voltage (220) * powerFactor (0.95) = 209
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("209.0"));
    }

    @Test
    void calculateSyntheticVariables_shouldHandleAllSupportedVariables() {
        // Given: expression using all 5 supported variables
        syntheticVariable.setExpression("kwConsumption + voltage + current + powerFactor + frequency");
        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(syntheticVariable));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then
        verify(syntheticVariableValueRepository).save(valueCaptor.capture());
        SyntheticVariableValue savedValue = valueCaptor.getValue();

        // 100 + 220 + 5 + 0.95 + 60 = 385.95
        assertThat(savedValue.getCalculatedValue()).isEqualByComparingTo(new BigDecimal("385.95"));
    }

    @Test
    void calculateSyntheticVariables_shouldContinueOnError_whenOneVariableFails() {
        // Given: Two synthetic variables, first one fails
        SyntheticVariable failingVar = SyntheticVariable.builder()
                .id(UUID.randomUUID())
                .name("failing")
                .expression("voltage / 0")  // Division by zero
                .enabled(true)
                .device(device)
                .build();

        SyntheticVariable successVar = SyntheticVariable.builder()
                .id(UUID.randomUUID())
                .name("success")
                .expression("voltage * current")
                .enabled(true)
                .device(device)
                .build();

        when(syntheticVariableRepository.findByDeviceExternalIdAndEnabledTrue("test-device-001"))
                .thenReturn(List.of(failingVar, successVar));
        when(syntheticVariableValueRepository.save(any(SyntheticVariableValue.class)))
                .thenAnswer(i -> i.getArgument(0));

        // When
        syntheticVariableService.calculateSyntheticVariables(telemetryRecord);

        // Then - should save only the successful one
        verify(syntheticVariableValueRepository, times(1)).save(any(SyntheticVariableValue.class));
    }
}
