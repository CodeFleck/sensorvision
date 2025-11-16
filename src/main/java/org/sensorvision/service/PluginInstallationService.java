package org.sensorvision.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.sensorvision.model.*;
import org.sensorvision.repository.DataPluginRepository;
import org.sensorvision.repository.InstalledPluginRepository;
import org.sensorvision.repository.PluginRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing plugin installations (one-click install/uninstall)
 */
@Service
public class PluginInstallationService {

    private static final Logger logger = LoggerFactory.getLogger(PluginInstallationService.class);

    private final InstalledPluginRepository installedPluginRepository;
    private final PluginRegistryRepository pluginRegistryRepository;
    private final DataPluginRepository dataPluginRepository;
    private final PluginRegistryService pluginRegistryService;

    public PluginInstallationService(InstalledPluginRepository installedPluginRepository,
                                    PluginRegistryRepository pluginRegistryRepository,
                                    DataPluginRepository dataPluginRepository,
                                    PluginRegistryService pluginRegistryService) {
        this.installedPluginRepository = installedPluginRepository;
        this.pluginRegistryRepository = pluginRegistryRepository;
        this.dataPluginRepository = dataPluginRepository;
        this.pluginRegistryService = pluginRegistryService;
    }

    /**
     * Install a plugin for an organization
     */
    @Transactional
    public InstalledPlugin installPlugin(String pluginKey, Organization organization, JsonNode configuration) {
        logger.info("Installing plugin {} for organization {}", pluginKey, organization.getId());

        // Check if already installed
        if (installedPluginRepository.existsByOrganizationAndPluginKey(organization, pluginKey)) {
            throw new IllegalStateException("Plugin already installed: " + pluginKey);
        }

        // Get plugin from registry
        PluginRegistry plugin = pluginRegistryRepository.findByPluginKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found in registry: " + pluginKey));

        // Create installed plugin record
        InstalledPlugin installedPlugin = new InstalledPlugin(
                organization,
                plugin,
                pluginKey,
                plugin.getVersion()
        );
        installedPlugin.setConfiguration(configuration);
        installedPlugin.setStatus(PluginInstallationStatus.INACTIVE);
        installedPlugin.setInstalledAt(Instant.now());

        installedPlugin = installedPluginRepository.save(installedPlugin);

        // Update installation count
        pluginRegistryService.incrementInstallationCount(pluginKey);

        logger.info("Plugin {} installed successfully for organization {}", pluginKey, organization.getId());
        return installedPlugin;
    }

