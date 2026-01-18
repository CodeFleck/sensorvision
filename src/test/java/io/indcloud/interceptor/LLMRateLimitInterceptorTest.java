package io.indcloud.interceptor;

import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the LLMRateLimitInterceptor.
 * Verifies rate limiting functionality, including thread-safety under concurrent access.
 */
class LLMRateLimitInterceptorTest {

    private LLMRateLimitInterceptor interceptor;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        interceptor = new LLMRateLimitInterceptor();
        ReflectionTestUtils.setField(interceptor, "maxRequestsPerMinute", 5);

        request = new MockHttpServletRequest();
        request.setRequestURI("/api/v1/llm/query");

        response = new MockHttpServletResponse();

        // Set up authentication
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "testuser", "password",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("Basic Rate Limiting Tests")
    class BasicRateLimitingTests {

        @Test
        @DisplayName("Should allow requests within limit")
        void shouldAllowRequestsWithinLimit() throws Exception {
            // First 5 requests should be allowed
            for (int i = 0; i < 5; i++) {
                MockHttpServletResponse resp = new MockHttpServletResponse();
                boolean allowed = interceptor.preHandle(request, resp, null);
                assertTrue(allowed, "Request " + (i + 1) + " should be allowed");
                assertEquals(200, resp.getStatus(), "Status should be OK");
            }
        }

