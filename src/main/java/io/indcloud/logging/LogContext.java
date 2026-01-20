package io.indcloud.logging;

import org.slf4j.MDC;

import java.util.UUID;

/**
 * Utility class for managing logging context via MDC (Mapped Diagnostic Context).
 * Use this to add contextual information to logs for Kibana filtering and debugging.
 *
 * Example usage:
 * <pre>
 * try (var ctx = LogContext.forDevice(deviceId)) {
 *     log.info("Processing telemetry");
 *     // ... processing code
 * } // Context automatically cleared
 * </pre>
 *
 * Or for quick context setting:
 * <pre>
 * LogContext.setDevice(deviceId);
 * LogContext.setOrganization(orgId);
 * try {
 *     // ... code
 * } finally {
 *     LogContext.clearDevice();
 *     LogContext.clearOrganization();
 * }
 * </pre>
 */
public final class LogContext {

    // MDC keys for various contexts
    public static final String DEVICE_ID = "deviceId";
    public static final String DEVICE_EXTERNAL_ID = "deviceExternalId";
    public static final String ORGANIZATION_ID = "organizationId";
    public static final String ORGANIZATION_NAME = "organizationName";
    public static final String DASHBOARD_ID = "dashboardId";
    public static final String WIDGET_ID = "widgetId";
    public static final String RULE_ID = "ruleId";
    public static final String ALERT_ID = "alertId";
    public static final String ML_MODEL_ID = "mlModelId";
    public static final String TRAINING_JOB_ID = "trainingJobId";
    public static final String OPERATION = "operation";
    public static final String VARIABLE_NAME = "variableName";

    private LogContext() {
        // Utility class
    }

    // =====================
    // Device Context
    // =====================

    public static void setDevice(UUID deviceId) {
        if (deviceId != null) {
            MDC.put(DEVICE_ID, deviceId.toString());
        }
    }

    public static void setDevice(UUID deviceId, String externalId) {
        if (deviceId != null) {
            MDC.put(DEVICE_ID, deviceId.toString());
        }
        if (externalId != null) {
            MDC.put(DEVICE_EXTERNAL_ID, externalId);
        }
    }

    public static void setDeviceExternalId(String externalId) {
        if (externalId != null) {
            MDC.put(DEVICE_EXTERNAL_ID, externalId);
        }
    }

    public static void clearDevice() {
        MDC.remove(DEVICE_ID);
        MDC.remove(DEVICE_EXTERNAL_ID);
    }

    // =====================
    // Organization Context
    // =====================

    public static void setOrganization(Long orgId) {
        if (orgId != null) {
            MDC.put(ORGANIZATION_ID, orgId.toString());
        }
    }

    public static void setOrganization(Long orgId, String orgName) {
        if (orgId != null) {
            MDC.put(ORGANIZATION_ID, orgId.toString());
        }
        if (orgName != null) {
            MDC.put(ORGANIZATION_NAME, orgName);
        }
    }

    public static void clearOrganization() {
        MDC.remove(ORGANIZATION_ID);
        MDC.remove(ORGANIZATION_NAME);
    }

    // =====================
    // Operation Context
    // =====================

    public static void setOperation(String operation) {
        if (operation != null) {
            MDC.put(OPERATION, operation);
        }
    }

    public static void clearOperation() {
        MDC.remove(OPERATION);
    }

    // =====================
    // Dashboard/Widget Context
    // =====================

    public static void setDashboard(Long dashboardId) {
        if (dashboardId != null) {
            MDC.put(DASHBOARD_ID, dashboardId.toString());
        }
    }

    public static void setWidget(Long widgetId) {
        if (widgetId != null) {
            MDC.put(WIDGET_ID, widgetId.toString());
        }
    }

    public static void clearDashboard() {
        MDC.remove(DASHBOARD_ID);
    }

    public static void clearWidget() {
        MDC.remove(WIDGET_ID);
    }

    // =====================
    // Rule/Alert Context
    // =====================

    public static void setRule(Long ruleId) {
        if (ruleId != null) {
            MDC.put(RULE_ID, ruleId.toString());
        }
    }

    public static void setAlert(Long alertId) {
        if (alertId != null) {
            MDC.put(ALERT_ID, alertId.toString());
        }
    }

    public static void clearRule() {
        MDC.remove(RULE_ID);
    }

    public static void clearAlert() {
        MDC.remove(ALERT_ID);
    }

    // =====================
    // ML Context
    // =====================

    public static void setMLModel(Long modelId) {
        if (modelId != null) {
            MDC.put(ML_MODEL_ID, modelId.toString());
        }
    }

    public static void setTrainingJob(Long jobId) {
        if (jobId != null) {
            MDC.put(TRAINING_JOB_ID, jobId.toString());
        }
    }

    public static void clearML() {
        MDC.remove(ML_MODEL_ID);
        MDC.remove(TRAINING_JOB_ID);
    }

    // =====================
    // Variable Context
    // =====================

    public static void setVariable(String variableName) {
        if (variableName != null) {
            MDC.put(VARIABLE_NAME, variableName);
        }
    }

    public static void clearVariable() {
        MDC.remove(VARIABLE_NAME);
    }

    // =====================
    // Auto-Closeable Contexts
    // =====================

    /**
     * Create an auto-closeable context for a device operation.
     * Usage:
     * <pre>
     * try (var ctx = LogContext.forDevice(deviceId, externalId)) {
     *     log.info("Processing device");
     * }
     * </pre>
     */
    public static DeviceLogContext forDevice(UUID deviceId, String externalId) {
        return new DeviceLogContext(deviceId, externalId);
    }

    public static DeviceLogContext forDevice(String externalId) {
        return new DeviceLogContext(null, externalId);
    }

    /**
     * Create an auto-closeable context for an organization operation.
     */
    public static OrganizationLogContext forOrganization(Long orgId, String orgName) {
        return new OrganizationLogContext(orgId, orgName);
    }

    /**
     * Create an auto-closeable context for a named operation.
     */
    public static OperationLogContext forOperation(String operation) {
        return new OperationLogContext(operation);
    }

    // =====================
    // Auto-Closeable Context Classes
    // =====================

    public static class DeviceLogContext implements AutoCloseable {
        public DeviceLogContext(UUID deviceId, String externalId) {
            LogContext.setDevice(deviceId, externalId);
        }

        @Override
        public void close() {
            LogContext.clearDevice();
        }
    }

    public static class OrganizationLogContext implements AutoCloseable {
        public OrganizationLogContext(Long orgId, String orgName) {
            LogContext.setOrganization(orgId, orgName);
        }

        @Override
        public void close() {
            LogContext.clearOrganization();
        }
    }

    public static class OperationLogContext implements AutoCloseable {
        public OperationLogContext(String operation) {
            LogContext.setOperation(operation);
        }

        @Override
        public void close() {
            LogContext.clearOperation();
        }
    }
}
