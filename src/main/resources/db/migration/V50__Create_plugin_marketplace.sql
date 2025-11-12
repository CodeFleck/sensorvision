-- Plugin Marketplace Schema
-- Create plugin_registry table to store available plugins metadata

CREATE TABLE plugin_registry (
    id BIGSERIAL PRIMARY KEY,
    plugin_key VARCHAR(100) NOT NULL UNIQUE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    category VARCHAR(50) NOT NULL,
    version VARCHAR(20) NOT NULL,
    author VARCHAR(200),
    author_url VARCHAR(500),
    icon_url VARCHAR(500),
    repository_url VARCHAR(500),
    documentation_url VARCHAR(500),
    min_sensorvision_version VARCHAR(20),
    max_sensorvision_version VARCHAR(20),
    is_official BOOLEAN DEFAULT false,
    is_verified BOOLEAN DEFAULT false,
    installation_count INTEGER DEFAULT 0,
    rating_average DECIMAL(3, 2),
    rating_count INTEGER DEFAULT 0,
    plugin_provider VARCHAR(50) NOT NULL,
    plugin_type VARCHAR(50) NOT NULL,
    config_schema JSONB,
    tags TEXT[],
    screenshots TEXT[],
    changelog TEXT,
    published_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Create installed_plugins table to track user installations
CREATE TABLE installed_plugins (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    plugin_registry_id BIGINT NOT NULL REFERENCES plugin_registry(id) ON DELETE CASCADE,
    plugin_key VARCHAR(100) NOT NULL,
    installed_version VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'INACTIVE',
    configuration JSONB,
    data_plugin_id BIGINT REFERENCES data_plugins(id) ON DELETE SET NULL,
    installed_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    activated_at TIMESTAMP WITH TIME ZONE,
    last_used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_org_plugin UNIQUE (organization_id, plugin_key)
);

-- Create plugin_ratings table for user reviews
CREATE TABLE plugin_ratings (
    id BIGSERIAL PRIMARY KEY,
    plugin_registry_id BIGINT NOT NULL REFERENCES plugin_registry(id) ON DELETE CASCADE,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    rating INTEGER NOT NULL CHECK (rating >= 1 AND rating <= 5),
    review_text TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT unique_org_rating UNIQUE (plugin_registry_id, organization_id)
);

-- Create indexes for better query performance
CREATE INDEX idx_plugin_registry_category ON plugin_registry(category);
CREATE INDEX idx_plugin_registry_provider ON plugin_registry(plugin_provider);
CREATE INDEX idx_plugin_registry_official ON plugin_registry(is_official);
CREATE INDEX idx_plugin_registry_published ON plugin_registry(published_at DESC);
CREATE INDEX idx_installed_plugins_org ON installed_plugins(organization_id);
CREATE INDEX idx_installed_plugins_status ON installed_plugins(status);
CREATE INDEX idx_plugin_ratings_registry ON plugin_ratings(plugin_registry_id);

-- Add updated_at trigger for plugin_registry
CREATE TRIGGER update_plugin_registry_updated_at
    BEFORE UPDATE ON plugin_registry
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add updated_at trigger for installed_plugins
CREATE TRIGGER update_installed_plugins_updated_at
    BEFORE UPDATE ON installed_plugins
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- Add updated_at trigger for plugin_ratings
CREATE TRIGGER update_plugin_ratings_updated_at
    BEFORE UPDATE ON plugin_ratings
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();
