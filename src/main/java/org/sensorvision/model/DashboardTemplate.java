package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Dashboard template for quick dashboard creation
 * Templates are pre-configured dashboards with widgets that users can instantiate
 */
@Entity
@Table(name = "dashboard_templates")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(of = "id")
public class DashboardTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private DashboardTemplateCategory category;

    @Column(length = 50)
    private String icon;

    @Column(name = "preview_image_url", length = 255)
    private String previewImageUrl;

    /**
     * Dashboard configuration (metadata like name, description, layout)
     * Example: {"name": "Smart Meter Dashboard", "description": "...", "layoutConfig": {...}}
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "dashboard_config", nullable = false)
    private JsonNode dashboardConfig;

    /**
     * Widget configurations array
     * Example: [{"name": "Power Consumption", "type": "LINE_CHART", "deviceId": null, ...}, ...]
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "widgets_config", nullable = false)
    private JsonNode widgetsConfig;

    /**
     * System templates are built-in and cannot be deleted
     */
    @Column(name = "is_system", nullable = false)
    @Builder.Default
    private Boolean isSystem = false;

    /**
     * Track how many times this template has been used
     */
    @Column(name = "usage_count", nullable = false)
    @Builder.Default
    private Integer usageCount = 0;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    /**
     * Increment usage count
     */
    public void incrementUsageCount() {
        this.usageCount = (this.usageCount == null ? 0 : this.usageCount) + 1;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
