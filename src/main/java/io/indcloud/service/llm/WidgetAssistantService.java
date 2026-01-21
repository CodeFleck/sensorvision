package io.indcloud.service.llm;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.dto.WidgetCreateRequest;
import io.indcloud.dto.WidgetResponse;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for AI-powered widget creation via natural language.
 * Uses LLMServiceRouter to parse user requests and suggest widget configurations.
 *
 * Conversations are persisted to the database for durability across restarts
 * and support for multi-instance deployments.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class WidgetAssistantService {

    private final LLMServiceRouter llmServiceRouter;
    private final DashboardService dashboardService;
    private final DashboardRepository dashboardRepository;
    private final DeviceRepository deviceRepository;
    private final VariableRepository variableRepository;
    private final WidgetAssistantConversationRepository conversationRepository;
    private final WidgetAssistantMessageRepository messageRepository;
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${llm.features.widget-assistant.enabled:true}")
    private boolean widgetAssistantEnabled;

    @Value("${llm.features.widget-assistant.conversation-ttl-minutes:30}")
    private int conversationTtlMinutes;

    @Value("${llm.features.widget-assistant.max-messages-per-conversation:50}")
    private int maxMessagesPerConversation;

    private static final String SYSTEM_PROMPT = """
        You are an AI widget assistant for an IoT dashboard. Your job is to help users create widgets
        to visualize their device data. When a user describes what they want to see, you should:

        1. Identify which device they're referring to
        2. Identify which variable/metric they want to display
        3. Suggest an appropriate widget type
        4. Provide the configuration as a JSON object

        Available widget types:
        - LINE_CHART: For time-series data and trends (best for most metrics)
        - GAUGE: For current values with min/max ranges (good for single metrics)
        - METRIC_CARD: Simple numeric display with label
        - BAR_CHART: For comparing values across time or categories
        - AREA_CHART: Similar to line chart but filled
        - PIE_CHART: For proportional data
        - INDICATOR: Simple on/off or status indicator
        - TABLE: For tabular data display
        - MAP: For location data

        When you determine what the user wants, respond with ONLY a JSON object in this exact format:
        {
            "type": "suggestion",
            "widget": {
                "name": "Widget Name",
                "type": "LINE_CHART",
                "deviceId": "device-external-id",
                "deviceName": "Device Display Name",
                "variableName": "variable_name",
                "width": 6,
                "height": 4,
                "config": {}
            },
            "message": "I'll create a line chart showing temperature from Device X."
        }

        If you need more information, respond with:
        {
            "type": "clarification",
            "message": "I need more information. Which device would you like to use?"
        }

        If you cannot fulfill the request, respond with:
        {
            "type": "error",
            "message": "Explanation of why you cannot fulfill the request."
        }

        IMPORTANT: Always respond with ONLY the JSON object, no additional text.
        """;

    private static final int MAX_MESSAGE_LENGTH = 2000;

    /**
     * Process a chat message and return AI response with optional widget suggestion.
     */
    @Transactional
    public Mono<ChatResponse> chat(ChatRequest request) {
        if (!widgetAssistantEnabled) {
            return Mono.just(new ChatResponse(
                null, "Widget assistant is currently disabled.", null, false,
                null, null, null, null
            ));
        }

        // Validate message
        if (request.message() == null || request.message().isBlank()) {
            return Mono.just(new ChatResponse(
                null, "Message cannot be empty.", null, false,
                null, null, null, null
            ));
        }

        if (request.message().length() > MAX_MESSAGE_LENGTH) {
            return Mono.just(new ChatResponse(
                null, "Message too long. Please limit to " + MAX_MESSAGE_LENGTH + " characters.", null, false,
                null, null, null, null
            ));
        }

        Organization org = securityUtils.getCurrentUserOrganization();
        User user = securityUtils.getCurrentUser();

        // Verify dashboard access - must belong to user's organization
        Dashboard dashboard = null;
        if (request.dashboardId() != null) {
            dashboard = dashboardRepository.findByIdAndOrganization(request.dashboardId(), org).orElse(null);
            if (dashboard == null) {
                log.warn("User {} attempted to access dashboard {} without permission",
                        user.getEmail(), request.dashboardId());
                return Mono.just(new ChatResponse(
                    null, "You do not have access to this dashboard.", null, false,
                    null, null, null, null
                ));
            }
        }

        // Get or create conversation
        WidgetAssistantConversation conversation;
        if (request.conversationId() != null) {
            conversation = conversationRepository.findByIdWithMessages(request.conversationId()).orElse(null);
            if (conversation == null) {
                // Conversation not found, create a new one
                conversation = createNewConversation(user, org, dashboard);
            } else {
                // Verify conversation belongs to current user
                if (!conversation.getUser().getId().equals(user.getId())) {
                    log.warn("User {} attempted to access conversation {} belonging to another user",
                            user.getEmail(), request.conversationId());
                    return Mono.just(new ChatResponse(
                        null, "Invalid conversation.", null, false,
                        null, null, null, null
                    ));
                }
                // Update timestamp
                conversation.setUpdatedAt(Instant.now());
            }
        } else {
            conversation = createNewConversation(user, org, dashboard);
        }

        // Check conversation length limit
        if (conversation.getMessages().size() >= maxMessagesPerConversation) {
            return Mono.just(new ChatResponse(
                conversation.getId(),
                "This conversation has reached its limit of " + maxMessagesPerConversation +
                " messages. Please start a new conversation.",
                null, false, null, null, null, null
            ));
        }

        // Build context with available devices and variables
        String contextPrompt = buildContextPrompt(org, request.dashboardId());

        // Build LLM message history from database
        List<LLMRequest.Message> history = conversation.getMessages().stream()
            .map(m -> LLMRequest.Message.builder()
                .role(m.getRole().name())
                .content(m.getContent())
                .build())
            .collect(Collectors.toList());

        // Add user message to database
        WidgetAssistantMessage userMessage = WidgetAssistantMessage.builder()
            .role(WidgetAssistantMessage.MessageRole.user)
            .content(request.message())
            .build();
        conversation.addMessage(userMessage);
        messageRepository.save(userMessage);

        // Add to history for LLM
        history.add(LLMRequest.Message.builder()
            .role("user")
            .content(request.message())
            .build());

        // Build LLM request
        LLMRequest llmRequest = LLMRequest.builder()
            .featureType(LLMFeatureType.WIDGET_ASSISTANT)
            .systemPrompt(SYSTEM_PROMPT + "\n\n" + contextPrompt)
            .userMessage(request.message())
            .conversationHistory(history)
            .maxTokens(1024)
            .temperature(0.3)
            .build();

        final WidgetAssistantConversation finalConversation = conversation;
        conversationRepository.save(conversation);

        return llmServiceRouter.complete(llmRequest, org, user)
            .map(response -> processLLMResponse(response, finalConversation));
    }

    /**
     * Create a new conversation for the user.
     */
    private WidgetAssistantConversation createNewConversation(User user, Organization org, Dashboard dashboard) {
        WidgetAssistantConversation conversation = WidgetAssistantConversation.builder()
            .id(UUID.randomUUID())
            .user(user)
            .organization(org)
            .dashboard(dashboard)
            .build();
        return conversationRepository.save(conversation);
    }

    /**
     * Confirm and create a suggested widget.
     */
    @Transactional
    public ConfirmResponse confirmWidget(ConfirmRequest request) {
        if (!request.confirmed()) {
            // Clear pending suggestion
            conversationRepository.findById(request.conversationId()).ifPresent(conv -> {
                conv.setPendingSuggestion(null);
                conversationRepository.save(conv);
            });
            return new ConfirmResponse(false, null, "Widget creation cancelled.");
        }

        // Verify dashboard access
        Organization org = securityUtils.getCurrentUserOrganization();
        User user = securityUtils.getCurrentUser();

        if (request.dashboardId() != null) {
            boolean hasAccess = dashboardRepository.findByIdAndOrganization(request.dashboardId(), org).isPresent();
            if (!hasAccess) {
                log.warn("User attempted to create widget on dashboard {} without permission", request.dashboardId());
                return new ConfirmResponse(false, null, "You do not have access to this dashboard.");
            }
        }

        // Get conversation and verify ownership
        WidgetAssistantConversation conversation = conversationRepository.findById(request.conversationId()).orElse(null);
        if (conversation == null) {
            return new ConfirmResponse(false, null, "Conversation not found.");
        }

        if (!conversation.getUser().getId().equals(user.getId())) {
            log.warn("User {} attempted to confirm widget in conversation {} belonging to another user",
                    user.getEmail(), request.conversationId());
            return new ConfirmResponse(false, null, "Invalid conversation.");
        }

        // Get pending suggestion from database
        if (conversation.getPendingSuggestion() == null) {
            return new ConfirmResponse(false, null, "No pending widget suggestion found.");
        }

        WidgetSuggestion suggestion;
        try {
            suggestion = objectMapper.readValue(conversation.getPendingSuggestion(), WidgetSuggestion.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to parse pending suggestion: {}", e.getMessage());
            conversation.setPendingSuggestion(null);
            conversationRepository.save(conversation);
            return new ConfirmResponse(false, null, "Invalid widget suggestion. Please try again.");
        }

        // Verify device ownership - device must belong to user's organization
        if (suggestion.deviceId() != null && !suggestion.deviceId().isBlank()) {
            boolean deviceBelongsToOrg = deviceRepository.findActiveByExternalId(suggestion.deviceId())
                    .filter(d -> d.getOrganization() != null && d.getOrganization().getId().equals(org.getId()))
                    .isPresent();
            if (!deviceBelongsToOrg) {
                log.warn("Widget creation blocked: device {} does not belong to organization {}",
                        suggestion.deviceId(), org.getId());
                conversation.setPendingSuggestion(null);
                conversationRepository.save(conversation);
                return new ConfirmResponse(false, null, "The specified device is not available.");
            }
        }

        try {
            // Create the widget
            WidgetCreateRequest widgetRequest = new WidgetCreateRequest(
                suggestion.name(),
                suggestion.type(),
                0, // positionX - will be auto-positioned
                calculateNextPositionY(request.dashboardId()), // positionY
                suggestion.width() != null ? suggestion.width() : 6,
                suggestion.height() != null ? suggestion.height() : 4,
                suggestion.deviceId(),
                null, // secondDeviceId
                suggestion.variableName(),
                null, // secondVariableName
                null, // deviceLabel
                null, // secondDeviceLabel
                WidgetAggregation.NONE,
                60, // timeRangeMinutes
                suggestion.config() != null ? objectMapper.valueToTree(suggestion.config()) : null
            );

            WidgetResponse widget = dashboardService.addWidget(request.dashboardId(), widgetRequest);

            // Clear pending suggestion and mark conversation as having created a widget
            conversation.setPendingSuggestion(null);
            conversationRepository.save(conversation);

            // Add a system message noting the widget was created
            WidgetAssistantMessage systemMessage = WidgetAssistantMessage.builder()
                .role(WidgetAssistantMessage.MessageRole.assistant)
                .content("Widget '" + suggestion.name() + "' has been created successfully.")
                .widgetCreated(true)
                .build();
            conversation.addMessage(systemMessage);
            messageRepository.save(systemMessage);

            return new ConfirmResponse(true, widget.id(),
                "Widget '" + suggestion.name() + "' created successfully!");
        } catch (Exception e) {
            log.error("Failed to create widget: {}", e.getMessage(), e);
            return new ConfirmResponse(false, null, "Failed to create widget: " + e.getMessage());
        }
    }

    /**
     * Get context information about available devices and variables.
     */
    @Transactional(readOnly = true)
    public ContextResponse getContext(Long dashboardId) {
        Organization org = securityUtils.getCurrentUserOrganization();

        Dashboard dashboard = dashboardRepository.findByIdAndOrganization(dashboardId, org)
            .orElseThrow(() -> new RuntimeException("Dashboard not found"));

        List<Device> devices = deviceRepository.findActiveByOrganization(org);

        List<DeviceInfo> deviceInfos = devices.stream()
            .map(device -> {
                List<Variable> variables = variableRepository.findByDeviceId(device.getId());
                List<VariableInfo> variableInfos = variables.stream()
                    .map(v -> new VariableInfo(
                        v.getName(),
                        v.getEffectiveDisplayName(),
                        v.getUnit(),
                        v.getLastValue() != null ? v.getLastValue().doubleValue() : null
                    ))
                    .collect(Collectors.toList());

                return new DeviceInfo(
                    device.getId(),
                    device.getExternalId(),
                    device.getName(),
                    device.getDeviceType() != null ? device.getDeviceType().getName() : device.getSensorType(),
                    variableInfos
                );
            })
            .collect(Collectors.toList());

        return new ContextResponse(dashboardId, dashboard.getName(), deviceInfos);
    }

    /**
     * Build context prompt with available devices and variables.
     */
    private String buildContextPrompt(Organization org, Long dashboardId) {
        List<Device> devices = deviceRepository.findActiveByOrganization(org);

        StringBuilder context = new StringBuilder();
        context.append("AVAILABLE DEVICES AND VARIABLES:\n");
        context.append("================================\n\n");

        for (Device device : devices) {
            context.append("Device: ").append(device.getName())
                   .append(" (ID: ").append(device.getExternalId()).append(")\n");

            if (device.getDeviceType() != null) {
                context.append("  Type: ").append(device.getDeviceType().getName()).append("\n");
            } else if (device.getSensorType() != null) {
                context.append("  Type: ").append(device.getSensorType()).append("\n");
            }

            List<Variable> variables = variableRepository.findByDeviceId(device.getId());
            if (!variables.isEmpty()) {
                context.append("  Variables:\n");
                for (Variable v : variables) {
                    context.append("    - ").append(v.getName());
                    if (v.getDisplayName() != null && !v.getDisplayName().equals(v.getName())) {
                        context.append(" (\"").append(v.getDisplayName()).append("\")");
                    }
                    if (v.getUnit() != null) {
                        context.append(" [").append(v.getUnit()).append("]");
                    }
                    if (v.getLastValue() != null) {
                        context.append(" = ").append(v.getLastValue());
                    }
                    context.append("\n");
                }
            }
            context.append("\n");
        }

        if (devices.isEmpty()) {
            context.append("No devices found. The user may need to add devices first.\n");
        }

        return context.toString();
    }

    /**
     * Process LLM response and extract widget suggestion if present.
     * Uses programmatic transaction management since this runs in a reactive context.
     */
    private ChatResponse processLLMResponse(LLMResponse llmResponse, WidgetAssistantConversation conversation) {
        if (!llmResponse.isSuccess()) {
            return new ChatResponse(
                conversation.getId(),
                "I encountered an error: " + llmResponse.getErrorMessage(),
                null, false,
                llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                llmResponse.getModelId(),
                llmResponse.getTotalTokens(),
                llmResponse.getLatencyMs()
            );
        }

        String content = llmResponse.getContent();

        // Use programmatic transaction for database operations in reactive context
        return transactionTemplate.execute(status -> {
            // Reload conversation within transaction to avoid detached entity issues
            WidgetAssistantConversation managedConversation = conversationRepository
                .findByIdWithMessages(conversation.getId())
                .orElse(conversation);

            // Save assistant response to database
            WidgetAssistantMessage assistantMessage = WidgetAssistantMessage.builder()
                .role(WidgetAssistantMessage.MessageRole.assistant)
                .content(content)
                .build();
            managedConversation.addMessage(assistantMessage);
            messageRepository.save(assistantMessage);

            try {
                // Parse JSON response
                JsonNode json = objectMapper.readTree(content);
                String type = json.path("type").asText();
                String message = json.path("message").asText();

                if ("suggestion".equals(type) && json.has("widget")) {
                    JsonNode widgetNode = json.get("widget");

                    // Parse widget type safely
                    WidgetType widgetType;
                    String widgetTypeStr = widgetNode.path("type").asText();
                    try {
                        widgetType = WidgetType.valueOf(widgetTypeStr);
                    } catch (IllegalArgumentException e) {
                        log.warn("LLM returned invalid widget type '{}', asking for clarification", widgetTypeStr);
                        return new ChatResponse(
                            managedConversation.getId(),
                            "I suggested an unsupported widget type. Could you specify what type of widget you want? Available types are: line chart, gauge, metric card, bar chart, area chart, pie chart, indicator, table, or map.",
                            null,
                            true,
                            llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                            llmResponse.getModelId(),
                            llmResponse.getTotalTokens(),
                            llmResponse.getLatencyMs()
                        );
                    }

                    WidgetSuggestion suggestion = new WidgetSuggestion(
                        widgetNode.path("name").asText(),
                        widgetType,
                        widgetNode.path("deviceId").asText(),
                        widgetNode.path("deviceName").asText(null),
                        widgetNode.path("variableName").asText(),
                        widgetNode.path("width").asInt(6),
                        widgetNode.path("height").asInt(4),
                        widgetNode.has("config") ? objectMapper.convertValue(widgetNode.get("config"), Map.class) : null
                    );

                    // Store pending suggestion in database
                    try {
                        managedConversation.setPendingSuggestion(objectMapper.writeValueAsString(suggestion));
                        conversationRepository.save(managedConversation);
                    } catch (JsonProcessingException e) {
                        log.error("Failed to serialize widget suggestion: {}", e.getMessage());
                    }

                    return new ChatResponse(
                        managedConversation.getId(),
                        message,
                        suggestion,
                        false,
                        llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                        llmResponse.getModelId(),
                        llmResponse.getTotalTokens(),
                        llmResponse.getLatencyMs()
                    );
                } else if ("clarification".equals(type)) {
                    return new ChatResponse(
                        managedConversation.getId(),
                        message,
                        null,
                        true,
                        llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                        llmResponse.getModelId(),
                        llmResponse.getTotalTokens(),
                        llmResponse.getLatencyMs()
                    );
                } else {
                    return new ChatResponse(
                        managedConversation.getId(),
                        message,
                        null,
                        false,
                        llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                        llmResponse.getModelId(),
                        llmResponse.getTotalTokens(),
                        llmResponse.getLatencyMs()
                    );
                }
            } catch (JsonProcessingException e) {
                // If not valid JSON, treat as plain text response
                log.warn("Failed to parse LLM response as JSON: {}", e.getMessage());
                return new ChatResponse(
                    managedConversation.getId(),
                    content,
                    null,
                    true, // Assume clarification needed if response wasn't structured
                    llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                    llmResponse.getModelId(),
                    llmResponse.getTotalTokens(),
                    llmResponse.getLatencyMs()
                );
            }
        });
    }

    /**
     * Calculate the next Y position for a new widget on the dashboard.
     */
    private int calculateNextPositionY(Long dashboardId) {
        try {
            Dashboard dashboard = dashboardRepository.findByIdWithWidgets(dashboardId).orElse(null);
            if (dashboard == null || dashboard.getWidgets().isEmpty()) {
                return 0;
            }

            // Find the maximum Y position + height
            return dashboard.getWidgets().stream()
                .mapToInt(w -> w.getPositionY() + w.getHeight())
                .max()
                .orElse(0);
        } catch (Exception e) {
            log.warn("Error calculating widget position: {}", e.getMessage());
            return 0;
        }
    }

    /**
     * Scheduled cleanup of abandoned conversations.
     * Runs every 5 minutes to remove conversations that haven't been accessed
     * within the configured TTL period.
     */
    @Scheduled(fixedRate = 300000) // 5 minutes
    @Transactional
    public void cleanupAbandonedConversations() {
        Instant cutoff = Instant.now().minusSeconds(conversationTtlMinutes * 60L);

        int deletedCount = conversationRepository.deleteAbandonedConversations(cutoff);

        if (deletedCount > 0) {
            log.info("Widget assistant cleanup: removed {} abandoned conversations (TTL: {} minutes)",
                    deletedCount, conversationTtlMinutes);
        }
    }

    /**
     * Get the count of active conversations (for monitoring/debugging).
     */
    public long getActiveConversationCount() {
        Instant since = Instant.now().minusSeconds(conversationTtlMinutes * 60L);
        return conversationRepository.countActiveConversationsSince(since);
    }
}
