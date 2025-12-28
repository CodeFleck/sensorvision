package io.indcloud.model;

/**
 * Categories for dashboard templates
 */
public enum DashboardTemplateCategory {
    SMART_METER("Smart Meter", "Electric meter monitoring with power, voltage, current metrics"),
    ENVIRONMENTAL("Environmental", "Temperature, humidity, air quality sensors"),
    INDUSTRIAL("Industrial", "Manufacturing equipment and process monitoring"),
    FLEET("Fleet Management", "Vehicle tracking and fleet analytics"),
    ENERGY("Energy Management", "Solar, battery, energy consumption monitoring"),
    CUSTOM("Custom", "User-created custom templates");

    private final String displayName;
    private final String description;

    DashboardTemplateCategory(String displayName, String description) {
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
