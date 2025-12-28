package io.indcloud.repository;

import io.indcloud.model.UserPhoneNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for UserPhoneNumber entities.
 * Supports phone number management, verification, and primary phone lookup.
 */
@Repository
public interface UserPhoneNumberRepository extends JpaRepository<UserPhoneNumber, UUID> {

    /**
     * Find all phone numbers for a specific user
     */
    List<UserPhoneNumber> findByUserId(Long userId);

    /**
     * Find the primary phone number for a user
     */
    Optional<UserPhoneNumber> findByUserIdAndIsPrimaryTrue(Long userId);

    /**
     * Find all verified phone numbers for a user
     */
    List<UserPhoneNumber> findByUserIdAndVerifiedTrue(Long userId);

    /**
     * Find all enabled phone numbers for a user
     */
    List<UserPhoneNumber> findByUserIdAndEnabledTrue(Long userId);

    /**
     * Find specific phone number for a user
     */
    Optional<UserPhoneNumber> findByUserIdAndPhoneNumber(Long userId, String phoneNumber);

    /**
     * Check if phone number exists for user
     */
    boolean existsByUserIdAndPhoneNumber(Long userId, String phoneNumber);

    /**
     * Delete all phone numbers for a user
     */
    void deleteByUserId(Long userId);
}
