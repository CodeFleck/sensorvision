-- Migration V57: Performance optimization indexes for pilot program
-- Purpose: Optimize database performance for pilot-scale workload (100 users, 500 devices, 1M telemetry points/day)
-- Date: 2025-12-06

-- ==============================================================================
-- TELEMETRY PERFORMANCE INDEXES
-- ==============================================================================

-- Composite index for telemetry queries by device and time range (most common query pattern)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_telemetry_device_timestamp_desc
    ON telemetry_records(device_id, timestamp DESC)
    WHERE timestamp > NOW() - INTERVAL '90 days';

-- Partial index for recent telemetry (last 7 days) - hot data
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_telemetry_recent_hot
    ON telemetry_records(device_id, variable_name, timestamp DESC)
    WHERE timestamp > NOW() - INTERVAL '7 days';

-- Index for variable-specific queries with time range
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_telemetry_variable_time
    ON telemetry_records(variable_name, timestamp DESC)
    WHERE timestamp > NOW() - INTERVAL '30 days';

-- Composite index for analytics queries (aggregations by device and variable)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_telemetry_analytics
    ON telemetry_records(device_id, variable_name, timestamp, numeric_value)
    WHERE timestamp > NOW() - INTERVAL '30 days' AND numeric_value IS NOT NULL;

-- ==============================================================================
-- DEVICE PERFORMANCE INDEXES
-- ==============================================================================

-- Composite index for active devices by organization (pilot quota queries)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_devices_org_active_created
    ON devices(organization_id, active, created_at DESC)
    WHERE active = true;

-- Index for device status and last seen (health monitoring)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_devices_status_last_seen
    ON devices(status, last_seen DESC)
    WHERE status IN ('ONLINE', 'OFFLINE');

-- Partial index for recently active devices (performance optimization)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_devices_recently_active
    ON devices(organization_id, last_seen DESC)
    WHERE last_seen > NOW() - INTERVAL '24 hours';

-- ==============================================================================
-- USER AND ORGANIZATION INDEXES
-- ==============================================================================

-- Index for user login queries and session management
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_org_last_login
    ON users(organization_id, last_login DESC)
    WHERE active = true;

-- Index for organization-scoped user counts (quota enforcement)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_org_active_count
    ON users(organization_id)
    WHERE active = true;

-- ==============================================================================
-- DASHBOARD AND WIDGET PERFORMANCE
-- ==============================================================================

-- Index for dashboard queries by organization and user
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_dashboards_org_user_updated
    ON dashboards(organization_id, created_by, updated_at DESC);

-- Index for widget queries by dashboard
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_widgets_dashboard_order
    ON widgets(dashboard_id, widget_order);

-- ==============================================================================
-- RULES AND ALERTS PERFORMANCE
-- ==============================================================================

-- Composite index for active rules evaluation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_rules_org_enabled_device
    ON rules(organization_id, enabled, device_id)
    WHERE enabled = true;

-- Index for recent alerts by organization and severity
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_alerts_org_severity_created
    ON alerts(organization_id, severity, created_at DESC)
    WHERE created_at > NOW() - INTERVAL '30 days';

-- Index for unacknowledged alerts (dashboard queries)
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_alerts_unacknowledged
    ON alerts(organization_id, acknowledged, created_at DESC)
    WHERE acknowledged = false;

-- ==============================================================================
-- PILOT-SPECIFIC PERFORMANCE INDEXES
-- ==============================================================================

-- Index for pilot feedback analytics
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pilot_feedback_org_rating_date
    ON pilot_feedback(organization_id, rating, submitted_at DESC);

-- Index for pilot feedback sentiment analysis
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_pilot_feedback_category_rating
    ON pilot_feedback(category, rating, submitted_at DESC)
    WHERE category IS NOT NULL;

-- ==============================================================================
-- PLUGIN MARKETPLACE PERFORMANCE
-- ==============================================================================

-- Index for plugin installation queries by organization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_installed_plugins_org_active
    ON installed_plugins(organization_id, status, installed_at DESC)
    WHERE status = 'ACTIVE';

-- Index for plugin popularity and ratings
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_plugin_registry_popularity
    ON plugin_registry(installation_count DESC, rating_average DESC, published_at DESC);

-- ==============================================================================
-- SYNTHETIC VARIABLES PERFORMANCE
-- ==============================================================================

-- Index for synthetic variable calculations
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_synthetic_variables_device_enabled
    ON synthetic_variables(device_id, enabled)
    WHERE enabled = true;

