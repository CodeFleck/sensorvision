package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Template for dashboard widgets that will be automatically created when a device
 * is assigned to this device type.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "device_type_dashboard_templates")
public class DeviceTypeDashboardTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_type_id", nullable = false)
    private DeviceType deviceType;

    @Enumerated(EnumType.STRING)
    @Column(name = "widget_type", nullable = false, length = 50)
    private WidgetType widgetType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "variable_name", length = 100)
    private String variableName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "config", columnDefinition = "jsonb")
    private JsonNode config;

    @Column(name = "grid_x", nullable = false)
    @Builder.Default
    private Integer gridX = 0;

    @Column(name = "grid_y", nullable = false)
    @Builder.Default
    private Integer gridY = 0;

    @Column(name = "grid_width", nullable = false)
    @Builder.Default
    private Integer gridWidth = 4;

    @Column(name = "grid_height", nullable = false)
    @Builder.Default
    private Integer gridHeight = 2;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }

    public enum WidgetType {
        LINE_CHART,
        GAUGE,
        METRIC,
        TABLE,
        MAP,
        STATUS_INDICATOR,
        CONTROL_BUTTON,
        THERMOMETER,
        TANK,
        IMAGE,
        TEXT
    }
}
