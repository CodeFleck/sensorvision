package org.sensorvision.repository;

import org.sensorvision.model.UserApiKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserApiKeyRepository extends JpaRepository<UserApiKey, Long> {

    /**
     * Find an active API key by its value (for authentication).
     * Eagerly fetches the user and organization for authentication context.
     */
    @Query("SELECT k FROM UserApiKey k " +
           "JOIN FETCH k.user u " +
           "JOIN FETCH u.organization " +
           "WHERE k.keyValue = :keyValue AND k.revokedAt IS NULL")
    Optional<UserApiKey> findActiveByKeyValue(@Param("keyValue") String keyValue);

    /**
     * Find all API keys for a user (including revoked ones).
     */
    @Query("SELECT k FROM UserApiKey k WHERE k.user.id = :userId ORDER BY k.createdAt DESC")
    List<UserApiKey> findAllByUserId(@Param("userId") Long userId);

    /**
     * Find all active API keys for a user.
     */
    @Query("SELECT k FROM UserApiKey k WHERE k.user.id = :userId AND k.revokedAt IS NULL ORDER BY k.createdAt DESC")
    List<UserApiKey> findActiveByUserId(@Param("userId") Long userId);

    /**
     * Check if a user has any active API keys.
     */
    @Query("SELECT COUNT(k) > 0 FROM UserApiKey k WHERE k.user.id = :userId AND k.revokedAt IS NULL")
    boolean hasActiveKeys(@Param("userId") Long userId);

    /**
     * Update the last used timestamp for an API key.
     */
    @Modifying
    @Query("UPDATE UserApiKey k SET k.lastUsedAt = :timestamp WHERE k.id = :keyId")
    void updateLastUsedAt(@Param("keyId") Long keyId, @Param("timestamp") LocalDateTime timestamp);

    /**
     * Check if a key value already exists.
     */
    boolean existsByKeyValue(String keyValue);
}
