package org.sensorvision.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.PluginExecution;
import org.sensorvision.service.DataPluginService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller for receiving webhook data from external systems.
 * Webhooks are routed to specific plugins based on the plugin name in the URL.
 *
 * URL pattern: /api/v1/webhooks/{organizationId}/{pluginName}
 *
 * Example:
 * POST /api/v1/webhooks/1/my-lorawan-integration
 * Body: { TTN webhook payload }
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhooks")
@RequiredArgsConstructor
public class WebhookReceiverController {

    private final DataPluginService pluginService;

    /**
     * Receive webhook data and process it through the specified plugin.
     *
     * @param organizationId Organization ID (for multi-tenancy)
     * @param pluginName Name of the plugin to execute
     * @param payload Raw webhook payload (JSON string)
     * @return Execution result
     */
    @PostMapping("/{organizationId}/{pluginName}")
    public ResponseEntity<Map<String, Object>> receiveWebhook(
            @PathVariable Long organizationId,
            @PathVariable String pluginName,
            @RequestBody String payload
    ) {
        log.info("Received webhook for plugin: {} (org: {})", pluginName, organizationId);
        log.debug("Webhook payload: {}", payload);

        try {
            PluginExecution execution = pluginService.executePluginByName(
                    organizationId,
                    pluginName,
                    payload
            );

            Map<String, Object> response = Map.of(
                    "success", execution.getStatus().name().equals("SUCCESS") ||
                            execution.getStatus().name().equals("PARTIAL"),
                    "executionId", execution.getId(),
                    "status", execution.getStatus().name(),
                    "recordsProcessed", execution.getRecordsProcessed(),
                    "durationMs", execution.getDurationMs()
            );

            if (execution.getErrorMessage() != null) {
                response = new java.util.HashMap<>(response);
                response.put("errorMessage", execution.getErrorMessage());
            }

            return ResponseEntity.ok(response);

        } catch (IllegalArgumentException e) {
            log.error("Plugin not found: {}", e.getMessage());
            return ResponseEntity.notFound().build();

        } catch (IllegalStateException e) {
            log.error("Plugin execution error: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));

        } catch (Exception e) {
            log.error("Unexpected error processing webhook: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Internal server error"));
        }
    }

    /**
     * Test endpoint to verify webhook configuration.
     * Returns 200 OK if the plugin exists and is enabled.
     */
    @GetMapping("/{organizationId}/{pluginName}/test")
    public ResponseEntity<Map<String, Object>> testWebhook(
            @PathVariable Long organizationId,
            @PathVariable String pluginName
    ) {
        try {
            // This will throw if plugin doesn't exist or is disabled
            pluginService.executePluginByName(organizationId, pluginName, "{}");

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Plugin is configured and enabled"
            ));

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();

        } catch (Exception e) {
            return ResponseEntity.ok(Map.of(
                    "success", false,
                    "message", "Plugin exists but configuration may need adjustment",
                    "error", e.getMessage()
            ));
        }
    }
}
