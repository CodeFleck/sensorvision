package io.indcloud.controller;

import io.indcloud.dto.llm.WidgetAssistantDto.*;
import io.indcloud.service.llm.WidgetAssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

/**
 * REST controller for AI-powered widget creation assistant.
 * Provides endpoints for natural language widget creation.
 */
@RestController
@RequestMapping("/api/v1/widget-assistant")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Widget Assistant", description = "AI-powered widget creation via natural language")
public class WidgetAssistantController {

    private final WidgetAssistantService widgetAssistantService;

    /**
     * Send a message to the widget assistant and get AI response.
     *
     * @param request The chat request containing user message and context
     * @return AI response with optional widget suggestion
     */
    @PostMapping("/chat")
    @Operation(summary = "Chat with widget assistant",
               description = "Send a natural language message describing the widget you want to create")
    public Mono<ResponseEntity<ChatResponse>> chat(@RequestBody ChatRequest request) {
        log.info("Widget assistant chat request for dashboard {}: {}",
                request.dashboardId(),
                request.message() != null ? request.message().substring(0, Math.min(50, request.message().length())) + "..." : "null");

        return widgetAssistantService.chat(request)
            .map(ResponseEntity::ok)
            .doOnNext(response -> log.info("Widget assistant response: suggestion={}",
                response.getBody() != null && response.getBody().widgetSuggestion() != null))
            .onErrorResume(e -> {
                log.error("Widget assistant chat error: {}", e.getMessage(), e);
                return Mono.just(ResponseEntity.internalServerError().build());
            });
    }

    /**
     * Confirm and create a suggested widget.
     *
     * @param request Confirmation request with conversation ID
     * @return Result of widget creation
     */
    @PostMapping("/confirm")
    @Operation(summary = "Confirm widget creation",
               description = "Confirm and create a widget that was suggested by the assistant")
    public ResponseEntity<ConfirmResponse> confirmWidget(@RequestBody ConfirmRequest request) {
        log.info("Widget assistant confirm request: conversationId={}, dashboardId={}, confirmed={}",
                request.conversationId(), request.dashboardId(), request.confirmed());

        ConfirmResponse response = widgetAssistantService.confirmWidget(request);

        log.info("Widget creation result: success={}, widgetId={}",
                response.success(), response.widgetId());

        return ResponseEntity.ok(response);
    }

    /**
     * Get context information about available devices and variables for a dashboard.
     *
     * @param dashboardId The dashboard ID
     * @return Available devices and their variables
     */
    @GetMapping("/context/{dashboardId}")
    @Operation(summary = "Get dashboard context",
               description = "Get available devices and variables for widget creation")
    public ResponseEntity<ContextResponse> getContext(@PathVariable Long dashboardId) {
        log.debug("Widget assistant context request for dashboard {}", dashboardId);

        ContextResponse context = widgetAssistantService.getContext(dashboardId);

        log.debug("Widget assistant context: {} devices", context.devices().size());

        return ResponseEntity.ok(context);
    }
}
