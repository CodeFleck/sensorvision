package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.*;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.WidgetRepository;
import io.indcloud.security.SecurityUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Comprehensive tests for Dynamic Dashboard functionality.
 * Tests dual device support, device label binding, and dashboard default device.
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
    void createWidget_shouldSetDualDeviceFields_whenProvidedInRequest() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Comparison Widget",
            WidgetType.LINE_CHART,
            0, 0, 4, 4,
            "device-001",        // primary device
            "device-002",        // second device
            "kwConsumption",     // primary variable
            "kwConsumption",     // second variable
            null,                // deviceLabel
            null,                // secondDeviceLabel
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
        assertThat(savedWidget.getDeviceId()).isEqualTo("device-001");
        assertThat(savedWidget.getSecondDeviceId()).isEqualTo("device-002");
        assertThat(savedWidget.getVariableName()).isEqualTo("kwConsumption");
        assertThat(savedWidget.getSecondVariableName()).isEqualTo("kwConsumption");
        assertThat(response.deviceId()).isEqualTo("device-001");
        assertThat(response.secondDeviceId()).isEqualTo("device-002");
    }

    @Test
    void createWidget_shouldSetDeviceLabels_whenProvidedInRequest() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Labeled Comparison Widget",
            WidgetType.METRIC_CARD,
            0, 0, 3, 2,
            "device-001",
            "device-002",
            "kwConsumption",
            "kwConsumption",
            "Primary Meter",   // deviceLabel
            "Backup Meter",    // secondDeviceLabel
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
        assertThat(savedWidget.getDeviceLabel()).isEqualTo("Primary Meter");
        assertThat(savedWidget.getSecondDeviceLabel()).isEqualTo("Backup Meter");
        assertThat(response.deviceLabel()).isEqualTo("Primary Meter");
        assertThat(response.secondDeviceLabel()).isEqualTo("Backup Meter");
    }

    @Test
    void createWidget_shouldCreateSingleDeviceWidget_whenSecondDeviceNotProvided() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Single Device Widget",
            WidgetType.LINE_CHART,
            0, 0, 6, 4,
            "device-123",
            null,  // no second device
            "voltage",
            null,  // no second variable
            null,
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
        assertThat(savedWidget.getDeviceId()).isEqualTo("device-123");
        assertThat(savedWidget.getSecondDeviceId()).isNull();
        assertThat(savedWidget.getVariableName()).isEqualTo("voltage");
        assertThat(savedWidget.getSecondVariableName()).isNull();
    }

    @Test
    void updateWidget_shouldUpdateSecondDevice_whenProvided() {
        // Arrange
        Widget existingWidget = new Widget();
        existingWidget.setId(100L);
        existingWidget.setDashboard(testDashboard);
        existingWidget.setName("Test Widget");
        existingWidget.setType(WidgetType.GAUGE);
        existingWidget.setDeviceId("device-001");
        existingWidget.setVariableName("kwConsumption");

        WidgetUpdateRequest request = new WidgetUpdateRequest(
            null, null, null, null, null, null,
            null,
            "device-002",      // add second device
            null,
            "kwConsumption",   // add second variable
            null, null,
            null, null, null
        );

        lenient().when(widgetRepository.findById(100L)).thenReturn(Optional.of(existingWidget));
        lenient().when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        WidgetResponse response = dashboardService.updateWidget(100L, request);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository).save(widgetCaptor.capture());

        Widget updatedWidget = widgetCaptor.getValue();
        assertThat(updatedWidget.getSecondDeviceId()).isEqualTo("device-002");
        assertThat(updatedWidget.getSecondVariableName()).isEqualTo("kwConsumption");
        assertThat(response.secondDeviceId()).isEqualTo("device-002");
        assertThat(response.secondVariableName()).isEqualTo("kwConsumption");
    }

    @Test
    void updateWidget_shouldUpdateDeviceLabels_whenProvided() {
        // Arrange
        Widget existingWidget = new Widget();
        existingWidget.setId(101L);
        existingWidget.setDashboard(testDashboard);
        existingWidget.setName("Labeled Widget");
        existingWidget.setType(WidgetType.METRIC_CARD);
        existingWidget.setDeviceLabel("Old Primary");
        existingWidget.setSecondDeviceLabel("Old Secondary");

        WidgetUpdateRequest request = new WidgetUpdateRequest(
            null, null, null, null, null, null,
            null, null, null, null,
            "New Primary",   // update primary device label
            "New Secondary", // update secondary device label
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
        assertThat(updatedWidget.getDeviceLabel()).isEqualTo("New Primary");
        assertThat(updatedWidget.getSecondDeviceLabel()).isEqualTo("New Secondary");
        assertThat(response.deviceLabel()).isEqualTo("New Primary");
        assertThat(response.secondDeviceLabel()).isEqualTo("New Secondary");
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
    void createWidget_shouldSupportDualDeviceWithLabels() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Dual Device Labeled Widget",
            WidgetType.GAUGE,
            0, 0, 4, 4,
            "device-001",
            "device-002",
            "temperature",
            "temperature",
            "Sensor A",        // deviceLabel
            "Sensor B",        // secondDeviceLabel
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
        assertThat(savedWidget.getDeviceId()).isEqualTo("device-001");
        assertThat(savedWidget.getSecondDeviceId()).isEqualTo("device-002");
        assertThat(savedWidget.getDeviceLabel()).isEqualTo("Sensor A");
        assertThat(savedWidget.getSecondDeviceLabel()).isEqualTo("Sensor B");
        assertThat(response.deviceLabel()).isEqualTo("Sensor A");
        assertThat(response.secondDeviceLabel()).isEqualTo("Sensor B");
    }

    @Test
    void createWidget_shouldCreateWidgetWithPrimaryDeviceOnly() {
        // Arrange
        WidgetCreateRequest request = new WidgetCreateRequest(
            "Single Device Widget",
            WidgetType.BAR_CHART,
            0, 0, 5, 3,
            "static-device-456",
            null,  // no second device
            "current",
            null,  // no second variable
            null,
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
        assertThat(savedWidget.getDeviceId()).isEqualTo("static-device-456");
        assertThat(savedWidget.getSecondDeviceId()).isNull();
        assertThat(savedWidget.getVariableName()).isEqualTo("current");
        assertThat(savedWidget.getSecondVariableName()).isNull();
        assertThat(response.deviceId()).isEqualTo("static-device-456");
        assertThat(response.secondDeviceId()).isNull();
    }
}
