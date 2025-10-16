package org.sensorvision.repository;

import org.sensorvision.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.organization LEFT JOIN FETCH u.roles WHERE u.id = :id")
    Optional<User> findByIdWithOrganizationAndRoles(@Param("id") Long id);

    Optional<User> findByPasswordResetToken(String passwordResetToken);

    Optional<User> findByEmailVerificationToken(String emailVerificationToken);
}
