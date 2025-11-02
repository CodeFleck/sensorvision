package org.sensorvision.scheduler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.FunctionTrigger;
import org.sensorvision.model.FunctionTriggerType;
import org.sensorvision.repository.FunctionTriggerRepository;
import org.sensorvision.service.FunctionExecutionService;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;

/**
 * Executes serverless functions on a schedule using cron expressions.
 * Dynamically loads and updates schedules from the database.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ScheduledFunctionExecutor {

    private final TaskScheduler taskScheduler;
    private final FunctionTriggerRepository triggerRepository;
    private final FunctionExecutionService executionService;
    private final ObjectMapper objectMapper;

    // Map of trigger ID to scheduled task
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new ConcurrentHashMap<>();

    /**
     * Initialize schedules when application starts.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        log.info("Initializing scheduled function triggers...");
        reloadSchedules();
        log.info("Scheduled function triggers initialized");
    }

    /**
     * Reload schedules every minute to pick up changes.
     * This allows users to add/modify/delete scheduled triggers without restarting.
     */
    @Scheduled(fixedRate = 60000, initialDelay = 60000) // Every 1 minute
    public void reloadSchedules() {
        try {
            List<FunctionTrigger> triggers = triggerRepository
                .findByTriggerTypeAndEnabledTrue(FunctionTriggerType.SCHEDULED);

            log.debug("Reloading {} scheduled function triggers", triggers.size());

            // Add or update triggers
            for (FunctionTrigger trigger : triggers) {
                if (!scheduledTasks.containsKey(trigger.getId())) {
                    scheduleFunction(trigger);
                } else {
                    // Check if cron expression changed
                    String currentCron = getCronExpression(trigger);
                    ScheduledFuture<?> existing = scheduledTasks.get(trigger.getId());

                    // For simplicity, if trigger exists, we keep it
                    // In a more advanced implementation, we could check if cron changed
                    log.trace("Scheduled trigger {} already exists", trigger.getId());
                }
            }

            // Remove deleted or disabled triggers
            scheduledTasks.entrySet().removeIf(entry -> {
                Long triggerId = entry.getKey();
                boolean exists = triggers.stream().anyMatch(t -> t.getId().equals(triggerId));

                if (!exists) {
                    log.info("Cancelling scheduled trigger {} (deleted or disabled)", triggerId);
                    entry.getValue().cancel(false);
                    return true;
                }
                return false;
            });

        } catch (Exception e) {
            log.error("Error reloading scheduled function triggers: {}", e.getMessage(), e);
        }
    }

    /**
     * Schedule a function to execute on a cron schedule.
     */
    private void scheduleFunction(FunctionTrigger trigger) {
        try {
            String cronExpression = getCronExpression(trigger);

            // Validate cron expression
            if (!isValidCronExpression(cronExpression)) {
                log.error("Invalid cron expression for trigger {}: {}",
                    trigger.getId(), cronExpression);
                return;
            }

            // Get timezone (default to UTC)
            String timezone = getTimezone(trigger);
            ZoneId zoneId = ZoneId.of(timezone);

            // Create cron trigger
            CronTrigger cronTrigger = new CronTrigger(cronExpression, zoneId);

            // Schedule the task
            ScheduledFuture<?> scheduledTask = taskScheduler.schedule(() -> {
                executeScheduledFunction(trigger);
            }, cronTrigger);

            scheduledTasks.put(trigger.getId(), scheduledTask);

            log.info("Scheduled function {} (ID: {}) with cron: {} (timezone: {})",
                trigger.getFunction().getName(),
                trigger.getId(),
                cronExpression,
                timezone);

        } catch (Exception e) {
            log.error("Error scheduling function trigger {}: {}",
                trigger.getId(), e.getMessage(), e);
        }
    }

    /**
     * Execute a scheduled function.
     */
    private void executeScheduledFunction(FunctionTrigger trigger) {
        try {
            log.debug("Executing scheduled function {} (trigger: {})",
                trigger.getFunction().getName(), trigger.getId());

            // Build event payload
            JsonNode eventPayload = buildEventPayload(trigger);

            // Execute function asynchronously
            executionService.executeFunctionAsync(
                trigger.getFunction().getId(),
                eventPayload,
                trigger
            );

            log.info("Executed scheduled function {} (trigger: {})",
                trigger.getFunction().getName(), trigger.getId());

        } catch (Exception e) {
            log.error("Error executing scheduled function trigger {}: {}",
                trigger.getId(), e.getMessage(), e);
        }
    }

    /**
     * Build event payload for scheduled execution.
     */
    private JsonNode buildEventPayload(FunctionTrigger trigger) {
        ObjectNode payload = objectMapper.createObjectNode();

        payload.put("eventType", "scheduled");
        payload.put("triggerId", trigger.getId());
        payload.put("timestamp", Instant.now().toString());
        payload.put("cronExpression", getCronExpression(trigger));

        // Add any custom data from trigger config
        JsonNode config = trigger.getTriggerConfig();
        if (config.has("data")) {
            payload.set("data", config.get("data"));
        }

        return payload;
    }

    /**
     * Get cron expression from trigger config.
     */
    private String getCronExpression(FunctionTrigger trigger) {
        JsonNode config = trigger.getTriggerConfig();
        return config.path("cronExpression").asText("0 0 * * * *"); // Default: every hour
    }

    /**
     * Get timezone from trigger config.
     */
    private String getTimezone(FunctionTrigger trigger) {
        JsonNode config = trigger.getTriggerConfig();
        return config.path("timezone").asText("UTC");
    }

    /**
     * Validate cron expression.
     */
    private boolean isValidCronExpression(String cronExpression) {
        try {
            CronExpression.parse(cronExpression);
            return true;
        } catch (Exception e) {
            log.warn("Invalid cron expression: {}", cronExpression);
            return false;
        }
    }

    /**
     * Cancel all scheduled tasks (useful for testing or shutdown).
     */
    public void cancelAllSchedules() {
        log.info("Cancelling all {} scheduled function triggers", scheduledTasks.size());
        scheduledTasks.values().forEach(task -> task.cancel(false));
        scheduledTasks.clear();
    }

    /**
     * Get count of active scheduled tasks.
     */
    public int getActiveScheduleCount() {
        return scheduledTasks.size();
    }

    /**
     * Check if a specific trigger is scheduled.
     */
    public boolean isScheduled(Long triggerId) {
        return scheduledTasks.containsKey(triggerId);
    }
}
