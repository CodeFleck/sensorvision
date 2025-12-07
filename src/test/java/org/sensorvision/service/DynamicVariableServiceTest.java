package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Variable;
import org.sensorvision.model.Variable.DataSource;
import org.sensorvision.model.VariableValue;
import org.sensorvision.repository.VariableRepository;
import org.sensorvision.repository.VariableValueRepository;
import org.springframework.dao.DataIntegrityViolationException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DynamicVariableServiceTest {

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private VariableValueRepository variableValueRepository;

    private DynamicVariableService dynamicVariableService;

    private Organization testOrg;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        dynamicVariableService = new DynamicVariableService(variableRepository, variableValueRepository);

        testOrg = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .status(DeviceStatus.ONLINE)
                .organization(testOrg)
                .build();
    }

    @Test
    void getOrCreateVariable_existingVariable_shouldReturnExisting() {
        // Arrange
        Variable existingVariable = Variable.builder()
                .id(1L)
                .device(testDevice)
                .organization(testOrg)
                .name("temperature")
                .displayName("Temperature")
                .dataSource(DataSource.AUTO)
                .build();

        when(variableRepository.findByDeviceAndName(testDevice, "temperature"))
                .thenReturn(Optional.of(existingVariable));

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "temperature");

        // Assert
        assertNotNull(result);
        assertEquals("temperature", result.getName());
        assertEquals(existingVariable.getId(), result.getId());
        verify(variableRepository).findByDeviceAndName(testDevice, "temperature");
        verify(variableRepository, never()).save(any());
    }

    @Test
    void getOrCreateVariable_newVariable_shouldAutoProvision() {
        // Arrange
        when(variableRepository.findByDeviceAndName(testDevice, "humidity"))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> {
                    Variable v = invocation.getArgument(0);
                    v.setId(2L);
                    return v;
                });

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "humidity");

        // Assert
        assertNotNull(result);
        assertEquals("humidity", result.getName());
        assertEquals("Humidity", result.getDisplayName()); // Humanized
        assertEquals(DataSource.AUTO, result.getDataSource());
        assertEquals(testDevice, result.getDevice());
        assertEquals(testOrg, result.getOrganization());

        verify(variableRepository).findByDeviceAndName(testDevice, "humidity");
        verify(variableRepository).save(any(Variable.class));
    }

    @Test
    void getOrCreateVariable_snakeCaseName_shouldHumanizeDisplayName() {
        // Arrange
        when(variableRepository.findByDeviceAndName(testDevice, "kw_consumption"))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "kw_consumption");

        // Assert
        assertEquals("Kw Consumption", result.getDisplayName());
    }

    @Test
    void getOrCreateVariable_camelCaseName_shouldHumanizeDisplayName() {
        // Arrange
        when(variableRepository.findByDeviceAndName(testDevice, "powerFactor"))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "powerFactor");

        // Assert
        assertEquals("Power Factor", result.getDisplayName());
    }

    @Test
    void recordValue_shouldCreateVariableValueAndUpdateLastValue() {
        // Arrange
        Variable variable = Variable.builder()
                .id(1L)
                .device(testDevice)
                .organization(testOrg)
                .name("temperature")
                .displayName("Temperature")
                .dataSource(DataSource.AUTO)
                .build();

        BigDecimal value = new BigDecimal("23.5");
        Instant timestamp = Instant.now();

        when(variableValueRepository.save(any(VariableValue.class)))
                .thenAnswer(invocation -> {
                    VariableValue vv = invocation.getArgument(0);
                    vv.setId(UUID.randomUUID());
                    return vv;
                });
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        VariableValue result = dynamicVariableService.recordValue(variable, value, timestamp, null);

        // Assert
        assertNotNull(result);
        assertEquals(value, result.getValue());
        assertEquals(timestamp, result.getTimestamp());
        assertEquals(variable, result.getVariable());

        // Verify variable's lastValue was updated
        ArgumentCaptor<Variable> variableCaptor = ArgumentCaptor.forClass(Variable.class);
        verify(variableRepository).save(variableCaptor.capture());
        Variable savedVariable = variableCaptor.getValue();
        assertEquals(value, savedVariable.getLastValue());
        assertEquals(timestamp, savedVariable.getLastValueAt());
    }

    @Test
    void recordValue_withOlderTimestamp_shouldNotUpdateLastValue() {
        // Arrange
        Instant newerTimestamp = Instant.now();
        Instant olderTimestamp = newerTimestamp.minusSeconds(3600); // 1 hour ago

        Variable variable = Variable.builder()
                .id(1L)
                .device(testDevice)
                .organization(testOrg)
                .name("temperature")
                .lastValue(new BigDecimal("25.0"))
                .lastValueAt(newerTimestamp)
                .build();

        BigDecimal value = new BigDecimal("23.5");

        when(variableValueRepository.save(any(VariableValue.class)))
                .thenAnswer(invocation -> {
                    VariableValue vv = invocation.getArgument(0);
                    vv.setId(UUID.randomUUID());
                    return vv;
                });

        // Act
        dynamicVariableService.recordValue(variable, value, olderTimestamp, null);

        // Assert - variable should NOT be saved (lastValue not updated)
        verify(variableValueRepository).save(any(VariableValue.class));
        verify(variableRepository, never()).save(any(Variable.class));
    }

    @Test
    void processTelemetry_shouldProcessAllVariables() {
        // Arrange
        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put("temperature", new BigDecimal("23.5"));
        variables.put("humidity", new BigDecimal("65.0"));
        variables.put("pressure", new BigDecimal("1013.25"));

        Instant timestamp = Instant.now();

        when(variableRepository.findByDeviceAndName(eq(testDevice), anyString()))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> {
                    Variable v = invocation.getArgument(0);
                    v.setId((long) (Math.random() * 1000));
                    return v;
                });
        when(variableValueRepository.save(any(VariableValue.class)))
                .thenAnswer(invocation -> {
                    VariableValue vv = invocation.getArgument(0);
                    vv.setId(UUID.randomUUID());
                    return vv;
                });

        // Act
        Map<String, VariableValue> result = dynamicVariableService.processTelemetry(
                testDevice, variables, timestamp, null);

        // Assert
        assertEquals(3, result.size());
        assertTrue(result.containsKey("temperature"));
        assertTrue(result.containsKey("humidity"));
        assertTrue(result.containsKey("pressure"));

        // Verify 3 variables were created and 3 values saved
        verify(variableRepository, times(3)).findByDeviceAndName(eq(testDevice), anyString());
        verify(variableRepository, times(6)).save(any(Variable.class)); // 3 creates + 3 lastValue updates
        verify(variableValueRepository, times(3)).save(any(VariableValue.class));
    }

    @Test
    void processTelemetry_withNullValue_shouldSkipVariable() {
        // Arrange
        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put("temperature", new BigDecimal("23.5"));
        variables.put("humidity", null);

        Instant timestamp = Instant.now();

        when(variableRepository.findByDeviceAndName(eq(testDevice), eq("temperature")))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> {
                    Variable v = invocation.getArgument(0);
                    v.setId(1L);
                    return v;
                });
        when(variableValueRepository.save(any(VariableValue.class)))
                .thenAnswer(invocation -> {
                    VariableValue vv = invocation.getArgument(0);
                    vv.setId(UUID.randomUUID());
                    return vv;
                });

        // Act
        Map<String, VariableValue> result = dynamicVariableService.processTelemetry(
                testDevice, variables, timestamp, null);

        // Assert
        assertEquals(1, result.size());
        assertTrue(result.containsKey("temperature"));
        assertFalse(result.containsKey("humidity"));
    }

    @Test
    void processTelemetry_withEmptyMap_shouldReturnEmpty() {
        // Arrange
        Map<String, BigDecimal> variables = Collections.emptyMap();
        Instant timestamp = Instant.now();

        // Act
        Map<String, VariableValue> result = dynamicVariableService.processTelemetry(
                testDevice, variables, timestamp, null);

        // Assert
        assertTrue(result.isEmpty());
        verify(variableRepository, never()).findByDeviceAndName(any(), anyString());
        verify(variableValueRepository, never()).save(any());
    }

    @Test
    void processTelemetry_withNullMap_shouldReturnEmpty() {
        // Act
        Map<String, VariableValue> result = dynamicVariableService.processTelemetry(
                testDevice, null, Instant.now(), null);

        // Assert
        assertTrue(result.isEmpty());
    }

    @Test
    void getDeviceVariables_shouldReturnOrderedList() {
        // Arrange
        List<Variable> variables = Arrays.asList(
                Variable.builder().id(1L).name("alpha").build(),
                Variable.builder().id(2L).name("beta").build()
        );

        when(variableRepository.findByDeviceIdOrderByNameAsc(testDevice.getId()))
                .thenReturn(variables);

        // Act
        List<Variable> result = dynamicVariableService.getDeviceVariables(testDevice.getId());

        // Assert
        assertEquals(2, result.size());
        assertEquals("alpha", result.get(0).getName());
        assertEquals("beta", result.get(1).getName());
    }

    @Test
    void getLatestValues_shouldReturnMapOfVariableNamesToValues() {
        // Arrange
        List<Variable> variables = Arrays.asList(
                Variable.builder()
                        .id(1L)
                        .name("temperature")
                        .lastValue(new BigDecimal("23.5"))
                        .build(),
                Variable.builder()
                        .id(2L)
                        .name("humidity")
                        .lastValue(new BigDecimal("65.0"))
                        .build(),
                Variable.builder()
                        .id(3L)
                        .name("pressure")
                        .lastValue(null) // No value yet
                        .build()
        );

        when(variableRepository.findByDeviceId(testDevice.getId()))
                .thenReturn(variables);

        // Act
        Map<String, BigDecimal> result = dynamicVariableService.getLatestValues(testDevice.getId());

        // Assert
        assertEquals(2, result.size()); // Only 2 with values
        assertEquals(new BigDecimal("23.5"), result.get("temperature"));
        assertEquals(new BigDecimal("65.0"), result.get("humidity"));
        assertFalse(result.containsKey("pressure"));
    }

    @Test
    void getStatistics_shouldReturnAggregatedValues() {
        // Arrange
        Long variableId = 1L;
        Instant startTime = Instant.now().minusSeconds(3600);
        Instant endTime = Instant.now();

        when(variableValueRepository.calculateAverageByVariableIdAndTimeRange(variableId, startTime, endTime))
                .thenReturn(23.5);
        when(variableValueRepository.calculateMinByVariableIdAndTimeRange(variableId, startTime, endTime))
                .thenReturn(20.0);
        when(variableValueRepository.calculateMaxByVariableIdAndTimeRange(variableId, startTime, endTime))
                .thenReturn(27.0);
        when(variableValueRepository.calculateSumByVariableIdAndTimeRange(variableId, startTime, endTime))
                .thenReturn(235.0);
        when(variableValueRepository.countByVariableIdAndTimeRange(variableId, startTime, endTime))
                .thenReturn(10L);

        // Act
        DynamicVariableService.VariableStatistics result =
                dynamicVariableService.getStatistics(variableId, startTime, endTime);

        // Assert
        assertEquals(23.5, result.average());
        assertEquals(20.0, result.min());
        assertEquals(27.0, result.max());
        assertEquals(235.0, result.sum());
        assertEquals(10L, result.count());
    }

    // ==================== Race Condition Tests ====================

    @Test
    void getOrCreateVariable_raceCondition_shouldReturnExistingVariableAfterConflict() {
        // Arrange - Simulate race condition where another thread creates the variable
        // between our check and our save
        Variable existingVariable = Variable.builder()
                .id(1L)
                .device(testDevice)
                .organization(testOrg)
                .name("temperature")
                .displayName("Temperature")
                .dataSource(DataSource.AUTO)
                .build();

        // First call returns empty (variable doesn't exist yet)
        // Second call (after exception) returns the variable created by another thread
        when(variableRepository.findByDeviceAndName(testDevice, "temperature"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingVariable));

        // Save throws DataIntegrityViolationException (unique constraint violation)
        when(variableRepository.save(any(Variable.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "temperature");

        // Assert
        assertNotNull(result);
        assertEquals("temperature", result.getName());
        assertEquals(existingVariable.getId(), result.getId());

        // Verify findByDeviceAndName was called twice (once initially, once after exception)
        verify(variableRepository, times(2)).findByDeviceAndName(testDevice, "temperature");
        // Verify save was attempted once
        verify(variableRepository, times(1)).save(any(Variable.class));
    }

    @Test
    void getOrCreateVariable_raceCondition_shouldThrowIfVariableNotFoundAfterConflict() {
        // Arrange - Edge case: constraint violation but variable still not found
        // (shouldn't happen in practice, but we need to handle it)
        when(variableRepository.findByDeviceAndName(testDevice, "temperature"))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.empty()); // Still not found after exception

        when(variableRepository.save(any(Variable.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () ->
                dynamicVariableService.getOrCreateVariable(testDevice, "temperature"));

        assertTrue(exception.getMessage().contains("Failed to create or find variable"));
        assertTrue(exception.getCause() instanceof DataIntegrityViolationException);
    }

    @Test
    void getOrCreateVariable_noRaceCondition_shouldCreateNormally() {
        // Arrange - Normal case: no race condition
        when(variableRepository.findByDeviceAndName(testDevice, "newVariable"))
                .thenReturn(Optional.empty());
        when(variableRepository.save(any(Variable.class)))
                .thenAnswer(invocation -> {
                    Variable v = invocation.getArgument(0);
                    v.setId(100L);
                    return v;
                });

        // Act
        Variable result = dynamicVariableService.getOrCreateVariable(testDevice, "newVariable");

        // Assert
        assertNotNull(result);
        assertEquals("newVariable", result.getName());
        assertEquals(100L, result.getId());

        // Verify findByDeviceAndName was called only once (no retry needed)
        verify(variableRepository, times(1)).findByDeviceAndName(testDevice, "newVariable");
        verify(variableRepository, times(1)).save(any(Variable.class));
    }
}
