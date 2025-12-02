package org.sensorvision.security;

import jakarta.servlet.FilterChain;
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
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UserApiKeyAuthenticationFilter.
 * Tests API key extraction, validation, authentication, and rate limiting.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class UserApiKeyAuthenticationFilterTest {

    @Mock
    private UserApiKeyService userApiKeyService;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    private UserApiKeyAuthenticationFilter filter;

    private Organization testOrganization;
    private User testUser;
    private UserApiKey testApiKey;

    @BeforeEach
    void setUp() {
        filter = new UserApiKeyAuthenticationFilter(userApiKeyService);
        SecurityContextHolder.clearContext();

        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        Role userRole = new Role();
        userRole.setId(1L);
        userRole.setName("USER");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("testuser@example.com")
                .organization(testOrganization)
                .roles(Set.of(userRole))
                .build();

        testApiKey = UserApiKey.builder()
                .id(1L)
                .user(testUser)
                .keyPrefix("550e8400")
                .keyHash("$2a$10$hashedvalue")
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        // Default mock for remote address
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== X-API-Key Header Tests ====================

    @Test
    void doFilterInternal_withValidXApiKeyHeader_shouldAuthenticate() throws Exception {
        // Given
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(userApiKeyService.validateApiKey(apiKey)).thenReturn(Optional.of(testApiKey));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth).isInstanceOf(UserApiKeyAuthenticationFilter.UserApiKeyAuthentication.class);

        UserApiKeyAuthenticationFilter.UserApiKeyAuthentication apiKeyAuth =
                (UserApiKeyAuthenticationFilter.UserApiKeyAuthentication) auth;
        assertThat(apiKeyAuth.getUserId()).isEqualTo(1L);
        assertThat(apiKeyAuth.getOrganizationId()).isEqualTo(1L);
        assertThat(apiKeyAuth.getAuthorities()).hasSize(1);

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService).updateLastUsedAtAsync(1L);
    }

    @Test
    void doFilterInternal_withInvalidXApiKeyHeader_shouldNotAuthenticate() throws Exception {
        // Given
        String invalidKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(invalidKey);
        when(userApiKeyService.validateApiKey(invalidKey)).thenReturn(Optional.empty());

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).updateLastUsedAtAsync(any());
    }

    // ==================== Bearer Token No Longer Supported ====================

    @Test
    void doFilterInternal_withBearerToken_shouldNotAuthenticate() throws Exception {
        // Given - Bearer tokens are now reserved for JWT, not user API keys
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then - Should not authenticate via Bearer
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    // ==================== No API Key Tests ====================

    @Test
    void doFilterInternal_withNoApiKey_shouldContinueChain() throws Exception {
        // Given
        when(request.getHeader("X-API-Key")).thenReturn(null);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    @Test
    void doFilterInternal_withBlankApiKey_shouldContinueChain() throws Exception {
        // Given
        when(request.getHeader("X-API-Key")).thenReturn("   ");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    // ==================== Already Authenticated Tests ====================

    @Test
    void doFilterInternal_whenAlreadyAuthenticated_shouldSkipProcessing() throws Exception {
        // Given - already authenticated
        Authentication existingAuth = mock(Authentication.class);
        when(existingAuth.isAuthenticated()).thenReturn(true);
        SecurityContextHolder.getContext().setAuthentication(existingAuth);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isEqualTo(existingAuth); // Same auth, not replaced

        verify(filterChain).doFilter(request, response);
        // Service shouldn't be called since we're already authenticated
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    // ==================== Invalid Format Tests ====================

    @Test
    void doFilterInternal_withNonUuidFormat_shouldNotAuthenticate() throws Exception {
        // Given - not a UUID format
        String invalidFormat = "not-a-valid-uuid";
        when(request.getHeader("X-API-Key")).thenReturn(invalidFormat);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    @Test
    void doFilterInternal_withJwtToken_shouldNotAuthenticate() throws Exception {
        // Given - JWT token instead of API key
        String jwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U";
        when(request.getHeader("X-API-Key")).thenReturn(jwtToken);

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    // ==================== Exception Handling Tests ====================

    @Test
    void doFilterInternal_whenServiceThrows_shouldContinueChain() throws Exception {
        // Given
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(userApiKeyService.validateApiKey(apiKey)).thenThrow(new RuntimeException("Database error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        // Filter chain should still continue
        verify(filterChain).doFilter(request, response);
    }

    // ==================== Rate Limiting Tests ====================

    @Test
    void doFilterInternal_afterManyFailedAttempts_shouldRateLimit() throws Exception {
        // Given - simulate 10 failed attempts
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(userApiKeyService.validateApiKey(apiKey)).thenReturn(Optional.empty());

        // Simulate 10 failed attempts to trigger rate limiting
        for (int i = 0; i < 10; i++) {
            filter.doFilterInternal(request, response, filterChain);
        }

        // The 11th attempt should be rate limited
        reset(userApiKeyService);
        filter.doFilterInternal(request, response, filterChain);

        // Then - service should not be called due to rate limiting
        verify(userApiKeyService, never()).validateApiKey(any());
    }

    @Test
    void doFilterInternal_withXForwardedFor_shouldUseRealClientIp() throws Exception {
        // Given
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(apiKey);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.100, 10.0.0.1");
        when(request.getRequestURI()).thenReturn("/api/v1/test");
        when(userApiKeyService.validateApiKey(apiKey)).thenReturn(Optional.of(testApiKey));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then - should authenticate successfully
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
    }

    // ==================== UserApiKeyAuthentication Class Tests ====================

    @Test
    void userApiKeyAuthentication_shouldProvideAccessToUserAndOrg() {
        // Given
        UserApiKeyAuthenticationFilter.UserApiKeyAuthentication auth =
                new UserApiKeyAuthenticationFilter.UserApiKeyAuthentication(
                        testUser,
                        testApiKey,
                        java.util.Collections.emptyList()
                );

        // Then
        assertThat(auth.getUser()).isEqualTo(testUser);
        assertThat(auth.getApiKey()).isEqualTo(testApiKey);
        assertThat(auth.getUserId()).isEqualTo(1L);
        assertThat(auth.getOrganizationId()).isEqualTo(1L);
        assertThat(auth.getOrganizationName()).isEqualTo("Test Organization");
        assertThat(auth.getPrincipal()).isEqualTo("testuser");
        assertThat(auth.isAuthenticated()).isTrue();
    }

    @Test
    void userApiKeyAuthentication_withNullOrganization_shouldReturnNull() {
        // Given
        User userWithoutOrg = User.builder()
                .id(2L)
                .username("noorguser")
                .organization(null)
                .roles(Set.of())
                .build();

        UserApiKeyAuthenticationFilter.UserApiKeyAuthentication auth =
                new UserApiKeyAuthenticationFilter.UserApiKeyAuthentication(
                        userWithoutOrg,
                        testApiKey,
                        java.util.Collections.emptyList()
                );

        // Then
        assertThat(auth.getOrganizationId()).isNull();
        assertThat(auth.getOrganizationName()).isNull();
    }
}
