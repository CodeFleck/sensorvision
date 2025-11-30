package org.sensorvision.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.repository.UserRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for SecurityUtils class.
 * Tests null safety, authentication handling, and organization retrieval.
 */
@ExtendWith(MockitoExtension.class)
class SecurityUtilsTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SecurityUtils securityUtils;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ==================== getCurrentUserOrganization() Tests ====================

    @Test
    void getCurrentUserOrganization_whenUserHasOrganization_shouldReturnOrganization() {
        // Given: A user with a valid organization
        Organization organization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        User user = User.builder()
                .id(1L)
                .username("testuser")
                .organization(organization)
                .build();

        Jwt jwt = createMockJwt("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithOrganizationAndRoles(1L)).thenReturn(Optional.of(user));

        // When
        Organization result = securityUtils.getCurrentUserOrganization();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getName()).isEqualTo("Test Organization");
    }

    @Test
    void getCurrentUserOrganization_whenUserHasNullOrganization_shouldThrowException() {
        // Given: A user without an organization (null)
        User user = User.builder()
                .id(1L)
                .username("testuser")
                .organization(null)  // No organization assigned
                .build();

        Jwt jwt = createMockJwt("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithOrganizationAndRoles(1L)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUserOrganization())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("User does not have an associated organization");
    }

    @Test
    void getCurrentUserOrganization_whenNoAuthentication_shouldThrowException() {
        // Given: No authentication in security context
        when(securityContext.getAuthentication()).thenReturn(null);

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUserOrganization())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No authenticated user found");
    }

    @Test
    void getCurrentUserOrganization_whenNotAuthenticated_shouldThrowException() {
        // Given: Authentication exists but not authenticated
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUserOrganization())
                .isInstanceOf(RuntimeException.class)
                .hasMessage("No authenticated user found");
    }

    // ==================== getCurrentUser() Tests ====================

    @Test
    void getCurrentUser_withJwtAuthentication_shouldReturnUser() {
        // Given: JWT authentication
        Organization organization = Organization.builder().id(1L).name("Test Org").build();
        User user = User.builder()
                .id(1L)
                .username("jwtuser")
                .organization(organization)
                .build();

        Jwt jwt = createMockJwt("jwtuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("jwtuser")).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithOrganizationAndRoles(1L)).thenReturn(Optional.of(user));

        // When
        User result = securityUtils.getCurrentUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("jwtuser");
        verify(userRepository).findByUsername("jwtuser");
        verify(userRepository).findByIdWithOrganizationAndRoles(1L);
    }

    @Test
    void getCurrentUser_withUserPrincipalAuthentication_shouldReturnUser() {
        // Given: UserPrincipal authentication
        Organization organization = Organization.builder().id(1L).name("Test Org").build();
        User user = User.builder()
                .id(2L)
                .username("principaluser")
                .organization(organization)
                .build();

        UserPrincipal userPrincipal = new UserPrincipal(
                2L,
                "principaluser",
                "principal@test.com",
                "password",
                1L,
                true,
                java.util.Collections.emptyList()
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userRepository.findByIdWithOrganizationAndRoles(2L)).thenReturn(Optional.of(user));

        // When
        User result = securityUtils.getCurrentUser();

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getUsername()).isEqualTo("principaluser");
        verify(userRepository).findByIdWithOrganizationAndRoles(2L);
        verify(userRepository, never()).findByUsername(any());
    }

    @Test
    void getCurrentUser_withJwtAndUserNotFound_shouldThrowException() {
        // Given: JWT authentication but user not in database
        Jwt jwt = createMockJwt("nonexistent");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("nonexistent")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with username: nonexistent");
    }

    @Test
    void getCurrentUser_withInvalidPrincipalType_shouldThrowException() {
        // Given: Invalid principal type (String)
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn("invalid-principal");

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUser())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid authentication principal type");
    }

    // ==================== getCurrentUserId() Tests ====================

    @Test
    void getCurrentUserId_shouldReturnUserId() {
        // Given
        Organization organization = Organization.builder().id(1L).name("Test Org").build();
        User user = User.builder()
                .id(42L)
                .username("testuser")
                .organization(organization)
                .build();

        Jwt jwt = createMockJwt("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(user));
        when(userRepository.findByIdWithOrganizationAndRoles(42L)).thenReturn(Optional.of(user));

        // When
        Long result = securityUtils.getCurrentUserId();

        // Then
        assertThat(result).isEqualTo(42L);
    }

    @Test
    void getCurrentUserId_whenUserIdIsNull_shouldThrowIllegalStateException() {
        // Given: A user with null ID (data integrity issue)
        Organization organization = Organization.builder().id(1L).name("Test Org").build();
        User user = User.builder()
                .id(null)  // Null ID - data integrity issue
                .username("testuser")
                .organization(organization)
                .build();

        // Need a temporary ID for the lookup, then return user with null ID
        User lookupUser = User.builder().id(1L).username("testuser").build();

        Jwt jwt = createMockJwt("testuser");
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(jwt);
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(lookupUser));
        when(userRepository.findByIdWithOrganizationAndRoles(1L)).thenReturn(Optional.of(user));

        // When & Then
        assertThatThrownBy(() -> securityUtils.getCurrentUserId())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("data integrity issue");
    }

    // ==================== Helper Methods ====================

    private Jwt createMockJwt(String subject) {
        return new Jwt(
                "test-token-value",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Map.of("alg", "RS256"),
                Map.of("sub", subject)
        );
    }
}
