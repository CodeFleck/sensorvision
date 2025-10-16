-- Organizations table for multi-tenancy
CREATE TABLE organizations (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Users table
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    organization_id BIGINT NOT NULL REFERENCES organizations(id) ON DELETE CASCADE,
    username VARCHAR(100) NOT NULL UNIQUE,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_user_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE
);

-- Roles table
CREATE TABLE roles (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- User-Role join table (many-to-many)
CREATE TABLE user_roles (
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role_id BIGINT NOT NULL REFERENCES roles(id) ON DELETE CASCADE,
    PRIMARY KEY (user_id, role_id)
);

-- Insert default roles
INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN', 'Administrator with full access'),
    ('ROLE_USER', 'Standard user with read/write access'),
    ('ROLE_VIEWER', 'Read-only access');

-- Create indexes
CREATE INDEX idx_users_organization ON users(organization_id);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_username ON users(username);

-- Add organization_id to existing tables for multi-tenancy
ALTER TABLE devices ADD COLUMN organization_id BIGINT;
ALTER TABLE dashboards ADD COLUMN organization_id BIGINT;
ALTER TABLE rules ADD COLUMN organization_id BIGINT;
ALTER TABLE synthetic_variables ADD COLUMN organization_id BIGINT;

-- Create a default organization for existing data
INSERT INTO organizations (name) VALUES ('Default Organization');

-- Update existing records to belong to default organization
UPDATE devices SET organization_id = (SELECT id FROM organizations WHERE name = 'Default Organization');
UPDATE dashboards SET organization_id = (SELECT id FROM organizations WHERE name = 'Default Organization');
UPDATE rules SET organization_id = (SELECT id FROM organizations WHERE name = 'Default Organization');
UPDATE synthetic_variables SET organization_id = (SELECT id FROM organizations WHERE name = 'Default Organization');

-- Make organization_id NOT NULL after data migration
ALTER TABLE devices ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE dashboards ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE rules ALTER COLUMN organization_id SET NOT NULL;
ALTER TABLE synthetic_variables ALTER COLUMN organization_id SET NOT NULL;

-- Add foreign key constraints
ALTER TABLE devices ADD CONSTRAINT fk_device_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE dashboards ADD CONSTRAINT fk_dashboard_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE rules ADD CONSTRAINT fk_rule_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
ALTER TABLE synthetic_variables ADD CONSTRAINT fk_synthetic_variable_organization FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;

-- Create indexes for organization filtering
CREATE INDEX idx_devices_organization ON devices(organization_id);
CREATE INDEX idx_dashboards_organization ON dashboards(organization_id);
CREATE INDEX idx_rules_organization ON rules(organization_id);
CREATE INDEX idx_synthetic_variables_organization ON synthetic_variables(organization_id);
