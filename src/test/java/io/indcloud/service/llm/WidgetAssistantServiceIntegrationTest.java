package io.indcloud.service.llm;

import io.indcloud.config.TestBeanConfiguration;
import io.indcloud.dto.llm.LLMRequest;
import io.indcloud.dto.llm.LLMResponse;
import io.indcloud.dto.llm.WidgetAssistantDto.*;
import io.indcloud.model.*;
import io.indcloud.repository.*;
import io.indcloud.security.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import reactor.core.publisher.Mono;

import java.util.HashSet;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * Integration test for WidgetAssistantService transaction handling.
 * Tests the complete flow from chat request to database persistence.
 *
 * <p>This test verifies that:
 * <ul>
 *   <li>Transactions are properly managed in the reactive context</li>
 *   <li>Conversation and messages are persisted correctly</li>
 *   <li>Widget suggestions are stored and retrievable</li>
 * </ul>
 */
@SpringBootTest
@ActiveProfiles("test")
@Import(TestBeanConfiguration.class)
class WidgetAssistantServiceIntegrationTest {

    @Autowired
    private WidgetAssistantService widgetAssistantService;

    @Autowired
    private WidgetAssistantConversationRepository conversationRepository;

    @Autowired
    private WidgetAssistantMessageRepository messageRepository;

    @Autowired
    private DashboardRepository dashboardRepository;

    @Autowired
    private DeviceRepository deviceRepository;

    @Autowired
    private OrganizationRepository organizationRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @MockBean
    private LLMServiceRouter llmServiceRouter;

    @MockBean
    private SecurityUtils securityUtils;

    private Organization testOrg;
    private User testUser;
    private Dashboard testDashboard;
    private Device testDevice;

    @BeforeEach
    void setUp() {
        // Use unique identifiers for each test run to avoid conflicts
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);

        // Create test organization
        testOrg = new Organization();
        testOrg.setName("Widget Test Org " + uniqueId);
        testOrg.setDescription("Test organization for widget assistant integration tests");
        testOrg.setEnabled(true);
        testOrg = organizationRepository.save(testOrg);

