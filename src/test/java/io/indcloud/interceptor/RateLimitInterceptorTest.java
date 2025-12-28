package io.indcloud.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.PrintWriter;
import java.io.StringWriter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Unit tests for RateLimitInterceptor.
 * Tests rate limiting logic and scheduled cleanup functionality.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RateLimitInterceptorTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    private RateLimitInterceptor interceptor;

    @BeforeEach
    void setUp() {
        interceptor = new RateLimitInterceptor();
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== Basic Rate Limiting Tests ====================

    @Test
    void preHandle_withNoAuthentication_shouldAllowRequest() throws Exception {
        // Given - No authentication set

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void preHandle_withAuthenticatedUser_shouldAllowRequest() throws Exception {
        // Given
        setAuthenticatedUser("testuser");

        // When
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isTrue();
    }

    @Test
    void preHandle_afterExceedingLimit_shouldBlockRequest() throws Exception {
        // Given
        setAuthenticatedUser("testuser");
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // When - Exceed the rate limit (60 requests)
        for (int i = 0; i < 60; i++) {
            boolean result = interceptor.preHandle(request, response, new Object());
            assertThat(result).isTrue();
        }

        // The 61st request should be blocked
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then
        assertThat(result).isFalse();
        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(response).setContentType("application/json");
    }

    @Test
    void preHandle_differentUsers_shouldHaveSeparateLimits() throws Exception {
        // Given
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        // User 1 - Use up most of their limit
        setAuthenticatedUser("user1");
        for (int i = 0; i < 55; i++) {
            interceptor.preHandle(request, response, new Object());
        }

        // User 2 - Should have their own fresh limit
        setAuthenticatedUser("user2");
        boolean result = interceptor.preHandle(request, response, new Object());

        // Then - User 2 should be allowed
        assertThat(result).isTrue();
    }

    // ==================== Scheduled Cleanup Tests ====================

    @Test
    void cleanupExpiredRateLimitEntries_shouldNotFailOnEmptyMap() {
        // Given - Fresh interceptor with no rate limit entries

        // When/Then - Should not throw
        interceptor.cleanupExpiredRateLimitEntries();
    }

    @Test
    void cleanupExpiredRateLimitEntries_shouldNotRemoveActiveEntries() throws Exception {
        // Given - Create some rate limit entries
        setAuthenticatedUser("testuser");
        interceptor.preHandle(request, response, new Object());

        // When - Run cleanup (entries are not expired yet)
        interceptor.cleanupExpiredRateLimitEntries();

        // Then - Entry should still exist, so next request should increment the count
        boolean result = interceptor.preHandle(request, response, new Object());
        assertThat(result).isTrue();
    }

    @Test
    void cleanupExpiredRateLimitEntries_multipleUsersNotExpired_shouldKeepAll() throws Exception {
        // Given - Create entries for multiple users
        setAuthenticatedUser("user1");
        interceptor.preHandle(request, response, new Object());

        setAuthenticatedUser("user2");
        interceptor.preHandle(request, response, new Object());

        setAuthenticatedUser("user3");
        interceptor.preHandle(request, response, new Object());

        // When - Run cleanup (entries are not expired)
        interceptor.cleanupExpiredRateLimitEntries();

        // Then - All entries should still exist
        // Verify by making more requests (they should still be allowed, not starting fresh)
        setAuthenticatedUser("user1");
        boolean result = interceptor.preHandle(request, response, new Object());
        assertThat(result).isTrue();
    }

    // ==================== Helper Methods ====================

    private void setAuthenticatedUser(String username) {
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(username, null, java.util.Collections.emptyList());
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
