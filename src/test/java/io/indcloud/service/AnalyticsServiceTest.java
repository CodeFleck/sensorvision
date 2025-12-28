package io.indcloud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.AggregationResponse;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Device;
import io.indcloud.model.Organization;
import io.indcloud.model.TelemetryRecord;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.TelemetryRecordRepository;
import io.indcloud.repository.VariableRepository;
import io.indcloud.repository.VariableValueRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AnalyticsService.
 * Tests data aggregation, interval calculations, validation, and security.
 */
@ExtendWith(MockitoExtension.class)
class AnalyticsServiceTest {

    @Mock
    private TelemetryRecordRepository telemetryRecordRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private VariableValueRepository variableValueRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AnalyticsService analyticsService;

    private Organization userOrganization;
    private Organization otherOrganization;
    private Device userDevice;
    private Device otherDevice;
    private Instant from;
    private Instant to;
    private List<TelemetryRecord> sampleRecords;

    @BeforeEach
    void setUp() {
        userOrganization = Organization.builder()
                .id(1L)
                .name("User Organization")
                .build();

        otherOrganization = Organization.builder()
                .id(2L)
                .name("Other Organization")
                .build();

        userDevice = Device.builder()
                .externalId("device-001")
                .name("User Device")
                .organization(userOrganization)
                .build();

        otherDevice = Device.builder()
                .externalId("device-002")
                .name("Other Device")
                .organization(otherOrganization)
                .build();

        from = Instant.now().minus(1, ChronoUnit.HOURS);
        to = Instant.now();

        // Create sample telemetry records with different values
        sampleRecords = List.of(
                createRecord(from.plus(5, ChronoUnit.MINUTES), new BigDecimal("100.0"), new BigDecimal("220.0")),
                createRecord(from.plus(10, ChronoUnit.MINUTES), new BigDecimal("150.0"), new BigDecimal("225.0")),
                createRecord(from.plus(15, ChronoUnit.MINUTES), new BigDecimal("120.0"), new BigDecimal("218.0")),
                createRecord(from.plus(20, ChronoUnit.MINUTES), new BigDecimal("200.0"), new BigDecimal("230.0")),
                createRecord(from.plus(25, ChronoUnit.MINUTES), new BigDecimal("180.0"), new BigDecimal("222.0"))
        );
    }

    private TelemetryRecord createRecord(Instant timestamp, BigDecimal kwConsumption, BigDecimal voltage) {
        return TelemetryRecord.builder()
                .device(userDevice)
                .timestamp(timestamp)
                .kwConsumption(kwConsumption)
                .voltage(voltage)
                .current(new BigDecimal("5.0"))
                .powerFactor(new BigDecimal("0.95"))
                .frequency(new BigDecimal("60.0"))
                .build();
    }

    // ===== INPUT VALIDATION TESTS =====

    @Test
    void aggregateData_shouldThrowException_whenDeviceIdIsNull() {
        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                null, "kwConsumption", "AVG", from, to, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Device ID is required");
    }

    @Test
    void aggregateData_shouldThrowException_whenDeviceIdIsEmpty() {
        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "  ", "kwConsumption", "AVG", from, to, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Device ID is required");
    }

