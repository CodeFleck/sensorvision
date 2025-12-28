package io.indcloud.dto;

import com.fasterxml.jackson.databind.JsonNode;
import io.indcloud.model.PluginCategory;
import io.indcloud.model.PluginProvider;
import io.indcloud.model.PluginType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * DTO for plugin registry information
 */
public class PluginRegistryDto {
    private Long id;
    private String pluginKey;
    private String name;
    private String description;
    private PluginCategory category;
    private String version;
    private String author;
    private String authorUrl;
    private String iconUrl;
    private String repositoryUrl;
    private String documentationUrl;
    private String minSensorvisionVersion;
    private String maxSensorvisionVersion;
    private Boolean isOfficial;
    private Boolean isVerified;
    private Integer installationCount;
    private BigDecimal ratingAverage;
    private Integer ratingCount;
    private PluginProvider pluginProvider;
    private PluginType pluginType;
    private JsonNode configSchema;
    private List<String> tags;
    private List<String> screenshots;
    private String changelog;
    private Instant publishedAt;
    private Boolean isInstalled;
    private Boolean isActive;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getPluginKey() {
        return pluginKey;
    }

    public void setPluginKey(String pluginKey) {
        this.pluginKey = pluginKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PluginCategory getCategory() {
        return category;
    }

    public void setCategory(PluginCategory category) {
        this.category = category;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getAuthorUrl() {
        return authorUrl;
    }

    public void setAuthorUrl(String authorUrl) {
        this.authorUrl = authorUrl;
    }

    public String getIconUrl() {
        return iconUrl;
    }

    public void setIconUrl(String iconUrl) {
        this.iconUrl = iconUrl;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void setRepositoryUrl(String repositoryUrl) {
        this.repositoryUrl = repositoryUrl;
    }

    public String getDocumentationUrl() {
        return documentationUrl;
    }

    public void setDocumentationUrl(String documentationUrl) {
        this.documentationUrl = documentationUrl;
    }

    public String getMinSensorvisionVersion() {
        return minSensorvisionVersion;
    }

    public void setMinSensorvisionVersion(String minSensorvisionVersion) {
        this.minSensorvisionVersion = minSensorvisionVersion;
    }

    public String getMaxSensorvisionVersion() {
        return maxSensorvisionVersion;
    }

    public void setMaxSensorvisionVersion(String maxSensorvisionVersion) {
        this.maxSensorvisionVersion = maxSensorvisionVersion;
    }

    public Boolean getIsOfficial() {
        return isOfficial;
    }

    public void setIsOfficial(Boolean isOfficial) {
        this.isOfficial = isOfficial;
    }

    public Boolean getIsVerified() {
        return isVerified;
    }

    public void setIsVerified(Boolean isVerified) {
        this.isVerified = isVerified;
    }

    public Integer getInstallationCount() {
        return installationCount;
    }

    public void setInstallationCount(Integer installationCount) {
        this.installationCount = installationCount;
    }

    public BigDecimal getRatingAverage() {
        return ratingAverage;
    }

    public void setRatingAverage(BigDecimal ratingAverage) {
        this.ratingAverage = ratingAverage;
    }

    public Integer getRatingCount() {
        return ratingCount;
    }

    public void setRatingCount(Integer ratingCount) {
        this.ratingCount = ratingCount;
    }

    public PluginProvider getPluginProvider() {
        return pluginProvider;
    }

    public void setPluginProvider(PluginProvider pluginProvider) {
        this.pluginProvider = pluginProvider;
    }

    public PluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
        this.pluginType = pluginType;
    }

    public JsonNode getConfigSchema() {
        return configSchema;
    }

    public void setConfigSchema(JsonNode configSchema) {
        this.configSchema = configSchema;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public List<String> getScreenshots() {
        return screenshots;
    }

    public void setScreenshots(List<String> screenshots) {
        this.screenshots = screenshots;
    }

    public String getChangelog() {
        return changelog;
    }

    public void setChangelog(String changelog) {
        this.changelog = changelog;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }

    public Boolean getIsInstalled() {
        return isInstalled;
    }

    public void setIsInstalled(Boolean isInstalled) {
        this.isInstalled = isInstalled;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
