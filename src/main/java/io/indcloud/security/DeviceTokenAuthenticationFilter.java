package io.indcloud.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Device;
import io.indcloud.service.DeviceTokenService;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;
import java.util.Optional;

/**
 * Authentication filter for device API tokens.
 * <p>
 * This filter intercepts requests with Bearer tokens and checks if they are device API tokens (UUID format).
 * If a valid device token is found, it authenticates the request in the security context.
 * <p>
 * Token format expected: Authorization: Bearer 550e8400-e29b-41d4-a716-446655440000
 * <p>
 * This filter runs before the JWT authentication filter, allowing device tokens to be processed first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceTokenAuthenticationFilter extends OncePerRequestFilter {

    private final DeviceTokenService deviceTokenService;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        try {
            String token = extractTokenFromRequest(request);

            // Only process if we have a token and it looks like a device token (UUID format)
            if (token != null && deviceTokenService.isDeviceToken(token)) {
                // Check if the token is valid and associated with a device
                Optional<Device> deviceOpt = deviceTokenService.getDeviceByToken(token);

                if (deviceOpt.isPresent()) {
                    Device device = deviceOpt.get();

                    // Create authentication object with device information
                    // Principal will be the device's externalId
                    DeviceTokenAuthentication authentication = new DeviceTokenAuthentication(
                            device.getExternalId(),
                            device,
                            Collections.singletonList(new SimpleGrantedAuthority("ROLE_DEVICE"))
                    );

                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // Set authentication in security context
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // Update last used timestamp asynchronously
                    deviceTokenService.updateTokenLastUsed(token);

                    log.debug("Device token authenticated successfully for device: {}", device.getExternalId());
                } else {
                    // Don't log any token characters to prevent token reconstruction attacks
                    log.debug("Invalid device token provided (token not logged for security)");
                }
            }
        } catch (Exception e) {
            log.error("Error processing device token authentication: {}", e.getMessage(), e);
            // Don't block the request, let it continue to other filters
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Extract the Bearer token from the Authorization header
     *
     * @param request The HTTP request
     * @return The token string, or null if not found
     */
    private String extractTokenFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");

        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7); // Remove "Bearer " prefix
        }

        return null;
    }

    /**
     * Custom authentication class for device token authentication
     */
    public static class DeviceTokenAuthentication extends UsernamePasswordAuthenticationToken {
        private final Device device;

        public DeviceTokenAuthentication(
                String principal,
                Device device,
                java.util.Collection<? extends org.springframework.security.core.GrantedAuthority> authorities
        ) {
            super(principal, null, authorities);
            this.device = device;
            // Note: super() 3-arg constructor already sets authenticated=true, no need to set again
        }

        public Device getDevice() {
            return device;
        }

        public Long getOrganizationId() {
            return device.getOrganization() != null ? device.getOrganization().getId() : null;
        }
    }
}
