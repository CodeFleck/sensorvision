package io.indcloud.service;

import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import io.indcloud.config.TestBeanConfiguration;
import io.indcloud.model.*;
import io.indcloud.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration test for Synthetic Variables with Statistical Time-Series Functions.
 * Tests the complete pipeline from telemetry ingestion to calculated synthetic values.
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeanConfiguration.class)
@Transactional
class SyntheticVariableServiceIntegrationTest {

    @Autowired
    private SyntheticVariableService syntheticVariableService;

    @Autowired
    private SyntheticVariableRepository syntheticVariableRepository;

    @Autowired
    private SyntheticVariableValueRepository syntheticVariableValueRepository;

    @Autowired
    private TelemetryRecordRepository telemetryRecordRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private EntityManager entityManager;

    private Organization testOrg;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // Clean up any existing test data
        syntheticVariableValueRepository.deleteAll();
        syntheticVariableRepository.deleteAll();
        telemetryRecordRepository.deleteAll();
        deviceRepository.deleteAll();
        organizationRepository.deleteAll();

        // Create test organization
        testOrg = new Organization();
        testOrg.setName("Test Org");
        testOrg.setDescription("Test organization for integration tests");
        testOrg.setEnabled(true);
        testOrg = organizationRepository.save(testOrg);

