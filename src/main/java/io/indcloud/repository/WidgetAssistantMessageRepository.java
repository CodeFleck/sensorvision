package io.indcloud.repository;

import io.indcloud.model.WidgetAssistantMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface WidgetAssistantMessageRepository extends JpaRepository<WidgetAssistantMessage, UUID> {

    /**
     * Find all messages for a conversation ordered by creation time.
     */
    List<WidgetAssistantMessage> findByConversationIdOrderByCreatedAtAsc(UUID conversationId);

    /**
     * Count messages in a conversation.
     */
    @Query("SELECT COUNT(m) FROM WidgetAssistantMessage m WHERE m.conversation.id = :conversationId")
    int countByConversationId(@Param("conversationId") UUID conversationId);
}
