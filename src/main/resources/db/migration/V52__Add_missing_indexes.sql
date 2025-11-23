-- Migration V52: Add missing indexes on foreign keys for plugin marketplace tables
-- Purpose: Improve query performance for plugin-related operations
-- Date: 2025-11-14

-- Index on installed_plugins.plugin_registry_id (foreign key lookup)
CREATE INDEX IF NOT EXISTS idx_installed_plugins_registry
    ON installed_plugins(plugin_registry_id);

-- Index on installed_plugins.data_plugin_id (foreign key lookup)
CREATE INDEX IF NOT EXISTS idx_installed_plugins_data_plugin
    ON installed_plugins(data_plugin_id);

-- Composite index on organization_id and status (common query pattern)
CREATE INDEX IF NOT EXISTS idx_installed_plugins_org_status
    ON installed_plugins(organization_id, status);

-- Index on installed_plugins.organization_id and plugin_key (unique lookup pattern)
CREATE INDEX IF NOT EXISTS idx_installed_plugins_org_plugin_key
    ON installed_plugins(organization_id, plugin_key);

-- Index on plugin_ratings.plugin_registry_id (aggregation queries)
CREATE INDEX IF NOT EXISTS idx_plugin_ratings_registry
    ON plugin_ratings(plugin_registry_id);

-- Index on plugin_ratings.organization_id (user rating lookups)
CREATE INDEX IF NOT EXISTS idx_plugin_ratings_org
    ON plugin_ratings(organization_id);

-- Composite index on plugin_registry category and is_official (marketplace filtering)
CREATE INDEX IF NOT EXISTS idx_plugin_registry_category_official
    ON plugin_registry(category, is_official);

-- Index on plugin_registry.installation_count for popular plugins sorting
CREATE INDEX IF NOT EXISTS idx_plugin_registry_installation_count
    ON plugin_registry(installation_count DESC);

-- Index on plugin_registry.rating_average for top-rated plugins sorting
CREATE INDEX IF NOT EXISTS idx_plugin_registry_rating_avg
    ON plugin_registry(rating_average DESC);

-- Index on plugin_registry.published_at for recent plugins sorting
CREATE INDEX IF NOT EXISTS idx_plugin_registry_published_at
    ON plugin_registry(published_at DESC);
