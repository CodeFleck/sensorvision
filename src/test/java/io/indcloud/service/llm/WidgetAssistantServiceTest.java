package io.indcloud.service.llm;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.dto.llm.WidgetAssistantDto.*;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.VariableRepository;
import io.indcloud.repository.WidgetAssistantConversationRepository;
import io.indcloud.repository.WidgetAssistantMessageRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DashboardService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WidgetAssistantServiceTest {

    @Mock
    private LLMServiceRouter llmServiceRouter;

    @Mock
    private DashboardService dashboardService;

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private VariableRepository variableRepository;

    @Mock
    private WidgetAssistantConversationRepository conversationRepository;

    @Mock
    private WidgetAssistantMessageRepository messageRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TransactionTemplate transactionTemplate;

    private WidgetAssistantService widgetAssistantService;
    private ObjectMapper objectMapper;

    private Organization testOrg;
    private User testUser;
    private Dashboard testDashboard;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        widgetAssistantService = new WidgetAssistantService(
                llmServiceRouter,
                dashboardService,
                dashboardRepository,
                deviceRepository,
                variableRepository,
                conversationRepository,
                messageRepository,
                securityUtils,
                objectMapper,
                transactionTemplate
        );

        // Enable widget assistant for testing
        ReflectionTestUtils.setField(widgetAssistantService, "widgetAssistantEnabled", true);
        ReflectionTestUtils.setField(widgetAssistantService, "conversationTtlMinutes", 30);
        ReflectionTestUtils.setField(widgetAssistantService, "maxMessagesPerConversation", 50);

        // Configure transactionTemplate to execute the callback directly (lenient since not all tests use it)
        lenient().when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            org.springframework.transaction.support.TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });

        testOrg = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testUser = new User();
        ReflectionTestUtils.setField(testUser, "id", 1L);
        testUser.setEmail("test@example.com");
        testUser.setOrganization(testOrg);

        testDashboard = new Dashboard();
        testDashboard.setId(1L);
        testDashboard.setName("Test Dashboard");
        testDashboard.setOrganization(testOrg);
        testDashboard.setWidgets(new ArrayList<>());

        testDevice = Device.builder()
                .id(UUID.randomUUID())
                .externalId("test-device-001")
                .name("Test Device")
                .organization(testOrg)
                .build();
    }

    @Test
    void chat_whenDisabled_shouldReturnDisabledMessage() {
        // Arrange
        ReflectionTestUtils.setField(widgetAssistantService, "widgetAssistantEnabled", false);

        ChatRequest request = new ChatRequest(
                "Create a gauge for temperature",
                1L,
                null
        );

        // No need to mock security utils when disabled - service returns early

        // Act
        ChatResponse response = widgetAssistantService.chat(request).block();

        // Assert
        assertNotNull(response);
        assertTrue(response.response().contains("disabled"));
        assertNull(response.widgetSuggestion());
    }

    @Test
    void chat_withValidRequest_shouldReturnResponse() {
        // Arrange
        ChatRequest request = new ChatRequest(
                "Create a gauge for temperature from device-001",
                1L,
                null
        );

        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(dashboardRepository.findByIdAndOrganization(1L, testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(deviceRepository.findActiveByOrganization(testOrg)).thenReturn(List.of(testDevice));
        when(variableRepository.findByDeviceId(testDevice.getId())).thenReturn(Collections.emptyList());

        // Mock conversation creation and retrieval
        when(conversationRepository.save(any(WidgetAssistantConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.findByIdWithMessages(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    WidgetAssistantConversation conv = WidgetAssistantConversation.builder()
                            .id(id)
                            .user(testUser)
                            .organization(testOrg)
                            .dashboard(testDashboard)
                            .build();
                    return Optional.of(conv);
                });
        when(messageRepository.save(any(WidgetAssistantMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LLMResponse llmResponse = LLMResponse.builder()
                .success(true)
                .content("""
                    {
                        "type": "suggestion",
                        "widget": {
                            "name": "Temperature Gauge",
                            "type": "GAUGE",
                            "deviceId": "test-device-001",
                            "variableName": "temperature",
                            "width": 3,
                            "height": 4
                        },
                        "message": "I'll create a gauge showing temperature from test-device-001."
                    }
                    """)
                .provider(LLMProvider.CLAUDE)
                .modelId("claude-3")
                .totalTokens(100)
                .latencyMs(500)
                .build();

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
                .thenReturn(Mono.just(llmResponse));

        // Act
        ChatResponse response = widgetAssistantService.chat(request).block();

        // Assert
        assertNotNull(response);
        assertNotNull(response.conversationId());
        assertTrue(response.response().contains("I'll create a gauge"));
        assertNotNull(response.widgetSuggestion());
        assertEquals("Temperature Gauge", response.widgetSuggestion().name());
        assertEquals(WidgetType.GAUGE, response.widgetSuggestion().type());
    }

    @Test
    void chat_withClarificationNeeded_shouldReturnClarificationResponse() {
        // Arrange
        ChatRequest request = new ChatRequest(
                "Create a widget",
                1L,
                null
        );

        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(dashboardRepository.findByIdAndOrganization(1L, testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(deviceRepository.findActiveByOrganization(testOrg)).thenReturn(List.of(testDevice));
        when(variableRepository.findByDeviceId(testDevice.getId())).thenReturn(Collections.emptyList());

        // Mock conversation creation and retrieval
        when(conversationRepository.save(any(WidgetAssistantConversation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(conversationRepository.findByIdWithMessages(any(UUID.class)))
                .thenAnswer(invocation -> {
                    UUID id = invocation.getArgument(0);
                    WidgetAssistantConversation conv = WidgetAssistantConversation.builder()
                            .id(id)
                            .user(testUser)
                            .organization(testOrg)
                            .dashboard(testDashboard)
                            .build();
                    return Optional.of(conv);
                });
        when(messageRepository.save(any(WidgetAssistantMessage.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        LLMResponse llmResponse = LLMResponse.builder()
                .success(true)
                .content("""
                    {
                        "type": "clarification",
                        "message": "I need more information. Which device would you like to use?"
                    }
                    """)
                .provider(LLMProvider.CLAUDE)
                .build();

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
                .thenReturn(Mono.just(llmResponse));

        // Act
        ChatResponse response = widgetAssistantService.chat(request).block();

        // Assert
        assertNotNull(response);
        assertTrue(response.needsClarification());
        assertNull(response.widgetSuggestion());
        assertTrue(response.response().contains("need more information"));
    }

    @Test
    void getContext_shouldReturnDashboardContext() {
        // Arrange
        Variable testVariable = Variable.builder()
                .id(1L)
                .name("temperature")
                .displayName("Temperature")
                .unit("C")
                .lastValue(new java.math.BigDecimal("23.5"))
                .device(testDevice)
                .organization(testOrg)
                .build();

        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
        when(dashboardRepository.findByIdAndOrganization(1L, testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(deviceRepository.findActiveByOrganization(testOrg)).thenReturn(List.of(testDevice));
        when(variableRepository.findByDeviceId(testDevice.getId())).thenReturn(List.of(testVariable));

        // Act
        ContextResponse context = widgetAssistantService.getContext(1L);

        // Assert
        assertNotNull(context);
        assertEquals(1L, context.dashboardId());
        assertEquals("Test Dashboard", context.dashboardName());
        assertEquals(1, context.devices().size());

        DeviceInfo deviceInfo = context.devices().get(0);
        assertEquals("test-device-001", deviceInfo.externalId());
        assertEquals("Test Device", deviceInfo.name());
        assertEquals(1, deviceInfo.variables().size());

        VariableInfo variableInfo = deviceInfo.variables().get(0);
        assertEquals("temperature", variableInfo.name());
        assertEquals("Temperature", variableInfo.displayName());
        assertEquals("C", variableInfo.unit());
        assertEquals(23.5, variableInfo.lastValue());
    }

    @Test
    void confirmWidget_whenNotConfirmed_shouldCancel() {
        // Arrange
        UUID conversationId = UUID.randomUUID();
        ConfirmRequest request = new ConfirmRequest(
                conversationId,
                1L,
                false
        );

        // Mock conversation lookup for cleanup
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.empty());

        // Act
        ConfirmResponse response = widgetAssistantService.confirmWidget(request);

        // Assert
        assertFalse(response.success());
        assertTrue(response.message().contains("cancelled"));
    }

    @Test
    void confirmWidget_withNoPendingSuggestion_shouldReturnError() {
        // Arrange
        UUID conversationId = UUID.randomUUID();
        ConfirmRequest request = new ConfirmRequest(
                conversationId,
                1L,
                true
        );

        // Create a conversation without pending suggestion
        WidgetAssistantConversation conversation = WidgetAssistantConversation.builder()
                .id(conversationId)
                .user(testUser)
                .organization(testOrg)
                .dashboard(testDashboard)
                .pendingSuggestion(null)
                .build();

        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
        when(dashboardRepository.findByIdAndOrganization(1L, testOrg))
                .thenReturn(Optional.of(testDashboard));
        when(conversationRepository.findById(conversationId))
                .thenReturn(Optional.of(conversation));

        // Act
        ConfirmResponse response = widgetAssistantService.confirmWidget(request);

        // Assert
        assertFalse(response.success());
        assertTrue(response.message().contains("No pending"));
    }
}
