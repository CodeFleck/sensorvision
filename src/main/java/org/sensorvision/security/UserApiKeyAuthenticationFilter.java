package org.sensorvision.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Authentication filter for user API keys.
 * <p>
 * This filter processes requests that use the X-API-Key header or Bearer token
 * to authenticate with a user-level API key (like Ubidots Default Token).
 * <p>
 * User API keys grant access to all devices in the user's organization.
 * <p>
 * Supported formats:
 * - X-API-Key: {api-key-value}
 * - Authorization: Bearer {api-key-value}
 * <p>
 * This filter runs AFTER the device token filter but BEFORE the JWT filter,
 * allowing user API keys to be processed after device-specific tokens.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private final UserApiKeyService userApiKeyService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            // Skip if already authenticated (e.g., by device token filter)
            if (SecurityContextHolder.getContext().getAuthentication() != null &&
                SecurityContextHolder.getContext().getAuthentication().isAuthenticated()) {
                filterChain.doFilter(request, response);
                return;
            }

            String apiKey = extractApiKeyFromRequest(request);

            if (apiKey != null && isValidApiKeyFormat(apiKey)) {
                Optional<UserApiKey> userApiKeyOpt = userApiKeyService.validateApiKey(apiKey);

                if (userApiKeyOpt.isPresent()) {
                    UserApiKey userApiKey = userApiKeyOpt.get();
                    User user = userApiKey.getUser();

                    // Create authentication with user's roles
                    UserApiKeyAuthentication authentication = new UserApiKeyAuthentication(
                            user,
                            userApiKey,
                            user.getRoles().stream()
                                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().toUpperCase()))
                                    .collect(Collectors.toList())
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Update last used timestamp asynchronously
                    userApiKeyService.updateLastUsedAt(userApiKey.getId());

                    log.debug("User API key authenticated successfully for user: {} (org: {})",
                            user.getUsername(), user.getOrganization().getName());
                } else {
                    log.debug("Invalid user API key provided: {}...",
                            apiKey.length() > 8 ? apiKey.substring(0, 8) : apiKey);
                }
            }
        } catch (Exception e) {
            log.error("Error processing user API key authentication: {}", e.getMessage(), e);
            // Don't block the request, let it continue to other filters
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract API key from request headers.
     * Checks X-API-Key header first, then falls back to Bearer token.
     */
    private String extractApiKeyFromRequest(HttpServletRequest request) {
        // First check X-API-Key header (preferred for user API keys)
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
        }

        // Fall back to Bearer token in Authorization header
        String bearerToken = request.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7).trim();
        }

        return null;
    }

    /**
     * Check if the API key has a valid UUID format.
     */
    private boolean isValidApiKeyFormat(String apiKey) {
        // UUID format: 8-4-4-4-12 hex characters with dashes
        return apiKey.matches("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
    }

    /**
     * Custom authentication class for user API key authentication.
     * Provides access to the authenticated user and their organization.
     */
    public static class UserApiKeyAuthentication extends UsernamePasswordAuthenticationToken {
        private final User user;
        private final UserApiKey apiKey;

        public UserApiKeyAuthentication(
                User user,
                UserApiKey apiKey,
                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities
        ) {
            super(user.getUsername(), null, authorities);
            this.user = user;
            this.apiKey = apiKey;
        }

        public User getUser() {
            return user;
        }

        public UserApiKey getApiKey() {
            return apiKey;
        }

        public Long getUserId() {
            return user.getId();
        }

        public Long getOrganizationId() {
            return user.getOrganization() != null ? user.getOrganization().getId() : null;
        }

        public String getOrganizationName() {
            return user.getOrganization() != null ? user.getOrganization().getName() : null;
        }
    }
}
