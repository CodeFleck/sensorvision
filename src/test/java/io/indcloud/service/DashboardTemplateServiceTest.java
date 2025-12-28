package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.DashboardResponse;
import io.indcloud.dto.DashboardTemplateResponse;
import io.indcloud.dto.InstantiateDashboardTemplateRequest;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.DashboardTemplateRepository;
import io.indcloud.repository.WidgetRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for Dashboard Template Service
 */
@ExtendWith(MockitoExtension.class)
class DashboardTemplateServiceTest {

    @Mock
    private DashboardTemplateRepository templateRepository;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private WidgetRepository widgetRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Captor
    private ArgumentCaptor<Dashboard> dashboardCaptor;

    @Captor
    private ArgumentCaptor<List<Widget>> widgetsCaptor;

    @Captor
    private ArgumentCaptor<DashboardTemplate> templateCaptor;

    private DashboardTemplateService templateService;
    private ObjectMapper objectMapper;
    private Organization testOrganization;
    private DashboardTemplate testTemplate;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        templateService = new DashboardTemplateService(
            templateRepository,
            dashboardRepository,
            widgetRepository,
            securityUtils,
            objectMapper
        );

        testOrganization = Organization.builder()
            .id(1L)
            .name("Test Organization")
            .build();

        // Lenient stubbing since not all tests need organization context
        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);

        // Create test template
        ObjectNode dashboardConfig = objectMapper.createObjectNode();
        dashboardConfig.put("name", "Test Dashboard");
        dashboardConfig.put("description", "Test Description");

        ArrayNode widgetsConfig = objectMapper.createArrayNode();
        ObjectNode widget1 = objectMapper.createObjectNode();
        widget1.put("name", "Widget 1");
        widget1.put("type", "LINE_CHART");
        widget1.put("positionX", 0);
        widget1.put("positionY", 0);
        widget1.put("width", 6);
        widget1.put("height", 4);
        widget1.putNull("deviceId");
        widget1.put("variableName", "temperature");
        widget1.put("aggregation", "AVG");
        widget1.put("timeRangeMinutes", 60);
        widgetsConfig.add(widget1);

        testTemplate = DashboardTemplate.builder()
            .id(1L)
            .name("Smart Meter Template")
            .description("Template for smart meter monitoring")
            .category(DashboardTemplateCategory.SMART_METER)
            .icon("⚡")
            .dashboardConfig(dashboardConfig)
            .widgetsConfig(widgetsConfig)
            .isSystem(true)
            .usageCount(0)
            .build();
    }

    @Test
    void getAllTemplates_shouldReturnAllTemplates() {
        // Given
        List<DashboardTemplate> templates = List.of(testTemplate);
        when(templateRepository.findAll()).thenReturn(templates);

        // When
        List<DashboardTemplateResponse> result = templateService.getAllTemplates();

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Smart Meter Template");
        assertThat(result.get(0).getCategory()).isEqualTo(DashboardTemplateCategory.SMART_METER);
        verify(templateRepository).findAll();
    }

    @Test
    void getTemplatesByCategory_shouldFilterByCategory() {
        // Given
        when(templateRepository.findByCategory(DashboardTemplateCategory.SMART_METER))
            .thenReturn(List.of(testTemplate));

        // When
        List<DashboardTemplateResponse> result =
            templateService.getTemplatesByCategory(DashboardTemplateCategory.SMART_METER);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getCategory()).isEqualTo(DashboardTemplateCategory.SMART_METER);
        verify(templateRepository).findByCategory(DashboardTemplateCategory.SMART_METER);
    }

    @Test
    void getTemplate_shouldReturnTemplateWithFullConfig() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

        // When
        DashboardTemplateResponse result = templateService.getTemplate(1L);

        // Then
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Smart Meter Template");
        assertThat(result.getDashboardConfig()).isNotNull();
        assertThat(result.getWidgetsConfig()).isNotNull();
        assertThat(result.getWidgetsConfig().isArray()).isTrue();
    }

    @Test
    void getTemplate_shouldThrowException_whenTemplateNotFound() {
        // Given
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> templateService.getTemplate(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Template not found");
    }

    @Test
    void instantiateTemplate_shouldCreateDashboardWithWidgets() {
        // Given
        InstantiateDashboardTemplateRequest request =
            new InstantiateDashboardTemplateRequest("My Dashboard", "Description", null);

        Dashboard savedDashboard = new Dashboard();
        savedDashboard.setId(10L);
        savedDashboard.setName(request.dashboardName());
        savedDashboard.setOrganization(testOrganization);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(savedDashboard);
        when(widgetRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dashboardRepository.findByIdWithWidgets(10L)).thenReturn(Optional.of(savedDashboard));

        // When
        DashboardResponse result = templateService.instantiateTemplate(1L, request);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(10L);

        // Verify dashboard was saved
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard createdDashboard = dashboardCaptor.getValue();
        assertThat(createdDashboard.getName()).isEqualTo("My Dashboard");
        assertThat(createdDashboard.getDescription()).isEqualTo("Description");
        assertThat(createdDashboard.getOrganization()).isEqualTo(testOrganization);

        // Verify widgets were created
        verify(widgetRepository).saveAll(widgetsCaptor.capture());
        List<Widget> createdWidgets = widgetsCaptor.getValue();
        assertThat(createdWidgets).hasSize(1);
        assertThat(createdWidgets.get(0).getName()).isEqualTo("Widget 1");
        assertThat(createdWidgets.get(0).getType()).isEqualTo(WidgetType.LINE_CHART);

        // Verify template usage count incremented
        verify(templateRepository).save(templateCaptor.capture());
        DashboardTemplate updatedTemplate = templateCaptor.getValue();
        assertThat(updatedTemplate.getUsageCount()).isEqualTo(1);
    }

    @Test
    void instantiateTemplate_shouldBindWidgetsToDevice_whenDeviceIdProvided() {
        // Given
        InstantiateDashboardTemplateRequest request =
            new InstantiateDashboardTemplateRequest("My Dashboard", null, "device-123");

        Dashboard savedDashboard = new Dashboard();
        savedDashboard.setId(10L);
        savedDashboard.setOrganization(testOrganization);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(savedDashboard);
        when(widgetRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(dashboardRepository.findByIdWithWidgets(10L)).thenReturn(Optional.of(savedDashboard));

        // When
        templateService.instantiateTemplate(1L, request);

        // Then
        verify(widgetRepository).saveAll(widgetsCaptor.capture());
        List<Widget> createdWidgets = widgetsCaptor.getValue();
        assertThat(createdWidgets).hasSize(1);
        assertThat(createdWidgets.get(0).getDeviceId()).isEqualTo("device-123");
    }

    @Test
    void instantiateTemplate_shouldUseTemplateDescription_whenNoneProvided() {
        // Given
        InstantiateDashboardTemplateRequest request =
            new InstantiateDashboardTemplateRequest("My Dashboard", null, null);

        Dashboard savedDashboard = new Dashboard();
        savedDashboard.setId(10L);
        savedDashboard.setOrganization(testOrganization);

        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));
        when(dashboardRepository.save(any(Dashboard.class))).thenReturn(savedDashboard);
        when(widgetRepository.saveAll(any())).thenReturn(List.of());
        when(dashboardRepository.findByIdWithWidgets(10L)).thenReturn(Optional.of(savedDashboard));

        // When
        templateService.instantiateTemplate(1L, request);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard createdDashboard = dashboardCaptor.getValue();
        assertThat(createdDashboard.getDescription()).isEqualTo(testTemplate.getDescription());
    }

    @Test
    void deleteTemplate_shouldDeleteNonSystemTemplate() {
        // Given
        DashboardTemplate userTemplate = DashboardTemplate.builder()
            .id(2L)
            .name("User Template")
            .isSystem(false)
            .build();

        when(templateRepository.findById(2L)).thenReturn(Optional.of(userTemplate));

        // When
        templateService.deleteTemplate(2L);

        // Then
        verify(templateRepository).delete(userTemplate);
    }

    @Test
    void deleteTemplate_shouldThrowException_whenDeletingSystemTemplate() {
        // Given
        when(templateRepository.findById(1L)).thenReturn(Optional.of(testTemplate));

        // When & Then
        assertThatThrownBy(() -> templateService.deleteTemplate(1L))
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("Cannot delete system templates");

        verify(templateRepository, never()).delete(any());
    }

    @Test
    void deleteTemplate_shouldThrowException_whenTemplateNotFound() {
        // Given
        when(templateRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> templateService.deleteTemplate(999L))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Template not found");
    }
}
