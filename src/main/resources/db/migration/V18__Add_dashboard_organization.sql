-- Add organization support to dashboards for multi-tenancy
-- This migration is idempotent - safe to run even if schema already exists

-- Add organization_id column only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'dashboards' AND column_name = 'organization_id'
    ) THEN
        ALTER TABLE dashboards ADD COLUMN organization_id BIGINT;

        -- Set organization_id for existing dashboards to first organization (for migration)
        UPDATE dashboards
        SET organization_id = (SELECT id FROM organizations ORDER BY id LIMIT 1)
        WHERE organization_id IS NULL;

        -- Now make it NOT NULL
        ALTER TABLE dashboards ALTER COLUMN organization_id SET NOT NULL;
    END IF;
END $$;

-- Add foreign key constraint only if it doesn't exist
-- Note: If a constraint with a different name exists, this will add another one
-- In production, you'd check for ANY FK constraint on this column
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE constraint_name = 'fk_dashboards_organization' AND table_name = 'dashboards'
    ) THEN
        -- Only add if there's no FK constraint on organization_id at all
        IF NOT EXISTS (
            SELECT 1 FROM information_schema.constraint_column_usage ccu
            JOIN information_schema.table_constraints tc ON tc.constraint_name = ccu.constraint_name
            WHERE tc.table_name = 'dashboards'
              AND ccu.column_name = 'organization_id'
              AND tc.constraint_type = 'FOREIGN KEY'
        ) THEN
            ALTER TABLE dashboards ADD CONSTRAINT fk_dashboards_organization
                FOREIGN KEY (organization_id) REFERENCES organizations(id) ON DELETE CASCADE;
        END IF;
    END IF;
END $$;

-- Add index only if it doesn't exist
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE tablename = 'dashboards' AND indexname = 'idx_dashboards_organization_id'
    ) THEN
        -- Check if ANY index exists on organization_id
        IF NOT EXISTS (
            SELECT 1 FROM pg_indexes
            WHERE tablename = 'dashboards'
              AND indexdef LIKE '%organization_id%'
        ) THEN
            CREATE INDEX idx_dashboards_organization_id ON dashboards(organization_id);
        END IF;
    END IF;
END $$;
