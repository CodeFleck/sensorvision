package io.indcloud.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting interceptor for API endpoints.
 * Limits operations to 60 requests per minute per user to prevent abuse
 * while allowing normal interactive UI usage.
 */
@Slf4j
@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Rate limit: 60 requests per minute (1 per second average)
    private static final int MAX_REQUESTS_PER_MINUTE = 60;
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
     * Scheduled cleanup of expired rate limit entries.
     * Runs every 5 minutes to remove entries whose window has expired.
     * This prevents memory buildup from inactive users.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void cleanupExpiredRateLimitEntries() {
        long now = System.currentTimeMillis();
        int removedCount = 0;

        var iterator = rateLimitMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().windowStart.get() > WINDOW_SIZE_MS) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired rate limit entries", removedCount);
        }
    }

    /**
     * Stores rate limit information for a user
     */
    private static class RateLimitInfo {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger requestCount = new AtomicInteger(0);
    }
}
