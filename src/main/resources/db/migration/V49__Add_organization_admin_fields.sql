-- V49: Add admin management fields to organizations table
-- Author: Claude AI
-- Date: 2025-11-11

-- Add description and enabled columns to organizations table
ALTER TABLE organizations
ADD COLUMN IF NOT EXISTS description TEXT,
ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true;

-- Create index on enabled column for faster queries
CREATE INDEX IF NOT EXISTS idx_organizations_enabled ON organizations(enabled);

-- Add comment
COMMENT ON COLUMN organizations.description IS 'Organization description or notes';
COMMENT ON COLUMN organizations.enabled IS 'Whether the organization is active';