    /**
     * Activate an installed plugin
     */
    @Transactional
    public InstalledPlugin activatePlugin(String pluginKey, Organization organization) {
        logger.info("Activating plugin {} for organization {}", pluginKey, organization.getId());

        InstalledPlugin installedPlugin = installedPluginRepository
                .findByOrganizationAndPluginKey(organization, pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not installed: " + pluginKey));

        if (installedPlugin.getStatus() == PluginInstallationStatus.ACTIVE) {
            logger.info("Plugin {} is already active", pluginKey);
            return installedPlugin;
        }

        // Create or update DataPlugin for active use
        DataPlugin dataPlugin = createOrUpdateDataPlugin(installedPlugin);
        installedPlugin.setDataPlugin(dataPlugin);

        installedPlugin.setStatus(PluginInstallationStatus.ACTIVE);
        installedPlugin.setActivatedAt(Instant.now());
        installedPlugin.setLastUsedAt(Instant.now());

        installedPlugin = installedPluginRepository.save(installedPlugin);

        logger.info("Plugin {} activated successfully for organization {}", pluginKey, organization.getId());
        return installedPlugin;
    }

    /**
     * Deactivate an installed plugin
     */
    @Transactional
    public InstalledPlugin deactivatePlugin(String pluginKey, Organization organization) {
        logger.info("Deactivating plugin {} for organization {}", pluginKey, organization.getId());

        InstalledPlugin installedPlugin = installedPluginRepository
                .findByOrganizationAndPluginKey(organization, pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not installed: " + pluginKey));

        if (installedPlugin.getStatus() == PluginInstallationStatus.INACTIVE) {
            logger.info("Plugin {} is already inactive", pluginKey);
            return installedPlugin;
        }

        // Disable associated DataPlugin
        if (installedPlugin.getDataPlugin() != null) {
            DataPlugin dataPlugin = installedPlugin.getDataPlugin();
            dataPlugin.setEnabled(false);
            dataPluginRepository.save(dataPlugin);
        }

        installedPlugin.setStatus(PluginInstallationStatus.INACTIVE);
        installedPlugin = installedPluginRepository.save(installedPlugin);

        logger.info("Plugin {} deactivated successfully for organization {}", pluginKey, organization.getId());
        return installedPlugin;
    }

    /**
     * Uninstall a plugin
     */
    @Transactional
    public void uninstallPlugin(String pluginKey, Organization organization) {
        logger.info("Uninstalling plugin {} for organization {}", pluginKey, organization.getId());

        InstalledPlugin installedPlugin = installedPluginRepository
                .findByOrganizationAndPluginKey(organization, pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not installed: " + pluginKey));

        // Validate that plugin is not currently active
        if (installedPlugin.getStatus() == PluginInstallationStatus.ACTIVE) {
            throw new IllegalStateException("Cannot uninstall active plugin. Please deactivate it first: " + pluginKey);
        }

        // Delete associated DataPlugin if exists and validate it's not enabled
        if (installedPlugin.getDataPlugin() != null) {
            DataPlugin dataPlugin = installedPlugin.getDataPlugin();
            if (dataPlugin.getEnabled()) {
                throw new IllegalStateException("Cannot uninstall plugin with active DataPlugin. Deactivate first: " + pluginKey);
            }
            dataPluginRepository.delete(dataPlugin);
        }

        // Delete installed plugin record
        installedPluginRepository.delete(installedPlugin);

        // Update installation count
        pluginRegistryService.incrementInstallationCount(pluginKey);

        logger.info("Plugin {} uninstalled successfully for organization {}", pluginKey, organization.getId());
    }

    /**
     * Update plugin configuration
     */
    @Transactional
    public InstalledPlugin updatePluginConfiguration(String pluginKey, Organization organization, JsonNode configuration) {
        logger.info("Updating configuration for plugin {} for organization {}", pluginKey, organization.getId());

        InstalledPlugin installedPlugin = installedPluginRepository
                .findByOrganizationAndPluginKey(organization, pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not installed: " + pluginKey));

        installedPlugin.setConfiguration(configuration);

        // Update associated DataPlugin configuration if active
        if (installedPlugin.getDataPlugin() != null) {
            DataPlugin dataPlugin = installedPlugin.getDataPlugin();
            dataPlugin.setConfiguration(configuration);
            dataPluginRepository.save(dataPlugin);
        }

        installedPlugin = installedPluginRepository.save(installedPlugin);

        logger.info("Configuration updated for plugin {} for organization {}", pluginKey, organization.getId());
        return installedPlugin;
    }

    /**
     * Get all installed plugins for an organization
     */
    public List<InstalledPlugin> getInstalledPlugins(Organization organization) {
        return installedPluginRepository.findByOrganization(organization);
    }

    /**
     * Get active plugins for an organization
     */
    public List<InstalledPlugin> getActivePlugins(Organization organization) {
        return installedPluginRepository.findActivePluginsByOrganization(organization);
    }

    /**
     * Get installed plugin by key
     */
    public Optional<InstalledPlugin> getInstalledPlugin(String pluginKey, Organization organization) {
        return installedPluginRepository.findByOrganizationAndPluginKey(organization, pluginKey);
    }

    /**
     * Check if plugin is installed
     */
    public boolean isPluginInstalled(String pluginKey, Organization organization) {
        return installedPluginRepository.existsByOrganizationAndPluginKey(organization, pluginKey);
    }

    /**
     * Create or update DataPlugin for an installed plugin
     */
    private DataPlugin createOrUpdateDataPlugin(InstalledPlugin installedPlugin) {
        PluginRegistry pluginRegistry = installedPlugin.getPluginRegistry();

        DataPlugin dataPlugin;
        if (installedPlugin.getDataPlugin() != null) {
            dataPlugin = installedPlugin.getDataPlugin();
        } else {
            dataPlugin = new DataPlugin();
            dataPlugin.setOrganization(installedPlugin.getOrganization());
        }

        dataPlugin.setName(pluginRegistry.getName());
        dataPlugin.setDescription(pluginRegistry.getDescription());
        dataPlugin.setPluginType(pluginRegistry.getPluginType());
        dataPlugin.setProvider(pluginRegistry.getPluginProvider());
        dataPlugin.setConfiguration(installedPlugin.getConfiguration());
        dataPlugin.setEnabled(true);

        return dataPluginRepository.save(dataPlugin);
    }
}
