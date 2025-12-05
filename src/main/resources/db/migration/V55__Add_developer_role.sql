-- Add DEVELOPER role for system logs access
INSERT INTO roles (name, description)
VALUES ('ROLE_DEVELOPER', 'Developer access for viewing system logs and debugging')
ON CONFLICT (name) DO NOTHING;
