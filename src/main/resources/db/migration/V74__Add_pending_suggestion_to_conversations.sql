-- V74: Add pending_suggestion column to widget_assistant_conversations
-- Stores the pending widget suggestion JSON for confirmation

ALTER TABLE widget_assistant_conversations
    ADD COLUMN pending_suggestion TEXT;

COMMENT ON COLUMN widget_assistant_conversations.pending_suggestion IS 'JSON representation of pending widget suggestion awaiting user confirmation';