-- Index for synthetic variable values with time range
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_synthetic_values_var_time
    ON synthetic_variable_values(synthetic_variable_id, calculated_at DESC)
    WHERE calculated_at > NOW() - INTERVAL '30 days';

-- ==============================================================================
-- GLOBAL RULES PERFORMANCE (Fleet Monitoring)
-- ==============================================================================

-- Index for global rule evaluation
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_global_rules_org_enabled
    ON global_rules(organization_id, enabled, last_evaluated)
    WHERE enabled = true;

-- Index for global alerts by organization
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_global_alerts_org_created
    ON global_alerts(organization_id, created_at DESC);

-- ==============================================================================
-- SESSION AND AUTHENTICATION PERFORMANCE
-- ==============================================================================

-- Index for JWT token validation and user sessions
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_users_username_active
    ON users(username)
    WHERE active = true;

-- Index for device token authentication
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_devices_external_id_active
    ON devices(external_id)
    WHERE active = true;

-- ==============================================================================
-- STATISTICS AND ANALYTICS VIEWS
-- ==============================================================================

-- Materialized view for pilot program statistics (refreshed hourly)
CREATE MATERIALIZED VIEW IF NOT EXISTS pilot_program_stats AS
SELECT 
    DATE_TRUNC('hour', NOW()) as stats_timestamp,
    COUNT(DISTINCT o.id) as total_organizations,
    COUNT(DISTINCT u.id) as total_users,
    COUNT(DISTINCT d.id) as total_devices,
    COUNT(DISTINCT CASE WHEN d.last_seen > NOW() - INTERVAL '24 hours' THEN d.id END) as active_devices_24h,
    COUNT(DISTINCT CASE WHEN u.last_login > NOW() - INTERVAL '7 days' THEN u.id END) as active_users_7d,
    COUNT(DISTINCT db.id) as total_dashboards,
    COUNT(DISTINCT r.id) as total_rules,
    COUNT(DISTINCT a.id) FILTER (WHERE a.created_at > NOW() - INTERVAL '24 hours') as alerts_24h,
    AVG(pf.rating) as avg_pilot_rating
FROM organizations o
LEFT JOIN users u ON o.id = u.organization_id
LEFT JOIN devices d ON o.id = d.organization_id
LEFT JOIN dashboards db ON o.id = db.organization_id
LEFT JOIN rules r ON o.id = r.organization_id
LEFT JOIN alerts a ON d.id = a.device_id
LEFT JOIN pilot_feedback pf ON o.id = pf.organization_id AND pf.submitted_at > NOW() - INTERVAL '30 days';

-- Create unique index on materialized view
CREATE UNIQUE INDEX IF NOT EXISTS idx_pilot_stats_timestamp 
    ON pilot_program_stats(stats_timestamp);

-- Materialized view for telemetry volume statistics (refreshed every 15 minutes)
CREATE MATERIALIZED VIEW IF NOT EXISTS telemetry_volume_stats AS
SELECT 
    DATE_TRUNC('hour', timestamp) as hour_bucket,
    device_id,
    organization_id,
    COUNT(*) as message_count,
    COUNT(DISTINCT variable_name) as unique_variables,
    AVG(CASE WHEN numeric_value IS NOT NULL THEN numeric_value END) as avg_numeric_value,
    MIN(timestamp) as first_message,
    MAX(timestamp) as last_message
FROM telemetry_records tr
JOIN devices d ON tr.device_id = d.external_id
WHERE timestamp > NOW() - INTERVAL '7 days'
GROUP BY DATE_TRUNC('hour', timestamp), device_id, organization_id;

-- Create indexes on telemetry volume stats
CREATE INDEX IF NOT EXISTS idx_telemetry_volume_hour_org 
    ON telemetry_volume_stats(hour_bucket DESC, organization_id);

CREATE INDEX IF NOT EXISTS idx_telemetry_volume_device 
    ON telemetry_volume_stats(device_id, hour_bucket DESC);

-- ==============================================================================
-- PERFORMANCE MONITORING FUNCTIONS
-- ==============================================================================

-- Function to refresh pilot statistics (called by scheduler)
CREATE OR REPLACE FUNCTION refresh_pilot_stats()
RETURNS void AS $$
BEGIN
    REFRESH MATERIALIZED VIEW CONCURRENTLY pilot_program_stats;
    REFRESH MATERIALIZED VIEW CONCURRENTLY telemetry_volume_stats;
    
    -- Log refresh completion
    INSERT INTO events (event_type, description, created_at)
    VALUES ('STATS_REFRESH', 'Pilot program statistics refreshed', NOW());
