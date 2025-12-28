package io.indcloud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.service.FunctionExecutionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controller for HTTP-triggered serverless functions.
 * Provides webhook endpoints for external systems to invoke functions.
 */
@RestController
@RequestMapping("/api/v1/webhooks/functions")
public class FunctionWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(FunctionWebhookController.class);

    private final FunctionTriggerRepository triggerRepository;
    private final FunctionExecutionService executionService;
    private final ObjectMapper objectMapper;

    public FunctionWebhookController(
        FunctionTriggerRepository triggerRepository,
        FunctionExecutionService executionService,
        ObjectMapper objectMapper
    ) {
        this.triggerRepository = triggerRepository;
        this.executionService = executionService;
        this.objectMapper = objectMapper;
    }

    /**
     * Generic webhook endpoint for HTTP-triggered functions.
     * URL format: /api/v1/webhooks/functions/{path}
     *
     * The {path} is configured in the trigger's config as {"path": "my-function"}
     */
    @PostMapping("/{path}")
    public ResponseEntity<Map<String, Object>> invokeFunction(
        @PathVariable String path,
        @RequestBody(required = false) JsonNode payload,
        @RequestHeader Map<String, String> headers
    ) {
        logger.info("Webhook invoked: path={}", path);

        // Find HTTP triggers matching this path
        List<FunctionTrigger> triggers = triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP);

        FunctionTrigger matchingTrigger = triggers.stream()
            .filter(trigger -> {
                JsonNode config = trigger.getTriggerConfig();
                if (config != null && config.has("path")) {
                    String triggerPath = config.get("path").asText();
                    return triggerPath.equals(path);
                }
                return false;
            })
            .findFirst()
            .orElse(null);

        if (matchingTrigger == null) {
            logger.warn("No HTTP trigger found for path: {}", path);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                    "error", "Function not found",
                    "path", path
                ));
        }

        // Check if function is enabled
        if (!matchingTrigger.getFunction().getEnabled()) {
            logger.warn("Function is disabled: {}", matchingTrigger.getFunction().getName());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                    "error", "Function is disabled",
                    "function", matchingTrigger.getFunction().getName()
                ));
        }

        // Build input data with payload and metadata
        Map<String, Object> inputData = new HashMap<>();
        inputData.put("body", payload);
        inputData.put("headers", headers);
        inputData.put("path", path);

        JsonNode input = objectMapper.valueToTree(inputData);

        try {
            // Execute function asynchronously
            executionService.executeFunctionAsync(
                matchingTrigger.getFunction().getId(),
                input,
                matchingTrigger
            );

            logger.info("Function invoked asynchronously: {}", matchingTrigger.getFunction().getName());

            // Return immediate acknowledgment
            return ResponseEntity.accepted()
                .body(Map.of(
                    "message", "Function invoked",
                    "function", matchingTrigger.getFunction().getName(),
                    "status", "processing"
                ));

        } catch (Exception e) {
            logger.error("Error invoking function: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of(
                    "error", "Function invocation failed",
                    "message", e.getMessage()
                ));
        }
    }
}
