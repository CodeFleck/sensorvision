package org.sensorvision.controller;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.dto.InstalledPluginDto;
import org.sensorvision.dto.PluginRegistryDto;
import org.sensorvision.model.*;
import org.sensorvision.service.PluginConfigurationService;
import org.sensorvision.service.PluginInstallationService;
import org.sensorvision.service.PluginRegistryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * REST API controller for plugin marketplace operations
 */
@RestController
@RequestMapping("/api/v1/plugins")
public class PluginMarketplaceController {

    private static final Logger logger = LoggerFactory.getLogger(PluginMarketplaceController.class);

    private final PluginRegistryService pluginRegistryService;
    private final PluginInstallationService pluginInstallationService;
    private final PluginConfigurationService pluginConfigurationService;

    public PluginMarketplaceController(PluginRegistryService pluginRegistryService,
                                      PluginInstallationService pluginInstallationService,
                                      PluginConfigurationService pluginConfigurationService) {
        this.pluginRegistryService = pluginRegistryService;
        this.pluginInstallationService = pluginInstallationService;
        this.pluginConfigurationService = pluginConfigurationService;
    }

    /**
     * GET /api/v1/plugins - List all available plugins in marketplace
     */
    @GetMapping
    public ResponseEntity<List<PluginRegistryDto>> getAllPlugins(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) PluginCategory category,
            @RequestParam(required = false) String sort,
            @AuthenticationPrincipal User currentUser) {

        List<PluginRegistry> plugins;

        // Apply search and category filters
        if (category != null && search != null) {
            plugins = pluginRegistryService.searchPluginsByCategory(category, search);
        } else if (category != null) {
            plugins = pluginRegistryService.getPluginsByCategory(category);
        } else if (search != null) {
            plugins = pluginRegistryService.searchPlugins(search);
        } else {
            // Apply sorting
            if ("popular".equals(sort)) {
                plugins = pluginRegistryService.getMostPopularPlugins();
            } else if ("rating".equals(sort)) {
                plugins = pluginRegistryService.getTopRatedPlugins();
            } else if ("recent".equals(sort)) {
                plugins = pluginRegistryService.getRecentPlugins();
            } else {
                plugins = pluginRegistryService.getAllPlugins();
            }
        }

        Organization organization = currentUser.getOrganization();
        List<PluginRegistryDto> dtos = plugins.stream()
                .map(plugin -> toDto(plugin, organization))
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * GET /api/v1/plugins/{pluginKey} - Get plugin details
     */
    @GetMapping("/{pluginKey}")
    public ResponseEntity<PluginRegistryDto> getPlugin(
            @PathVariable String pluginKey,
            @AuthenticationPrincipal User currentUser) {

        PluginRegistry plugin = pluginRegistryService.getPluginByKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        Organization organization = currentUser.getOrganization();
        PluginRegistryDto dto = toDto(plugin, organization);

        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/v1/plugins/{pluginKey}/install - Install a plugin
     */
    @PostMapping("/{pluginKey}/install")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<InstalledPluginDto> installPlugin(
            @PathVariable String pluginKey,
            @RequestBody(required = false) Map<String, Object> request,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();

        // Get configuration from request
        JsonNode configuration = null;
        if (request != null && request.containsKey("configuration")) {
            configuration = (JsonNode) request.get("configuration");
        }

        // Validate configuration if provided
        if (configuration != null) {
            PluginRegistry plugin = pluginRegistryService.getPluginByKey(pluginKey)
                    .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

            PluginConfigurationService.ConfigurationValidationResult validationResult =
                    pluginConfigurationService.validateConfiguration(plugin, configuration);

            if (!validationResult.isValid()) {
                return ResponseEntity.badRequest().body(null);
            }
        }

        InstalledPlugin installedPlugin = pluginInstallationService.installPlugin(
                pluginKey, organization, configuration);

        InstalledPluginDto dto = toInstalledDto(installedPlugin);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    /**
     * POST /api/v1/plugins/{pluginKey}/activate - Activate an installed plugin
     */
    @PostMapping("/{pluginKey}/activate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<InstalledPluginDto> activatePlugin(
            @PathVariable String pluginKey,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();
        InstalledPlugin installedPlugin = pluginInstallationService.activatePlugin(pluginKey, organization);

        InstalledPluginDto dto = toInstalledDto(installedPlugin);
        return ResponseEntity.ok(dto);
    }

    /**
     * POST /api/v1/plugins/{pluginKey}/deactivate - Deactivate an installed plugin
     */
    @PostMapping("/{pluginKey}/deactivate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<InstalledPluginDto> deactivatePlugin(
            @PathVariable String pluginKey,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();
        InstalledPlugin installedPlugin = pluginInstallationService.deactivatePlugin(pluginKey, organization);

        InstalledPluginDto dto = toInstalledDto(installedPlugin);
        return ResponseEntity.ok(dto);
    }

    /**
     * DELETE /api/v1/plugins/{pluginKey} - Uninstall a plugin
     */
    @DeleteMapping("/{pluginKey}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> uninstallPlugin(
            @PathVariable String pluginKey,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();
        pluginInstallationService.uninstallPlugin(pluginKey, organization);

        return ResponseEntity.noContent().build();
    }

    /**
     * PUT /api/v1/plugins/{pluginKey}/configuration - Update plugin configuration
     */
    @PutMapping("/{pluginKey}/configuration")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<InstalledPluginDto> updateConfiguration(
            @PathVariable String pluginKey,
            @RequestBody JsonNode configuration,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();

        // Validate configuration
        PluginRegistry plugin = pluginRegistryService.getPluginByKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        PluginConfigurationService.ConfigurationValidationResult validationResult =
                pluginConfigurationService.validateConfiguration(plugin, configuration);

        if (!validationResult.isValid()) {
            return ResponseEntity.badRequest().body(null);
        }

        InstalledPlugin installedPlugin = pluginInstallationService.updatePluginConfiguration(
                pluginKey, organization, configuration);

        InstalledPluginDto dto = toInstalledDto(installedPlugin);
        return ResponseEntity.ok(dto);
    }

    /**
     * GET /api/v1/plugins/installed - Get all installed plugins for current organization
     */
    @GetMapping("/installed")
    public ResponseEntity<List<InstalledPluginDto>> getInstalledPlugins(
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();
        List<InstalledPlugin> installedPlugins = pluginInstallationService.getInstalledPlugins(organization);

        List<InstalledPluginDto> dtos = installedPlugins.stream()
                .map(this::toInstalledDto)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    /**
     * POST /api/v1/plugins/{pluginKey}/rate - Rate a plugin
     */
    @PostMapping("/{pluginKey}/rate")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
    public ResponseEntity<Void> ratePlugin(
            @PathVariable String pluginKey,
            @RequestBody Map<String, Object> request,
            @AuthenticationPrincipal User currentUser) {

        Organization organization = currentUser.getOrganization();
        Integer rating = (Integer) request.get("rating");
        String reviewText = (String) request.get("reviewText");

        pluginRegistryService.ratePlugin(pluginKey, organization, rating, reviewText);

        return ResponseEntity.ok().build();
    }

    /**
     * GET /api/v1/plugins/{pluginKey}/default-config - Get default configuration for a plugin
     */
    @GetMapping("/{pluginKey}/default-config")
    public ResponseEntity<JsonNode> getDefaultConfiguration(@PathVariable String pluginKey) {
        PluginRegistry plugin = pluginRegistryService.getPluginByKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        JsonNode defaultConfig = pluginConfigurationService.getDefaultConfiguration(plugin);
        return ResponseEntity.ok(defaultConfig);
    }

    // Helper methods to convert entities to DTOs
    private PluginRegistryDto toDto(PluginRegistry plugin, Organization organization) {
        PluginRegistryDto dto = new PluginRegistryDto();
        dto.setId(plugin.getId());
        dto.setPluginKey(plugin.getPluginKey());
        dto.setName(plugin.getName());
        dto.setDescription(plugin.getDescription());
        dto.setCategory(plugin.getCategory());
        dto.setVersion(plugin.getVersion());
        dto.setAuthor(plugin.getAuthor());
        dto.setAuthorUrl(plugin.getAuthorUrl());
        dto.setIconUrl(plugin.getIconUrl());
        dto.setRepositoryUrl(plugin.getRepositoryUrl());
        dto.setDocumentationUrl(plugin.getDocumentationUrl());
        dto.setMinSensorvisionVersion(plugin.getMinSensorvisionVersion());
        dto.setMaxSensorvisionVersion(plugin.getMaxSensorvisionVersion());
        dto.setIsOfficial(plugin.getIsOfficial());
        dto.setIsVerified(plugin.getIsVerified());
        dto.setInstallationCount(plugin.getInstallationCount());
        dto.setRatingAverage(plugin.getRatingAverage());
        dto.setRatingCount(plugin.getRatingCount());
        dto.setPluginProvider(plugin.getPluginProvider());
        dto.setPluginType(plugin.getPluginType());
        dto.setConfigSchema(plugin.getConfigSchema());
        dto.setTags(plugin.getTags());
        dto.setScreenshots(plugin.getScreenshots());
        dto.setChangelog(plugin.getChangelog());
        dto.setPublishedAt(plugin.getPublishedAt());

        // Set installation status
        boolean isInstalled = pluginInstallationService.isPluginInstalled(plugin.getPluginKey(), organization);
        dto.setIsInstalled(isInstalled);

        if (isInstalled) {
            pluginInstallationService.getInstalledPlugin(plugin.getPluginKey(), organization)
                    .ifPresent(installed -> {
                        dto.setIsActive(installed.getStatus() == PluginInstallationStatus.ACTIVE);
                    });
        } else {
            dto.setIsActive(false);
        }

        return dto;
    }

    private InstalledPluginDto toInstalledDto(InstalledPlugin installedPlugin) {
        InstalledPluginDto dto = new InstalledPluginDto();
        dto.setId(installedPlugin.getId());
        dto.setPluginKey(installedPlugin.getPluginKey());
        dto.setName(installedPlugin.getPluginRegistry().getName());
        dto.setDescription(installedPlugin.getPluginRegistry().getDescription());
        dto.setInstalledVersion(installedPlugin.getInstalledVersion());
        dto.setLatestVersion(installedPlugin.getPluginRegistry().getVersion());
        dto.setStatus(installedPlugin.getStatus());
        dto.setConfiguration(installedPlugin.getConfiguration());
        dto.setDataPluginId(installedPlugin.getDataPlugin() != null ? installedPlugin.getDataPlugin().getId() : null);
        dto.setInstalledAt(installedPlugin.getInstalledAt());
        dto.setActivatedAt(installedPlugin.getActivatedAt());
        dto.setLastUsedAt(installedPlugin.getLastUsedAt());

        // Check if update is available
        boolean updateAvailable = !installedPlugin.getInstalledVersion()
                .equals(installedPlugin.getPluginRegistry().getVersion());
        dto.setUpdateAvailable(updateAvailable);

        return dto;
    }
}
