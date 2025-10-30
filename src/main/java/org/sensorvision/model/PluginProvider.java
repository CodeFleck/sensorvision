package org.sensorvision.model;

/**
 * Pre-built plugin providers
 */
public enum PluginProvider {
    /**
     * The Things Network (TTN) LoRaWAN integration
     */
    LORAWAN_TTN("LoRaWAN (The Things Network)", PluginType.WEBHOOK),

    /**
     * Modbus TCP protocol parser
     */
    MODBUS_TCP("Modbus TCP", PluginType.PROTOCOL_PARSER),

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
     * Custom MQTT topic with transformation
     */
    MQTT_CUSTOM("MQTT Custom Format", PluginType.PROTOCOL_PARSER);

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
