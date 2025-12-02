package org.sensorvision.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.model.UserApiKey;
import org.sensorvision.service.UserApiKeyService;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
 * Tests API key extraction, validation, and authentication.
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

    @InjectMocks
    private UserApiKeyAuthenticationFilter filter;

    private Organization testOrganization;
    private User testUser;
    private UserApiKey testApiKey;

    @BeforeEach
    void setUp() {
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
                .keyValue("550e8400-e29b-41d4-a716-446655440000")
                .name("Default Token")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
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
        when(request.getHeader("Authorization")).thenReturn(null);
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
        verify(userApiKeyService).updateLastUsedAt(1L);
    }

    @Test
    void doFilterInternal_withInvalidXApiKeyHeader_shouldNotAuthenticate() throws Exception {
        // Given
        String invalidKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(invalidKey);
        when(request.getHeader("Authorization")).thenReturn(null);
        when(userApiKeyService.validateApiKey(invalidKey)).thenReturn(Optional.empty());

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        verify(filterChain).doFilter(request, response);
        verify(userApiKeyService, never()).updateLastUsedAt(any());
    }

    // ==================== Bearer Token Tests ====================

    @Test
    void doFilterInternal_withValidBearerToken_shouldAuthenticate() throws Exception {
        // Given
        String apiKey = "550e8400-e29b-41d4-a716-446655440000";
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + apiKey);
        when(userApiKeyService.validateApiKey(apiKey)).thenReturn(Optional.of(testApiKey));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
        assertThat(auth).isInstanceOf(UserApiKeyAuthenticationFilter.UserApiKeyAuthentication.class);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_withInvalidBearerPrefix_shouldNotAuthenticate() throws Exception {
        // Given
        when(request.getHeader("X-API-Key")).thenReturn(null);
        when(request.getHeader("Authorization")).thenReturn("Basic sometoken");

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
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
        when(request.getHeader("Authorization")).thenReturn(null);

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
        when(request.getHeader("Authorization")).thenReturn(null);

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
    }

    // ==================== Invalid Format Tests ====================

    @Test
    void doFilterInternal_withNonUuidFormat_shouldNotAuthenticate() throws Exception {
        // Given - not a UUID format
        String invalidFormat = "not-a-valid-uuid";
        when(request.getHeader("X-API-Key")).thenReturn(invalidFormat);
        when(request.getHeader("Authorization")).thenReturn(null);

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
        when(request.getHeader("Authorization")).thenReturn(null);

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
        when(request.getHeader("Authorization")).thenReturn(null);
        when(userApiKeyService.validateApiKey(apiKey)).thenThrow(new RuntimeException("Database error"));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNull();

        // Filter chain should still continue
        verify(filterChain).doFilter(request, response);
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

    // ==================== X-API-Key Priority Over Bearer Tests ====================

    @Test
    void doFilterInternal_xApiKeyHeader_shouldTakePriorityOverBearer() throws Exception {
        // Given - both headers present, X-API-Key should be used
        String xApiKey = "550e8400-e29b-41d4-a716-446655440000";
        String bearerKey = "660e8400-e29b-41d4-a716-446655440001";
        when(request.getHeader("X-API-Key")).thenReturn(xApiKey);
        when(request.getHeader("Authorization")).thenReturn("Bearer " + bearerKey);
        when(userApiKeyService.validateApiKey(xApiKey)).thenReturn(Optional.of(testApiKey));

        // When
        filter.doFilterInternal(request, response, filterChain);

        // Then
        verify(userApiKeyService).validateApiKey(xApiKey);
        verify(userApiKeyService, never()).validateApiKey(bearerKey);

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth).isNotNull();
    }
}
