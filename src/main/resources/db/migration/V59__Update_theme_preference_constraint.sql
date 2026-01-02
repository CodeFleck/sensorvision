-- V59: Update theme_preference constraint to allow all theme variants
-- Previous constraint only allowed: light, dark, system
-- Now allows: light, dark, dark-dimmed, dark-high-contrast, light-luxury, dark-luxury, system

-- Drop the existing constraint
ALTER TABLE users DROP CONSTRAINT IF EXISTS check_theme_preference;

-- Add updated constraint with all valid themes
ALTER TABLE users ADD CONSTRAINT check_theme_preference
CHECK (theme_preference IN ('light', 'dark', 'dark-dimmed', 'dark-high-contrast', 'light-luxury', 'dark-luxury', 'system'));
