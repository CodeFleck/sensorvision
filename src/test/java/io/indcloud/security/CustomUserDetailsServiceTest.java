package io.indcloud.security;

import io.indcloud.model.Organization;
import io.indcloud.model.Role;
import io.indcloud.model.User;
import io.indcloud.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.time.Instant;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Tests")
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService customUserDetailsService;

    private User testUser;
    private Organization testOrganization;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        testUser = User.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .passwordHash("$2a$10$hashedpassword")
                .organization(testOrganization)
                .enabled(true)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();
    }

    @Nested
    @DisplayName("loadUserByUsername")
    class LoadUserByUsername {

        @Test
        @DisplayName("should find user by username")
        void shouldFindUserByUsername() {
            when(userRepository.findByUsernameOrEmailIgnoreCase("testuser"))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserByUsername("testuser");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).findByUsernameOrEmailIgnoreCase("testuser");
        }

        @Test
        @DisplayName("should find user by email")
        void shouldFindUserByEmail() {
            when(userRepository.findByUsernameOrEmailIgnoreCase("test@example.com"))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserByUsername("test@example.com");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
            verify(userRepository).findByUsernameOrEmailIgnoreCase("test@example.com");
        }

        @Test
        @DisplayName("should find user by email case-insensitively")
        void shouldFindUserByEmailCaseInsensitive() {
            when(userRepository.findByUsernameOrEmailIgnoreCase("TEST@EXAMPLE.COM"))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserByUsername("TEST@EXAMPLE.COM");

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should throw generic error when user not found")
        void shouldThrowGenericErrorWhenUserNotFound() {
            when(userRepository.findByUsernameOrEmailIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("nonexistent"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("should throw generic error for deleted user to prevent enumeration")
        void shouldThrowGenericErrorForDeletedUser() {
            testUser.setDeletedAt(Instant.now());
            when(userRepository.findByUsernameOrEmailIgnoreCase("testuser"))
                    .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("testuser"))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessage("Invalid credentials");
        }

        @Test
        @DisplayName("should return UserPrincipal with correct authorities")
        void shouldReturnUserPrincipalWithCorrectAuthorities() {
            when(userRepository.findByUsernameOrEmailIgnoreCase("testuser"))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserByUsername("testuser");

            assertThat(result).isInstanceOf(UserPrincipal.class);
            UserPrincipal userPrincipal = (UserPrincipal) result;
            assertThat(userPrincipal.getId()).isEqualTo(1L);
            assertThat(userPrincipal.getEmail()).isEqualTo("test@example.com");
            assertThat(userPrincipal.getOrganizationId()).isEqualTo(1L);
            assertThat(userPrincipal.getAuthorities())
                    .extracting("authority")
                    .contains("ROLE_USER");
        }

        @Test
        @DisplayName("should handle email with special characters")
        void shouldHandleEmailWithSpecialCharacters() {
            testUser.setEmail("user+tag@example.com");
            when(userRepository.findByUsernameOrEmailIgnoreCase("user+tag@example.com"))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserByUsername("user+tag@example.com");

            assertThat(result).isNotNull();
            verify(userRepository).findByUsernameOrEmailIgnoreCase("user+tag@example.com");
        }

        @Test
        @DisplayName("should use single query to prevent timing attacks")
        void shouldUseSingleQueryToPreventTimingAttacks() {
            when(userRepository.findByUsernameOrEmailIgnoreCase(anyString()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserByUsername("anyinput"))
                    .isInstanceOf(UsernameNotFoundException.class);

            // Verify only ONE database query is made (prevents timing attack)
            verify(userRepository, times(1)).findByUsernameOrEmailIgnoreCase(anyString());
            // Verify the old sequential methods are never called
            verify(userRepository, never()).findByUsername(anyString());
            verify(userRepository, never()).findByEmail(anyString());
        }
    }

    @Nested
    @DisplayName("loadUserById")
    class LoadUserById {

        @Test
        @DisplayName("should find user by ID")
        void shouldFindUserById() {
            when(userRepository.findByIdWithOrganizationAndRoles(1L))
                    .thenReturn(Optional.of(testUser));

            UserDetails result = customUserDetailsService.loadUserById(1L);

            assertThat(result).isNotNull();
            assertThat(result.getUsername()).isEqualTo("testuser");
        }

        @Test
        @DisplayName("should throw exception when user not found by ID")
        void shouldThrowExceptionWhenUserNotFoundById() {
            when(userRepository.findByIdWithOrganizationAndRoles(999L))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> customUserDetailsService.loadUserById(999L))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User not found with id: 999");
        }

        @Test
        @DisplayName("should throw exception for deleted user by ID")
        void shouldThrowExceptionForDeletedUserById() {
            testUser.setDeletedAt(Instant.now());
            when(userRepository.findByIdWithOrganizationAndRoles(1L))
                    .thenReturn(Optional.of(testUser));

            assertThatThrownBy(() -> customUserDetailsService.loadUserById(1L))
                    .isInstanceOf(UsernameNotFoundException.class)
                    .hasMessageContaining("User account has been deleted");
        }
    }
}