        // Create test device
        testDevice = new Device();
        testDevice.setExternalId("test-device-001");
        testDevice.setName("Test Device");
        testDevice.setSensorType("smart_meter");
        testDevice.setOrganization(testOrg);
        testDevice.setActive(true);
        testDevice = deviceRepository.save(testDevice);
    }

    @Test
    void testBasicArithmeticSyntheticVariable() {
        // Create synthetic variable: apparent power = voltage * current
        SyntheticVariable synVar = createSyntheticVariable(
            "apparentPower",
            "voltage * current",
            "VA",
            true
        );

        // Create telemetry record
        TelemetryRecord telemetry = createTelemetryRecord(
            Instant.now(),
            new BigDecimal("220.0"),  // voltage
            new BigDecimal("5.0"),     // current
            null, null, null
        );

        // Calculate synthetic variables
        syntheticVariableService.calculateSyntheticVariables(telemetry);

        // Verify result
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("1100"); // 220 * 5
    }

    @Test
    void testMathFunctionSyntheticVariable() {
        // Create synthetic variable: rounded power
        SyntheticVariable synVar = createSyntheticVariable(
            "roundedPower",
            "round(voltage * current)",
            "VA",
            true
        );

        // Create telemetry record with decimal values
        TelemetryRecord telemetry = createTelemetryRecord(
            Instant.now(),
            new BigDecimal("220.7"),
            new BigDecimal("5.3"),
            null, null, null
        );

        // Calculate
        syntheticVariableService.calculateSyntheticVariables(telemetry);

        // Verify
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("1170"); // round(220.7 * 5.3)
    }

    @Test
    void testConditionalLogicSyntheticVariable() {
        // Create synthetic variable: overvoltage flag
        SyntheticVariable synVar = createSyntheticVariable(
            "overvoltageFlag",
            "if(voltage > 230, 1, 0)",
            "flag",
            true
        );

        // Test with normal voltage
        TelemetryRecord telemetry1 = createTelemetryRecord(
            Instant.now().minusSeconds(10),
            new BigDecimal("220.0"),
            new BigDecimal("5.0"),
            null, null, null
        );
        syntheticVariableService.calculateSyntheticVariables(telemetry1);

        // Test with overvoltage
        TelemetryRecord telemetry2 = createTelemetryRecord(
            Instant.now(),
            new BigDecimal("235.0"),
            new BigDecimal("5.0"),
            null, null, null
        );
        syntheticVariableService.calculateSyntheticVariables(telemetry2);

        // Verify
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(2);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("0"); // Normal
        assertThat(values.get(1).getCalculatedValue()).isEqualByComparingTo("1"); // Overvoltage
    }

    @Test
    void testStatisticalFunction_Average() {
        // Create synthetic variable: 5-minute average voltage
        SyntheticVariable synVar = createSyntheticVariable(
            "avgVoltage5m",
            "avg(\"voltage\", \"5m\")",
            "V",
            true
        );

        // Create historical telemetry data over 5 minutes
        // Use fixed base time and create records within the time window
        // All records are in the past relative to latestTime to ensure they're all queryable
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(250), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(200), new BigDecimal("222.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(150), new BigDecimal("218.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(100), new BigDecimal("225.0"), new BigDecimal("5.0"), null, null, null);
        // Latest record is also in the past but most recent, so query from its perspective sees all 5
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("215.0"), new BigDecimal("5.0"), null, null, null);

        // Flush and calculate synthetic variable for latest record
        flushAndCalculate(latest);

        // Verify: average of 220, 222, 218, 225, 215 = 1100/5 = 220
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("220");
    }

    @Test
    void testStatisticalFunction_StandardDeviation() {
        // Create synthetic variable: voltage volatility (stddev)
        SyntheticVariable synVar = createSyntheticVariable(
            "voltageVolatility",
            "stddev(\"voltage\", \"5m\")",
            "V",
            true
        );

        // Create data with some variance - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(250), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(200), new BigDecimal("230.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(150), new BigDecimal("210.0"), new BigDecimal("5.0"), null, null, null);
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: stddev should be > 0 (there is variance)
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue().doubleValue()).isGreaterThan(0);
        assertThat(values.get(0).getCalculatedValue().doubleValue()).isLessThan(15); // Reasonable range
    }

    @Test
    void testStatisticalFunction_Sum() {
        // Create synthetic variable: total consumption over 1 hour
        SyntheticVariable synVar = createSyntheticVariable(
            "hourlyConsumption",
            "sum(\"kwConsumption\", \"1h\")",
            "kWh",
            true
        );

        // Create hourly data - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(3550), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("10.0"), null, null);
        createTelemetryRecord(latestTime.minusSeconds(2400), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("15.0"), null, null);
        createTelemetryRecord(latestTime.minusSeconds(1200), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("20.0"), null, null);
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("25.0"), null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: sum = 10 + 15 + 20 + 25 = 70
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("70");
    }

    @Test
    void testStatisticalFunction_RateOfChange() {
        // Create synthetic variable: consumption growth rate
        SyntheticVariable synVar = createSyntheticVariable(
            "consumptionRate",
            "rate(\"kwConsumption\", \"1h\")",
            "kW/h",
            true
        );

        // Create data with increasing trend - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(3550), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("100.0"), null, null);
        createTelemetryRecord(latestTime.minusSeconds(1800), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("120.0"), null, null);
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("140.0"), null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: rate = (140 - 100) / 1 hour = 40 per hour
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("40");
    }

    @Test
    void testStatisticalFunction_PercentChange() {
        // Create synthetic variable: consumption percent change
        SyntheticVariable synVar = createSyntheticVariable(
            "consumptionChangePercent",
            "percentChange(\"kwConsumption\", \"1h\")",
            "%",
            true
        );

        // Create data: 100 -> 150 = 50% increase - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(3550), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("100.0"), null, null);
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("150.0"), null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: 50% increase
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("50");
    }

    @Test
    void testComplexExpression_SpikeDetection() {
        // Create synthetic variable: spike detection (consumption > 1.5x hourly average)
        SyntheticVariable synVar = createSyntheticVariable(
            "consumptionSpike",
            "if(kwConsumption > avg(\"kwConsumption\", \"1h\") * 1.5, 1, 0)",
            "flag",
            true
        );

        // Create hourly baseline data (average kwConsumption around 100) - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(3550), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("95.0"), null, null);
        createTelemetryRecord(latestTime.minusSeconds(2400), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("100.0"), null, null);
        createTelemetryRecord(latestTime.minusSeconds(1200), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("105.0"), null, null);

        // Create spike: 200 > (100 * 1.5) = 200 > 150 → TRUE
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("220.0"), new BigDecimal("5.0"), new BigDecimal("200.0"), null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: spike detected (flag = 1)
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("1");
    }

    @Test
    void testComplexExpression_AnomalyDetection() {
        // Create synthetic variable: anomaly = voltage significantly above historical average
        // Using a simpler threshold comparison instead of stddev to avoid mathematical edge cases
        // where the outlier value affects both the avg and stddev calculations
        SyntheticVariable synVar = createSyntheticVariable(
            "voltageAnomaly",
            "if(voltage > avg(\"voltage\", \"5m\") * 1.2, 1, 0)",  // Flag if voltage > 120% of average
            "flag",
            true
        );

        // Create stable baseline data (average 220) - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(250), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(200), new BigDecimal("221.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(150), new BigDecimal("219.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(100), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);

        // Create anomaly: voltage = 350V, avg will be (220+221+219+220+350)/5 = 246
        // Check: 350 > 246 * 1.2 = 295.2? YES! (350 > 295.2)
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("350.0"), new BigDecimal("5.0"), null, null, null);

        // Flush and calculate
        flushAndCalculate(latest);

        // Verify: anomaly detected (voltage 350 > avg 246 * 1.2 = 295.2)
        List<SyntheticVariableValue> values = syntheticVariableValueRepository
            .findBySyntheticVariableId(synVar.getId());

        assertThat(values).hasSize(1);
        assertThat(values.get(0).getCalculatedValue()).isEqualByComparingTo("1");
    }

    @Test
    void testMultipleSyntheticVariables() {
        // Create multiple synthetic variables
        SyntheticVariable var1 = createSyntheticVariable("apparentPower", "voltage * current", "VA", true);
        SyntheticVariable var2 = createSyntheticVariable("avgVoltage", "avg(\"voltage\", \"5m\")", "V", true);
        SyntheticVariable var3 = createSyntheticVariable("overloadFlag", "if(current > 8, 1, 0)", "flag", true);

        // Create telemetry with historical data - all timestamps in the past to ensure visibility
        Instant latestTime = Instant.now();
        createTelemetryRecord(latestTime.minusSeconds(250), new BigDecimal("220.0"), new BigDecimal("5.0"), null, null, null);
        createTelemetryRecord(latestTime.minusSeconds(150), new BigDecimal("225.0"), new BigDecimal("6.0"), null, null, null);
        TelemetryRecord latest = createTelemetryRecord(latestTime.minusSeconds(50), new BigDecimal("230.0"), new BigDecimal("9.0"), null, null, null);

        // Flush and calculate all synthetic variables
        flushAndCalculate(latest);

        // Verify all three were calculated
        List<SyntheticVariableValue> values1 = syntheticVariableValueRepository.findBySyntheticVariableId(var1.getId());
        List<SyntheticVariableValue> values2 = syntheticVariableValueRepository.findBySyntheticVariableId(var2.getId());
        List<SyntheticVariableValue> values3 = syntheticVariableValueRepository.findBySyntheticVariableId(var3.getId());

        assertThat(values1).hasSize(1);
        assertThat(values1.get(0).getCalculatedValue()).isEqualByComparingTo("2070"); // 230 * 9

        assertThat(values2).hasSize(1);
        assertThat(values2.get(0).getCalculatedValue()).isEqualByComparingTo("225"); // avg(220, 225, 230)

        assertThat(values3).hasSize(1);
        assertThat(values3.get(0).getCalculatedValue()).isEqualByComparingTo("1"); // 9 > 8
    }

    // Helper methods

    private SyntheticVariable createSyntheticVariable(String name, String expression, String unit, boolean enabled) {
        SyntheticVariable var = new SyntheticVariable();
        var.setName(name);
        var.setExpression(expression);
        var.setUnit(unit);
        var.setEnabled(enabled);
        var.setDevice(testDevice);
        var.setOrganization(testOrg);
        return syntheticVariableRepository.save(var);
    }

    private TelemetryRecord createTelemetryRecord(Instant timestamp, BigDecimal voltage, BigDecimal current,
                                                    BigDecimal kwConsumption, BigDecimal powerFactor, BigDecimal frequency) {
        TelemetryRecord record = new TelemetryRecord();
        record.setDevice(testDevice);
        record.setOrganization(testOrg);  // Required field - set organization from test context
        record.setTimestamp(timestamp);
        record.setVoltage(voltage);
        record.setCurrent(current);
        record.setKwConsumption(kwConsumption != null ? kwConsumption : BigDecimal.ZERO);
        record.setPowerFactor(powerFactor != null ? powerFactor : BigDecimal.ONE);
        record.setFrequency(frequency != null ? frequency : new BigDecimal("60.0"));
        return telemetryRecordRepository.saveAndFlush(record);
    }

    /**
     * Flush all pending changes to the database and calculate synthetic variables.
     * This ensures statistical functions can query the telemetry data that was just saved.
     */
    private void flushAndCalculate(TelemetryRecord record) {
        entityManager.flush();
        syntheticVariableService.calculateSyntheticVariables(record);
    }
}
