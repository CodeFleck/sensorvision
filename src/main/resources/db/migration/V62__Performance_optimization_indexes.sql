-- Migration V62: Performance optimization indexes
-- Purpose: Optimize database performance for production workloads
-- Date: 2025-12-08
-- Updated: 2026-01-03 - Fixed column names to match actual schema

-- ==============================================================================
-- TELEMETRY PERFORMANCE INDEXES (telemetry_records uses measurement_timestamp)
-- ==============================================================================

-- Note: idx_telemetry_device_time already exists in V1 migration
-- Skipping duplicate: ON telemetry_records(device_id, measurement_timestamp DESC)

-- ==============================================================================
-- DEVICE PERFORMANCE INDEXES
-- ==============================================================================

-- Composite index for active devices by organization
CREATE INDEX IF NOT EXISTS idx_devices_org_active_created
    ON devices(organization_id, active, created_at DESC);

-- Index for device status and last seen (health monitoring)
CREATE INDEX IF NOT EXISTS idx_devices_status_last_seen
    ON devices(status, last_seen_at DESC);

-- ==============================================================================
-- USER AND ORGANIZATION INDEXES
-- ==============================================================================

-- Note: idx_users_org_last_login removed - users table has no last_login column
-- If last login tracking is needed, add the column first via a separate migration

-- Index for username lookup (authentication)
-- Note: idx_users_username already exists in V6 migration
-- CREATE INDEX IF NOT EXISTS idx_users_username_active ON users(username);

-- ==============================================================================
-- DASHBOARD AND WIDGET PERFORMANCE
-- ==============================================================================

-- Index for dashboard queries by organization
-- Note: dashboards table has no created_by column
CREATE INDEX IF NOT EXISTS idx_dashboards_org_updated
    ON dashboards(organization_id, updated_at DESC);

-- Index for widget queries by dashboard position
-- Note: widgets table uses position_x/position_y, not widget_order
CREATE INDEX IF NOT EXISTS idx_widgets_dashboard_position
    ON widgets(dashboard_id, position_y, position_x);

-- ==============================================================================
-- RULES AND ALERTS PERFORMANCE
-- ==============================================================================

-- Composite index for active rules evaluation
CREATE INDEX IF NOT EXISTS idx_rules_org_enabled_device
    ON rules(organization_id, enabled, device_id);

-- Index for recent alerts by severity (alerts has no organization_id column)
CREATE INDEX IF NOT EXISTS idx_alerts_severity_created
    ON alerts(severity, created_at DESC);

-- Note: idx_alerts_acknowledged_created already exists in V2 migration
-- Index for unacknowledged alerts with device (dashboard queries)
CREATE INDEX IF NOT EXISTS idx_alerts_device_unacknowledged
    ON alerts(device_id, acknowledged, created_at DESC);

-- ==============================================================================
-- DYNAMIC VARIABLES PERFORMANCE (EAV Pattern)
-- ==============================================================================

-- Index for variable lookups by device
CREATE INDEX IF NOT EXISTS idx_variables_device_name
    ON variables(device_id, name);

-- Note: idx_variable_values_variable_timestamp already exists in V56 migration
-- Skipping duplicate: variable_values(variable_id, timestamp DESC)

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

-- Note: idx_synthetic_variables_device_enabled already exists in V3 migration
-- Note: idx_synthetic_variable_values_var_time already exists in V3 migration
-- Skipping duplicates for synthetic_variables and synthetic_variable_values

-- ==============================================================================
-- GLOBAL RULES PERFORMANCE (Fleet Monitoring)
-- ==============================================================================

-- Index for global rule evaluation
-- Note: idx_global_rules_last_evaluated already exists in V48 migration
CREATE INDEX IF NOT EXISTS idx_global_rules_org_enabled
    ON global_rules(organization_id, enabled, last_evaluated_at);

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

-- Note: support_tickets table does not exist yet
-- These indexes should be created when the support tickets feature is added
