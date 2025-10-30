package org.sensorvision.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "widgets")
public class Widget extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WidgetType type;

    // Position and size in grid layout
    @Column(name = "position_x", nullable = false)
    private Integer positionX = 0;

    @Column(name = "position_y", nullable = false)
    private Integer positionY = 0;

    @Column(nullable = false)
    private Integer width = 4;

    @Column(nullable = false)
    private Integer height = 4;

    // Data source configuration
    @Column(name = "device_id")
    private String deviceId;

    @Column(name = "variable_name")
    private String variableName;

    // Dynamic dashboard support
    @Column(name = "use_context_device", nullable = false)
    private Boolean useContextDevice = false;

    @Column(name = "device_label")
    private String deviceLabel;

    @Enumerated(EnumType.STRING)
    @Column
    private WidgetAggregation aggregation = WidgetAggregation.NONE;

    @Column(name = "time_range_minutes")
    private Integer timeRangeMinutes;

    // Widget-specific configuration (thresholds, colors, limits, etc)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private JsonNode config;

    // Constructors
    public Widget() {
    }

    public Widget(String name, WidgetType type) {
        this.name = name;
        this.type = type;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public WidgetType getType() {
        return type;
    }

    public void setType(WidgetType type) {
        this.type = type;
    }

    public Integer getPositionX() {
        return positionX;
    }

    public void setPositionX(Integer positionX) {
        this.positionX = positionX;
    }

    public Integer getPositionY() {
        return positionY;
    }

    public void setPositionY(Integer positionY) {
        this.positionY = positionY;
    }

    public Integer getWidth() {
        return width;
    }

    public void setWidth(Integer width) {
        this.width = width;
    }

    public Integer getHeight() {
        return height;
    }

    public void setHeight(Integer height) {
        this.height = height;
    }

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getVariableName() {
        return variableName;
    }

    public void setVariableName(String variableName) {
        this.variableName = variableName;
    }

    public WidgetAggregation getAggregation() {
        return aggregation;
    }

    public void setAggregation(WidgetAggregation aggregation) {
        this.aggregation = aggregation;
    }

    public Integer getTimeRangeMinutes() {
        return timeRangeMinutes;
    }

    public void setTimeRangeMinutes(Integer timeRangeMinutes) {
        this.timeRangeMinutes = timeRangeMinutes;
    }

    public JsonNode getConfig() {
        return config;
    }

    public void setConfig(JsonNode config) {
        this.config = config;
    }

    public Boolean getUseContextDevice() {
        return useContextDevice;
    }

    public void setUseContextDevice(Boolean useContextDevice) {
        this.useContextDevice = useContextDevice;
    }

    public String getDeviceLabel() {
        return deviceLabel;
    }

    public void setDeviceLabel(String deviceLabel) {
        this.deviceLabel = deviceLabel;
    }
}
