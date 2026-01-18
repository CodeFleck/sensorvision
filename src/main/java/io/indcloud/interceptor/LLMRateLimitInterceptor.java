package io.indcloud.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Rate limiting interceptor specifically for LLM endpoints.
 * Enforces stricter limits than general API endpoints due to:
 * - Cost: LLM API calls incur real costs
 * - Performance: LLM calls are slower and resource-intensive
 * - Abuse prevention: Prevents token exhaustion attacks
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LLMRateLimitInterceptor implements HandlerInterceptor {

    /**
     * Maximum LLM requests per minute per user.
     * Default is 20, which allows reasonable usage while preventing abuse.
     */
    @Value("${llm.rate-limit.requests-per-minute:20}")
    private int maxRequestsPerMinute;

    private static final long WINDOW_SIZE_MS = 60_000; // 1 minute

    // Map of username -> RateLimitInfo
    private final ConcurrentMap<String, LLMRateLimitInfo> rateLimitMap = new ConcurrentHashMap<>();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) {
            return true; // Let Spring Security handle auth
        }

        String username = auth.getName();
        String endpoint = request.getRequestURI();
        LLMRateLimitInfo rateLimitInfo = rateLimitMap.computeIfAbsent(username, k -> new LLMRateLimitInfo());

        long now = System.currentTimeMillis();
        int currentCount;
        long windowStart;

        // Atomic window check and increment using synchronized block
        // This prevents the race condition where two threads could both see an expired window
        // and reset it, causing incorrect counting
        synchronized (rateLimitInfo) {
            windowStart = rateLimitInfo.windowStart.get();

            // Reset window if expired
            if (now - windowStart > WINDOW_SIZE_MS) {
                rateLimitInfo.windowStart.set(now);
                rateLimitInfo.requestCount.set(0);
                windowStart = now; // Update local variable to reflect the reset
            }

            // Increment request count atomically within the synchronized block
            currentCount = rateLimitInfo.requestCount.incrementAndGet();
        }

        if (currentCount > maxRequestsPerMinute) {
            log.warn("LLM rate limit exceeded for user: {} on endpoint: {} (requests: {})",
                    username, endpoint, currentCount);

            long retryAfterSeconds = Math.max(1, (windowStart + WINDOW_SIZE_MS - now) / 1000);

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
            response.setHeader("X-RateLimit-Remaining", "0");
            response.setHeader("X-RateLimit-Reset", String.valueOf((windowStart + WINDOW_SIZE_MS) / 1000));
            response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

            response.getWriter().write(String.format(
                    "{\"error\": \"LLM rate limit exceeded\", " +
                            "\"message\": \"Maximum %d LLM requests per minute. Please try again in %d seconds.\", " +
                            "\"retryAfter\": %d}",
                    maxRequestsPerMinute, retryAfterSeconds, retryAfterSeconds
            ));
            return false;
        }

        // Add rate limit headers for informational purposes
        response.setHeader("X-RateLimit-Limit", String.valueOf(maxRequestsPerMinute));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(maxRequestsPerMinute - currentCount));
        response.setHeader("X-RateLimit-Reset", String.valueOf((windowStart + WINDOW_SIZE_MS) / 1000));

        log.debug("LLM request {} of {} for user: {} on endpoint: {}",
                currentCount, maxRequestsPerMinute, username, endpoint);

        return true;
    }

    /**
     * Scheduled cleanup of expired rate limit entries.
     * Runs every 5 minutes to remove entries whose window has expired.
     */
    @Scheduled(fixedRate = 300_000) // Every 5 minutes
    public void cleanupExpiredRateLimitEntries() {
        long now = System.currentTimeMillis();
        int removedCount = 0;

        var iterator = rateLimitMap.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (now - entry.getValue().windowStart.get() > WINDOW_SIZE_MS * 2) {
                iterator.remove();
                removedCount++;
            }
        }

        if (removedCount > 0) {
            log.debug("Cleaned up {} expired LLM rate limit entries", removedCount);
        }
    }

    /**
     * Get remaining requests for a user (for UI display).
     */
    public int getRemainingRequests(String username) {
        LLMRateLimitInfo info = rateLimitMap.get(username);
        if (info == null) {
            return maxRequestsPerMinute;
        }

        long now = System.currentTimeMillis();
        if (now - info.windowStart.get() > WINDOW_SIZE_MS) {
            return maxRequestsPerMinute;
        }

        return Math.max(0, maxRequestsPerMinute - info.requestCount.get());
    }

    /**
     * Stores rate limit information for a user
     */
    private static class LLMRateLimitInfo {
        final AtomicLong windowStart = new AtomicLong(System.currentTimeMillis());
        final AtomicInteger requestCount = new AtomicInteger(0);
    }
}
