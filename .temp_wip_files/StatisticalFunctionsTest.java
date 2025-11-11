package org.sensorvision.expression.functions;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.expression.ContextualExpressionFunction;
import org.sensorvision.expression.StatisticalFunctionContext;
import org.sensorvision.model.Device;
import org.sensorvision.model.TelemetryRecord;
import org.sensorvision.repository.TelemetryRecordRepository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StatisticalFunctionsTest {

    @Mock
    private TelemetryRecordRepository telemetryRepository;

    private StatisticalFunctionContext context;
    private Instant currentTime;

    @BeforeEach
    void setUp() {
        currentTime = Instant.parse("2025-11-11T12:00:00Z");
        context = StatisticalFunctionContext.builder()
                .deviceExternalId("device-001")
                .currentTimestamp(currentTime)
                .telemetryRepository(telemetryRepository)
                .build();
    }

    private List<TelemetryRecord> createMockRecords(double... values) {
        List<TelemetryRecord> records = new ArrayList<>();
        Device device = new Device();
        device.setExternalId("device-001");

        for (int i = 0; i < values.length; i++) {
            TelemetryRecord record = new TelemetryRecord();
            record.setDevice(device);
            record.setVoltage(BigDecimal.valueOf(values[i]));
            record.setTimestamp(currentTime.minusSeconds(300 * (values.length - i - 1)));
            records.add(record);
        }
        return records;
    }

    @Test
    void testAvg_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(220.0, 225.0, 230.0, 215.0, 220.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();
        BigDecimal result = avgFunc.evaluateWithContext(context, "voltage", "5m");

        assertEquals(new BigDecimal("222.0"), result);
    }

    @Test
    void testAvg_WithNoData() {
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();
        BigDecimal result = avgFunc.evaluateWithContext(context, "voltage", "5m");

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testStddev_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(10.0, 12.0, 23.0, 23.0, 16.0, 23.0, 21.0, 16.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction stddevFunc = StatisticalFunctions.stddev();
        BigDecimal result = stddevFunc.evaluateWithContext(context, "voltage", "5m");

        // Expected stddev should be around 5.237
        assertTrue(result.doubleValue() > 5.0 && result.doubleValue() < 5.5);
    }

    @Test
    void testStddev_WithSingleValue() {
        List<TelemetryRecord> records = createMockRecords(220.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction stddevFunc = StatisticalFunctions.stddev();
        BigDecimal result = stddevFunc.evaluateWithContext(context, "voltage", "5m");

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    void testSum_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(10.0, 20.0, 30.0, 40.0, 50.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction sumFunc = StatisticalFunctions.sum();
        BigDecimal result = sumFunc.evaluateWithContext(context, "voltage", "1h");

        assertEquals(new BigDecimal("150.0"), result);
    }

    @Test
    void testCount_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(1.0, 2.0, 3.0, 4.0, 5.0, 6.0, 7.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction countFunc = StatisticalFunctions.count();
        BigDecimal result = countFunc.evaluateWithContext(context, "voltage", "1h");

        assertEquals(new BigDecimal("7"), result);
    }

    @Test
    void testMinTime_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(225.0, 210.0, 230.0, 215.0, 220.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction minFunc = StatisticalFunctions.minTime();
        BigDecimal result = minFunc.evaluateWithContext(context, "voltage", "24h");

        assertEquals(new BigDecimal("210.0"), result);
    }

    @Test
    void testMaxTime_WithMultipleValues() {
        List<TelemetryRecord> records = createMockRecords(225.0, 210.0, 230.0, 215.0, 220.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction maxFunc = StatisticalFunctions.maxTime();
        BigDecimal result = maxFunc.evaluateWithContext(context, "voltage", "24h");

        assertEquals(new BigDecimal("230.0"), result);
    }

    @Test
    void testRate_WithIncreasingValues() {
        List<TelemetryRecord> records = createMockRecords(100.0, 110.0, 120.0, 130.0, 140.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction rateFunc = StatisticalFunctions.rate();
        BigDecimal result = rateFunc.evaluateWithContext(context, "voltage", "1h");

        // Change from 100 to 140 over 1 hour = 40 per hour
        assertEquals(new BigDecimal("40"), result);
    }

    @Test
    void testRate_WithDecreasingValues() {
        List<TelemetryRecord> records = createMockRecords(140.0, 130.0, 120.0, 110.0, 100.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction rateFunc = StatisticalFunctions.rate();
        BigDecimal result = rateFunc.evaluateWithContext(context, "voltage", "1h");

        // Change from 140 to 100 over 1 hour = -40 per hour
        assertEquals(new BigDecimal("-40"), result);
    }

    @Test
    void testMovingAvg_SameAsAvg() {
        List<TelemetryRecord> records = createMockRecords(220.0, 225.0, 230.0, 215.0, 220.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction movingAvgFunc = StatisticalFunctions.movingAvg();
        BigDecimal result = movingAvgFunc.evaluateWithContext(context, "voltage", "15m");

        assertEquals(new BigDecimal("222.0"), result);
    }

    @Test
    void testPercentChange_WithIncrease() {
        List<TelemetryRecord> records = createMockRecords(100.0, 110.0, 120.0, 130.0, 150.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction percentChangeFunc = StatisticalFunctions.percentChange();
        BigDecimal result = percentChangeFunc.evaluateWithContext(context, "voltage", "1h");

        // (150 - 100) / 100 * 100 = 50%
        assertEquals(new BigDecimal("50"), result);
    }

    @Test
    void testPercentChange_WithDecrease() {
        List<TelemetryRecord> records = createMockRecords(200.0, 180.0, 160.0, 140.0, 100.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction percentChangeFunc = StatisticalFunctions.percentChange();
        BigDecimal result = percentChangeFunc.evaluateWithContext(context, "voltage", "1h");

        // (100 - 200) / 200 * 100 = -50%
        assertEquals(new BigDecimal("-50"), result);
    }

    @Test
    void testMedian_WithOddNumberOfValues() {
        List<TelemetryRecord> records = createMockRecords(10.0, 20.0, 30.0, 40.0, 50.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction medianFunc = StatisticalFunctions.median();
        BigDecimal result = medianFunc.evaluateWithContext(context, "voltage", "1h");

        assertEquals(new BigDecimal("30.0"), result);
    }

    @Test
    void testMedian_WithEvenNumberOfValues() {
        List<TelemetryRecord> records = createMockRecords(10.0, 20.0, 30.0, 40.0);
        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction medianFunc = StatisticalFunctions.median();
        BigDecimal result = medianFunc.evaluateWithContext(context, "voltage", "1h");

        assertEquals(new BigDecimal("25.0"), result); // (20 + 30) / 2
    }

    @Test
    void testInvalidVariableName() {
        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();

        assertThrows(IllegalArgumentException.class,
            () -> avgFunc.evaluateWithContext(context, "invalidVariable", "5m"));
    }

    @Test
    void testInvalidTimeWindow() {
        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();

        assertThrows(IllegalArgumentException.class,
            () -> avgFunc.evaluateWithContext(context, "voltage", "10m"));
    }

    @Test
    void testWithoutContext() {
        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();

        assertThrows(IllegalStateException.class,
            () -> avgFunc.evaluateWithContext(null, "voltage", "5m"));
    }

    @Test
    void testIncorrectArgumentCount() {
        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();

        assertThrows(IllegalArgumentException.class,
            () -> avgFunc.evaluateWithContext(context, "voltage")); // Missing time window
    }

    @Test
    void testDifferentVariableNames() {
        // Test kwConsumption
        List<TelemetryRecord> records = new ArrayList<>();
        Device device = new Device();
        device.setExternalId("device-001");

        TelemetryRecord record = new TelemetryRecord();
        record.setDevice(device);
        record.setKwConsumption(BigDecimal.valueOf(50.5));
        record.setTimestamp(currentTime);
        records.add(record);

        when(telemetryRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestampAsc(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(records);

        ContextualExpressionFunction avgFunc = StatisticalFunctions.avg();
        BigDecimal result = avgFunc.evaluateWithContext(context, "kwConsumption", "5m");

        assertEquals(new BigDecimal("50.5"), result);
    }
}
