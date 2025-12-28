package io.indcloud.service.triggers;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.FunctionTrigger;

import java.util.List;

/**
 * Interface for handling function triggers.
 * Implementations are responsible for matching events against trigger configurations
 * and executing functions when triggers are activated.
 */
public interface FunctionTriggerHandler {

    /**
     * Get the type of trigger this handler supports.
     */
    String getSupportedTriggerType();

    /**
     * Process an event and execute matching functions.
     *
     * @param event The event data
     * @param context Additional context information
     */
    void handleEvent(JsonNode event, TriggerContext context);

    /**
     * Check if a trigger matches the given event.
     *
     * @param trigger The function trigger configuration
     * @param event The event data
     * @param context Additional context information
     * @return true if the trigger matches and should be executed
     */
    boolean matchesTrigger(FunctionTrigger trigger, JsonNode event, TriggerContext context);

    /**
     * Build the event payload that will be passed to the function.
     *
     * @param trigger The function trigger
     * @param event The original event data
     * @param context Additional context information
     * @return The event payload for the function
     */
    JsonNode buildEventPayload(FunctionTrigger trigger, JsonNode event, TriggerContext context);

    /**
     * Get all enabled triggers of this type.
     *
     * @return List of enabled triggers
     */
    List<FunctionTrigger> getEnabledTriggers();
}