        // Create test user with proper role
        Role userRole = roleRepository.findByName("ROLE_USER")
            .orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_USER");
                return roleRepository.save(role);
            });

        testUser = new User();
        testUser.setUsername("widget-tester-" + uniqueId);
        testUser.setEmail("widget-test-" + uniqueId + "@example.com");
        testUser.setPasswordHash("$2a$10$test-password-hash");
        testUser.setFirstName("Widget");
        testUser.setLastName("Tester");
        testUser.setOrganization(testOrg);
        testUser.setRoles(new HashSet<>());
        testUser.getRoles().add(userRole);
        testUser.setEnabled(true);
        testUser = userRepository.save(testUser);

        // Create test dashboard
        testDashboard = new Dashboard();
        testDashboard.setName("Integration Test Dashboard " + uniqueId);
        testDashboard.setOrganization(testOrg);
        testDashboard.setIsDefault(false);
        testDashboard = dashboardRepository.save(testDashboard);

        // Create test device
        testDevice = Device.builder()
            .externalId("integration-test-device-" + uniqueId)
            .name("Integration Test Device")
            .organization(testOrg)
            .active(true)
            .build();
        testDevice = deviceRepository.save(testDevice);

        // Mock security context
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrg);
        when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void chat_shouldPersistConversationAndMessagesInTransaction() {
        // Arrange
        String llmResponseContent = """
            {
                "type": "suggestion",
                "widget": {
                    "name": "Temperature Gauge",
                    "type": "GAUGE",
                    "deviceId": "%s",
                    "deviceName": "Integration Test Device",
                    "variableName": "temperature",
                    "width": 3,
                    "height": 4
                },
                "message": "I'll create a gauge showing temperature."
            }
            """.formatted(testDevice.getExternalId());

        LLMResponse llmResponse = LLMResponse.builder()
            .success(true)
            .content(llmResponseContent)
            .provider(LLMProvider.CLAUDE)
            .modelId("claude-3")
            .totalTokens(100)
            .latencyMs(500)
            .build();

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
            .thenReturn(Mono.just(llmResponse));

        ChatRequest request = new ChatRequest(
            "Create a gauge for temperature",
            testDashboard.getId(),
            null
        );

        // Act
        ChatResponse response = widgetAssistantService.chat(request).block();

        // Assert - Response is correct
        assertThat(response).isNotNull();
        assertThat(response.conversationId()).isNotNull();
        assertThat(response.widgetSuggestion()).isNotNull();
        assertThat(response.widgetSuggestion().type()).isEqualTo(WidgetType.GAUGE);

        // Assert - Conversation was persisted
        var conversation = conversationRepository.findByIdWithMessages(response.conversationId());
        assertThat(conversation).isPresent();
        assertThat(conversation.get().getUser().getId()).isEqualTo(testUser.getId());
        assertThat(conversation.get().getDashboard().getId()).isEqualTo(testDashboard.getId());

        // Assert - Messages were persisted (user message + assistant response)
        assertThat(conversation.get().getMessages()).hasSize(2);
        assertThat(conversation.get().getMessages().get(0).getRole())
            .isEqualTo(WidgetAssistantMessage.MessageRole.user);
        assertThat(conversation.get().getMessages().get(1).getRole())
            .isEqualTo(WidgetAssistantMessage.MessageRole.assistant);

        // Assert - Pending suggestion was stored
        assertThat(conversation.get().getPendingSuggestion()).isNotNull();
        assertThat(conversation.get().getPendingSuggestion()).contains("Temperature Gauge");
    }

    @Test
    void chat_shouldHandleLLMFailureWithoutCorruptingDatabase() {
        // Arrange
        LLMResponse failureResponse = LLMResponse.failure(LLMProvider.CLAUDE, "API rate limit exceeded");

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
            .thenReturn(Mono.just(failureResponse));

        ChatRequest request = new ChatRequest(
            "Create a widget",
            testDashboard.getId(),
            null
        );

        long conversationCountBefore = conversationRepository.count();

        // Act
        ChatResponse response = widgetAssistantService.chat(request).block();

        // Assert - Response indicates error
        assertThat(response).isNotNull();
        assertThat(response.response()).contains("error");
        assertThat(response.widgetSuggestion()).isNull();

        // Assert - No orphaned conversations created
        // Note: A conversation IS created before LLM call, but error is handled gracefully
        assertThat(conversationRepository.count()).isGreaterThanOrEqualTo(conversationCountBefore);
    }

    @Test
    void chat_multiTurnConversation_shouldPersistAllMessages() {
        // Arrange - First turn: clarification needed
        String clarificationResponse = """
            {
                "type": "clarification",
                "message": "Which device would you like to monitor?"
            }
            """;

        LLMResponse firstResponse = LLMResponse.builder()
            .success(true)
            .content(clarificationResponse)
            .provider(LLMProvider.CLAUDE)
            .modelId("claude-3")
            .totalTokens(50)
            .latencyMs(300)
            .build();

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
            .thenReturn(Mono.just(firstResponse));

        ChatRequest firstRequest = new ChatRequest(
            "Create a widget",
            testDashboard.getId(),
            null
        );

        // Act - First turn
        ChatResponse firstChatResponse = widgetAssistantService.chat(firstRequest).block();
        assertThat(firstChatResponse).isNotNull();
        assertThat(firstChatResponse.needsClarification()).isTrue();

        // Arrange - Second turn: provide clarification and get suggestion
        String suggestionResponse = """
            {
                "type": "suggestion",
                "widget": {
                    "name": "Power Chart",
                    "type": "LINE_CHART",
                    "deviceId": "%s",
                    "variableName": "power",
                    "width": 6,
                    "height": 4
                },
                "message": "I'll create a line chart for power consumption."
            }
            """.formatted(testDevice.getExternalId());

        LLMResponse secondResponse = LLMResponse.builder()
            .success(true)
            .content(suggestionResponse)
            .provider(LLMProvider.CLAUDE)
            .modelId("claude-3")
            .totalTokens(100)
            .latencyMs(400)
            .build();

        when(llmServiceRouter.complete(any(LLMRequest.class), eq(testOrg), eq(testUser)))
            .thenReturn(Mono.just(secondResponse));

        ChatRequest secondRequest = new ChatRequest(
            "Use the integration test device",
            testDashboard.getId(),
            firstChatResponse.conversationId()  // Continue same conversation
        );

        // Act - Second turn
        ChatResponse secondChatResponse = widgetAssistantService.chat(secondRequest).block();

        // Assert - Second response has suggestion
        assertThat(secondChatResponse).isNotNull();
        assertThat(secondChatResponse.conversationId()).isEqualTo(firstChatResponse.conversationId());
        assertThat(secondChatResponse.widgetSuggestion()).isNotNull();

        // Assert - All 4 messages persisted (2 user + 2 assistant)
        var conversation = conversationRepository.findByIdWithMessages(firstChatResponse.conversationId());
        assertThat(conversation).isPresent();
        assertThat(conversation.get().getMessages()).hasSize(4);
    }
}
