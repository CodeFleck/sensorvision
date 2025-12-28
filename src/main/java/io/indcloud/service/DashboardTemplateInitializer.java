package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.DashboardTemplate;
import io.indcloud.model.DashboardTemplateCategory;
import io.indcloud.repository.DashboardTemplateRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Initialize system dashboard templates on application startup
 */
@Component
@Slf4j
public class DashboardTemplateInitializer {

    private final DashboardTemplateRepository templateRepository;
    private final ObjectMapper objectMapper;

    @Autowired
    public DashboardTemplateInitializer(DashboardTemplateRepository templateRepository,
                                       ObjectMapper objectMapper) {
        this.templateRepository = templateRepository;
        this.objectMapper = objectMapper;
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void initializeSystemTemplates() {
        log.info("Initializing system dashboard templates...");

        createSmartMeterTemplate();
        createEnvironmentalTemplate();
        createIndustrialTemplate();

        log.info("System dashboard templates initialized successfully");
    }

    /**
     * Smart Meter Dashboard Template
     */
    private void createSmartMeterTemplate() {
        if (templateRepository.findByName("Smart Meter Dashboard").isPresent()) {
            log.debug("Smart Meter template already exists, skipping");
            return;
        }

        // Dashboard config
        ObjectNode dashboardConfig = objectMapper.createObjectNode();
        dashboardConfig.put("name", "Smart Meter Dashboard");
        dashboardConfig.put("description", "Monitor electrical consumption with real-time power, voltage, and current metrics");

        // Widgets config
        ArrayNode widgetsConfig = objectMapper.createArrayNode();

        // Widget 1: Power Consumption Line Chart
        ObjectNode widget1 = objectMapper.createObjectNode();
        widget1.put("name", "Power Consumption");
        widget1.put("type", "LINE_CHART");
        widget1.put("positionX", 0);
        widget1.put("positionY", 0);
        widget1.put("width", 8);
        widget1.put("height", 6);
        widget1.putNull("deviceId");  // Will be bound during instantiation
        widget1.put("variableName", "kw_consumption");
        widget1.put("aggregation", "AVG");
        widget1.put("timeRangeMinutes", 60);
        ObjectNode config1 = objectMapper.createObjectNode();
        config1.put("unit", "kW");
        config1.put("color", "#4F46E5");
        widget1.set("config", config1);
        widgetsConfig.add(widget1);

        // Widget 2: Current Power Gauge
        ObjectNode widget2 = objectMapper.createObjectNode();
        widget2.put("name", "Current Power");
        widget2.put("type", "GAUGE");
        widget2.put("positionX", 8);
        widget2.put("positionY", 0);
        widget2.put("width", 4);
        widget2.put("height", 3);
        widget2.putNull("deviceId");
        widget2.put("variableName", "kw_consumption");
        widget2.put("aggregation", "NONE");
        widget2.putNull("timeRangeMinutes");
        ObjectNode config2 = objectMapper.createObjectNode();
        config2.put("min", 0);
        config2.put("max", 100);
        config2.put("unit", "kW");
        config2.put("dangerThreshold", 80);
        widget2.set("config", config2);
        widgetsConfig.add(widget2);

        // Widget 3: Voltage Metric
        ObjectNode widget3 = objectMapper.createObjectNode();
        widget3.put("name", "Voltage");
        widget3.put("type", "METRIC");
        widget3.put("positionX", 8);
        widget3.put("positionY", 3);
        widget3.put("width", 2);
        widget3.put("height", 3);
        widget3.putNull("deviceId");
        widget3.put("variableName", "voltage");
        widget3.put("aggregation", "NONE");
        widget3.putNull("timeRangeMinutes");
        ObjectNode config3 = objectMapper.createObjectNode();
        config3.put("unit", "V");
        config3.put("precision", 1);
        widget3.set("config", config3);
        widgetsConfig.add(widget3);

        // Widget 4: Current Metric
        ObjectNode widget4 = objectMapper.createObjectNode();
        widget4.put("name", "Current");
        widget4.put("type", "METRIC");
        widget4.put("positionX", 10);
        widget4.put("positionY", 3);
        widget4.put("width", 2);
        widget4.put("height", 3);
        widget4.putNull("deviceId");
        widget4.put("variableName", "current");
        widget4.put("aggregation", "NONE");
        widget4.putNull("timeRangeMinutes");
        ObjectNode config4 = objectMapper.createObjectNode();
        config4.put("unit", "A");
        config4.put("precision", 2);
        widget4.set("config", config4);
        widgetsConfig.add(widget4);

        // Widget 5: Daily Energy Bar Chart
        ObjectNode widget5 = objectMapper.createObjectNode();
        widget5.put("name", "Daily Energy Usage");
        widget5.put("type", "BAR_CHART");
        widget5.put("positionX", 0);
        widget5.put("positionY", 6);
        widget5.put("width", 12);
        widget5.put("height", 4);
        widget5.putNull("deviceId");
        widget5.put("variableName", "kw_consumption");
        widget5.put("aggregation", "SUM");
        widget5.put("timeRangeMinutes", 1440);  // 24 hours
        ObjectNode config5 = objectMapper.createObjectNode();
        config5.put("unit", "kWh");
        config5.put("interval", "hourly");
        widget5.set("config", config5);
        widgetsConfig.add(widget5);

        DashboardTemplate template = DashboardTemplate.builder()
            .name("Smart Meter Dashboard")
            .description("Complete electrical monitoring dashboard with power, voltage, current metrics and historical usage")
            .category(DashboardTemplateCategory.SMART_METER)
            .icon("⚡")
            .dashboardConfig(dashboardConfig)
            .widgetsConfig(widgetsConfig)
            .isSystem(true)
            .build();

        templateRepository.save(template);
        log.info("Created Smart Meter dashboard template");
    }

    /**
     * Environmental Monitoring Dashboard Template
     */
    private void createEnvironmentalTemplate() {
        if (templateRepository.findByName("Environmental Monitoring").isPresent()) {
            log.debug("Environmental template already exists, skipping");
            return;
        }

        ObjectNode dashboardConfig = objectMapper.createObjectNode();
        dashboardConfig.put("name", "Environmental Monitoring");
        dashboardConfig.put("description", "Monitor temperature, humidity, and air quality sensors");

        ArrayNode widgetsConfig = objectMapper.createArrayNode();

        // Temperature Chart
        ObjectNode tempWidget = objectMapper.createObjectNode();
        tempWidget.put("name", "Temperature Trend");
        tempWidget.put("type", "LINE_CHART");
        tempWidget.put("positionX", 0);
        tempWidget.put("positionY", 0);
        tempWidget.put("width", 6);
        tempWidget.put("height", 5);
        tempWidget.putNull("deviceId");
        tempWidget.put("variableName", "temperature");
        tempWidget.put("aggregation", "AVG");
        tempWidget.put("timeRangeMinutes", 120);
        ObjectNode tempConfig = objectMapper.createObjectNode();
        tempConfig.put("unit", "°C");
        tempConfig.put("color", "#EF4444");
        tempWidget.set("config", tempConfig);
        widgetsConfig.add(tempWidget);

        // Humidity Chart
        ObjectNode humidityWidget = objectMapper.createObjectNode();
        humidityWidget.put("name", "Humidity Trend");
        humidityWidget.put("type", "LINE_CHART");
        humidityWidget.put("positionX", 6);
        humidityWidget.put("positionY", 0);
        humidityWidget.put("width", 6);
        humidityWidget.put("height", 5);
        humidityWidget.putNull("deviceId");
        humidityWidget.put("variableName", "humidity");
        humidityWidget.put("aggregation", "AVG");
        humidityWidget.put("timeRangeMinutes", 120);
        ObjectNode humidityConfig = objectMapper.createObjectNode();
        humidityConfig.put("unit", "%");
        humidityConfig.put("color", "#3B82F6");
        humidityWidget.set("config", humidityConfig);
        widgetsConfig.add(humidityWidget);

        // Current Temperature Gauge
        ObjectNode tempGauge = objectMapper.createObjectNode();
        tempGauge.put("name", "Current Temperature");
        tempGauge.put("type", "GAUGE");
        tempGauge.put("positionX", 0);
        tempGauge.put("positionY", 5);
        tempGauge.put("width", 4);
        tempGauge.put("height", 4);
        tempGauge.putNull("deviceId");
        tempGauge.put("variableName", "temperature");
        tempGauge.put("aggregation", "NONE");
        tempGauge.putNull("timeRangeMinutes");
        ObjectNode tempGaugeConfig = objectMapper.createObjectNode();
        tempGaugeConfig.put("min", -10);
        tempGaugeConfig.put("max", 50);
        tempGaugeConfig.put("unit", "°C");
        tempGaugeConfig.put("dangerThreshold", 35);
        tempGauge.set("config", tempGaugeConfig);
        widgetsConfig.add(tempGauge);

        // Current Humidity Gauge
        ObjectNode humidityGauge = objectMapper.createObjectNode();
        humidityGauge.put("name", "Current Humidity");
        humidityGauge.put("type", "GAUGE");
        humidityGauge.put("positionX", 4);
        humidityGauge.put("positionY", 5);
        humidityGauge.put("width", 4);
        humidityGauge.put("height", 4);
        humidityGauge.putNull("deviceId");
        humidityGauge.put("variableName", "humidity");
        humidityGauge.put("aggregation", "NONE");
        humidityGauge.putNull("timeRangeMinutes");
        ObjectNode humidityGaugeConfig = objectMapper.createObjectNode();
        humidityGaugeConfig.put("min", 0);
        humidityGaugeConfig.put("max", 100);
        humidityGaugeConfig.put("unit", "%");
        humidityGaugeConfig.put("dangerThreshold", 80);
        humidityGauge.set("config", humidityGaugeConfig);
        widgetsConfig.add(humidityGauge);

        // Air Quality Metric
        ObjectNode aqWidget = objectMapper.createObjectNode();
        aqWidget.put("name", "Air Quality Index");
        aqWidget.put("type", "METRIC");
        aqWidget.put("positionX", 8);
        aqWidget.put("positionY", 5);
        aqWidget.put("width", 4);
        aqWidget.put("height", 4);
        aqWidget.putNull("deviceId");
        aqWidget.put("variableName", "air_quality");
        aqWidget.put("aggregation", "NONE");
        aqWidget.putNull("timeRangeMinutes");
        ObjectNode aqConfig = objectMapper.createObjectNode();
        aqConfig.put("unit", "AQI");
        aqConfig.put("precision", 0);
        aqWidget.set("config", aqConfig);
        widgetsConfig.add(aqWidget);

        DashboardTemplate template = DashboardTemplate.builder()
            .name("Environmental Monitoring")
            .description("Monitor environmental conditions with temperature, humidity, and air quality sensors")
            .category(DashboardTemplateCategory.ENVIRONMENTAL)
            .icon("🌡️")
            .dashboardConfig(dashboardConfig)
            .widgetsConfig(widgetsConfig)
            .isSystem(true)
            .build();

        templateRepository.save(template);
        log.info("Created Environmental Monitoring dashboard template");
    }

    /**
     * Industrial Equipment Dashboard Template
     */
    private void createIndustrialTemplate() {
        if (templateRepository.findByName("Industrial Equipment Monitor").isPresent()) {
            log.debug("Industrial template already exists, skipping");
            return;
        }

        ObjectNode dashboardConfig = objectMapper.createObjectNode();
        dashboardConfig.put("name", "Industrial Equipment Monitor");
        dashboardConfig.put("description", "Monitor industrial equipment with pressure, temperature, vibration, and status metrics");

        ArrayNode widgetsConfig = objectMapper.createArrayNode();

        // Equipment Status
        ObjectNode statusWidget = objectMapper.createObjectNode();
        statusWidget.put("name", "Equipment Status");
        statusWidget.put("type", "METRIC");
        statusWidget.put("positionX", 0);
        statusWidget.put("positionY", 0);
        statusWidget.put("width", 3);
        statusWidget.put("height", 3);
        statusWidget.putNull("deviceId");
        statusWidget.put("variableName", "status");
        statusWidget.put("aggregation", "NONE");
        statusWidget.putNull("timeRangeMinutes");
        ObjectNode statusConfig = objectMapper.createObjectNode();
        statusConfig.put("displayType", "badge");
        statusWidget.set("config", statusConfig);
        widgetsConfig.add(statusWidget);

        // Operating Temperature
        ObjectNode tempWidget = objectMapper.createObjectNode();
        tempWidget.put("name", "Operating Temperature");
        tempWidget.put("type", "LINE_CHART");
        tempWidget.put("positionX", 3);
        tempWidget.put("positionY", 0);
        tempWidget.put("width", 9);
        tempWidget.put("height", 5);
        tempWidget.putNull("deviceId");
        tempWidget.put("variableName", "temperature");
        tempWidget.put("aggregation", "AVG");
        tempWidget.put("timeRangeMinutes", 60);
        ObjectNode tempConfig = objectMapper.createObjectNode();
        tempConfig.put("unit", "°C");
        tempConfig.put("color", "#F59E0B");
        tempWidget.set("config", tempConfig);
        widgetsConfig.add(tempWidget);

        // Pressure Gauge
        ObjectNode pressureWidget = objectMapper.createObjectNode();
        pressureWidget.put("name", "Pressure");
        pressureWidget.put("type", "GAUGE");
        pressureWidget.put("positionX", 0);
        pressureWidget.put("positionY", 3);
        pressureWidget.put("width", 3);
        pressureWidget.put("height", 4);
        pressureWidget.putNull("deviceId");
        pressureWidget.put("variableName", "pressure");
        pressureWidget.put("aggregation", "NONE");
        pressureWidget.putNull("timeRangeMinutes");
        ObjectNode pressureConfig = objectMapper.createObjectNode();
        pressureConfig.put("min", 0);
        pressureConfig.put("max", 200);
        pressureConfig.put("unit", "PSI");
        pressureConfig.put("dangerThreshold", 180);
        pressureWidget.set("config", pressureConfig);
        widgetsConfig.add(pressureWidget);

        // Vibration Chart
        ObjectNode vibrationWidget = objectMapper.createObjectNode();
        vibrationWidget.put("name", "Vibration Level");
        vibrationWidget.put("type", "LINE_CHART");
        vibrationWidget.put("positionX", 3);
        vibrationWidget.put("positionY", 5);
        vibrationWidget.put("width", 9);
        vibrationWidget.put("height", 4);
        vibrationWidget.putNull("deviceId");
        vibrationWidget.put("variableName", "vibration");
        vibrationWidget.put("aggregation", "AVG");
        vibrationWidget.put("timeRangeMinutes", 30);
        ObjectNode vibrationConfig = objectMapper.createObjectNode();
        vibrationConfig.put("unit", "mm/s");
        vibrationConfig.put("color", "#8B5CF6");
        vibrationWidget.set("config", vibrationConfig);
        widgetsConfig.add(vibrationWidget);

        DashboardTemplate template = DashboardTemplate.builder()
            .name("Industrial Equipment Monitor")
            .description("Comprehensive equipment monitoring with temperature, pressure, vibration, and status tracking")
            .category(DashboardTemplateCategory.INDUSTRIAL)
            .icon("🏭")
            .dashboardConfig(dashboardConfig)
            .widgetsConfig(widgetsConfig)
            .isSystem(true)
            .build();

        templateRepository.save(template);
        log.info("Created Industrial Equipment dashboard template");
    }
}
