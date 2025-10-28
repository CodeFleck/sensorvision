package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.*;
import org.sensorvision.model.*;
import org.sensorvision.repository.DashboardRepository;
import org.sensorvision.repository.WidgetRepository;
import org.sensorvision.security.SecurityUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Dynamic Dashboard functionality.
 * Tests widget context device support, device label binding, and dashboard default device.
 */
@ExtendWith(MockitoExtension.class)
class DynamicDashboardTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private WidgetRepository widgetRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private DefaultDashboardInitializer defaultDashboardInitializer;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private DashboardService dashboardService;

    private Organization testOrganization;
    private Dashboard testDashboard;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        testDashboard = new Dashboard();
        testDashboard.setId(1L);
        testDashboard.setName("Test Dashboard");
        testDashboard.setOrganization(testOrganization);
        testDashboard.setDefaultDeviceId("device-001");

        // Lenient stubbing for common mocks that may not be used in all tests
        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        lenient().when(objectMapper.createObjectNode()).thenReturn(new ObjectMapper().createObjectNode());
        lenient().when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        lenient().when(dashboardRepository.findByIdWithWidgets(1L)).thenReturn(Optional.of(testDashboard));
    }

    @Test
    void createWidget_shouldSetUseContextDevice_whenProvidedInRequest() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Power Widget",
            WidgetType.GAUGE,
            0, 0, 4, 4,
            null,  // no fixed deviceId
            "kwConsumption",
            true,  // useContextDevice = true
            null,
            WidgetAggregation.NONE,
            null,
            null
        );

        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget widget = invocation.getArgument(0);
            widget.setId(100L);
            return widget;
        });

        // Act
        WidgetResponse response = dashboardService.addWidget(1L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget savedWidget = widgetCaptor.getValue();
        assertThat(savedWidget.getUseContextDevice()).isTrue();
        assertThat(savedWidget.getDeviceId()).isNull();
        assertThat(response.useContextDevice()).isTrue();
    }

    @Test
    void createWidget_shouldSetDeviceLabel_whenProvidedInRequest() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Primary Meter Widget",
            WidgetType.METRIC_CARD,
            0, 0, 3, 2,
            null,
            "kwConsumption",
            false,
            "primary_meter",  // deviceLabel
            WidgetAggregation.NONE,
            null,
            null
        );

        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget widget = invocation.getArgument(0);
            widget.setId(101L);
            return widget;
        });

        // Act
        WidgetResponse response = dashboardService.addWidget(1L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget savedWidget = widgetCaptor.getValue();
        assertThat(savedWidget.getDeviceLabel()).isEqualTo("primary_meter");
        assertThat(response.deviceLabel()).isEqualTo("primary_meter");
    }

    @Test
    void createWidget_shouldDefaultUseContextDeviceToFalse_whenNotProvided() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Fixed Device Widget",
            WidgetType.LINE_CHART,
            0, 0, 6, 4,
            "device-123",
            "voltage",
            null,  // useContextDevice not provided
            null,
            WidgetAggregation.AVG,
            60,
            null
        );

        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget widget = invocation.getArgument(0);
            widget.setId(102L);
            return widget;
        });

        // Act
        WidgetResponse response = dashboardService.addWidget(1L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget savedWidget = widgetCaptor.getValue();
        assertThat(savedWidget.getUseContextDevice()).isFalse();
        assertThat(savedWidget.getDeviceId()).isEqualTo("device-123");
    }

    @Test
    void updateWidget_shouldUpdateUseContextDevice_whenProvided() {
        // Arrange
        Widget existingWidget = new Widget();
        existingWidget.setId(100L);
        existingWidget.setDashboard(testDashboard);
        existingWidget.setName("Test Widget");
        existingWidget.setType(WidgetType.GAUGE);
        existingWidget.setUseContextDevice(false);
        existingWidget.setDeviceId("device-001");

        WidgetUpdateRequest request = new WidgetUpdateRequest(
            null, null, null, null, null, null,
            null, null,
            true,  // change to use context device
            null, null, null, null
        );

        lenient().when(widgetRepository.findById(100L)).thenReturn(Optional.of(existingWidget));
        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WidgetResponse response = dashboardService.updateWidget(100L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget updatedWidget = widgetCaptor.getValue();
        assertThat(updatedWidget.getUseContextDevice()).isTrue();
        assertThat(response.useContextDevice()).isTrue();
    }

    @Test
    void updateWidget_shouldUpdateDeviceLabel_whenProvided() {
        // Arrange
        Widget existingWidget = new Widget();
        existingWidget.setId(101L);
        existingWidget.setDashboard(testDashboard);
        existingWidget.setName("Labeled Widget");
        existingWidget.setType(WidgetType.METRIC_CARD);
        existingWidget.setDeviceLabel("old_label");

        WidgetUpdateRequest request = new WidgetUpdateRequest(
            null, null, null, null, null, null,
            null, null, null,
            "new_label",  // update device label
            null, null, null
        );

        lenient().when(widgetRepository.findById(101L)).thenReturn(Optional.of(existingWidget));
        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WidgetResponse response = dashboardService.updateWidget(101L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget updatedWidget = widgetCaptor.getValue();
        assertThat(updatedWidget.getDeviceLabel()).isEqualTo("new_label");
        assertThat(response.deviceLabel()).isEqualTo("new_label");
    }

    @Test
    void updateDashboard_shouldUpdateDefaultDeviceId_whenProvided() {
        // Arrange
        Dashboard dashboard = new Dashboard();
        dashboard.setId(1L);
        dashboard.setName("Test Dashboard");
        dashboard.setOrganization(testOrganization);
        dashboard.setDefaultDeviceId("old-device");

        DashboardUpdateRequest request = new DashboardUpdateRequest(
            null, null, null,
            "new-device-123",  // new default device
            null
        );

        lenient().when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        DashboardResponse response = dashboardService.updateDashboard(1L, request);

        // Assert
        ArgumentCaptor<Dashboard> dashboardCaptor = ArgumentCaptor.forClass(Dashboard.class);
        verify(dashboardRepository).save(dashboardCaptor.capture());

        Dashboard updatedDashboard = dashboardCaptor.getValue();
        assertThat(updatedDashboard.getDefaultDeviceId()).isEqualTo("new-device-123");
        assertThat(response.defaultDeviceId()).isEqualTo("new-device-123");
    }

    @Test
    void getDashboardById_shouldIncludeDefaultDeviceId_inResponse() {
        // Arrange
        testDashboard.setDefaultDeviceId("device-primary");

        when(dashboardRepository.findByIdWithWidgets(1L)).thenReturn(Optional.of(testDashboard));

        // Act
        DashboardResponse response = dashboardService.getDashboardById(1L);

        // Assert
        assertThat(response.defaultDeviceId()).isEqualTo("device-primary");
    }

    @Test
    void createWidget_shouldAllowBothUseContextDeviceAndDeviceLabel() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Flexible Widget",
            WidgetType.GAUGE,
            0, 0, 4, 4,
            null,
            "temperature",
            true,              // useContextDevice
            "sensor_group_a", // deviceLabel for fallback/filtering
            WidgetAggregation.AVG,
            30,
            null
        );

        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget widget = invocation.getArgument(0);
            widget.setId(103L);
            return widget;
        });

        // Act
        WidgetResponse response = dashboardService.addWidget(1L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget savedWidget = widgetCaptor.getValue();
        assertThat(savedWidget.getUseContextDevice()).isTrue();
        assertThat(savedWidget.getDeviceLabel()).isEqualTo("sensor_group_a");
        assertThat(response.useContextDevice()).isTrue();
        assertThat(response.deviceLabel()).isEqualTo("sensor_group_a");
    }

    @Test
    void createWidget_shouldPreserveDeviceId_whenUseContextDeviceIsFalse() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Static Widget",
            WidgetType.BAR_CHART,
            0, 0, 5, 3,
            "static-device-456",
            "current",
            false,  // explicitly false
            null,
            WidgetAggregation.MAX,
            120,
            null
        );

        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget widget = invocation.getArgument(0);
            widget.setId(104L);
            return widget;
        });

        // Act
        WidgetResponse response = dashboardService.addWidget(1L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget savedWidget = widgetCaptor.getValue();
        assertThat(savedWidget.getUseContextDevice()).isFalse();
        assertThat(savedWidget.getDeviceId()).isEqualTo("static-device-456");
        assertThat(response.useContextDevice()).isFalse();
        assertThat(response.deviceId()).isEqualTo("static-device-456");
    }
}