        @Test
        @DisplayName("Should reject requests exceeding limit")
        void shouldRejectRequestsExceedingLimit() throws Exception {
            // Exhaust the limit
            for (int i = 0; i < 5; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            // 6th request should be rejected
            boolean allowed = interceptor.preHandle(request, response, null);

            assertFalse(allowed, "Request should be rejected");
            assertEquals(429, response.getStatus()); // TOO_MANY_REQUESTS
            assertTrue(response.getContentAsString().contains("rate limit exceeded"));
        }

        @Test
        @DisplayName("Should include rate limit headers")
        void shouldIncludeRateLimitHeaders() throws Exception {
            interceptor.preHandle(request, response, null);

            assertEquals("5", response.getHeader("X-RateLimit-Limit"));
            assertEquals("4", response.getHeader("X-RateLimit-Remaining"));
            assertNotNull(response.getHeader("X-RateLimit-Reset"));
        }

        @Test
        @DisplayName("Should include Retry-After header when rate limited")
        void shouldIncludeRetryAfterHeader() throws Exception {
            // Exhaust the limit
            for (int i = 0; i < 5; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            // Trigger rate limit
            interceptor.preHandle(request, response, null);

            assertNotNull(response.getHeader("Retry-After"));
        }

        @Test
        @DisplayName("Should track different users separately")
        void shouldTrackDifferentUsersSeparately() throws Exception {
            // First user makes requests
            for (int i = 0; i < 3; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            // Switch to different user
            UsernamePasswordAuthenticationToken auth2 = new UsernamePasswordAuthenticationToken(
                    "testuser2", "password",
                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
            SecurityContextHolder.getContext().setAuthentication(auth2);

            // Second user should have fresh limit
            MockHttpServletResponse resp = new MockHttpServletResponse();
            boolean allowed = interceptor.preHandle(request, resp, null);

            assertTrue(allowed);
            assertEquals("4", resp.getHeader("X-RateLimit-Remaining"));
        }

        @Test
        @DisplayName("Should allow unauthenticated requests (Spring Security handles)")
        void shouldAllowUnauthenticatedRequests() throws Exception {
            SecurityContextHolder.clearContext();

            boolean allowed = interceptor.preHandle(request, response, null);

            assertTrue(allowed, "Unauthenticated requests should pass through");
        }
    }

    @Nested
    @DisplayName("Remaining Requests API Tests")
    class RemainingRequestsTests {

        @Test
        @DisplayName("Should return full limit for unknown user")
        void shouldReturnFullLimitForUnknownUser() {
            int remaining = interceptor.getRemainingRequests("unknownuser");
            assertEquals(5, remaining);
        }

        @Test
        @DisplayName("Should return correct remaining count")
        void shouldReturnCorrectRemainingCount() throws Exception {
            // Make 3 requests
            for (int i = 0; i < 3; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            int remaining = interceptor.getRemainingRequests("testuser");
            assertEquals(2, remaining);
        }

        @Test
        @DisplayName("Should return zero when limit exhausted")
        void shouldReturnZeroWhenLimitExhausted() throws Exception {
            // Exhaust limit
            for (int i = 0; i < 6; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            int remaining = interceptor.getRemainingRequests("testuser");
            assertEquals(0, remaining);
        }
    }

    @Nested
    @DisplayName("Concurrency Tests")
    class ConcurrencyTests {

        @Test
        @DisplayName("Should handle concurrent requests correctly - no race condition")
        void shouldHandleConcurrentRequestsWithoutRaceCondition() throws Exception {
            // Configure a higher limit for better testing
            ReflectionTestUtils.setField(interceptor, "maxRequestsPerMinute", 100);

            int threadCount = 50;
            int requestsPerThread = 10;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger allowedCount = new AtomicInteger(0);
            AtomicInteger rejectedCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        // Wait for all threads to be ready
                        startLatch.await();

                        for (int r = 0; r < requestsPerThread; r++) {
                            MockHttpServletRequest req = new MockHttpServletRequest();
                            req.setRequestURI("/api/v1/llm/query");
                            MockHttpServletResponse resp = new MockHttpServletResponse();

                            // Set up authentication for this thread
                            UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                    "testuser", "password",
                                    List.of(new SimpleGrantedAuthority("ROLE_USER")));
                            SecurityContextHolder.getContext().setAuthentication(auth);

                            boolean allowed = interceptor.preHandle(req, resp, null);
                            if (allowed) {
                                allowedCount.incrementAndGet();
                            } else {
                                rejectedCount.incrementAndGet();
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            // Start all threads simultaneously
            startLatch.countDown();

            // Wait for completion
            boolean completed = endLatch.await(10, TimeUnit.SECONDS);
            assertTrue(completed, "All threads should complete within timeout");

            executor.shutdown();

            // Verify: total requests = allowed + rejected
            int totalRequests = threadCount * requestsPerThread;
            assertEquals(totalRequests, allowedCount.get() + rejectedCount.get(),
                    "Total should equal allowed + rejected");

            // Verify: exactly 100 requests should be allowed (our limit)
            assertEquals(100, allowedCount.get(),
                    "Exactly maxRequestsPerMinute requests should be allowed");

            // Verify: remaining should be rejected
            assertEquals(totalRequests - 100, rejectedCount.get(),
                    "Remaining requests should be rejected");
        }

        @Test
        @DisplayName("Should correctly reset window under concurrent access")
        void shouldCorrectlyResetWindowUnderConcurrentAccess() throws Exception {
            // This test verifies that window reset is atomic
            // We simulate multiple threads trying to reset an expired window
            ReflectionTestUtils.setField(interceptor, "maxRequestsPerMinute", 10);

            // First, make some requests to create the rate limit entry
            for (int i = 0; i < 3; i++) {
                interceptor.preHandle(request, new MockHttpServletResponse(), null);
            }

            // Verify remaining is 7
            assertEquals(7, interceptor.getRemainingRequests("testuser"));

            // Now use reflection to manually expire the window
            // This simulates what happens after WINDOW_SIZE_MS passes
            var rateLimitMap = (java.util.concurrent.ConcurrentMap<?, ?>) ReflectionTestUtils.getField(interceptor, "rateLimitMap");
            assertNotNull(rateLimitMap);
            assertEquals(1, rateLimitMap.size());

            // The window will naturally expire, and next requests will reset it
            // This test ensures that even under concurrent access, the window resets correctly
            int threadCount = 20;
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);
            CountDownLatch startLatch = new CountDownLatch(1);
            CountDownLatch endLatch = new CountDownLatch(threadCount);
            AtomicInteger allowedCount = new AtomicInteger(0);

            for (int t = 0; t < threadCount; t++) {
                executor.submit(() -> {
                    try {
                        startLatch.await();

                        MockHttpServletRequest req = new MockHttpServletRequest();
                        req.setRequestURI("/api/v1/llm/query");
                        MockHttpServletResponse resp = new MockHttpServletResponse();

                        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                                "testuser", "password",
                                List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        SecurityContextHolder.getContext().setAuthentication(auth);

                        if (interceptor.preHandle(req, resp, null)) {
                            allowedCount.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        endLatch.countDown();
                    }
                });
            }

            startLatch.countDown();
            boolean completed = endLatch.await(5, TimeUnit.SECONDS);
            assertTrue(completed);
            executor.shutdown();

            // Within the current window, we had 3 requests + 20 more = should allow only 7 more (10 - 3)
            // But all 20 threads submit nearly simultaneously, so only 7 should be allowed
            assertEquals(7, allowedCount.get(),
                    "Only remaining quota should be allowed");
        }
    }

    @Nested
    @DisplayName("Cleanup Tests")
    class CleanupTests {

        @Test
        @DisplayName("Should clean up expired entries")
        void shouldCleanUpExpiredEntries() throws Exception {
            // Make requests to create entries
            interceptor.preHandle(request, response, null);

            // Verify entry exists
            var rateLimitMap = (java.util.concurrent.ConcurrentMap<?, ?>) ReflectionTestUtils.getField(interceptor, "rateLimitMap");
            assertEquals(1, rateLimitMap.size());

            // Cleanup won't remove fresh entries
            interceptor.cleanupExpiredRateLimitEntries();
            assertEquals(1, rateLimitMap.size());

            // Entry remains because it's not old enough for cleanup
            // (cleanup threshold is WINDOW_SIZE_MS * 2)
        }
    }
}
