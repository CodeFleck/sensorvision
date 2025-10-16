-- User notification preferences
CREATE TABLE user_notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    destination VARCHAR(255),
    min_severity VARCHAR(20) NOT NULL DEFAULT 'LOW',
    immediate BOOLEAN NOT NULL DEFAULT TRUE,
    digest_interval_minutes INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_user_channel UNIQUE (user_id, channel)
);

-- Notification logs for tracking sent notifications
CREATE TABLE notification_logs (
    id BIGSERIAL PRIMARY KEY,
    alert_id UUID REFERENCES alerts(id) ON DELETE SET NULL,
    user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel VARCHAR(20) NOT NULL,
    destination VARCHAR(255) NOT NULL,
    subject VARCHAR(255),
    message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    error_message TEXT,
    sent_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for efficient querying
CREATE INDEX idx_notification_preferences_user ON user_notification_preferences(user_id);
CREATE INDEX idx_notification_preferences_channel ON user_notification_preferences(channel);
CREATE INDEX idx_notification_logs_alert ON notification_logs(alert_id);
CREATE INDEX idx_notification_logs_user ON notification_logs(user_id);
CREATE INDEX idx_notification_logs_status ON notification_logs(status);
CREATE INDEX idx_notification_logs_created ON notification_logs(created_at DESC);