END;
$$ LANGUAGE plpgsql;

-- Function to get database performance metrics
CREATE OR REPLACE FUNCTION get_db_performance_metrics()
RETURNS TABLE (
    metric_name text,
    metric_value numeric,
    metric_unit text
) AS $$
BEGIN
    RETURN QUERY
    SELECT 
        'active_connections'::text,
        (SELECT count(*) FROM pg_stat_activity WHERE state = 'active')::numeric,
        'connections'::text
    UNION ALL
    SELECT 
        'database_size'::text,
        (SELECT pg_database_size(current_database()) / 1024 / 1024)::numeric,
        'MB'::text
    UNION ALL
    SELECT 
        'cache_hit_ratio'::text,
        (SELECT ROUND(100.0 * sum(blks_hit) / (sum(blks_hit) + sum(blks_read)), 2) 
         FROM pg_stat_database WHERE datname = current_database())::numeric,
        'percent'::text
    UNION ALL
    SELECT 
        'avg_query_time'::text,
        (SELECT ROUND(mean_exec_time, 2) FROM pg_stat_statements 
         WHERE query LIKE '%telemetry_records%' LIMIT 1)::numeric,
        'ms'::text;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- CLEANUP AND MAINTENANCE
-- ==============================================================================

-- Function to cleanup old telemetry data (respects pilot 90-day retention)
CREATE OR REPLACE FUNCTION cleanup_old_telemetry()
RETURNS integer AS $$
DECLARE
    deleted_count integer;
BEGIN
    -- Delete telemetry older than 90 days (pilot retention policy)
    DELETE FROM telemetry_records 
    WHERE timestamp < NOW() - INTERVAL '90 days';
    
    GET DIAGNOSTICS deleted_count = ROW_COUNT;
    
    -- Log cleanup operation
    INSERT INTO events (event_type, description, metadata, created_at)
    VALUES (
        'DATA_CLEANUP', 
        'Cleaned up old telemetry data',
        jsonb_build_object('deleted_records', deleted_count),
        NOW()
    );
    
    RETURN deleted_count;
END;
$$ LANGUAGE plpgsql;

-- ==============================================================================
-- PERFORMANCE ANALYSIS QUERIES
-- ==============================================================================

-- View for slow query analysis
CREATE OR REPLACE VIEW slow_queries AS
SELECT 
    query,
    calls,
    total_exec_time,
    mean_exec_time,
    max_exec_time,
    stddev_exec_time,
    rows,
    100.0 * shared_blks_hit / nullif(shared_blks_hit + shared_blks_read, 0) AS hit_percent
FROM pg_stat_statements
WHERE mean_exec_time > 100  -- Queries slower than 100ms
ORDER BY mean_exec_time DESC;

-- View for index usage analysis
CREATE OR REPLACE VIEW index_usage AS
SELECT 
    schemaname,
    tablename,
    indexname,
    idx_tup_read,
    idx_tup_fetch,
    idx_scan,
    CASE 
        WHEN idx_scan = 0 THEN 'Never used'
        WHEN idx_scan < 100 THEN 'Rarely used'
        ELSE 'Frequently used'
    END as usage_category
FROM pg_stat_user_indexes
ORDER BY idx_scan DESC;

-- ==============================================================================
-- COMMENTS FOR DOCUMENTATION
-- ==============================================================================

COMMENT ON MATERIALIZED VIEW pilot_program_stats IS 'Aggregated statistics for pilot program monitoring, refreshed hourly';
COMMENT ON MATERIALIZED VIEW telemetry_volume_stats IS 'Telemetry volume statistics by hour and device, refreshed every 15 minutes';
COMMENT ON FUNCTION refresh_pilot_stats() IS 'Refreshes materialized views for pilot program statistics';
COMMENT ON FUNCTION get_db_performance_metrics() IS 'Returns key database performance metrics for monitoring';
COMMENT ON FUNCTION cleanup_old_telemetry() IS 'Cleans up telemetry data older than pilot retention period (90 days)';
COMMENT ON VIEW slow_queries IS 'Analysis view for identifying slow database queries';
COMMENT ON VIEW index_usage IS 'Analysis view for monitoring database index usage efficiency';