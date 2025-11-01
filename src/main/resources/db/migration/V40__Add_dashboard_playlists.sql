-- V39: Add dashboard playlists for production floor displays
-- Enables cycling through multiple dashboards on monitors with configurable timing

-- Main playlist table
CREATE TABLE dashboard_playlists (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    created_by_user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,

    -- Sharing capabilities (mirrors dashboard sharing pattern from V13)
    is_public BOOLEAN NOT NULL DEFAULT FALSE,
    public_share_token VARCHAR(64) UNIQUE,
    share_expires_at TIMESTAMP,

    -- Playlist behavior configuration
    loop_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    transition_effect VARCHAR(50) DEFAULT 'fade', -- fade, slide, none

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Playlist items: ordered sequence of dashboards with display durations
CREATE TABLE dashboard_playlist_items (
    id BIGSERIAL PRIMARY KEY,
    playlist_id BIGINT NOT NULL REFERENCES dashboard_playlists(id) ON DELETE CASCADE,
    dashboard_id BIGINT NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,

    -- Order and timing
    position INT NOT NULL, -- 0-based position in sequence
    display_duration_seconds INT NOT NULL DEFAULT 30,

    -- Metadata
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    -- Constraints
    UNIQUE(playlist_id, position),
    CHECK(position >= 0),
    CHECK(display_duration_seconds > 0 AND display_duration_seconds <= 3600) -- Max 1 hour per dashboard
);

-- Indexes for performance
CREATE INDEX idx_playlists_share_token ON dashboard_playlists(public_share_token)
    WHERE public_share_token IS NOT NULL;
CREATE INDEX idx_playlists_created_by ON dashboard_playlists(created_by_user_id);
CREATE INDEX idx_playlist_items_playlist ON dashboard_playlist_items(playlist_id, position);
CREATE INDEX idx_playlist_items_dashboard ON dashboard_playlist_items(dashboard_id);

-- Documentation comments
COMMENT ON TABLE dashboard_playlists IS 'Playlists for cycling through multiple dashboards on production floor displays';
COMMENT ON TABLE dashboard_playlist_items IS 'Ordered sequence of dashboards within a playlist with display timing';
COMMENT ON COLUMN dashboard_playlists.public_share_token IS 'Token for anonymous access to playlist (e.g., production floor monitors)';
COMMENT ON COLUMN dashboard_playlists.transition_effect IS 'Visual transition between dashboards: fade, slide, or none';
COMMENT ON COLUMN dashboard_playlist_items.position IS 'Zero-based order in playlist sequence';
COMMENT ON COLUMN dashboard_playlist_items.display_duration_seconds IS 'How long to display this dashboard before transitioning';

-- Create a sample playlist for testing (optional - can be removed in production)
DO $$
DECLARE
    default_dashboard_id BIGINT;
    sample_playlist_id BIGINT;
BEGIN
    -- Get the default dashboard
    SELECT id INTO default_dashboard_id FROM dashboards WHERE is_default = TRUE LIMIT 1;

    IF default_dashboard_id IS NOT NULL THEN
        -- Create sample playlist
        INSERT INTO dashboard_playlists (name, description, is_public, loop_enabled)
        VALUES (
            'Production Floor - Main Display',
            'Sample playlist for production floor monitoring',
            TRUE,
            TRUE
        ) RETURNING id INTO sample_playlist_id;

        -- Add the default dashboard to the playlist
        INSERT INTO dashboard_playlist_items (playlist_id, dashboard_id, position, display_duration_seconds)
        VALUES (sample_playlist_id, default_dashboard_id, 0, 45);
    END IF;
END $$;
