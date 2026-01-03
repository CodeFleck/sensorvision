package io.indcloud.model;

/**
 * Pre-built plugin providers
 */
public enum PluginProvider {
    /**
     * The Things Network (TTN) LoRaWAN integration
     */
    LORAWAN_TTN("LoRaWAN (The Things Network)", PluginType.WEBHOOK),

    /**
     * Modbus TCP polling plugin
     */
    MODBUS_TCP("Modbus TCP", PluginType.POLLING),

    /**
     * Sigfox integration
     */
    SIGFOX("Sigfox", PluginType.WEBHOOK),

    /**
     * Particle Cloud integration
     */
    PARTICLE_CLOUD("Particle Cloud", PluginType.WEBHOOK),

    /**
     * Generic HTTP webhook receiver
     */
    HTTP_WEBHOOK("HTTP Webhook", PluginType.WEBHOOK),

    /**
     * CSV file import
     */
    CSV_FILE("CSV File Import", PluginType.CSV_IMPORT),

    /**
     * Custom protocol parser (user-defined JavaScript/Python)
     */
    CUSTOM_PARSER("Custom Parser", PluginType.PROTOCOL_PARSER),

    /**
     * Custom MQTT topic with transformation (deprecated - use MQTT_BRIDGE)
     */
    MQTT_CUSTOM("MQTT Custom Format", PluginType.PROTOCOL_PARSER),

    /**
     * MQTT Bridge - Connect to external MQTT brokers
     */
    MQTT_BRIDGE("MQTT Bridge", PluginType.WEBHOOK),

    /**
     * Slack notification integration
     */
    SLACK("Slack Notifications", PluginType.INTEGRATION),

    /**
     * Discord notification integration
     */
    DISCORD("Discord Notifications", PluginType.INTEGRATION),

    /**
     * Telegram notification integration
     */
    TELEGRAM("Telegram Notifications", PluginType.INTEGRATION),

    /**
     * PagerDuty incident management integration
     */
    PAGERDUTY("PagerDuty Incidents", PluginType.INTEGRATION);

    private final String displayName;
    private final PluginType defaultType;

    PluginProvider(String displayName, PluginType defaultType) {
        this.displayName = displayName;
        this.defaultType = defaultType;
    }

    public String getDisplayName() {
        return displayName;
    }

    public PluginType getDefaultType() {
        return defaultType;
    }
}
