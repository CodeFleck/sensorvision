package io.indcloud.repository;

import io.indcloud.model.User;
import io.indcloud.model.WidgetAssistantConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WidgetAssistantConversationRepository extends JpaRepository<WidgetAssistantConversation, UUID> {

    /**
     * Find a conversation by ID with its messages eagerly fetched.
     */
    @Query("SELECT c FROM WidgetAssistantConversation c LEFT JOIN FETCH c.messages WHERE c.id = :id")
    Optional<WidgetAssistantConversation> findByIdWithMessages(@Param("id") UUID id);

    /**
     * Find all conversations for a user on a specific dashboard.
     */
    List<WidgetAssistantConversation> findByUserAndDashboardId(User user, Long dashboardId);

    /**
     * Find conversations that haven't been updated since the cutoff time.
     */
    @Query("SELECT c FROM WidgetAssistantConversation c WHERE c.updatedAt < :cutoff")
    List<WidgetAssistantConversation> findAbandonedConversations(@Param("cutoff") Instant cutoff);

    /**
     * Delete abandoned conversations older than the cutoff time.
     */
    @Modifying
    @Query("DELETE FROM WidgetAssistantConversation c WHERE c.updatedAt < :cutoff")
    int deleteAbandonedConversations(@Param("cutoff") Instant cutoff);

    /**
     * Count active conversations (for monitoring).
     */
    @Query("SELECT COUNT(c) FROM WidgetAssistantConversation c WHERE c.updatedAt > :since")
    long countActiveConversationsSince(@Param("since") Instant since);
}
