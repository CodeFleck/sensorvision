package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Tracks installed plugins for each organization
 */
@Entity
@Table(name = "installed_plugins")
public class InstalledPlugin extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "plugin_registry_id", nullable = false)
    private PluginRegistry pluginRegistry;

    @Column(name = "plugin_key", nullable = false, length = 100)
    private String pluginKey;

    @Column(name = "installed_version", nullable = false, length = 20)
    private String installedVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PluginInstallationStatus status = PluginInstallationStatus.INACTIVE;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private JsonNode configuration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "data_plugin_id")
    private DataPlugin dataPlugin;

    @Column(name = "installed_at")
    private Instant installedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    // Constructors
    public InstalledPlugin() {
    }

    public InstalledPlugin(Organization organization, PluginRegistry pluginRegistry,
                          String pluginKey, String installedVersion) {
        this.organization = organization;
        this.pluginRegistry = pluginRegistry;
        this.pluginKey = pluginKey;
        this.installedVersion = installedVersion;
        this.installedAt = Instant.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public PluginRegistry getPluginRegistry() {
        return pluginRegistry;
    }

    public void setPluginRegistry(PluginRegistry pluginRegistry) {
        this.pluginRegistry = pluginRegistry;
    }

    public String getPluginKey() {
        return pluginKey;
    }

    public void setPluginKey(String pluginKey) {
        this.pluginKey = pluginKey;
    }

    public String getInstalledVersion() {
        return installedVersion;
    }

    public void setInstalledVersion(String installedVersion) {
        this.installedVersion = installedVersion;
    }

    public PluginInstallationStatus getStatus() {
        return status;
    }

    public void setStatus(PluginInstallationStatus status) {
        this.status = status;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public DataPlugin getDataPlugin() {
        return dataPlugin;
    }

    public void setDataPlugin(DataPlugin dataPlugin) {
        this.dataPlugin = dataPlugin;
    }

    public Instant getInstalledAt() {
        return installedAt;
    }

    public void setInstalledAt(Instant installedAt) {
        this.installedAt = installedAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void setActivatedAt(Instant activatedAt) {
        this.activatedAt = activatedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }
}
