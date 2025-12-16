-- Add UUID column to users table for external identification
-- UUIDs are better for distributed systems and don't leak user count information

-- Step 1: Add the uuid column as nullable first
ALTER TABLE users ADD COLUMN uuid UUID;

-- Step 2: Generate UUIDs for existing users
UPDATE users SET uuid = gen_random_uuid() WHERE uuid IS NULL;

-- Step 3: Make the column NOT NULL and add unique constraint
ALTER TABLE users ALTER COLUMN uuid SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT users_uuid_unique UNIQUE (uuid);

-- Step 4: Create index for faster lookups by UUID
CREATE INDEX idx_users_uuid ON users(uuid);
