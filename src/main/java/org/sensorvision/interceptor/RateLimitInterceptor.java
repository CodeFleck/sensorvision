package org.sensorvision.interceptor;

import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.config.RateLimitConfig;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final RateLimitConfig rateLimitConfig;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!rateLimitConfig.isEnabled()) {
            return true;
        }

        String key = getClientIdentifier(request);
        String endpoint = request.getRequestURI();

        Bucket bucket = rateLimitConfig.resolveBucket(key, endpoint);
        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.addHeader("X-Rate-Limit-Remaining", String.valueOf(probe.getRemainingTokens()));
            return true;
        } else {
            long waitForRefill = probe.getNanosToWaitForRefill() / 1_000_000_000;

            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.addHeader("X-Rate-Limit-Retry-After-Seconds", String.valueOf(waitForRefill));

            String jsonResponse = String.format(
                "{\"error\":\"Too Many Requests\",\"message\":\"Rate limit exceeded. Please try again in %d seconds.\",\"retryAfter\":%d}",
                waitForRefill, waitForRefill
            );
            response.getWriter().write(jsonResponse);

            log.warn("Rate limit exceeded for client: {} on endpoint: {}", key, endpoint);
            return false;
        }
    }

    private String getClientIdentifier(HttpServletRequest request) {
        // Try to get authenticated user first
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated() &&
            !"anonymousUser".equals(authentication.getPrincipal())) {
            return "user:" + authentication.getName();
        }

        // Fall back to IP address for unauthenticated requests
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            // X-Forwarded-For can contain multiple IPs, take the first one
            clientIp = clientIp.split(",")[0].trim();
        }

        return "ip:" + clientIp;
    }
}
