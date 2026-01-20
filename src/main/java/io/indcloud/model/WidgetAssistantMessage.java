package io.indcloud.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Represents a single message in a Widget Assistant conversation.
 */
@Entity
@Table(name = "widget_assistant_messages")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@EqualsAndHashCode(callSuper = false, of = "id")
@ToString(exclude = {"conversation", "widget"})
public class WidgetAssistantMessage {

    @Id
    @Column(columnDefinition = "uuid")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private WidgetAssistantConversation conversation;

    @Column(nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private MessageRole role;

    @Column(nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "widget_created")
    @Builder.Default
    private Boolean widgetCreated = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "widget_id")
    private Widget widget;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        createdAt = Instant.now();
    }

    public enum MessageRole {
        user,
        assistant,
        system
    }
}
