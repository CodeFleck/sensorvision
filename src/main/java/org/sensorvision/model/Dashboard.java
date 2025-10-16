package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dashboards")
public class Dashboard extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_default")
    private Boolean isDefault = false;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "layout_config", columnDefinition = "jsonb")
    private JsonNode layoutConfig;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Widget> widgets = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "public_share_token", unique = true)
    private String publicShareToken;

    @OneToMany(mappedBy = "dashboard", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DashboardPermission> permissions = new ArrayList<>();

    // Constructors
    public Dashboard() {
    }

    public Dashboard(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Boolean getIsDefault() {
        return isDefault;
    }

    public void setIsDefault(Boolean isDefault) {
        this.isDefault = isDefault;
    }

    public JsonNode getLayoutConfig() {
        return layoutConfig;
    }

    public void setLayoutConfig(JsonNode layoutConfig) {
        this.layoutConfig = layoutConfig;
    }

    public List<Widget> getWidgets() {
        return widgets;
    }

    public void setWidgets(List<Widget> widgets) {
        this.widgets = widgets;
    }

    public Organization getOrganization() {
        return organization;
    }

    public void setOrganization(Organization organization) {
        this.organization = organization;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getPublicShareToken() {
        return publicShareToken;
    }

    public void setPublicShareToken(String publicShareToken) {
        this.publicShareToken = publicShareToken;
    }

    public List<DashboardPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(List<DashboardPermission> permissions) {
        this.permissions = permissions;
    }

    // Helper methods
    public void addWidget(Widget widget) {
        widgets.add(widget);
        widget.setDashboard(this);
    }

    public void removeWidget(Widget widget) {
        widgets.remove(widget);
        widget.setDashboard(null);
    }
}
