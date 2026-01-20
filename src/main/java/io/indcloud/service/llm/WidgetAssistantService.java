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
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * Service for AI-powered widget creation via natural language.
 * Uses LLMServiceRouter to parse user requests and suggest widget configurations.
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
    private final SecurityUtils securityUtils;
    private final ObjectMapper objectMapper;

    @Value("${llm.features.widget-assistant.enabled:true}")
    private boolean widgetAssistantEnabled;

    @Value("${llm.features.widget-assistant.conversation-ttl-minutes:30}")
    private int conversationTtlMinutes;

    // In-memory conversation storage (per conversation ID)
    // In production, use database storage with WidgetAssistantConversation entities
    private final Map<UUID, List<LLMRequest.Message>> conversationHistory = new ConcurrentHashMap<>();
    private final Map<UUID, WidgetSuggestion> pendingSuggestions = new ConcurrentHashMap<>();
    private final Map<UUID, Instant> conversationLastAccess = new ConcurrentHashMap<>();

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
    @Transactional(readOnly = true)
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
        if (request.dashboardId() != null) {
            boolean hasAccess = dashboardRepository.findByIdAndOrganization(request.dashboardId(), org).isPresent();
            if (!hasAccess) {
                log.warn("User {} attempted to access dashboard {} without permission",
                        user.getEmail(), request.dashboardId());
                return Mono.just(new ChatResponse(
                    null, "You do not have access to this dashboard.", null, false,
                    null, null, null, null
                ));
            }
        }

        // Create or get conversation ID
        UUID conversationId = request.conversationId() != null ?
            request.conversationId() : UUID.randomUUID();

        // Build context with available devices and variables
        String contextPrompt = buildContextPrompt(org, request.dashboardId());

        // Get conversation history and update last access time
        List<LLMRequest.Message> history = conversationHistory.computeIfAbsent(
            conversationId, k -> new ArrayList<>()
        );
        conversationLastAccess.put(conversationId, Instant.now());

        // Add user message to history
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

        return llmServiceRouter.complete(llmRequest, org, user)
            .map(response -> processLLMResponse(response, conversationId));
    }

    /**
     * Confirm and create a suggested widget.
     */
    @Transactional
    public ConfirmResponse confirmWidget(ConfirmRequest request) {
        if (!request.confirmed()) {
            pendingSuggestions.remove(request.conversationId());
            return new ConfirmResponse(false, null, "Widget creation cancelled.");
        }

        // Verify dashboard access
        Organization org = securityUtils.getCurrentUserOrganization();
        if (request.dashboardId() != null) {
            boolean hasAccess = dashboardRepository.findByIdAndOrganization(request.dashboardId(), org).isPresent();
            if (!hasAccess) {
                log.warn("User attempted to create widget on dashboard {} without permission", request.dashboardId());
                return new ConfirmResponse(false, null, "You do not have access to this dashboard.");
            }
        }

        WidgetSuggestion suggestion = pendingSuggestions.get(request.conversationId());
        if (suggestion == null) {
            return new ConfirmResponse(false, null, "No pending widget suggestion found.");
        }

        // Verify device ownership - device must belong to user's organization
        if (suggestion.deviceId() != null && !suggestion.deviceId().isBlank()) {
            boolean deviceBelongsToOrg = deviceRepository.findActiveByExternalId(suggestion.deviceId())
                    .filter(d -> d.getOrganization() != null && d.getOrganization().getId().equals(org.getId()))
                    .isPresent();
            if (!deviceBelongsToOrg) {
                log.warn("Widget creation blocked: device {} does not belong to organization {}",
                        suggestion.deviceId(), org.getId());
                pendingSuggestions.remove(request.conversationId());
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

            // Clean up
            pendingSuggestions.remove(request.conversationId());
            conversationHistory.remove(request.conversationId());

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
     */
    private ChatResponse processLLMResponse(LLMResponse llmResponse, UUID conversationId) {
        if (!llmResponse.isSuccess()) {
            return new ChatResponse(
                conversationId,
                "I encountered an error: " + llmResponse.getErrorMessage(),
                null, false,
                llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                llmResponse.getModelId(),
                llmResponse.getTotalTokens(),
                llmResponse.getLatencyMs()
            );
        }

        String content = llmResponse.getContent();

        // Add assistant response to history
        conversationHistory.computeIfPresent(conversationId, (k, history) -> {
            history.add(LLMRequest.Message.builder()
                .role("assistant")
                .content(content)
                .build());
            return history;
        });

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
                        conversationId,
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

                // Store pending suggestion
                pendingSuggestions.put(conversationId, suggestion);

                return new ChatResponse(
                    conversationId,
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
                    conversationId,
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
                    conversationId,
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
                conversationId,
                content,
                null,
                true, // Assume clarification needed if response wasn't structured
                llmResponse.getProvider() != null ? llmResponse.getProvider().name() : null,
                llmResponse.getModelId(),
                llmResponse.getTotalTokens(),
                llmResponse.getLatencyMs()
            );
        }
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
    public void cleanupAbandonedConversations() {
        if (conversationLastAccess.isEmpty()) {
            return;
        }

        Instant cutoff = Instant.now().minusSeconds(conversationTtlMinutes * 60L);
        int cleanedCount = 0;

        for (Map.Entry<UUID, Instant> entry : conversationLastAccess.entrySet()) {
            if (entry.getValue().isBefore(cutoff)) {
                UUID conversationId = entry.getKey();
                conversationHistory.remove(conversationId);
                pendingSuggestions.remove(conversationId);
                conversationLastAccess.remove(conversationId);
                cleanedCount++;
            }
        }

        if (cleanedCount > 0) {
            log.info("Widget assistant cleanup: removed {} abandoned conversations (TTL: {} minutes)",
                    cleanedCount, conversationTtlMinutes);
        }
    }

    /**
     * Get the count of active conversations (for monitoring/debugging).
     */
    public int getActiveConversationCount() {
        return conversationHistory.size();
    }
}