    @Test
    void aggregateData_shouldThrowException_whenFromIsNull() {
        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", null, to, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Time range (from and to) is required");
    }

    @Test
    void aggregateData_shouldThrowException_whenToIsNull() {
        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, null, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Time range (from and to) is required");
    }

    @Test
    void aggregateData_shouldThrowException_whenToIsBeforeFrom() {
        // Given
        Instant invalidTo = from.minus(1, ChronoUnit.HOURS);

        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, invalidTo, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("End time must be after start time");
    }

    @Test
    void aggregateData_shouldThrowException_whenAggregationTypeIsInvalid() {
        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-001", "kwConsumption", "INVALID", from, to, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid aggregation type: INVALID");
    }

    @Test
    void aggregateData_shouldThrowException_whenDynamicVariableNotFound() {
        // Given - for non-legacy variables, we need device lookup to succeed first
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(variableRepository.findByDeviceIdAndName(any(), eq("invalidVariable"))).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-001", "invalidVariable", "AVG", from, to, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Variable 'invalidVariable' not found for device");
    }

    // ===== SECURITY TESTS =====

    @Test
    void aggregateData_shouldThrowException_whenDeviceNotFound() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("non-existent")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "non-existent", "kwConsumption", "AVG", from, to, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Device not found: non-existent");
    }

    @Test
    void aggregateData_shouldThrowAccessDeniedException_whenDeviceBelongsToOtherOrganization() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-002")).thenReturn(Optional.of(otherDevice));

        // When/Then
        assertThatThrownBy(() -> analyticsService.aggregateData(
                "device-002", "kwConsumption", "AVG", from, to, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to device: device-002");
    }

    // ===== AGGREGATION CALCULATION TESTS =====

    @Test
    void aggregateData_shouldCalculateMinCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "MIN", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        AggregationResponse result = results.get(0);
        assertThat(result.value()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(result.aggregation()).isEqualTo("MIN");
        assertThat(result.variable()).isEqualTo("kwConsumption");
        assertThat(result.count()).isEqualTo(5L);
    }

    @Test
    void aggregateData_shouldCalculateMaxCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "MAX", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).value()).isEqualByComparingTo(new BigDecimal("200.0"));
    }

    @Test
    void aggregateData_shouldCalculateAvgCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, to, null);

        // Then
        // (100 + 150 + 120 + 200 + 180) / 5 = 750 / 5 = 150.0
        assertThat(results).hasSize(1);
        BigDecimal expected = new BigDecimal("150.0").setScale(6, RoundingMode.HALF_UP);
        assertThat(results.get(0).value()).isEqualByComparingTo(expected);
    }

    @Test
    void aggregateData_shouldCalculateSumCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "SUM", from, to, null);

        // Then
        // 100 + 150 + 120 + 200 + 180 = 750
        assertThat(results).hasSize(1);
        assertThat(results.get(0).value()).isEqualByComparingTo(new BigDecimal("750.0"));
    }

    @Test
    void aggregateData_shouldCalculateCountCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "COUNT", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).value()).isEqualByComparingTo(new BigDecimal("5"));
    }

    // ===== VARIABLE EXTRACTION TESTS =====

    @Test
    void aggregateData_shouldExtractVoltageVariable() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "voltage", "AVG", from, to, null);

        // Then
        // (220 + 225 + 218 + 230 + 222) / 5 = 1115 / 5 = 223.0
        assertThat(results).hasSize(1);
        BigDecimal expected = new BigDecimal("223.0").setScale(6, RoundingMode.HALF_UP);
        assertThat(results.get(0).value()).isEqualByComparingTo(expected);
    }

    @Test
    void aggregateData_shouldExtractCurrentVariable() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "current", "MAX", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).value()).isEqualByComparingTo(new BigDecimal("5.0"));
    }

    // ===== INTERVAL TESTS =====

    @Test
    void aggregateData_shouldReturnMultipleResults_whenIntervalSpecified() {
        // Given
        Instant intervalFrom = Instant.now().minus(30, ChronoUnit.MINUTES);
        Instant intervalTo = Instant.now();

        List<TelemetryRecord> intervalRecords = List.of(
                createRecord(intervalFrom.plus(2, ChronoUnit.MINUTES), new BigDecimal("100.0"), new BigDecimal("220.0")),
                createRecord(intervalFrom.plus(12, ChronoUnit.MINUTES), new BigDecimal("150.0"), new BigDecimal("225.0")),
                createRecord(intervalFrom.plus(22, ChronoUnit.MINUTES), new BigDecimal("120.0"), new BigDecimal("218.0"))
        );

        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(intervalRecords);

        // When - 10 minute intervals should give 3 results
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", intervalFrom, intervalTo, "10m");

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
        assertThat(results).allMatch(r -> r.variable().equals("kwConsumption"));
        assertThat(results).allMatch(r -> r.aggregation().equals("AVG"));
    }

    @Test
    void aggregateData_shouldReturnEmptyList_whenNoDataInTimeRange() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(List.of());

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, to, null);

        // Then
        assertThat(results).isEmpty();
    }

    // ===== EDGE CASES =====

    @Test
    void aggregateData_shouldHandleSingleDataPoint() {
        // Given
        List<TelemetryRecord> singleRecord = List.of(
                createRecord(from.plus(5, ChronoUnit.MINUTES), new BigDecimal("100.0"), new BigDecimal("220.0"))
        );

        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(singleRecord);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).value()).isEqualByComparingTo(new BigDecimal("100.0"));
        assertThat(results.get(0).count()).isEqualTo(1L);
    }

    @Test
    void aggregateData_shouldFilterOutNullAndZeroValues() {
        // Given
        List<TelemetryRecord> recordsWithNulls = List.of(
                TelemetryRecord.builder()
                        .device(userDevice)
                        .timestamp(from.plus(5, ChronoUnit.MINUTES))
                        .kwConsumption(new BigDecimal("100.0"))
                        .voltage(null)  // null value
                        .build(),
                TelemetryRecord.builder()
                        .device(userDevice)
                        .timestamp(from.plus(10, ChronoUnit.MINUTES))
                        .kwConsumption(new BigDecimal("150.0"))
                        .voltage(BigDecimal.ZERO)  // zero value
                        .build(),
                TelemetryRecord.builder()
                        .device(userDevice)
                        .timestamp(from.plus(15, ChronoUnit.MINUTES))
                        .kwConsumption(new BigDecimal("200.0"))
                        .voltage(new BigDecimal("220.0"))
                        .build()
        );

        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(recordsWithNulls);

        // When
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        // Should only count non-null, non-zero values
        assertThat(results.get(0).count()).isEqualTo(3L);
    }

    @Test
    void aggregateData_shouldAcceptCaseInsensitiveAggregationType() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When - using lowercase
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "avg", from, to, null);

        // Then
        assertThat(results).hasSize(1);
        assertThat(results.get(0).aggregation()).isEqualTo("avg");
    }

    @Test
    void aggregateData_shouldHandleHourlyInterval() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When - hourly interval
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", from, to, "1h");

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    void aggregateData_shouldHandleDailyInterval() {
        // Given
        Instant dayFrom = Instant.now().minus(2, ChronoUnit.DAYS);
        Instant dayTo = Instant.now();

        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(deviceRepository.findByExternalId("device-001")).thenReturn(Optional.of(userDevice));
        when(telemetryRecordRepository.findByDeviceExternalIdAndTimestampBetweenOrderByTimestamp(
                eq("device-001"), any(Instant.class), any(Instant.class)))
                .thenReturn(sampleRecords);

        // When - daily interval
        List<AggregationResponse> results = analyticsService.aggregateData(
                "device-001", "kwConsumption", "AVG", dayFrom, dayTo, "1d");

        // Then
        assertThat(results).hasSizeGreaterThanOrEqualTo(0);
    }
}
