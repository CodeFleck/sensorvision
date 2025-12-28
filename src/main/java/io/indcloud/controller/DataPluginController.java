package io.indcloud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.CreateDataPluginRequest;
import io.indcloud.dto.DataPluginDto;
import io.indcloud.dto.PluginExecutionDto;
import io.indcloud.model.*;
import io.indcloud.repository.DataPluginRepository;
import io.indcloud.repository.PluginExecutionRepository;
import io.indcloud.security.UserPrincipal;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DataPluginService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/data-plugins")
@RequiredArgsConstructor
public class DataPluginController {

    private final DataPluginRepository pluginRepository;
    private final PluginExecutionRepository executionRepository;
    private final DataPluginService pluginService;
    private final SecurityUtils securityUtils;

    /**
     * Get all plugins for the authenticated user's organization
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<DataPluginDto>> getPlugins(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<DataPlugin> plugins = pluginRepository.findByOrganizationId(org.getId(), pageable);

        Page<DataPluginDto> dtos = plugins.map(this::toDto);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a single plugin by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<DataPluginDto> getPlugin(@PathVariable Long id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataPlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));

        // Verify ownership
        if (!plugin.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(toDto(plugin));
    }

    /**
     * Create a new plugin
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<DataPluginDto> createPlugin(
            @Valid @RequestBody CreateDataPluginRequest request
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();
        User currentUser = securityUtils.getCurrentUser();

        // Check for duplicate name
        if (pluginRepository.existsByOrganizationIdAndName(org.getId(), request.name())) {
            return ResponseEntity.badRequest().build();
        }

        DataPlugin plugin = new DataPlugin();
        plugin.setOrganization(org);
        plugin.setName(request.name());
        plugin.setDescription(request.description());
        plugin.setPluginType(request.pluginType());
        plugin.setProvider(request.provider());
        plugin.setEnabled(request.enabled() != null ? request.enabled() : true);
        plugin.setConfiguration(request.configuration());
        plugin.setCreatedBy(currentUser);

        // Validate configuration
        var validationResult = pluginService.validatePlugin(plugin);
        if (!validationResult.isValid()) {
            log.error("Plugin validation failed: {}", validationResult.getErrors());
            return ResponseEntity.badRequest().build();
        }

        plugin = pluginRepository.save(plugin);
        log.info("Created plugin: {} (ID: {})", plugin.getName(), plugin.getId());

        return ResponseEntity.ok(toDto(plugin));
    }

    /**
     * Update an existing plugin
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<DataPluginDto> updatePlugin(
            @PathVariable Long id,
            @Valid @RequestBody CreateDataPluginRequest request
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataPlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));

        // Verify ownership
        if (!plugin.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        // Check for duplicate name (excluding current plugin)
        DataPlugin existingWithName = pluginRepository.findByOrganizationIdAndName(org.getId(), request.name())
                .orElse(null);
        if (existingWithName != null && !existingWithName.getId().equals(id)) {
            return ResponseEntity.badRequest().build();
        }

        plugin.setName(request.name());
        plugin.setDescription(request.description());
        plugin.setPluginType(request.pluginType());
        plugin.setProvider(request.provider());
        plugin.setEnabled(request.enabled() != null ? request.enabled() : true);
        plugin.setConfiguration(request.configuration());

        // Validate configuration
        var validationResult = pluginService.validatePlugin(plugin);
        if (!validationResult.isValid()) {
            log.error("Plugin validation failed: {}", validationResult.getErrors());
            return ResponseEntity.badRequest().build();
        }

        plugin = pluginRepository.save(plugin);
        log.info("Updated plugin: {} (ID: {})", plugin.getName(), plugin.getId());

        return ResponseEntity.ok(toDto(plugin));
    }

    /**
     * Delete a plugin
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Transactional
    public ResponseEntity<Void> deletePlugin(@PathVariable Long id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataPlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));

        // Verify ownership
        if (!plugin.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        pluginRepository.delete(plugin);
        log.info("Deleted plugin: {} (ID: {})", plugin.getName(), plugin.getId());

        return ResponseEntity.noContent().build();
    }

    /**
     * Get execution history for a plugin
     */
    @GetMapping("/{id}/executions")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<PluginExecutionDto>> getExecutions(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        DataPlugin plugin = pluginRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found"));

        // Verify ownership
        if (!plugin.getOrganization().getId().equals(org.getId())) {
            return ResponseEntity.notFound().build();
        }

        Pageable pageable = PageRequest.of(page, size);
        Page<PluginExecution> executions = executionRepository.findByPluginIdOrderByExecutedAtDesc(id, pageable);

        // Map inside transaction to avoid LazyInitializationException
        Page<PluginExecutionDto> dtos = executions.map(execution -> new PluginExecutionDto(
                execution.getId(),
                execution.getPlugin().getId(),
                execution.getPlugin().getName(),
                execution.getExecutedAt(),
                execution.getStatus(),
                execution.getRecordsProcessed(),
                execution.getErrorMessage(),
                execution.getDurationMs()
        ));
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get available plugin providers
     */
    @GetMapping("/providers")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> getProviders() {
        Map<String, String> providers = new HashMap<>();
        providers.put(PluginProvider.HTTP_WEBHOOK.name(), PluginProvider.HTTP_WEBHOOK.getDisplayName());
        providers.put(PluginProvider.LORAWAN_TTN.name(), PluginProvider.LORAWAN_TTN.getDisplayName());
        providers.put(PluginProvider.MODBUS_TCP.name(), PluginProvider.MODBUS_TCP.getDisplayName());
        providers.put(PluginProvider.CSV_FILE.name(), PluginProvider.CSV_FILE.getDisplayName());
        return ResponseEntity.ok(providers);
    }

    private DataPluginDto toDto(DataPlugin plugin) {
        return new DataPluginDto(
                plugin.getId(),
                plugin.getName(),
                plugin.getDescription(),
                plugin.getPluginType(),
                plugin.getProvider(),
                plugin.getEnabled(),
                plugin.getConfiguration(),
                plugin.getCreatedBy() != null ? plugin.getCreatedBy().getUsername() : null,
                plugin.getCreatedAt(),
                plugin.getUpdatedAt()
        );
    }

    private PluginExecutionDto toExecutionDto(PluginExecution execution) {
        // Access plugin fields eagerly within transaction to avoid LazyInitializationException
        Long pluginId = execution.getPlugin() != null ? execution.getPlugin().getId() : null;
        String pluginName = null;
        if (execution.getPlugin() != null) {
            try {
                pluginName = execution.getPlugin().getName();
            } catch (Exception e) {
                // Plugin data not available, use null
                pluginName = "Unknown";
            }
        }

        return new PluginExecutionDto(
                execution.getId(),
                pluginId,
                pluginName,
                execution.getExecutedAt(),
                execution.getStatus(),
                execution.getRecordsProcessed(),
                execution.getErrorMessage(),
                execution.getDurationMs()
        );
    }
}
