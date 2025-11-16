package org.sensorvision.service;

import org.sensorvision.model.*;
import org.sensorvision.repository.InstalledPluginRepository;
import org.sensorvision.repository.PluginRatingRepository;
import org.sensorvision.repository.PluginRegistryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing plugin registry (marketplace catalog)
 */
@Service
public class PluginRegistryService {

    private static final Logger logger = LoggerFactory.getLogger(PluginRegistryService.class);

    private final PluginRegistryRepository pluginRegistryRepository;
    private final InstalledPluginRepository installedPluginRepository;
    private final PluginRatingRepository pluginRatingRepository;

    public PluginRegistryService(PluginRegistryRepository pluginRegistryRepository,
                                InstalledPluginRepository installedPluginRepository,
                                PluginRatingRepository pluginRatingRepository) {
        this.pluginRegistryRepository = pluginRegistryRepository;
        this.installedPluginRepository = installedPluginRepository;
        this.pluginRatingRepository = pluginRatingRepository;
    }

    /**
     * Get all available plugins in the marketplace
     */
    public List<PluginRegistry> getAllPlugins() {
        return pluginRegistryRepository.findAll();
    }

    /**
     * Get plugin by key
     */
    public Optional<PluginRegistry> getPluginByKey(String pluginKey) {
        return pluginRegistryRepository.findByPluginKey(pluginKey);
    }

    /**
     * Search plugins by query string
     */
    public List<PluginRegistry> searchPlugins(String query) {
        if (query == null || query.trim().isEmpty()) {
            return getAllPlugins();
        }
        // Sanitize input to escape LIKE special characters
        String sanitized = sanitizeLikeQuery(query.trim());
        return pluginRegistryRepository.searchPlugins(sanitized);
    }

    /**
     * Get plugins by category
     */
    public List<PluginRegistry> getPluginsByCategory(PluginCategory category) {
        return pluginRegistryRepository.findByCategory(category);
    }

    /**
     * Search plugins by category and query
     */
    public List<PluginRegistry> searchPluginsByCategory(PluginCategory category, String query) {
        if (query == null || query.trim().isEmpty()) {
            return getPluginsByCategory(category);
        }
        // Sanitize input to escape LIKE special characters
        String sanitized = sanitizeLikeQuery(query.trim());
        return pluginRegistryRepository.searchPluginsByCategory(category, sanitized);
    }

    /**
     * Get official and verified plugins
     */
    public List<PluginRegistry> getOfficialAndVerifiedPlugins() {
        return pluginRegistryRepository.findOfficialAndVerified();
    }

    /**
     * Get most popular plugins (by installation count)
     */
    public List<PluginRegistry> getMostPopularPlugins() {
        return pluginRegistryRepository.findMostPopular();
    }

    /**
     * Get top rated plugins
     */
    public List<PluginRegistry> getTopRatedPlugins() {
        return pluginRegistryRepository.findTopRated();
    }

    /**
     * Get recently published plugins
     */
    public List<PluginRegistry> getRecentPlugins() {
        return pluginRegistryRepository.findRecent();
    }

    /**
     * Register a new plugin in the marketplace
     */
    @Transactional
    public PluginRegistry registerPlugin(PluginRegistry plugin) {
        logger.info("Registering new plugin: {}", plugin.getPluginKey());
        return pluginRegistryRepository.save(plugin);
    }

    /**
     * Update plugin metadata
     */
    @Transactional
    public PluginRegistry updatePlugin(String pluginKey, PluginRegistry updatedPlugin) {
        PluginRegistry existing = pluginRegistryRepository.findByPluginKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        existing.setName(updatedPlugin.getName());
        existing.setDescription(updatedPlugin.getDescription());
        existing.setVersion(updatedPlugin.getVersion());
        existing.setAuthor(updatedPlugin.getAuthor());
        existing.setAuthorUrl(updatedPlugin.getAuthorUrl());
        existing.setIconUrl(updatedPlugin.getIconUrl());
        existing.setRepositoryUrl(updatedPlugin.getRepositoryUrl());
        existing.setDocumentationUrl(updatedPlugin.getDocumentationUrl());
        existing.setConfigSchema(updatedPlugin.getConfigSchema());
        existing.setTags(updatedPlugin.getTags());
        existing.setScreenshots(updatedPlugin.getScreenshots());
        existing.setChangelog(updatedPlugin.getChangelog());

        logger.info("Updated plugin: {}", pluginKey);
        return pluginRegistryRepository.save(existing);
    }

    /**
     * Rate a plugin
     */
    @Transactional
    public PluginRating ratePlugin(String pluginKey, Organization organization, Integer rating, String reviewText) {
        if (rating < 1 || rating > 5) {
            throw new IllegalArgumentException("Rating must be between 1 and 5");
        }

        PluginRegistry plugin = pluginRegistryRepository.findByPluginKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        // Check if user already rated this plugin
        Optional<PluginRating> existingRating = pluginRatingRepository
                .findByPluginRegistryAndOrganization(plugin, organization);

        PluginRating pluginRating;
        if (existingRating.isPresent()) {
            // Update existing rating
            pluginRating = existingRating.get();
            pluginRating.setRating(rating);
            pluginRating.setReviewText(reviewText);
        } else {
            // Create new rating
            pluginRating = new PluginRating(plugin, organization, rating);
            pluginRating.setReviewText(reviewText);
        }

        pluginRating = pluginRatingRepository.save(pluginRating);

        // Update plugin's average rating
        updatePluginRatingStats(plugin);

        logger.info("Plugin {} rated {} stars by organization {}", pluginKey, rating, organization.getId());
        return pluginRating;
    }

    /**
     * Update plugin's rating statistics
     */
    @Transactional
    public void updatePluginRatingStats(PluginRegistry plugin) {
        BigDecimal avgRating = pluginRatingRepository.calculateAverageRating(plugin);
        Long ratingCount = pluginRatingRepository.countRatings(plugin);

        plugin.setRatingAverage(avgRating != null ? avgRating.setScale(2, RoundingMode.HALF_UP) : null);
        plugin.setRatingCount(ratingCount.intValue());

        pluginRegistryRepository.save(plugin);
    }

    /**
     * Increment installation count for a plugin (atomic operation)
     */
    @Transactional
    public void incrementInstallationCount(String pluginKey) {
        // Use atomic update query to avoid race conditions
        pluginRegistryRepository.updateInstallationCount(pluginKey);
        logger.debug("Updated installation count for plugin {}", pluginKey);
    }

    /**
     * Get plugin ratings
     */
    public List<PluginRating> getPluginRatings(String pluginKey) {
        PluginRegistry plugin = pluginRegistryRepository.findByPluginKey(pluginKey)
                .orElseThrow(() -> new IllegalArgumentException("Plugin not found: " + pluginKey));

        return pluginRatingRepository.findByPluginRegistry(plugin);
    }

    /**
     * Check if plugin exists
     */
    public boolean pluginExists(String pluginKey) {
        return pluginRegistryRepository.findByPluginKey(pluginKey).isPresent();
    }

    /**
     * Sanitize LIKE query to escape special characters
     */
    private String sanitizeLikeQuery(String query) {
        if (query == null) {
            return null;
        }
        // Escape LIKE special characters: % and _
        return query.replace("\\", "\\\\")
                    .replace("%", "\\%")
                    .replace("_", "\\_");
    }
}
