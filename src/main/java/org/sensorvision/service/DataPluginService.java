package org.sensorvision.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.DataPlugin;
import org.sensorvision.model.PluginExecution;
import org.sensorvision.model.PluginExecutionStatus;
import org.sensorvision.model.PluginProvider;
import org.sensorvision.mqtt.TelemetryPayload;
import org.sensorvision.plugin.DataPluginProcessor;
import org.sensorvision.plugin.PluginProcessingException;
import org.sensorvision.plugin.PluginValidationResult;
import org.sensorvision.repository.DataPluginRepository;
import org.sensorvision.repository.PluginExecutionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class DataPluginService {

    private final DataPluginRepository pluginRepository;
    private final PluginExecutionRepository executionRepository;
    private final TelemetryIngestionService telemetryIngestionService;
    private final Map<PluginProvider, DataPluginProcessor> processors = new HashMap<>();
    private final ObjectMapper objectMapper;

    public DataPluginService(
            DataPluginRepository pluginRepository,
            PluginExecutionRepository executionRepository,
            TelemetryIngestionService telemetryIngestionService,
            List<DataPluginProcessor> processorList,
            ObjectMapper objectMapper
    ) {
        this.pluginRepository = pluginRepository;
        this.executionRepository = executionRepository;
        this.telemetryIngestionService = telemetryIngestionService;
        this.objectMapper = objectMapper;

        // Register all plugin processors
        for (DataPluginProcessor processor : processorList) {
            processors.put(processor.getSupportedProvider(), processor);
            log.info("Registered plugin processor: {}", processor.getSupportedProvider());
        }
    }

    /**
     * Execute a plugin with raw data input
     *
     * @param pluginId ID of the plugin to execute
     * @param rawData Raw input data
     * @return Execution result
     */
    public PluginExecution executePlugin(Long pluginId, Object rawData) {
        DataPlugin plugin = pluginRepository.findById(pluginId)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginId));

        if (!plugin.getEnabled()) {
            throw new IllegalStateException("Plugin is disabled: " + plugin.getName());
        }

        return executePlugin(plugin, rawData);
    }

    /**
     * Execute a plugin by name within an organization
     *
     * @param organizationId Organization ID
     * @param pluginName Plugin name
     * @param rawData Raw input data
     * @return Execution result
     */
    public PluginExecution executePluginByName(Long organizationId, String pluginName, Object rawData) {
        DataPlugin plugin = pluginRepository.findByOrganizationIdAndName(organizationId, pluginName)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Plugin not found: " + pluginName + " in organization " + organizationId
                ));

        if (!plugin.getEnabled()) {
            throw new IllegalStateException("Plugin is disabled: " + plugin.getName());
        }

        return executePlugin(plugin, rawData);
    }

    /**
     * Execute a plugin with the given configuration
     *
     * @param plugin Plugin configuration
     * @param rawData Raw input data
     * @return Execution result
     */
    private PluginExecution executePlugin(DataPlugin plugin, Object rawData) {
        Instant startTime = Instant.now();
        PluginExecution execution = new PluginExecution();
        execution.setPlugin(plugin);
        execution.setExecutedAt(startTime);

        DataPluginProcessor processor = processors.get(plugin.getProvider());
        if (processor == null) {
            String errorMsg = "No processor registered for provider: " + plugin.getProvider();
            log.error(errorMsg);
            execution.setStatus(PluginExecutionStatus.FAILED);
            execution.setErrorMessage(errorMsg);
            execution.setRecordsProcessed(0);
            execution.setDurationMs(0L);
            return executionRepository.save(execution);
        }

        try {
            // Validate configuration
            PluginValidationResult validationResult = processor.validateConfiguration(plugin);
            if (!validationResult.isValid()) {
                String errorMsg = "Plugin configuration is invalid: " +
                        String.join(", ", validationResult.getErrors());
                log.error(errorMsg);
                execution.setStatus(PluginExecutionStatus.FAILED);
                execution.setErrorMessage(errorMsg);
                execution.setRecordsProcessed(0);
                execution.setDurationMs(0L);
                return executionRepository.save(execution);
            }

            // Process data
            List<TelemetryPayload> payloads = processor.process(plugin, rawData);

            // Ingest telemetry data
            int successCount = 0;
            int failureCount = 0;
            StringBuilder errors = new StringBuilder();

            for (TelemetryPayload payload : payloads) {
                try {
                    telemetryIngestionService.ingest(payload);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    errors.append(String.format("Device %s: %s; ",
                            payload.deviceId(), e.getMessage()));
                    log.error("Failed to ingest telemetry from plugin {}: {}",
                            plugin.getName(), e.getMessage());
                }
            }

            // Determine execution status
            PluginExecutionStatus status;
            if (failureCount == 0) {
                status = PluginExecutionStatus.SUCCESS;
            } else if (successCount > 0) {
                status = PluginExecutionStatus.PARTIAL;
            } else {
                status = PluginExecutionStatus.FAILED;
            }

            execution.setStatus(status);
            execution.setRecordsProcessed(successCount);
            if (errors.length() > 0) {
                execution.setErrorMessage(errors.toString());
            }

            log.info("Plugin {} executed: {} records processed, {} failed",
                    plugin.getName(), successCount, failureCount);

        } catch (PluginProcessingException e) {
            log.error("Plugin {} processing failed: {}", plugin.getName(), e.getMessage(), e);
            execution.setStatus(PluginExecutionStatus.FAILED);
            execution.setErrorMessage(e.getMessage());
            execution.setRecordsProcessed(0);
        } catch (Exception e) {
            log.error("Unexpected error executing plugin {}: {}", plugin.getName(), e.getMessage(), e);
            execution.setStatus(PluginExecutionStatus.FAILED);
            execution.setErrorMessage("Unexpected error: " + e.getMessage());
            execution.setRecordsProcessed(0);
        }

        Instant endTime = Instant.now();
        execution.setDurationMs(endTime.toEpochMilli() - startTime.toEpochMilli());

        return executionRepository.save(execution);
    }

    /**
     * Validate plugin configuration
     *
     * @param plugin Plugin to validate
     * @return Validation result
     */
    public PluginValidationResult validatePlugin(DataPlugin plugin) {
        DataPluginProcessor processor = processors.get(plugin.getProvider());
        if (processor == null) {
            return PluginValidationResult.invalid("No processor registered for provider: " + plugin.getProvider());
        }

        return processor.validateConfiguration(plugin);
    }

    /**
     * Get all available plugin providers
     *
     * @return Map of provider to processor
     */
    public Map<PluginProvider, DataPluginProcessor> getAvailableProcessors() {
        return new HashMap<>(processors);
    }
}
