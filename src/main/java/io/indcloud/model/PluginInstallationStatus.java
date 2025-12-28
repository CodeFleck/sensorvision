package io.indcloud.model;

/**
 * Status of an installed plugin
 */
public enum PluginInstallationStatus {
    /**
     * Plugin is installed but not yet activated
     */
    INACTIVE("Inactive", "Plugin is installed but not active"),

    /**
     * Plugin is installed and actively running
     */
    ACTIVE("Active", "Plugin is running"),

    /**
     * Plugin encountered an error and is disabled
     */
    ERROR("Error", "Plugin has errors"),

    /**
     * Plugin is being installed
     */
    INSTALLING("Installing", "Installation in progress"),

    /**
     * Plugin is being updated
     */
    UPDATING("Updating", "Update in progress"),

    /**
     * Plugin is being uninstalled
     */
    UNINSTALLING("Uninstalling", "Removal in progress");

    private final String displayName;
    private final String description;

    PluginInstallationStatus(String displayName, String description) {
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
