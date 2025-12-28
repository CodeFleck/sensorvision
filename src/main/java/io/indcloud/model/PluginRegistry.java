package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * Plugin Registry entity - stores metadata for available plugins in the marketplace
 */
@Entity
@Table(name = "plugin_registry")
public class PluginRegistry extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "plugin_key", nullable = false, unique = true, length = 100)
    private String pluginKey;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PluginCategory category;

    @Column(nullable = false, length = 20)
    private String version;

    @Column(length = 200)
    private String author;

    @Column(name = "author_url", length = 500)
    private String authorUrl;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "repository_url", length = 500)
    private String repositoryUrl;

    @Column(name = "documentation_url", length = 500)
    private String documentationUrl;

    @Column(name = "min_sensorvision_version", length = 20)
    private String minSensorvisionVersion;

    @Column(name = "max_sensorvision_version", length = 20)
    private String maxSensorvisionVersion;

    @Column(name = "is_official")
    private Boolean isOfficial = false;

    @Column(name = "is_verified")
    private Boolean isVerified = false;

    @Column(name = "installation_count")
    private Integer installationCount = 0;

    @Column(name = "rating_average", precision = 3, scale = 2)
    private BigDecimal ratingAverage;

    @Column(name = "rating_count")
    private Integer ratingCount = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "plugin_provider", nullable = false, length = 50)
    private PluginProvider pluginProvider;

    @Enumerated(EnumType.STRING)
    @Column(name = "plugin_type", nullable = false, length = 50)
    private PluginType pluginType;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config_schema")
    private JsonNode configSchema;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> tags = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> screenshots = new ArrayList<>();

    @Column(columnDefinition = "TEXT")
    private String changelog;

    @Column(name = "published_at")
    private Instant publishedAt;

    @OneToMany(mappedBy = "pluginRegistry", cascade = CascadeType.ALL)
    private List<PluginRating> ratings = new ArrayList<>();

    // Constructors
    public PluginRegistry() {
    }

    public PluginRegistry(String pluginKey, String name, PluginCategory category,
                         String version, PluginProvider pluginProvider, PluginType pluginType) {
        this.pluginKey = pluginKey;
        this.name = name;
        this.category = category;
        this.version = version;
        this.pluginProvider = pluginProvider;
        this.pluginType = pluginType;
    }

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

    public List<PluginRating> getRatings() {
        return ratings;
    }

    public void setRatings(List<PluginRating> ratings) {
        this.ratings = ratings;
    }
}
