package org.sensorvision.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting interceptor for plugin endpoints.
 * Limits plugin operations to 10 requests per minute per user to prevent abuse.
 */
@Slf4j
public class RateLimitInterceptor implements HandlerInterceptor {

    // Rate limit: 10 requests per minute
    private static final int MAX_REQUESTS_PER_MINUTE = 10;
    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    // Map of username -> RateLimitInfo
    private final ConcurrentMap<String, RateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true; // Let Spring Security handle auth
        }

        String username = auth.getName();
        RateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(username, k -> new RateLimitInfo());

        long now = System.currentTimeMillis();
        long windowStart = rateLimitInfo.windowStart.get();

        // Reset window if expired
        if (now - windowStart > WINDOW_SIZE_MS) {
            rateLimitInfo.windowStart.set(now);
            rateLimitInfo.requestCount.set(0);
        }

        // Increment and check
        int currentCount = rateLimitInfo.requestCount.incrementAndGet();

        if (currentCount > MAX_REQUESTS_PER_MINUTE) {
            log.warn("Rate limit exceeded for user: {} (attempt: {})", username, currentCount);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(String.format(
                "{\"error\": \"Rate limit exceeded\", \"message\": \"Maximum %d plugin operations per minute. Please try again later.\"}",
                MAX_REQUESTS_PER_MINUTE
            ));
            return false;
        }

        return true;
    }

    /**
     * Stores rate limit information for a user
     */
    private static class RateLimitInfo {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger requestCount = new AtomicInteger(0);
    }
}
