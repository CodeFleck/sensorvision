package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "data_plugins")
public class DataPlugin extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "plugin_type", nullable = false, length = 50)
    private PluginType pluginType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PluginProvider provider;

    @Column(nullable = false)
    private Boolean enabled = true;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false)
    private JsonNode configuration;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "plugin", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PluginExecution> executions = new ArrayList<>();

    // Constructors
    public DataPlugin() {
    }

    public DataPlugin(String name, PluginType pluginType, PluginProvider provider) {
        this.name = name;
        this.pluginType = pluginType;
        this.provider = provider;
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

    public PluginType getPluginType() {
        return pluginType;
    }

    public void setPluginType(PluginType pluginType) {
        this.pluginType = pluginType;
    }

    public PluginProvider getProvider() {
        return provider;
    }

    public void setProvider(PluginProvider provider) {
        this.provider = provider;
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public JsonNode getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JsonNode configuration) {
        this.configuration = configuration;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public List<PluginExecution> getExecutions() {
        return executions;
    }

    public void setExecutions(List<PluginExecution> executions) {
        this.executions = executions;
    }
}
