-- V72: Widget Assistant Conversations
-- Stores conversation history for the AI Widget Assistant feature

CREATE TABLE widget_assistant_conversations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    organization_id UUID NOT NULL REFERENCES organizations(id),
    dashboard_id BIGINT NOT NULL REFERENCES dashboards(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

CREATE TABLE widget_assistant_messages (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    conversation_id UUID NOT NULL REFERENCES widget_assistant_conversations(id) ON DELETE CASCADE,
    role VARCHAR(20) NOT NULL CHECK (role IN ('user', 'assistant', 'system')),
    content TEXT NOT NULL,
    widget_created BOOLEAN DEFAULT FALSE,
    widget_id BIGINT REFERENCES widgets(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Indexes for efficient querying
CREATE INDEX idx_widget_assistant_conversations_user ON widget_assistant_conversations(user_id);
CREATE INDEX idx_widget_assistant_conversations_dashboard ON widget_assistant_conversations(dashboard_id);
CREATE INDEX idx_widget_assistant_messages_conversation ON widget_assistant_messages(conversation_id);
CREATE INDEX idx_widget_assistant_messages_created ON widget_assistant_messages(created_at);
