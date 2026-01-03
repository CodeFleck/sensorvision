-- Migration V58: Performance optimization indexes
-- Purpose: Optimize database performance for production workloads
-- Date: 2025-12-08

-- ==============================================================================
-- TELEMETRY PERFORMANCE INDEXES
-- ==============================================================================

-- Composite index for telemetry queries by device and time range (most common query pattern)
CREATE INDEX IF NOT EXISTS idx_telemetry_device_timestamp_desc
    ON telemetry_records(device_id, timestamp DESC);

-- Index for variable-specific queries with time range
CREATE INDEX IF NOT EXISTS idx_telemetry_variable_time
    ON telemetry_records(variable_name, timestamp DESC);

-- Composite index for analytics queries (aggregations by device and variable)
CREATE INDEX IF NOT EXISTS idx_telemetry_analytics
    ON telemetry_records(device_id, variable_name, timestamp, numeric_value);

-- ==============================================================================
-- DEVICE PERFORMANCE INDEXES
-- ==============================================================================

-- Composite index for active devices by organization
CREATE INDEX IF NOT EXISTS idx_devices_org_active_created
    ON devices(organization_id, active, created_at DESC);

-- Index for device status and last seen (health monitoring)
CREATE INDEX IF NOT EXISTS idx_devices_status_last_seen
    ON devices(status, last_seen DESC);

-- ==============================================================================
-- USER AND ORGANIZATION INDEXES
-- ==============================================================================

-- Index for user login queries and session management
CREATE INDEX IF NOT EXISTS idx_users_org_last_login
    ON users(organization_id, last_login DESC);

-- Index for username lookup (authentication)
CREATE INDEX IF NOT EXISTS idx_users_username_active
    ON users(username);

-- ==============================================================================
-- DASHBOARD AND WIDGET PERFORMANCE
-- ==============================================================================

-- Index for dashboard queries by organization and user
CREATE INDEX IF NOT EXISTS idx_dashboards_org_user_updated
    ON dashboards(organization_id, created_by, updated_at DESC);

-- Index for widget queries by dashboard
CREATE INDEX IF NOT EXISTS idx_widgets_dashboard_order
    ON widgets(dashboard_id, widget_order);

-- ==============================================================================
-- RULES AND ALERTS PERFORMANCE
-- ==============================================================================

-- Composite index for active rules evaluation
CREATE INDEX IF NOT EXISTS idx_rules_org_enabled_device
    ON rules(organization_id, enabled, device_id);

-- Index for recent alerts by organization and severity
CREATE INDEX IF NOT EXISTS idx_alerts_org_severity_created
    ON alerts(organization_id, severity, created_at DESC);

-- Index for unacknowledged alerts (dashboard queries)
CREATE INDEX IF NOT EXISTS idx_alerts_unacknowledged
    ON alerts(organization_id, acknowledged, created_at DESC);

-- ==============================================================================
-- DYNAMIC VARIABLES PERFORMANCE (EAV Pattern)
-- ==============================================================================

-- Index for variable lookups by device
CREATE INDEX IF NOT EXISTS idx_variables_device_name
    ON variables(device_id, name);

-- Index for variable values time series queries
CREATE INDEX IF NOT EXISTS idx_variable_values_var_timestamp
    ON variable_values(variable_id, timestamp DESC);

-- ==============================================================================
-- PLUGIN MARKETPLACE PERFORMANCE
-- ==============================================================================

-- Index for plugin installation queries by organization
CREATE INDEX IF NOT EXISTS idx_installed_plugins_org_status
    ON installed_plugins(organization_id, status, installed_at DESC);

-- Index for plugin popularity and ratings
CREATE INDEX IF NOT EXISTS idx_plugin_registry_popularity
    ON plugin_registry(installation_count DESC, rating_average DESC, published_at DESC);

-- ==============================================================================
-- SYNTHETIC VARIABLES PERFORMANCE
-- ==============================================================================

-- Index for synthetic variable calculations
CREATE INDEX IF NOT EXISTS idx_synthetic_variables_device_enabled
    ON synthetic_variables(device_id, enabled);

-- Index for synthetic variable values with time range
CREATE INDEX IF NOT EXISTS idx_synthetic_values_var_time
    ON synthetic_variable_values(synthetic_variable_id, calculated_at DESC);

-- ==============================================================================
-- GLOBAL RULES PERFORMANCE (Fleet Monitoring)
-- ==============================================================================

-- Index for global rule evaluation
CREATE INDEX IF NOT EXISTS idx_global_rules_org_enabled
    ON global_rules(organization_id, enabled, last_evaluated);

-- Index for global alerts by organization
CREATE INDEX IF NOT EXISTS idx_global_alerts_org_created
    ON global_alerts(organization_id, created_at DESC);

-- ==============================================================================
-- USER API KEYS PERFORMANCE
-- ==============================================================================

-- Index for API key prefix lookups (authentication)
CREATE INDEX IF NOT EXISTS idx_user_api_keys_prefix_active
    ON user_api_keys(key_prefix);

-- Index for scheduled revocation processing
CREATE INDEX IF NOT EXISTS idx_user_api_keys_scheduled_revocation
    ON user_api_keys(scheduled_revocation_at);

-- ==============================================================================
-- SUPPORT TICKETS PERFORMANCE
-- ==============================================================================

-- Index for support ticket queries by organization and status
CREATE INDEX IF NOT EXISTS idx_support_tickets_org_status
    ON support_tickets(organization_id, status, created_at DESC);

-- Index for admin support ticket queries
CREATE INDEX IF NOT EXISTS idx_support_tickets_status_priority
    ON support_tickets(status, priority, created_at DESC);
