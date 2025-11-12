package org.sensorvision.model;

/**
 * Plugin categories for marketplace organization
 */
public enum PluginCategory {
    /**
     * Protocol parsers for IoT protocols (LoRaWAN, Modbus, Sigfox, Zigbee, etc.)
     */
    PROTOCOL_PARSER("Protocol Parsers", "Parse and decode IoT protocol data"),

    /**
     * Data source integrations (MQTT bridges, REST APIs, databases)
     */
    DATA_SOURCE("Data Sources", "Connect to external data sources"),

    /**
     * Notification channels (Slack, Discord, Telegram, Email, SMS, PagerDuty)
     */
    NOTIFICATION("Notifications", "Send alerts to external channels"),

    /**
     * Analytics and data processing
     */
    ANALYTICS("Analytics", "Advanced data analysis and processing"),

    /**
     * Data visualization plugins
     */
    VISUALIZATION("Visualizations", "Custom charts and dashboards"),

    /**
     * Third-party integrations (AWS IoT, Azure IoT, Google Cloud IoT, IFTTT)
     */
    INTEGRATION("Integrations", "Connect to external platforms"),

    /**
     * Custom transformations and calculations
     */
    TRANSFORMATION("Transformations", "Transform and manipulate data"),

    /**
     * Storage and export plugins
     */
    STORAGE("Storage & Export", "Store or export data to external systems"),

    /**
     * Security and authentication plugins
     */
    SECURITY("Security", "Authentication and security features"),

    /**
     * Utility and helper plugins
     */
    UTILITY("Utilities", "Helper tools and utilities");

    private final String displayName;
    private final String description;

    PluginCategory(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
