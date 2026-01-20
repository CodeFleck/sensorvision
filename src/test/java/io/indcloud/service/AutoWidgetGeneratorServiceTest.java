package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.WidgetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoWidgetGeneratorServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private WidgetRepository widgetRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DefaultDashboardInitializer defaultDashboardInitializer;

    private AutoWidgetGeneratorService autoWidgetGeneratorService;
    private ObjectMapper objectMapper;

    private Organization testOrg;
    private Device testDevice;
    private Dashboard testDashboard;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        autoWidgetGeneratorService = new AutoWidgetGeneratorService(
                dashboardRepository,
                widgetRepository,
                deviceRepository,
                defaultDashboardInitializer,
                objectMapper
        );

        // Enable auto-widgets for testing
        ReflectionTestUtils.setField(autoWidgetGeneratorService, "autoWidgetsEnabled", true);
        ReflectionTestUtils.setField(autoWidgetGeneratorService, "maxWidgetsPerVariable", 3);

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
                .initialWidgetsCreated(false)
                .build();

        testDashboard = new Dashboard();
        testDashboard.setId(1L);
        testDashboard.setName("Default Dashboard");
        testDashboard.setOrganization(testOrg);
        testDashboard.setWidgets(new ArrayList<>());
    }

    @Test
    void generateInitialWidgets_shouldCreateWidgetsForEachVariable() {
        // Arrange
        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put("temperature", new BigDecimal("23.5"));
        variables.put("humidity", new BigDecimal("65.0"));

        when(deviceRepository.findByIdWithLock(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> {
            Widget w = invocation.getArgument(0);
            w.setId((long) (Math.random() * 1000));
            return w;
        });

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        // Should create 3 widgets per variable (line chart, gauge, metric card)
        // 2 variables * 3 widgets = 6 total
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository, times(6)).save(widgetCaptor.capture());

        List<Widget> savedWidgets = widgetCaptor.getAllValues();

        // Verify widget types
        long lineChartCount = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.LINE_CHART).count();
        long gaugeCount = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.GAUGE).count();
        long metricCardCount = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.METRIC_CARD).count();

        assertEquals(2, lineChartCount, "Should create 2 line charts");
        assertEquals(2, gaugeCount, "Should create 2 gauges");
        assertEquals(2, metricCardCount, "Should create 2 metric cards");

        // Verify device was marked as having widgets created
        verify(deviceRepository).save(testDevice);
        assertTrue(testDevice.getInitialWidgetsCreated());
    }

    @Test
    void generateInitialWidgets_whenDisabled_shouldNotCreateWidgets() {
        // Arrange
        ReflectionTestUtils.setField(autoWidgetGeneratorService, "autoWidgetsEnabled", false);
        Map<String, BigDecimal> variables = Map.of("temperature", new BigDecimal("23.5"));

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        verify(widgetRepository, never()).save(any());
    }

    @Test
    void generateInitialWidgets_whenDeviceAlreadyHasWidgets_shouldNotCreateMore() {
        // Arrange
        Device deviceWithWidgets = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-002")
                .name("Test Device 2")
                .organization(testOrg)
                .initialWidgetsCreated(true) // Already has widgets
                .build();

        when(deviceRepository.findByIdWithLock(deviceWithWidgets.getId()))
                .thenReturn(Optional.of(deviceWithWidgets));

        Map<String, BigDecimal> variables = Map.of("temperature", new BigDecimal("23.5"));

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(deviceWithWidgets, variables);

        // Assert
        verify(widgetRepository, never()).save(any());
    }

    @Test
    void generateInitialWidgets_whenNoDashboard_shouldNotCreateWidgets() {
        // Arrange
        when(deviceRepository.findByIdWithLock(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(testOrg))
                .thenReturn(Optional.empty());

        Map<String, BigDecimal> variables = Map.of("temperature", new BigDecimal("23.5"));

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        verify(widgetRepository, never()).save(any());
    }

    @Test
    void generateInitialWidgets_shouldHumanizeVariableNames() {
        // Arrange
        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put("power_consumption", new BigDecimal("100.0"));

        when(deviceRepository.findByIdWithLock(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository, times(3)).save(widgetCaptor.capture());

        List<Widget> savedWidgets = widgetCaptor.getAllValues();

        // Verify widget names are humanized
        assertTrue(savedWidgets.stream().anyMatch(w -> w.getName().contains("Power Consumption")),
                "Widget names should be humanized from snake_case");
    }

    @Test
    void generateInitialWidgets_shouldPositionWidgetsCorrectly() {
        // Arrange
        Map<String, BigDecimal> variables = new LinkedHashMap<>();
        variables.put("temperature", new BigDecimal("23.5"));

        when(deviceRepository.findByIdWithLock(testDevice.getId())).thenReturn(Optional.of(testDevice));
        when(dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(widgetRepository.save(any(Widget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        ArgumentCaptor<Widget> widgetCaptor = ArgumentCaptor.forClass(Widget.class);
        verify(widgetRepository, times(3)).save(widgetCaptor.capture());

        List<Widget> savedWidgets = widgetCaptor.getAllValues();

        // Line chart at (0, 0)
        Widget lineChart = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.LINE_CHART)
                .findFirst().orElseThrow();
        assertEquals(0, lineChart.getPositionX());
        assertEquals(0, lineChart.getPositionY());

        // Gauge at (6, 0) - next to line chart
        Widget gauge = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.GAUGE)
                .findFirst().orElseThrow();
        assertEquals(6, gauge.getPositionX());
        assertEquals(0, gauge.getPositionY());

        // Metric card at (9, 0) - next to gauge
        Widget metricCard = savedWidgets.stream()
                .filter(w -> w.getType() == WidgetType.METRIC_CARD)
                .findFirst().orElseThrow();
        assertEquals(9, metricCard.getPositionX());
        assertEquals(0, metricCard.getPositionY());
    }

    @Test
    void generateInitialWidgets_withEmptyVariables_shouldNotCreateWidgets() {
        // Arrange
        Map<String, BigDecimal> variables = Collections.emptyMap();

        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, variables);

        // Assert
        verify(widgetRepository, never()).save(any());
    }

    @Test
    void generateInitialWidgets_withNullVariables_shouldNotCreateWidgets() {
        // Act
        autoWidgetGeneratorService.generateInitialWidgets(testDevice, null);

        // Assert
        verify(widgetRepository, never()).save(any());
    }
}
