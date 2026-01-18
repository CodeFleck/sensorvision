package io.indcloud.controller;

import io.indcloud.dto.*;
import io.indcloud.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.mockito.ArgumentCaptor;

/**
 * Unit tests for AuthController - Registration, Login, and Authentication endpoints
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @InjectMocks
    private AuthController authController;

    private JwtAuthenticationResponse mockAuthResponse;
    private UserResponse mockUserResponse;

    @BeforeEach
    void setUp() {
        // Standard mock auth response (using constructor)
        mockAuthResponse = new JwtAuthenticationResponse(
            "mock-access-token",
            "mock-refresh-token",
            1L,
            "testuser",
            "test@example.com",
            1L
        );

        // Standard mock user response (using builder)
        mockUserResponse = UserResponse.builder()
            .id(1L)
            .username("testuser")
            .email("test@example.com")
            .firstName("Test")
            .lastName("User")
            .organizationId(1L)
            .organizationName("Test Organization")
            .roles(Set.of("ROLE_USER"))
            .build();
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register")
    class RegisterEndpointTests {

        @Test
        @DisplayName("Should register new user with valid data")
        void shouldRegisterNewUserWithValidData() {
            // Given
            RegisterRequest request = createValidRegisterRequest();
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authController.register(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAccessToken()).isEqualTo("mock-access-token");
            assertThat(response.getBody().getRefreshToken()).isEqualTo("mock-refresh-token");

            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should register user without organization name")
        void shouldRegisterUserWithoutOrganizationName() {
            // Given
            RegisterRequest request = createValidRegisterRequest();
            request.setOrganizationName(null); // Optional field
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authController.register(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).register(any(RegisterRequest.class));
        }

        @Test
        @DisplayName("Should pass through registration request to auth service with exact values")
        void shouldPassThroughRegistrationRequestToAuthService() {
            // Given
            RegisterRequest request = createValidRegisterRequest();
            request.setUsername("uniqueuser");
            request.setEmail("unique@example.com");
            request.setPassword("securePassword123");
            request.setFirstName("John");
            request.setLastName("Doe");
            request.setOrganizationName("Acme Corp");

            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            // When
            authController.register(request);

            // Then - Use ArgumentCaptor for precise verification
            ArgumentCaptor<RegisterRequest> captor = ArgumentCaptor.forClass(RegisterRequest.class);
            verify(authService, times(1)).register(captor.capture());

            RegisterRequest captured = captor.getValue();
            assertThat(captured.getUsername()).isEqualTo("uniqueuser");
            assertThat(captured.getEmail()).isEqualTo("unique@example.com");
            assertThat(captured.getPassword()).isEqualTo("securePassword123");
            assertThat(captured.getFirstName()).isEqualTo("John");
            assertThat(captured.getLastName()).isEqualTo("Doe");
            assertThat(captured.getOrganizationName()).isEqualTo("Acme Corp");
        }

        @Test
        @DisplayName("Should propagate exception when registration fails due to duplicate username")
        void shouldPropagateExceptionWhenUsernameExists() {
            // Given
            RegisterRequest request = createValidRegisterRequest();
            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Username already exists"));

            // When/Then
            org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.register(request);
            });
        }

        @Test
        @DisplayName("Should propagate exception when registration fails due to duplicate email")
        void shouldPropagateExceptionWhenEmailExists() {
            // Given
            RegisterRequest request = createValidRegisterRequest();
            when(authService.register(any(RegisterRequest.class)))
                .thenThrow(new RuntimeException("Email already registered"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.register(request);
            });
            assertThat(ex.getMessage()).contains("Email already registered");
        }

        private RegisterRequest createValidRegisterRequest() {
            RegisterRequest request = new RegisterRequest();
            request.setUsername("testuser");
            request.setEmail("test@example.com");
            request.setPassword("password123");
            request.setFirstName("Test");
            request.setLastName("User");
            request.setOrganizationName("Test Organization");
            return request;
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login")
    class LoginEndpointTests {

        @Test
        @DisplayName("Should login user with valid credentials")
        void shouldLoginUserWithValidCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("password123");
            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authController.login(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAccessToken()).isNotNull();
            verify(authService).login(any(LoginRequest.class));
        }

        @Test
        @DisplayName("Should login with email instead of username")
        void shouldLoginWithEmailInsteadOfUsername() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("test@example.com");  // Can also be email
            request.setPassword("password123");
            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authController.login(request);

            // Then - Use ArgumentCaptor for precise verification
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ArgumentCaptor<LoginRequest> captor = ArgumentCaptor.forClass(LoginRequest.class);
            verify(authService, times(1)).login(captor.capture());

            LoginRequest captured = captor.getValue();
            assertThat(captured.getUsername()).isEqualTo("test@example.com");
            assertThat(captured.getPassword()).isEqualTo("password123");
        }

        @Test
        @DisplayName("Should propagate exception for invalid credentials")
        void shouldPropagateExceptionForInvalidCredentials() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("testuser");
            request.setPassword("wrongpassword");
            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Invalid credentials"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.login(request);
            });
            assertThat(ex.getMessage()).contains("Invalid credentials");
        }

        @Test
        @DisplayName("Should propagate exception for locked account")
        void shouldPropagateExceptionForLockedAccount() {
            // Given
            LoginRequest request = new LoginRequest();
            request.setUsername("lockeduser");
            request.setPassword("password123");
            when(authService.login(any(LoginRequest.class)))
                .thenThrow(new RuntimeException("Account is locked"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.login(request);
            });
            assertThat(ex.getMessage()).contains("Account is locked");
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me")
    class GetCurrentUserTests {

        @Test
        @DisplayName("Should return current authenticated user")
        void shouldReturnCurrentAuthenticatedUser() {
            // Given
            when(authService.getCurrentUser()).thenReturn(mockUserResponse);

            // When
            ResponseEntity<UserResponse> response = authController.getCurrentUser();

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUsername()).isEqualTo("testuser");
            assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
            assertThat(response.getBody().getOrganizationName()).isEqualTo("Test Organization");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/refresh")
    class RefreshTokenTests {

        @Test
        @DisplayName("Should refresh access token with valid refresh token")
        void shouldRefreshAccessTokenWithValidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("valid-refresh-token");
            when(authService.refreshToken(eq("valid-refresh-token"))).thenReturn(mockAuthResponse);

            // When
            ResponseEntity<JwtAuthenticationResponse> response = authController.refreshToken(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getAccessToken()).isEqualTo("mock-access-token");
            assertThat(response.getBody().getRefreshToken()).isEqualTo("mock-refresh-token");
            assertThat(response.getBody().getUserId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("Should propagate exception for invalid refresh token")
        void shouldPropagateExceptionForInvalidRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("invalid-token");
            when(authService.refreshToken(eq("invalid-token")))
                .thenThrow(new RuntimeException("Invalid refresh token"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.refreshToken(request);
            });
            assertThat(ex.getMessage()).contains("Invalid refresh token");
        }

        @Test
        @DisplayName("Should propagate exception for expired refresh token")
        void shouldPropagateExceptionForExpiredRefreshToken() {
            // Given
            RefreshTokenRequest request = new RefreshTokenRequest();
            request.setRefreshToken("expired-token");
            when(authService.refreshToken(eq("expired-token")))
                .thenThrow(new RuntimeException("Refresh token has expired"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.refreshToken(request);
            });
            assertThat(ex.getMessage()).contains("Refresh token has expired");
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password")
    class ForgotPasswordTests {

        @Test
        @DisplayName("Should initiate password reset for valid email")
        void shouldInitiatePasswordResetForValidEmail() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@example.com");
            doNothing().when(authService).forgotPassword(eq("test@example.com"));

            // When
            ResponseEntity<Void> response = authController.forgotPassword(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).forgotPassword(eq("test@example.com"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password")
    class ResetPasswordTests {

        @Test
        @DisplayName("Should reset password with valid token")
        void shouldResetPasswordWithValidToken() {
            // Given
            ResetPasswordRequest request = new ResetPasswordRequest();
            request.setToken("valid-reset-token");
            request.setNewPassword("newSecurePassword123");
            doNothing().when(authService).resetPassword(eq("valid-reset-token"), eq("newSecurePassword123"));

            // When
            ResponseEntity<Void> response = authController.resetPassword(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).resetPassword(eq("valid-reset-token"), eq("newSecurePassword123"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/verify-email")
    class VerifyEmailTests {

        @Test
        @DisplayName("Should verify email with valid token")
        void shouldVerifyEmailWithValidToken() {
            // Given
            String token = "valid-verification-token";
            doNothing().when(authService).verifyEmail(eq(token));

            // When
            ResponseEntity<Void> response = authController.verifyEmail(token);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).verifyEmail(eq(token));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/resend-verification")
    class ResendVerificationTests {

        @Test
        @DisplayName("Should resend verification email")
        void shouldResendVerificationEmail() {
            // Given
            ForgotPasswordRequest request = new ForgotPasswordRequest();
            request.setEmail("test@example.com");
            doNothing().when(authService).resendVerificationEmail(eq("test@example.com"));

            // When
            ResponseEntity<Void> response = authController.resendVerificationEmail(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).resendVerificationEmail(eq("test@example.com"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/auth/preferences")
    class UpdatePreferencesTests {

        @Test
        @DisplayName("Should update user preferences with theme setting")
        void shouldUpdateUserPreferencesWithTheme() {
            // Given - UpdateUserPreferencesRequest is a record with (themePreference, emailNotificationsEnabled)
            UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest("dark", true);

            // Configure response with updated preferences
            UserResponse updatedUser = UserResponse.builder()
                .id(1L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .organizationId(1L)
                .organizationName("Test Organization")
                .roles(Set.of("ROLE_USER"))
                .build();

            when(authService.updateUserPreferences(any(UpdateUserPreferencesRequest.class)))
                .thenReturn(updatedUser);

            // When
            ResponseEntity<UserResponse> response = authController.updateUserPreferences(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getUsername()).isEqualTo("testuser");

            // Verify correct request was passed through
            ArgumentCaptor<UpdateUserPreferencesRequest> captor =
                ArgumentCaptor.forClass(UpdateUserPreferencesRequest.class);
            verify(authService, times(1)).updateUserPreferences(captor.capture());

            UpdateUserPreferencesRequest captured = captor.getValue();
            assertThat(captured.themePreference()).isEqualTo("dark");
            assertThat(captured.emailNotificationsEnabled()).isTrue();
        }

        @Test
        @DisplayName("Should update preferences with email notifications disabled")
        void shouldUpdatePreferencesWithEmailNotificationsDisabled() {
            // Given
            UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest("light", false);
            when(authService.updateUserPreferences(any(UpdateUserPreferencesRequest.class)))
                .thenReturn(mockUserResponse);

            // When
            ResponseEntity<UserResponse> response = authController.updateUserPreferences(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

            ArgumentCaptor<UpdateUserPreferencesRequest> captor =
                ArgumentCaptor.forClass(UpdateUserPreferencesRequest.class);
            verify(authService).updateUserPreferences(captor.capture());
            assertThat(captor.getValue().emailNotificationsEnabled()).isFalse();
        }

        @Test
        @DisplayName("Should handle null theme in preferences update")
        void shouldHandleNullThemeInPreferencesUpdate() {
            // Given - null theme is valid (means don't change)
            UpdateUserPreferencesRequest request = new UpdateUserPreferencesRequest(null, true);
            when(authService.updateUserPreferences(any(UpdateUserPreferencesRequest.class)))
                .thenReturn(mockUserResponse);

            // When
            ResponseEntity<UserResponse> response = authController.updateUserPreferences(request);

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            verify(authService).updateUserPreferences(any(UpdateUserPreferencesRequest.class));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/auth/me - Edge Cases")
    class GetCurrentUserEdgeCaseTests {

        @Test
        @DisplayName("Should propagate exception when not authenticated")
        void shouldPropagateExceptionWhenNotAuthenticated() {
            // Given
            when(authService.getCurrentUser())
                .thenThrow(new RuntimeException("Not authenticated"));

            // When/Then
            RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class, () -> {
                authController.getCurrentUser();
            });
            assertThat(ex.getMessage()).contains("Not authenticated");
        }
    }
}
