package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.JwtAuthenticationResponse;
import org.sensorvision.dto.LoginRequest;
import org.sensorvision.dto.RegisterRequest;
import org.sensorvision.dto.UpdateUserPreferencesRequest;
import org.sensorvision.dto.UserResponse;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.repository.OrganizationRepository;
import org.sensorvision.repository.RoleRepository;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.security.CustomUserDetailsService;
import org.sensorvision.security.JwtService;
import org.sensorvision.security.UserPrincipal;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuthService.
 * Tests authentication, registration, password reset, and email verification flows.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private OrganizationRepository organizationRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private CustomUserDetailsService userDetailsService;

    @Mock
    private EmailNotificationService emailNotificationService;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private UserDetails userDetails;

    @InjectMocks
    private AuthService authService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Organization testOrganization;
    private User testUser;
    private Role userRole;
    private UserPrincipal userPrincipal;

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
                .firstName("Test")
                .lastName("User")
                .organization(testOrganization)
                .enabled(true)
                .emailVerified(false)
                .roles(new HashSet<>(Set.of(userRole)))
                .build();

        Collection<? extends GrantedAuthority> authorities = List.of(
                new SimpleGrantedAuthority("ROLE_USER")
        );

        userPrincipal = new UserPrincipal(
                testUser.getId(),
                testUser.getUsername(),
                testUser.getEmail(),
                testUser.getPasswordHash(),
                testUser.getOrganization().getId(),
                true, // enabled
                authorities
        );
    }

    // ===== LOGIN TESTS =====

    @Test
    void login_shouldAuthenticateUser_whenCredentialsAreValid() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(false);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userDetailsService.loadUserById(anyLong())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(), anyLong(), anyLong())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), anyLong(), anyLong())).thenReturn("refresh-token");

        // When
        JwtAuthenticationResponse response = authService.login(loginRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
        assertThat(response.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(response.getEmail()).isEqualTo(testUser.getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateAccessToken(any(), anyLong(), anyLong());
        verify(jwtService).generateRefreshToken(any(), anyLong(), anyLong());
        verify(jwtService, never()).generateRememberMeRefreshToken(any(), anyLong(), anyLong());
    }

    @Test
    void login_shouldGenerateRememberMeToken_whenRememberMeIsEnabled() {
        // Given
        LoginRequest loginRequest = new LoginRequest();
        loginRequest.setUsername("testuser");
        loginRequest.setPassword("password123");
        loginRequest.setRememberMe(true);

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userDetailsService.loadUserById(anyLong())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(), anyLong(), anyLong())).thenReturn("access-token");
        when(jwtService.generateRememberMeRefreshToken(any(), anyLong(), anyLong())).thenReturn("remember-me-token");

        // When
        JwtAuthenticationResponse response = authService.login(loginRequest);

        // Then
        assertThat(response.getRefreshToken()).isEqualTo("remember-me-token");
        verify(jwtService).generateRememberMeRefreshToken(any(), anyLong(), anyLong());
        verify(jwtService, never()).generateRefreshToken(any(), anyLong(), anyLong());
    }

    // ===== REGISTRATION TESTS =====

    @Test
    void register_shouldCreateNewUser_whenDataIsValid() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setOrganizationName("New Organization");

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(organizationRepository.findByName(anyString())).thenReturn(Optional.empty());
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userDetailsService.loadUserById(anyLong())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(), anyLong(), anyLong())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), anyLong(), anyLong())).thenReturn("refresh-token");

        // When
        JwtAuthenticationResponse response = authService.register(registerRequest);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");

        verify(userRepository).existsByUsername("newuser");
        verify(userRepository).existsByEmail("new@example.com");
        verify(passwordEncoder).encode("password123");
        verify(userRepository).save(any(User.class));
        verify(organizationRepository).save(any(Organization.class));
    }

    @Test
    void register_shouldThrowException_whenUsernameAlreadyExists() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("existinguser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setOrganizationName(null);

        when(userRepository.existsByUsername("existinguser")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldThrowException_whenEmailAlreadyExists() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("existing@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setOrganizationName(null);

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail("existing@example.com")).thenReturn(true);

        // When/Then
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email is already in use");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void register_shouldCreateUniqueOrganization_whenNoOrganizationNameProvided() {
        // Given
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("new@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");
        registerRequest.setOrganizationName(null);  // No organization name

        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(organizationRepository.save(any(Organization.class))).thenReturn(testOrganization);
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$10$encoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userPrincipal);
        when(userDetailsService.loadUserById(anyLong())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(), anyLong(), anyLong())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any(), anyLong(), anyLong())).thenReturn("refresh-token");

        // When
        authService.register(registerRequest);

        // Then
        verify(organizationRepository).save(argThat(org ->
                org.getName().equals("newuser's Organization")
        ));
    }

    // ===== PASSWORD RESET TESTS =====

    @Test
    void forgotPassword_shouldGenerateResetToken_whenEmailExists() {
        // Given
        String email = "test@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.forgotPassword(email);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordResetToken()).isNotNull();
        assertThat(savedUser.getPasswordResetTokenExpiry()).isAfter(LocalDateTime.now());
        verify(emailNotificationService).sendPasswordResetEmail(eq(email), anyString());
    }

    @Test
    void forgotPassword_shouldThrowException_whenEmailNotFound() {
        // Given
        String email = "nonexistent@example.com";
        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.forgotPassword(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User not found with email");

        verify(emailNotificationService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void resetPassword_shouldUpdatePassword_whenTokenIsValid() {
        // Given
        String resetToken = "valid-reset-token";
        String newPassword = "newPassword123";
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.encode(newPassword)).thenReturn("$2a$10$newencoded");
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.resetPassword(resetToken, newPassword);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getPasswordHash()).isEqualTo("$2a$10$newencoded");
        assertThat(savedUser.getPasswordResetToken()).isNull();
        assertThat(savedUser.getPasswordResetTokenExpiry()).isNull();
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsExpired() {
        // Given
        String resetToken = "expired-token";
        testUser.setPasswordResetToken(resetToken);
        testUser.setPasswordResetTokenExpiry(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByPasswordResetToken(resetToken)).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.resetPassword(resetToken, "newPassword"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Password reset token has expired");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void resetPassword_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid-token";
        when(userRepository.findByPasswordResetToken(invalidToken)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.resetPassword(invalidToken, "newPassword"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid password reset token");
    }

    // ===== EMAIL VERIFICATION TESTS =====

    @Test
    void verifyEmail_shouldMarkEmailAsVerified_whenTokenIsValid() {
        // Given
        String verificationToken = "valid-verification-token";
        testUser.setEmailVerificationToken(verificationToken);
        testUser.setEmailVerificationTokenExpiry(LocalDateTime.now().plusHours(24));
        testUser.setEmailVerified(false);

        when(userRepository.findByEmailVerificationToken(verificationToken))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.verifyEmail(verificationToken);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmailVerified()).isTrue();
        assertThat(savedUser.getEmailVerificationToken()).isNull();
        assertThat(savedUser.getEmailVerificationTokenExpiry()).isNull();
    }

    @Test
    void verifyEmail_shouldThrowException_whenTokenIsExpired() {
        // Given
        String expiredToken = "expired-verification-token";
        testUser.setEmailVerificationToken(expiredToken);
        testUser.setEmailVerificationTokenExpiry(LocalDateTime.now().minusHours(1)); // Expired

        when(userRepository.findByEmailVerificationToken(expiredToken))
                .thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.verifyEmail(expiredToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email verification token has expired");
    }

    @Test
    void resendVerificationEmail_shouldGenerateNewToken_whenEmailNotVerified() {
        // Given
        String email = "test@example.com";
        testUser.setEmailVerified(false);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.resendVerificationEmail(email);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmailVerificationToken()).isNotNull();
        assertThat(savedUser.getEmailVerificationTokenExpiry()).isAfter(LocalDateTime.now());
        verify(emailNotificationService).sendVerificationEmail(eq(email), anyString());
    }

    @Test
    void resendVerificationEmail_shouldThrowException_whenEmailAlreadyVerified() {
        // Given
        String email = "test@example.com";
        testUser.setEmailVerified(true);

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(testUser));

        // When/Then
        assertThatThrownBy(() -> authService.resendVerificationEmail(email))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Email already verified");

        verify(emailNotificationService, never()).sendVerificationEmail(anyString(), anyString());
    }

    // ===== REFRESH TOKEN TESTS =====

    @Test
    void refreshToken_shouldGenerateNewTokens_whenRefreshTokenIsValid() {
        // Given
        String refreshToken = "valid-refresh-token";

        when(jwtService.validateToken(refreshToken)).thenReturn(true);
        when(jwtService.isRefreshToken(refreshToken)).thenReturn(true);
        when(jwtService.getUsernameFromToken(refreshToken)).thenReturn("testuser");
        when(userRepository.findByUsername("testuser")).thenReturn(Optional.of(testUser));
        when(userRepository.findByIdWithOrganizationAndRoles(anyLong())).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserById(anyLong())).thenReturn(userDetails);
        when(jwtService.generateAccessToken(any(), anyLong(), anyLong())).thenReturn("new-access-token");
        when(jwtService.generateRefreshToken(any(), anyLong(), anyLong())).thenReturn("new-refresh-token");

        // When
        JwtAuthenticationResponse response = authService.refreshToken(refreshToken);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getUserId()).isEqualTo(testUser.getId());
    }

    @Test
    void refreshToken_shouldThrowException_whenTokenIsInvalid() {
        // Given
        String invalidToken = "invalid-token";
        when(jwtService.validateToken(invalidToken)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(invalidToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void refreshToken_shouldThrowException_whenTokenIsNotRefreshToken() {
        // Given
        String accessToken = "access-token";
        when(jwtService.validateToken(accessToken)).thenReturn(true);
        when(jwtService.isRefreshToken(accessToken)).thenReturn(false);

        // When/Then
        assertThatThrownBy(() -> authService.refreshToken(accessToken))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Token is not a refresh token");
    }

    // ===== USER PREFERENCES TESTS =====

    @Test
    void updateUserPreferences_shouldUpdateEmailNotifications_whenUserPrincipalAuthentication() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(null, false);

        // Setup authentication with UserPrincipal
        UserPrincipal userPrincipal = new UserPrincipal(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getEmail(),
            testUser.getPasswordHash(),
            testUser.getOrganization().getId(),
            true,  // enabled
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userPrincipal);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByIdWithOrganizationAndRoles(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        UserResponse response = authService.updateUserPreferences(request);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmailNotificationsEnabled()).isFalse();
        assertThat(response).isNotNull();
    }

    @Test
    void updateUserPreferences_shouldUpdateEmailNotifications_whenOnlyEmailNotificationProvided() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(null, true);

        UserPrincipal userPrincipal = new UserPrincipal(
            testUser.getId(),
            testUser.getUsername(),
            testUser.getEmail(),
            testUser.getPasswordHash(),
            testUser.getOrganization().getId(),
            true,  // enabled
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userPrincipal);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.findByIdWithOrganizationAndRoles(testUser.getId())).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        authService.updateUserPreferences(request);

        // Then
        verify(userRepository).save(userCaptor.capture());
        User savedUser = userCaptor.getValue();

        assertThat(savedUser.getEmailNotificationsEnabled()).isTrue();
    }

    @Test
    void updateUserPreferences_shouldThrowException_whenUserNotFound() {
        // Given
        UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(null, true);

        UserPrincipal userPrincipal = new UserPrincipal(
            999L,  // Non-existent user
            "nonexistent",
            "nonexistent@test.com",
            "hash",
            1L,
            true,  // enabled
            List.of(new SimpleGrantedAuthority("ROLE_USER"))
        );

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userPrincipal);

        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(securityContext);

        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> authService.updateUserPreferences(request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found");

        verify(userRepository, never()).save(any(User.class));
    }
}
