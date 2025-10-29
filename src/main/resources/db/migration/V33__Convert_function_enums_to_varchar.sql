-- Migration V33: Convert function enum types to VARCHAR for Hibernate compatibility
-- Fixes issue where Hibernate @Enumerated(EnumType.STRING) doesn't work well with PostgreSQL custom enums

-- Convert runtime column to VARCHAR
ALTER TABLE serverless_functions
ALTER COLUMN runtime TYPE VARCHAR(50) USING runtime::text;

-- Convert trigger_type column to VARCHAR
ALTER TABLE function_triggers
ALTER COLUMN trigger_type TYPE VARCHAR(50) USING trigger_type::text;

-- Drop the now-unused enum types
DROP TYPE IF EXISTS function_runtime CASCADE;
DROP TYPE IF EXISTS function_trigger_type CASCADE;

-- Add check constraints to ensure valid values
ALTER TABLE serverless_functions
ADD CONSTRAINT check_runtime CHECK (runtime IN ('PYTHON_3_11', 'NODEJS_18'));

ALTER TABLE function_triggers
ADD CONSTRAINT check_trigger_type CHECK (trigger_type IN ('HTTP', 'MQTT', 'SCHEDULED', 'DEVICE_EVENT'));
