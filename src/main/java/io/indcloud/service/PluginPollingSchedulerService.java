package io.indcloud.service;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.DataPlugin;
import io.indcloud.model.Device;
import io.indcloud.model.DeviceStatus;
import io.indcloud.model.Organization;
import io.indcloud.model.PluginExecution;
import io.indcloud.model.PluginExecutionStatus;
import io.indcloud.model.PluginProvider;
import io.indcloud.mqtt.TelemetryPayload;
import io.indcloud.plugin.BasePollingPlugin;
import io.indcloud.plugin.DataPluginProcessor;
import io.indcloud.repository.DataPluginRepository;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.PluginExecutionRepository;
import io.indcloud.security.DeviceTokenAuthenticationFilter;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.PreDestroy;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.UUID;

/**
 * Service that schedules and executes polling plugins.
 * Polling plugins actively fetch data from external sources (Modbus, SNMP, APIs, etc.)
 * unlike webhook plugins that receive data passively.
 */
@Slf4j
@Service
public class PluginPollingSchedulerService {

    private final DataPluginRepository pluginRepository;
    private final PluginExecutionRepository executionRepository;
    private final DeviceRepository deviceRepository;
    private final TelemetryIngestionService telemetryIngestionService;
    private final Map<PluginProvider, BasePollingPlugin> pollingPlugins;
    private final TaskScheduler taskScheduler;
    private final Map<Long, ScheduledFuture<?>> scheduledTasks = new HashMap<>();

    // Self-reference for calling @Transactional method through proxy
    private PluginPollingSchedulerService self;

    public PluginPollingSchedulerService(
            DataPluginRepository pluginRepository,
            PluginExecutionRepository executionRepository,
            DeviceRepository deviceRepository,
            TelemetryIngestionService telemetryIngestionService,
            List<DataPluginProcessor> processors
    ) {
        this.pluginRepository = pluginRepository;
        this.executionRepository = executionRepository;
        this.deviceRepository = deviceRepository;
        this.telemetryIngestionService = telemetryIngestionService;

        // Filter out polling plugins from all processors
        this.pollingPlugins = new HashMap<>();
        for (DataPluginProcessor processor : processors) {
            if (processor instanceof BasePollingPlugin) {
                pollingPlugins.put(processor.getSupportedProvider(), (BasePollingPlugin) processor);
                log.info("Registered polling plugin: {}", processor.getSupportedProvider());
            }
        }

        // Create task scheduler for dynamic scheduling
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10); // Support up to 10 concurrent polling tasks
        scheduler.setThreadNamePrefix("plugin-poller-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.setAwaitTerminationSeconds(30);
        scheduler.initialize();
        this.taskScheduler = scheduler;
    }

    /**
     * Inject self-reference for proxy calls
     * Called by Spring after bean construction
     * @Lazy breaks circular dependency during bean creation
     */
    @org.springframework.beans.factory.annotation.Autowired
    public void setSelf(@Lazy PluginPollingSchedulerService self) {
        this.self = self;
    }

    /**
     * Initialize polling schedules when application starts
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializePollingSchedules() {
        log.info("Initializing polling plugin schedules...");

        List<DataPlugin> allPlugins = pluginRepository.findAll();
        int pollingCount = 0;

        for (DataPlugin plugin : allPlugins) {
            if (pollingPlugins.containsKey(plugin.getProvider()) && plugin.getEnabled()) {
                schedulePlugin(plugin);
                pollingCount++;
            }
        }

        log.info("Initialized {} polling plugin schedules", pollingCount);
    }

    /**
     * Schedule a polling plugin for periodic execution
     *
     * @param plugin Plugin to schedule
     */
    public void schedulePlugin(DataPlugin plugin) {
        // Validate it's a polling plugin
        if (!pollingPlugins.containsKey(plugin.getProvider())) {
            log.warn("Cannot schedule non-polling plugin: {}", plugin.getName());
            return;
        }

        // Cancel existing schedule if any
        unschedulePlugin(plugin.getId());

        // Get polling interval from configuration
        JsonNode config = plugin.getConfiguration();
        int intervalSeconds = config.has("pollingIntervalSeconds")
                ? config.get("pollingIntervalSeconds").asInt(60)
                : 60;

        // Schedule the polling task - use self-reference to call through proxy for @Transactional
        ScheduledFuture<?> scheduledTask = taskScheduler.scheduleAtFixedRate(
                () -> self.executePollTask(plugin.getId()),
                Instant.now().plus(Duration.ofSeconds(5)), // Start after 5 seconds
                Duration.ofSeconds(intervalSeconds)
        );

        scheduledTasks.put(plugin.getId(), scheduledTask);

        log.info("Scheduled polling plugin '{}' (ID: {}) to run every {} seconds",
                plugin.getName(), plugin.getId(), intervalSeconds);
    }

    /**
     * Unschedule a polling plugin
     *
     * @param pluginId Plugin ID to unschedule
     */
    public void unschedulePlugin(Long pluginId) {
        ScheduledFuture<?> existingTask = scheduledTasks.remove(pluginId);
        if (existingTask != null) {
            existingTask.cancel(false);
            log.info("Unscheduled polling plugin ID: {}", pluginId);
        }
    }

    /**
     * Reschedule a plugin (useful when configuration changes)
     *
     * @param pluginId Plugin ID to reschedule
     */
    public void reschedulePlugin(Long pluginId) {
        pluginRepository.findById(pluginId).ifPresent(plugin -> {
            if (plugin.getEnabled() && pollingPlugins.containsKey(plugin.getProvider())) {
                schedulePlugin(plugin);
            } else {
                unschedulePlugin(pluginId);
            }
        });
    }

    /**
     * Execute a single poll task for a plugin
     * Must be public and @Transactional so Spring AOP can proxy it and keep transaction open
     *
     * @param pluginId Plugin ID to poll
     */
    @Transactional
    public void executePollTask(Long pluginId) {
        Instant startTime = Instant.now();

        // Reload plugin with organization eagerly loaded to avoid LazyInitializationException
        DataPlugin plugin = pluginRepository.findById(pluginId).orElse(null);
        if (plugin == null) {
            log.error("Plugin not found: {}", pluginId);
            return;
        }

        // Force load organization to avoid lazy loading issues
        Organization org = plugin.getOrganization();
        if (org != null) {
            org.getId(); // Touch to initialize
        }

        PluginExecution execution = new PluginExecution();
        execution.setPlugin(plugin);
        execution.setExecutedAt(startTime);

        BasePollingPlugin pollingPlugin = pollingPlugins.get(plugin.getProvider());
        if (pollingPlugin == null) {
            String errorMsg = "Polling plugin processor not found for: " + plugin.getProvider();
            log.error(errorMsg);
            execution.setStatus(PluginExecutionStatus.FAILED);
            execution.setErrorMessage(errorMsg);
            execution.setRecordsProcessed(0);
            execution.setDurationMs(0L);
            executionRepository.save(execution);
            return;
        }

        try {
            log.debug("Executing poll for plugin '{}' (ID: {})", plugin.getName(), plugin.getId());

            // Set up security context to ensure auto-provisioned devices land in the correct organization
            // Create a transient (non-persisted) device for authentication purposes
            SecurityContext oldContext = SecurityContextHolder.getContext();
            try {
                // Create transient device with all required fields but don't persist
                Device authDevice = Device.builder()
                        .id(UUID.randomUUID())
                        .externalId("_plugin_auth_" + plugin.getId())
                        .name("Plugin Auth Device")
                        .organization(org)
                        .status(DeviceStatus.ONLINE)
                        .active(true)
                        .healthScore(100)
                        .build();

                // Create device token authentication
                Authentication auth = new DeviceTokenAuthenticationFilter.DeviceTokenAuthentication(
                        "plugin_" + plugin.getId(),
                        authDevice,
                        Collections.emptyList()
                );

                SecurityContext newContext = SecurityContextHolder.createEmptyContext();
                newContext.setAuthentication(auth);
                SecurityContextHolder.setContext(newContext);

                // Execute the poll
                BasePollingPlugin.PollingResult result = pollingPlugin.poll(plugin.getConfiguration());

                // Convert to telemetry payload
                TelemetryPayload payload = new TelemetryPayload(
                        result.getDeviceId(),
                        result.getTimestamp(),
                        result.getVariables(),
                        new HashMap<>() // Empty metadata for now
                );

                // Ingest into telemetry system
                telemetryIngestionService.ingest(payload);

                // Record successful execution
                execution.setStatus(PluginExecutionStatus.SUCCESS);
                execution.setRecordsProcessed(1); // One telemetry record ingested

                log.debug("Successfully polled and ingested data for device '{}' from plugin '{}'",
                        result.getDeviceId(), plugin.getName());

            } finally {
                // Always restore original security context
                SecurityContextHolder.setContext(oldContext);
            }

        } catch (Exception e) {
            log.error("Failed to execute poll for plugin '{}' (ID: {}): {}",
                    plugin.getName(), plugin.getId(), e.getMessage(), e);

            // Record failed execution
            execution.setStatus(PluginExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setRecordsProcessed(0);

            // Don't rethrow - we want the schedule to continue even if one poll fails
        } finally {
            // Calculate duration and save execution
            Instant endTime = Instant.now();
            execution.setDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli());
            executionRepository.save(execution);
        }
    }

    /**
     * Get the current polling status for all scheduled plugins
     *
     * @return Map of plugin ID to whether it's currently scheduled
     */
    public Map<Long, Boolean> getPollingStatus() {
        Map<Long, Boolean> status = new HashMap<>();
        for (Map.Entry<Long, ScheduledFuture<?>> entry : scheduledTasks.entrySet()) {
            status.put(entry.getKey(), !entry.getValue().isCancelled());
        }
        return status;
    }

    /**
     * Manually trigger a poll for a specific plugin (for testing)
     *
     * @param pluginId Plugin ID to poll
     */
    public void triggerManualPoll(Long pluginId) {
        pluginRepository.findById(pluginId).ifPresent(plugin -> {
            if (!pollingPlugins.containsKey(plugin.getProvider())) {
                throw new IllegalArgumentException("Plugin is not a polling plugin: " + plugin.getName());
            }

            log.info("Manually triggering poll for plugin '{}' (ID: {})", plugin.getName(), pluginId);
            // Use self-reference to call through proxy for @Transactional
            self.executePollTask(pluginId);
        });
    }

    /**
     * Cleanup scheduled tasks on shutdown
     */
    @PreDestroy
    public void cleanup() {
        log.info("Shutting down polling scheduler, canceling {} scheduled tasks", scheduledTasks.size());
        for (ScheduledFuture<?> task : scheduledTasks.values()) {
            task.cancel(false);
        }
        scheduledTasks.clear();
    }
}
