-- Add missing updated_at column to synthetic_variable_values table
ALTER TABLE synthetic_variable_values
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW();