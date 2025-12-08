package org.sensorvision.security;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import org.springframework.scheduling.annotation.Scheduled;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Authentication filter for user API keys.
 * <p>
 * This filter processes requests that use the X-API-Key header
 * to authenticate with a user-level API key (like Ubidots Default Token).
 * <p>
 * User API keys grant access to all devices in the user's organization.
 * <p>
 * Supported format:
 * - X-API-Key: {api-key-value}
 * <p>
 * Note: Bearer tokens in Authorization header are reserved for JWT authentication.
 * <p>
 * This filter runs AFTER the device token filter but BEFORE the JWT filter,
 * allowing user API keys to be processed after device-specific tokens.
 * <p>
 * Security features:
 * - Rate limiting on failed authentication attempts per IP
 * - Async last-used timestamp updates to reduce latency
 */
@Slf4j
@Component
public class UserApiKeyAuthenticationFilter extends OncePerRequestFilter {

    private static final int MAX_FAILED_ATTEMPTS = 10;
    private static final long RATE_LIMIT_WINDOW_MS = 15 * 60 * 1000; // 15 minutes

    private final UserApiKeyService userApiKeyService;
    private final boolean trustProxyHeaders;

    // Rate limiting: track failed attempts per IP
    private final ConcurrentMap<String, RateLimitEntry> failedAttempts = new ConcurrentHashMap<>();

    // Prometheus metrics
    private final Counter authSuccessCounter;
    private final Counter authFailureCounter;
    private final Counter rateLimitedCounter;
    private final Timer authTimer;

    public UserApiKeyAuthenticationFilter(
            UserApiKeyService userApiKeyService,
            MeterRegistry meterRegistry,
            @Value("${security.proxy.trust-forwarded-headers:false}") boolean trustProxyHeaders) {
        this.userApiKeyService = userApiKeyService;
        this.trustProxyHeaders = trustProxyHeaders;

        // Initialize Prometheus metrics
        this.authSuccessCounter = Counter.builder("sensorvision.api_key.auth")
                .tag("result", "success")
                .description("Total successful API key authentications")
                .register(meterRegistry);

        this.authFailureCounter = Counter.builder("sensorvision.api_key.auth")
                .tag("result", "failure")
                .description("Total failed API key authentications")
                .register(meterRegistry);

        this.rateLimitedCounter = Counter.builder("sensorvision.api_key.auth")
                .tag("result", "rate_limited")
                .description("Total requests blocked by rate limiting")
                .register(meterRegistry);

        this.authTimer = Timer.builder("sensorvision.api_key.auth.duration")
                .description("API key authentication duration")
                .register(meterRegistry);

        // Gauge for tracking current rate limit entries
        Gauge.builder("sensorvision.api_key.rate_limit.entries", failedAttempts, ConcurrentMap::size)
                .description("Current number of IPs being tracked for rate limiting")
                .register(meterRegistry);

        if (!trustProxyHeaders) {
            log.info("X-Forwarded-For header trust is DISABLED (default). " +
                    "Set security.proxy.trust-forwarded-headers=true when behind a trusted reverse proxy.");
        } else {
            log.warn("X-Forwarded-For header trust is ENABLED. " +
                    "Ensure your reverse proxy is properly configured to set this header.");
        }
    }

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

            String clientIp = getClientIp(request);

            // Check rate limiting
            if (isRateLimited(clientIp)) {
                log.warn("Rate limited API key authentication attempts from IP: {}", clientIp);
                rateLimitedCounter.increment();
                filterChain.doFilter(request, response);
                return;
            }

            String apiKey = extractApiKeyFromRequest(request);

            if (apiKey != null && isValidApiKeyFormat(apiKey)) {
                // Start timer only when we're actually doing authentication work
                Timer.Sample timerSample = Timer.start();

                try {
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

                        // Update last used timestamp asynchronously to avoid blocking
                        userApiKeyService.updateLastUsedAtAsync(userApiKey.getId());

                        // Clear any failed attempts on successful auth
                        failedAttempts.remove(clientIp);

                        // Record successful authentication metric
                        authSuccessCounter.increment();

                        log.info("API key '{}' authenticated user '{}' (org: {}) from IP {} for {}",
                                userApiKey.getName(),
                                user.getUsername(),
                                user.getOrganization() != null ? user.getOrganization().getName() : "N/A",
                                clientIp,
                                request.getRequestURI());
                    } else {
                        // Track failed attempt for rate limiting
                        recordFailedAttempt(clientIp);

                        // Record failed authentication metric
                        authFailureCounter.increment();

                        log.debug("Invalid user API key provided from IP {} (key value not logged for security)", clientIp);
                    }
                } finally {
                    // Always stop the timer when authentication is attempted
                    timerSample.stop(authTimer);
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
     * Only uses X-API-Key header to avoid conflicts with JWT Bearer tokens.
     */
    private String extractApiKeyFromRequest(HttpServletRequest request) {
        // Only use X-API-Key header for user API keys
        // Bearer tokens in Authorization header are reserved for JWT authentication
        String apiKey = request.getHeader("X-API-Key");
        if (apiKey != null && !apiKey.isBlank()) {
            return apiKey.trim();
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
     * Get the client IP address, handling proxies.
     * <p>
     * SECURITY: X-Forwarded-For header is only trusted when explicitly enabled via
     * security.proxy.trust-forwarded-headers=true. This prevents IP spoofing attacks
     * when the application is not behind a properly configured reverse proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        if (trustProxyHeaders) {
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                // Take the first IP in the chain (original client)
                return xForwardedFor.split(",")[0].trim();
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Check if an IP is rate limited.
     */
    private boolean isRateLimited(String clientIp) {
        RateLimitEntry entry = failedAttempts.get(clientIp);
        if (entry == null) {
            return false;
        }

        // Check if the rate limit window has expired
        if (System.currentTimeMillis() - entry.firstAttemptTime > RATE_LIMIT_WINDOW_MS) {
            failedAttempts.remove(clientIp);
            return false;
        }

        return entry.attempts.get() >= MAX_FAILED_ATTEMPTS;
    }

    /**
     * Record a failed authentication attempt.
     */
    private void recordFailedAttempt(String clientIp) {
        failedAttempts.compute(clientIp, (ip, existing) -> {
            if (existing == null || System.currentTimeMillis() - existing.firstAttemptTime > RATE_LIMIT_WINDOW_MS) {
                return new RateLimitEntry();
            }
            existing.attempts.incrementAndGet();
            return existing;
        });
    }

    /**
     * Scheduled cleanup of expired rate limit entries.
     * Runs every 5 minutes to remove entries whose window has expired.
     * This prevents memory leaks from attackers using many different IPs.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void cleanupExpiredRateLimitEntries() {
        long now = System.currentTimeMillis();
        int removedCount = 0;

        var iterator = failedAttempts.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().firstAttemptTime > RATE_LIMIT_WINDOW_MS) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired rate limit entries", removedCount);
        }
    }

    /**
     * Rate limit tracking entry.
     */
    private static class RateLimitEntry {
        final long firstAttemptTime = System.currentTimeMillis();
        final AtomicInteger attempts = new AtomicInteger(1);
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
