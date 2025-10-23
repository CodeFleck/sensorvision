package org.sensorvision.security;

import lombok.RequiredArgsConstructor;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

/**
 * Utility service for retrieving authenticated user information from Spring Security context.
 * This bean should be injected into services and controllers that need access to the current user.
 */
@Component
@RequiredArgsConstructor
public class SecurityUtils {

    private final UserRepository userRepository;

    /**
     * Get the currently authenticated user from Spring Security context.
     * @return The authenticated User entity with eagerly loaded associations
     * @throws RuntimeException if no user is authenticated or user not found
     */
    public User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("No authenticated user found");
        }

        User user;

        // Handle OAuth2 JWT authentication (for requests with JWT token)
        if (authentication.getPrincipal() instanceof Jwt) {
            Jwt jwt = (Jwt) authentication.getPrincipal();
            String username = jwt.getSubject(); // The 'sub' claim contains the username

            user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new RuntimeException("User not found with username: " + username));

            // Eagerly load associations
            user = userRepository.findByIdWithOrganizationAndRoles(user.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        // Handle UserPrincipal authentication (for login/register responses)
        else if (authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();

            user = userRepository.findByIdWithOrganizationAndRoles(userPrincipal.getId())
                    .orElseThrow(() -> new RuntimeException("User not found"));
        }
        else {
            throw new RuntimeException("Invalid authentication principal type: " +
                    authentication.getPrincipal().getClass().getName());
        }

        return user;
    }

    /**
     * Get the organization of the currently authenticated user.
     * @return The user's Organization entity
     */
    public Organization getCurrentUserOrganization() {
        User user = getCurrentUser();
        return user.getOrganization();
    }

    /**
     * Get the ID of the currently authenticated user.
     * @return The user's ID
     */
    public Long getCurrentUserId() {
        User user = getCurrentUser();
        return user.getId();
    }
}
