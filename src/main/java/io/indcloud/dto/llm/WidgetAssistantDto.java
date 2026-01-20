package io.indcloud.dto.llm;

import io.indcloud.model.WidgetType;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * DTOs for the AI Widget Assistant feature.
 */
public class WidgetAssistantDto {

    /**
     * Request to send a message to the widget assistant.
     */
    public record ChatRequest(
        String message,
        Long dashboardId,
        UUID conversationId
    ) {}

    /**
     * Response from the widget assistant.
     */
    public record ChatResponse(
        UUID conversationId,
        String response,
        WidgetSuggestion widgetSuggestion,
        boolean needsClarification,
        String provider,
        String modelId,
        Integer tokensUsed,
        Integer latencyMs
    ) {}

    /**
     * Widget configuration suggested by the AI.
     */
    public record WidgetSuggestion(
        String name,
        WidgetType type,
        String deviceId,
        String deviceName,
        String variableName,
        Integer width,
        Integer height,
        Map<String, Object> config
    ) {}

    /**
     * Request to confirm and create a suggested widget.
     */
    public record ConfirmRequest(
        UUID conversationId,
        Long dashboardId,
        boolean confirmed
    ) {}

    /**
     * Response after widget creation.
     */
    public record ConfirmResponse(
        boolean success,
        Long widgetId,
        String message
    ) {}

    /**
     * Context information about available devices and variables.
     */
    public record ContextResponse(
        Long dashboardId,
        String dashboardName,
        List<DeviceInfo> devices
    ) {}

    /**
     * Device information with its variables.
     */
    public record DeviceInfo(
        UUID id,
        String externalId,
        String name,
        String deviceType,
        List<VariableInfo> variables
    ) {}

    /**
     * Variable information for a device.
     */
    public record VariableInfo(
        String name,
        String displayName,
        String unit,
        Double lastValue
    ) {}
}
