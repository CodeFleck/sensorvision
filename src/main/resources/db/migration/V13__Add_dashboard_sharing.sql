-- Add sharing capabilities to dashboards
ALTER TABLE dashboards ADD COLUMN is_public BOOLEAN NOT NULL DEFAULT false;
ALTER TABLE dashboards ADD COLUMN public_share_token VARCHAR(64) UNIQUE;
ALTER TABLE dashboards ADD COLUMN share_expires_at TIMESTAMP;
ALTER TABLE dashboards ADD COLUMN allow_anonymous_view BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX idx_dashboards_share_token ON dashboards(public_share_token) WHERE public_share_token IS NOT NULL;

-- Dashboard access permissions for specific users
CREATE TABLE dashboard_permissions (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE CASCADE,
    permission_level VARCHAR(20) NOT NULL DEFAULT 'VIEW',  -- VIEW, EDIT, ADMIN
    granted_by BIGINT NOT NULL REFERENCES users(id),
    granted_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    UNIQUE(dashboard_id, user_id)
);

CREATE INDEX idx_dashboard_permissions_dashboard ON dashboard_permissions(dashboard_id);
CREATE INDEX idx_dashboard_permissions_user ON dashboard_permissions(user_id);

-- Dashboard view/access logs for analytics
CREATE TABLE dashboard_access_logs (
    id BIGSERIAL PRIMARY KEY,
    dashboard_id BIGINT NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    access_type VARCHAR(20) NOT NULL,  -- VIEW, EDIT, SHARE, EXPORT
    accessed_via VARCHAR(50),  -- WEB, MOBILE, API, PUBLIC_LINK
    ip_address VARCHAR(45),
    user_agent TEXT,
    accessed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dashboard_access_logs_dashboard ON dashboard_access_logs(dashboard_id);
CREATE INDEX idx_dashboard_access_logs_accessed_at ON dashboard_access_logs(accessed_at);

-- Dashboard templates for quick setup
CREATE TABLE dashboard_templates (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    category VARCHAR(100),  -- ENERGY, INDUSTRIAL, SMART_HOME, AGRICULTURE, etc.
    preview_image_url TEXT,
    template_config JSONB NOT NULL,  -- Full dashboard configuration
    is_featured BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_dashboard_templates_category ON dashboard_templates(category);
CREATE INDEX idx_dashboard_templates_featured ON dashboard_templates(is_featured) WHERE is_featured = true;
