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

    @Column(name = "default_device_id")
    private String defaultDeviceId;

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

    public String getDefaultDeviceId() {
        return defaultDeviceId;
    }

    public void setDefaultDeviceId(String defaultDeviceId) {
        this.defaultDeviceId = defaultDeviceId;
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

    /**
     * IMPORTANT: With orphanRemoval=true, we must NEVER replace the collection instance.
     * This method clears the existing collection and adds all new widgets properly.
     */
    public void setWidgets(List<Widget> newWidgets) {
        if (newWidgets == null) {
            this.widgets.clear();
            return;
        }

        // Remove widgets that are no longer in the new list
        this.widgets.removeIf(existingWidget ->
            newWidgets.stream().noneMatch(w -> w.getId() != null && w.getId().equals(existingWidget.getId()))
        );

        // Add or update widgets
        for (Widget newWidget : newWidgets) {
            if (newWidget.getId() == null) {
                // New widget - add it
                addWidget(newWidget);
            } else {
                // Existing widget - check if it's already in the collection
                boolean exists = this.widgets.stream()
                    .anyMatch(w -> w.getId().equals(newWidget.getId()));
                if (!exists) {
                    addWidget(newWidget);
                }
            }
        }
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

    /**
     * IMPORTANT: With orphanRemoval=true, we must NEVER replace the collection instance.
     * This method clears the existing collection and adds all new permissions properly.
     */
    public void setPermissions(List<DashboardPermission> newPermissions) {
        if (newPermissions == null) {
            this.permissions.clear();
            return;
        }

        // Remove permissions that are no longer in the new list
        this.permissions.removeIf(existingPerm ->
            newPermissions.stream().noneMatch(p -> p.getId() != null && p.getId().equals(existingPerm.getId()))
        );

        // Add new permissions
        for (DashboardPermission newPerm : newPermissions) {
            if (newPerm.getId() == null || this.permissions.stream().noneMatch(p -> p.getId().equals(newPerm.getId()))) {
                this.permissions.add(newPerm);
                newPerm.setDashboard(this);
            }
        }
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
