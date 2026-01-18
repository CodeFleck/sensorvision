package io.indcloud.controller;

import io.indcloud.config.TestBeanConfiguration;
import io.indcloud.dto.*;
import io.indcloud.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for AuthController validation using MockMvc.
 * These tests verify that @Valid annotations and Bean Validation constraints
 * are properly enforced at the HTTP layer.
 *
 * Uses @SpringBootTest with addFilters=false to load full context but disable
 * security filters, allowing clean validation testing.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
@Import(TestBeanConfiguration.class)
class AuthControllerValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    private JwtAuthenticationResponse mockAuthResponse;

    @BeforeEach
    void setUp() {
        mockAuthResponse = new JwtAuthenticationResponse(
            "mock-access-token",
            "mock-refresh-token",
            1L,
            "testuser",
            "test@example.com",
            1L
        );
    }

    @Nested
    @DisplayName("POST /api/v1/auth/register - Validation Tests")
    class RegisterValidationTests {

        @Test
        @DisplayName("Should return 200 OK for valid registration request")
        void shouldReturn200ForValidRequest() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when username is blank")
        void shouldReturn400WhenUsernameBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when username is null")
        void shouldReturn400WhenUsernameNull() throws Exception {
            String invalidRequest = """
                {
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when username is too short (< 3 chars)")
        void shouldReturn400WhenUsernameTooShort() throws Exception {
            String invalidRequest = """
                {
                    "username": "ab",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid format")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "not-an-email",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is too short (< 6 chars)")
        void shouldReturn400WhenPasswordTooShort() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "12345",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password exceeds max length (> 100 chars)")
        void shouldReturn400WhenPasswordTooLong() throws Exception {
            String longPassword = "a".repeat(101);
            String invalidRequest = String.format("""
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "%s",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """, longPassword);

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when firstName is blank")
        void shouldReturn400WhenFirstNameBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when lastName is blank")
        void shouldReturn400WhenLastNameBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": ""
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept request with optional organizationName")
        void shouldAcceptRequestWithOrganizationName() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User",
                    "organizationName": "Acme Corp"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept minimum valid password length (6 chars)")
        void shouldAcceptMinimumPasswordLength() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "123456",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept maximum valid password length (100 chars)")
        void shouldAcceptMaximumPasswordLength() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            String maxPassword = "a".repeat(100);
            String validRequest = String.format("""
                {
                    "username": "testuser",
                    "email": "test@example.com",
                    "password": "%s",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """, maxPassword);

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept minimum valid username length (3 chars)")
        void shouldAcceptMinimumUsernameLength() throws Exception {
            when(authService.register(any(RegisterRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "abc",
                    "email": "test@example.com",
                    "password": "password123",
                    "firstName": "Test",
                    "lastName": "User"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/login - Validation Tests")
    class LoginValidationTests {

        @Test
        @DisplayName("Should return 200 OK for valid login request")
        void shouldReturn200ForValidLogin() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "password": "password123"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").exists());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when username is blank")
        void shouldReturn400WhenUsernameBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "",
                    "password": "password123"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when password is blank")
        void shouldReturn400WhenPasswordBlank() throws Exception {
            String invalidRequest = """
                {
                    "username": "testuser",
                    "password": ""
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should accept login with rememberMe flag set to true")
        void shouldAcceptLoginWithRememberMeTrue() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "password": "password123",
                    "rememberMe": true
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should accept login with rememberMe flag set to false")
        void shouldAcceptLoginWithRememberMeFalse() throws Exception {
            when(authService.login(any(LoginRequest.class))).thenReturn(mockAuthResponse);

            String validRequest = """
                {
                    "username": "testuser",
                    "password": "password123",
                    "rememberMe": false
                }
                """;

            mockMvc.perform(post("/api/v1/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/forgot-password - Validation Tests")
    class ForgotPasswordValidationTests {

        @Test
        @DisplayName("Should return 200 OK for valid forgot password request")
        void shouldReturn200ForValidRequest() throws Exception {
            String validRequest = """
                {
                    "email": "test@example.com"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is invalid")
        void shouldReturn400WhenEmailInvalid() throws Exception {
            String invalidRequest = """
                {
                    "email": "not-an-email"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when email is blank")
        void shouldReturn400WhenEmailBlank() throws Exception {
            String invalidRequest = """
                {
                    "email": ""
                }
                """;

            mockMvc.perform(post("/api/v1/auth/forgot-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/auth/reset-password - Validation Tests")
    class ResetPasswordValidationTests {

        @Test
        @DisplayName("Should return 200 OK for valid reset password request")
        void shouldReturn200ForValidRequest() throws Exception {
            String validRequest = """
                {
                    "token": "valid-reset-token",
                    "newPassword": "newPassword123"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(validRequest))
                .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when token is blank")
        void shouldReturn400WhenTokenBlank() throws Exception {
            String invalidRequest = """
                {
                    "token": "",
                    "newPassword": "newPassword123"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("Should return 400 Bad Request when newPassword is too short")
        void shouldReturn400WhenNewPasswordTooShort() throws Exception {
            String invalidRequest = """
                {
                    "token": "valid-token",
                    "newPassword": "12345"
                }
                """;

            mockMvc.perform(post("/api/v1/auth/reset-password")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(invalidRequest))
                .andExpect(status().isBadRequest());
        }
    }
}
