package org.sensorvision.model;

/**
 * Status of a plugin execution
 */
public enum PluginExecutionStatus {
    /**
     * Plugin executed successfully and processed all data
     */
    SUCCESS,

    /**
     * Plugin failed completely
     */
    FAILED,

    /**
     * Plugin processed some data but encountered errors
     */
    PARTIAL
}
