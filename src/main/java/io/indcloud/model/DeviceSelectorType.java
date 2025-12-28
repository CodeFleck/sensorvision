package io.indcloud.model;

/**
 * Enum for device selector types in global rules
 */
public enum DeviceSelectorType {
    TAG,              // Select devices by tag name
    GROUP,            // Select devices by group ID
    ORGANIZATION,     // Select all devices in organization
    CUSTOM_FILTER     // Select devices using custom query expression
}
