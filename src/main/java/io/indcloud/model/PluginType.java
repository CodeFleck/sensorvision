package io.indcloud.model;

/**
 * Types of data plugins supported by the system
 */
public enum PluginType {
    /**
     * Protocol parser - converts binary/custom protocols to JSON
     * Examples: LoRaWAN payload decoder, Modbus parser
     */
    PROTOCOL_PARSER,

    /**
     * Webhook receiver - accepts HTTP POST webhooks from external systems
     * Examples: TTN webhook, Particle webhook
     */
    WEBHOOK,

    /**
     * Integration - pulls data from 3rd party APIs
     * Examples: External REST API integration
     */
    INTEGRATION,

    /**
     * CSV import - imports historical data from CSV files
     */
    CSV_IMPORT,

    /**
     * Polling - actively polls external devices/services on a schedule
     * Examples: Modbus TCP, SNMP, REST API polling
     */
    POLLING
}
