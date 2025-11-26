package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Device;
import org.sensorvision.model.DeviceStatus;
import org.sensorvision.model.Organization;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.repository.DeviceRepository;
import org.sensorvision.repository.TelemetryRecordRepository;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for DeviceHealthService.
 * Tests device online/offline status updates and health score calculations.
 */
@ExtendWith(MockitoExtension.class)
class DeviceHealthServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private TelemetryRecordRepository telemetryRecordRepository;

    @InjectMocks
    private DeviceHealthService deviceHealthService;

    @Captor
    private ArgumentCaptor<Device> deviceCaptor;

    private Organization testOrganization;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();
    }

    @Test
    void updateDeviceOnlineStatus_shouldMarkDeviceAsOnline_whenLastSeenWithinOneHour() {
        // Given: Device seen 30 minutes ago (within 1 hour threshold)
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        Device device = createTestDevice("device-001", DeviceStatus.UNKNOWN, thirtyMinutesAgo);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getStatus()).isEqualTo(DeviceStatus.ONLINE);
    }

    @Test
    void updateDeviceOnlineStatus_shouldMarkDeviceAsOffline_whenLastSeenMoreThanOneHourAgo() {
        // Given: Device seen 90 minutes ago (beyond 1 hour threshold)
        Instant ninetyMinutesAgo = Instant.now().minus(90, ChronoUnit.MINUTES);
        Device device = createTestDevice("device-002", DeviceStatus.ONLINE, ninetyMinutesAgo);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    void updateDeviceOnlineStatus_shouldMarkDeviceAsOffline_whenLastSeenExactlyOneHourAgo() {
        // Given: Device seen exactly 60 minutes ago (at threshold boundary)
        Instant exactlyOneHourAgo = Instant.now().minus(60, ChronoUnit.MINUTES);
        Device device = createTestDevice("device-003", DeviceStatus.ONLINE, exactlyOneHourAgo);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        // Device at exactly 1 hour boundary should be OFFLINE (isBefore returns false for equal timestamps)
        assertThat(savedDevice.getStatus()).isEqualTo(DeviceStatus.OFFLINE);
    }

    @Test
    void updateDeviceOnlineStatus_shouldMarkDeviceAsUnknown_whenLastSeenAtIsNull() {
        // Given: Device with no lastSeenAt timestamp
        Device device = createTestDevice("device-004", DeviceStatus.ONLINE, null);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(device)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then
        verify(deviceRepository).save(deviceCaptor.capture());
        Device savedDevice = deviceCaptor.getValue();
        assertThat(savedDevice.getStatus()).isEqualTo(DeviceStatus.UNKNOWN);
    }

    @Test
    void updateDeviceOnlineStatus_shouldNotSaveDevice_whenStatusUnchanged() {
        // Given: Device already ONLINE and seen 30 minutes ago (status should remain ONLINE)
        Instant thirtyMinutesAgo = Instant.now().minus(30, ChronoUnit.MINUTES);
        Device device = createTestDevice("device-005", DeviceStatus.ONLINE, thirtyMinutesAgo);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(device)));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then: Should not save since status didn't change
        verify(deviceRepository, never()).save(any(Device.class));
    }

    @Test
    void updateDeviceOnlineStatus_shouldHandleMultipleDevicesWithDifferentStatuses() {
        // Given: Multiple devices with different lastSeenAt times
        Instant twentyMinutesAgo = Instant.now().minus(20, ChronoUnit.MINUTES);
        Instant twoHoursAgo = Instant.now().minus(120, ChronoUnit.MINUTES);

        Device onlineDevice = createTestDevice("device-006", DeviceStatus.UNKNOWN, twentyMinutesAgo);
        Device offlineDevice = createTestDevice("device-007", DeviceStatus.ONLINE, twoHoursAgo);
        Device unknownDevice = createTestDevice("device-008", DeviceStatus.OFFLINE, null);

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(onlineDevice, offlineDevice, unknownDevice)));
        when(deviceRepository.save(any(Device.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then: Should save all 3 devices with status changes
        verify(deviceRepository, times(3)).save(deviceCaptor.capture());
        List<Device> savedDevices = deviceCaptor.getAllValues();

        assertThat(savedDevices).hasSize(3);
        assertThat(savedDevices.get(0).getStatus()).isEqualTo(DeviceStatus.ONLINE);  // onlineDevice
        assertThat(savedDevices.get(1).getStatus()).isEqualTo(DeviceStatus.OFFLINE); // offlineDevice
        assertThat(savedDevices.get(2).getStatus()).isEqualTo(DeviceStatus.UNKNOWN); // unknownDevice
    }

    @Test
    void updateDeviceOnlineStatus_shouldContinueProcessing_whenOneDeviceFails() {
        // Given: Two devices, first one will throw exception
        Device failingDevice = createTestDevice("device-009", DeviceStatus.ONLINE, Instant.now().minus(90, ChronoUnit.MINUTES));
        Device successDevice = createTestDevice("device-010", DeviceStatus.UNKNOWN, Instant.now().minus(30, ChronoUnit.MINUTES));

        when(deviceRepository.findAll(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(Arrays.asList(failingDevice, successDevice)));
        when(deviceRepository.save(failingDevice)).thenThrow(new RuntimeException("Database error"));
        when(deviceRepository.save(successDevice)).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        deviceHealthService.updateDeviceOnlineStatus();

        // Then: Should still process the second device despite first one failing
        verify(deviceRepository, times(2)).save(any(Device.class));
    }

    private Device createTestDevice(String externalId, DeviceStatus status, Instant lastSeenAt) {
        return Device.builder()
                .id(UUID.randomUUID())
                .externalId(externalId)
                .name("Test Device " + externalId)
                .status(status)
                .lastSeenAt(lastSeenAt)
                .organization(testOrganization)
                .build();
    }
}
