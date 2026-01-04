package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder(toBuilder = true)
@Entity
@Table(name = "device_types")
public class DeviceType extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "icon", length = 50)
    private String icon;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @OneToMany(mappedBy = "deviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceTypeVariable> variables = new ArrayList<>();

    @OneToMany(mappedBy = "deviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceTypeRuleTemplate> ruleTemplates = new ArrayList<>();

    @OneToMany(mappedBy = "deviceType", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<DeviceTypeDashboardTemplate> dashboardTemplates = new ArrayList<>();

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "is_system_template", nullable = false)
    @Builder.Default
    private Boolean isSystemTemplate = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "template_category", length = 50)
    private TemplateCategory templateCategory;

    @Column(name = "color", length = 7)
    private String color;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dashboard_layout", columnDefinition = "jsonb")
    private JsonNode dashboardLayout;

    /**
     * Add a variable to this device type
     */
    public void addVariable(DeviceTypeVariable variable) {
        variables.add(variable);
        variable.setDeviceType(this);
    }

    /**
     * Remove a variable from this device type
     */
    public void removeVariable(DeviceTypeVariable variable) {
        variables.remove(variable);
        variable.setDeviceType(null);
    }

    /**
     * Add a rule template to this device type
     */
    public void addRuleTemplate(DeviceTypeRuleTemplate ruleTemplate) {
        ruleTemplates.add(ruleTemplate);
        ruleTemplate.setDeviceType(this);
    }

    /**
     * Remove a rule template from this device type
     */
    public void removeRuleTemplate(DeviceTypeRuleTemplate ruleTemplate) {
        ruleTemplates.remove(ruleTemplate);
        ruleTemplate.setDeviceType(null);
    }

    /**
     * Add a dashboard template to this device type
     */
    public void addDashboardTemplate(DeviceTypeDashboardTemplate dashboardTemplate) {
        dashboardTemplates.add(dashboardTemplate);
        dashboardTemplate.setDeviceType(this);
    }

    /**
     * Remove a dashboard template from this device type
     */
    public void removeDashboardTemplate(DeviceTypeDashboardTemplate dashboardTemplate) {
        dashboardTemplates.remove(dashboardTemplate);
        dashboardTemplate.setDeviceType(null);
    }

    /**
     * Check if this is a system template that cannot be modified
     */
    public boolean isEditable() {
        return !Boolean.TRUE.equals(isSystemTemplate);
    }

    public enum TemplateCategory {
        ENERGY,         // Smart meters, solar inverters, energy monitoring
        ENVIRONMENTAL,  // Temperature, humidity, air quality sensors
        INDUSTRIAL,     // Pumps, motors, PLCs, industrial equipment
        SMART_HOME,     // Home automation, thermostats, lighting
        CUSTOM          // User-defined custom templates
    }
}
