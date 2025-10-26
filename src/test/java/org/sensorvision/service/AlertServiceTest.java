package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.AlertResponse;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.*;
import org.sensorvision.repository.AlertRepository;
import org.sensorvision.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AlertService.
 * Tests alert retrieval, acknowledgment, saving, and security (organization isolation).
 */
@ExtendWith(MockitoExtension.class)
class AlertServiceTest {

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private AlertService alertService;

    @Captor
    private ArgumentCaptor<Alert> alertCaptor;

    private Organization userOrganization;
    private Organization otherOrganization;
    private Device userDevice;
    private Device otherDevice;
    private Rule userRule;
    private Alert testAlert;
    private Alert acknowledgedAlert;

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
                .externalId("user-device-001")
                .name("User Device")
                .organization(userOrganization)
                .build();

        otherDevice = Device.builder()
                .externalId("other-device-001")
                .name("Other Device")
                .organization(otherOrganization)
                .build();

        userRule = Rule.builder()
                .id(UUID.randomUUID())
                .name("High Voltage Alert")
                .device(userDevice)
                .variable("voltage")
                .operator(RuleOperator.GT)
                .threshold(new BigDecimal("250.0"))
                .enabled(true)
                .organization(userOrganization)
                .build();

        testAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Voltage exceeded threshold")
                .severity(AlertSeverity.HIGH)
                .triggeredValue(new BigDecimal("260.0"))
                .acknowledged(false)
                .build();

        acknowledgedAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Previous alert")
                .severity(AlertSeverity.MEDIUM)
                .triggeredValue(new BigDecimal("255.0"))
                .acknowledged(true)
                .acknowledgedAt(Instant.now().minusSeconds(3600))
                .build();
    }

    // ===== GET ALL ALERTS TESTS =====

    @Test
    void getAllAlerts_shouldReturnAllAlertsForUserOrganization() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of(testAlert, acknowledgedAlert));

        // When
        List<AlertResponse> alerts = alertService.getAllAlerts();

        // Then
        assertThat(alerts).hasSize(2);
        assertThat(alerts.get(0).id()).isEqualTo(testAlert.getId());
        assertThat(alerts.get(0).deviceId()).isEqualTo("user-device-001");
        assertThat(alerts.get(0).severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(alerts.get(1).acknowledged()).isTrue();

        verify(securityUtils).getCurrentUserOrganization();
        verify(alertRepository).findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization);
    }

    @Test
    void getAllAlerts_shouldReturnEmptyList_whenNoAlerts() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of());

        // When
        List<AlertResponse> alerts = alertService.getAllAlerts();

        // Then
        assertThat(alerts).isEmpty();
        verify(alertRepository).findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization);
    }

    @Test
    void getAllAlerts_shouldNotReturnAlertsFromOtherOrganizations() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);

        Alert otherOrgAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(otherDevice)  // Different organization
                .message("Alert from other org")
                .severity(AlertSeverity.LOW)
                .build();

        when(alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of(testAlert));  // Only user's alerts

        // When
        List<AlertResponse> alerts = alertService.getAllAlerts();

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).deviceId()).isEqualTo("user-device-001");
        // otherOrgAlert should not be included
    }

    // ===== GET UNACKNOWLEDGED ALERTS TESTS =====

    @Test
    void getUnacknowledgedAlerts_shouldReturnOnlyUnacknowledgedAlerts() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationAndAcknowledgedFalseOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of(testAlert));

        // When
        List<AlertResponse> alerts = alertService.getUnacknowledgedAlerts();

        // Then
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).acknowledged()).isFalse();
        assertThat(alerts.get(0).id()).isEqualTo(testAlert.getId());

        verify(alertRepository).findByDeviceOrganizationAndAcknowledgedFalseOrderByCreatedAtDesc(userOrganization);
    }

    @Test
    void getUnacknowledgedAlerts_shouldReturnEmptyList_whenAllAlertsAcknowledged() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationAndAcknowledgedFalseOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of());

        // When
        List<AlertResponse> alerts = alertService.getUnacknowledgedAlerts();

        // Then
        assertThat(alerts).isEmpty();
    }

    // ===== ACKNOWLEDGE ALERT TESTS =====

    @Test
    void acknowledgeAlert_shouldSetAcknowledgedFlag_whenAlertExists() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        UUID alertId = testAlert.getId();
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(testAlert));
        when(alertRepository.save(any(Alert.class))).thenReturn(testAlert);

        // When
        alertService.acknowledgeAlert(alertId);

        // Then
        verify(alertRepository).save(alertCaptor.capture());
        Alert savedAlert = alertCaptor.getValue();

        assertThat(savedAlert.getAcknowledged()).isTrue();
        assertThat(savedAlert.getAcknowledgedAt()).isNotNull();
        assertThat(savedAlert.getAcknowledgedAt()).isBeforeOrEqualTo(Instant.now());
    }

    @Test
    void acknowledgeAlert_shouldThrowException_whenAlertNotFound() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        UUID nonExistentId = UUID.randomUUID();
        when(alertRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> alertService.acknowledgeAlert(nonExistentId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Alert not found: " + nonExistentId);

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledgeAlert_shouldThrowAccessDeniedException_whenAlertBelongsToOtherOrganization() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);

        Alert otherOrgAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(otherDevice)  // Different organization
                .message("Other org alert")
                .severity(AlertSeverity.LOW)
                .acknowledged(false)
                .build();

        when(alertRepository.findById(otherOrgAlert.getId())).thenReturn(Optional.of(otherOrgAlert));

        // When/Then
        assertThatThrownBy(() -> alertService.acknowledgeAlert(otherOrgAlert.getId()))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to alert: " + otherOrgAlert.getId());

        verify(alertRepository, never()).save(any(Alert.class));
    }

    @Test
    void acknowledgeAlert_shouldNotUpdateAlreadyAcknowledgedAlert() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        UUID alertId = acknowledgedAlert.getId();
        when(alertRepository.findById(alertId)).thenReturn(Optional.of(acknowledgedAlert));

        // When
        alertService.acknowledgeAlert(alertId);

        // Then
        verify(alertRepository, never()).save(any(Alert.class));
    }

    // ===== SAVE ALERT TESTS =====

    @Test
    void saveAlert_shouldSaveAlertAndTriggerNotifications() {
        // Given
        when(alertRepository.save(testAlert)).thenReturn(testAlert);

        // When
        Alert savedAlert = alertService.saveAlert(testAlert);

        // Then
        assertThat(savedAlert).isEqualTo(testAlert);
        verify(alertRepository).save(testAlert);
        verify(notificationService).sendAlertNotifications(testAlert);
    }

    @Test
    void saveAlert_shouldCallNotificationService_forAllSeverityLevels() {
        // Given
        Alert criticalAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Critical alert")
                .severity(AlertSeverity.CRITICAL)
                .build();

        when(alertRepository.save(criticalAlert)).thenReturn(criticalAlert);

        // When
        alertService.saveAlert(criticalAlert);

        // Then
        verify(notificationService).sendAlertNotifications(criticalAlert);
    }

    // ===== SEND ALERT NOTIFICATION TESTS =====

    @Test
    void sendAlertNotification_shouldDelegateToNotificationService() {
        // When
        alertService.sendAlertNotification(testAlert);

        // Then
        verify(notificationService).sendAlertNotifications(testAlert);
    }

    @Test
    void sendAlertNotification_shouldWork_forAllSeverityLevels() {
        // Given
        Alert lowAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Low severity alert")
                .severity(AlertSeverity.LOW)
                .build();

        Alert mediumAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Medium severity alert")
                .severity(AlertSeverity.MEDIUM)
                .build();

        Alert highAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("High severity alert")
                .severity(AlertSeverity.HIGH)
                .build();

        Alert criticalAlert = Alert.builder()
                .id(UUID.randomUUID())
                .rule(userRule)
                .device(userDevice)
                .message("Critical severity alert")
                .severity(AlertSeverity.CRITICAL)
                .build();

        // When
        alertService.sendAlertNotification(lowAlert);
        alertService.sendAlertNotification(mediumAlert);
        alertService.sendAlertNotification(highAlert);
        alertService.sendAlertNotification(criticalAlert);

        // Then
        verify(notificationService, times(4)).sendAlertNotifications(any(Alert.class));
        verify(notificationService).sendAlertNotifications(lowAlert);
        verify(notificationService).sendAlertNotifications(mediumAlert);
        verify(notificationService).sendAlertNotifications(highAlert);
        verify(notificationService).sendAlertNotifications(criticalAlert);
    }

    // ===== MAPPING TESTS =====

    @Test
    void toResponse_shouldMapAllFieldsCorrectly() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of(testAlert));

        // When
        List<AlertResponse> alerts = alertService.getAllAlerts();

        // Then
        assertThat(alerts).hasSize(1);
        AlertResponse response = alerts.get(0);

        assertThat(response.id()).isEqualTo(testAlert.getId());
        assertThat(response.ruleId()).isEqualTo(userRule.getId());
        assertThat(response.ruleName()).isEqualTo("High Voltage Alert");
        assertThat(response.deviceId()).isEqualTo("user-device-001");
        assertThat(response.deviceName()).isEqualTo("User Device");
        assertThat(response.message()).isEqualTo("Voltage exceeded threshold");
        assertThat(response.severity()).isEqualTo(AlertSeverity.HIGH);
        assertThat(response.triggeredValue()).isEqualByComparingTo(new BigDecimal("260.0"));
        assertThat(response.acknowledged()).isFalse();
    }

    @Test
    void toResponse_shouldIncludeAcknowledgementDetails_whenAcknowledged() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(userOrganization);
        when(alertRepository.findByDeviceOrganizationOrderByCreatedAtDesc(userOrganization))
                .thenReturn(List.of(acknowledgedAlert));

        // When
        List<AlertResponse> alerts = alertService.getAllAlerts();

        // Then
        assertThat(alerts).hasSize(1);
        AlertResponse response = alerts.get(0);

        assertThat(response.acknowledged()).isTrue();
        assertThat(response.acknowledgedAt()).isNotNull();
        assertThat(response.acknowledgedAt()).isEqualTo(acknowledgedAlert.getAcknowledgedAt());
    }
}
