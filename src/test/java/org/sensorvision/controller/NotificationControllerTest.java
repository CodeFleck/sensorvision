package org.sensorvision.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sensorvision.dto.NotificationPreferenceRequest;
import org.sensorvision.model.*;
import org.sensorvision.repository.NotificationLogRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for NotificationController REST API endpoints.
 * Uses MockMvc to test HTTP layer with mocked services.
 */
@WebMvcTest(NotificationController.class)
@ActiveProfiles("test")
class NotificationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private NotificationService notificationService;

    @MockBean
    private NotificationLogRepository notificationLogRepository;

    @MockBean
    private SecurityUtils securityUtils;

    private User testUser;
    private UserNotificationPreference emailPreference;
    private NotificationLog sentLog;

    @BeforeEach
    void setUp() {
        testUser = createTestUser();
        emailPreference = createEmailPreference();
        sentLog = createSentLog();

        // Mock SecurityUtils to return test user
        mockStatic(SecurityUtils.class);
        when(SecurityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetPreferences_ReturnsUserPreferences() throws Exception {
        // Given
        List<UserNotificationPreference> preferences = Arrays.asList(
                emailPreference,
                createSmsPreference()
        );
        when(notificationService.getUserPreferences(any(User.class)))
                .thenReturn(preferences);

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].channel", is("EMAIL")))
                .andExpect(jsonPath("$[0].enabled", is(true)))
                .andExpect(jsonPath("$[0].minSeverity", is("LOW")))
                .andExpect(jsonPath("$[1].channel", is("SMS")));

        verify(notificationService).getUserPreferences(any(User.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetPreferences_ReturnsEmptyListWhenNoPreferences() throws Exception {
        // Given
        when(notificationService.getUserPreferences(any(User.class)))
                .thenReturn(Collections.emptyList());

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSavePreference_CreatesNewPreference() throws Exception {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                NotificationChannel.EMAIL,
                true,
                "custom@example.com",
                AlertSeverity.HIGH,
                true,
                null
        );
        when(notificationService.savePreference(any(UserNotificationPreference.class)))
                .thenReturn(emailPreference);

        // When/Then
        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.channel", is("EMAIL")))
                .andExpect(jsonPath("$.enabled", is(true)))
                .andExpect(jsonPath("$.destination", notNullValue()));

        verify(notificationService).savePreference(any(UserNotificationPreference.class));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSavePreference_WithDigestInterval() throws Exception {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                NotificationChannel.EMAIL,
                true,
                null,
                AlertSeverity.LOW,
                false,
                60 // 60 minute digest
        );
        UserNotificationPreference digestPref = createEmailPreference();
        digestPref.setImmediate(false);
        digestPref.setDigestIntervalMinutes(60);
        when(notificationService.savePreference(any(UserNotificationPreference.class)))
                .thenReturn(digestPref);

        // When/Then
        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.immediate", is(false)))
                .andExpect(jsonPath("$.digestIntervalMinutes", is(60)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testSavePreference_InvalidChannel_ReturnsBadRequest() throws Exception {
        // Given - Invalid JSON with unknown channel
        String invalidJson = """
                {
                    "channel": "INVALID_CHANNEL",
                    "enabled": true,
                    "minSeverity": "LOW"
                }
                """;

        // When/Then
        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(notificationService, never()).savePreference(any());
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeletePreference_Success() throws Exception {
        // Given
        doNothing().when(notificationService)
                .deletePreference(any(User.class), eq(NotificationChannel.EMAIL));

        // When/Then
        mockMvc.perform(delete("/api/v1/notifications/preferences/EMAIL")
                        .with(csrf()))
                .andExpect(status().isNoContent());

        verify(notificationService).deletePreference(any(User.class), eq(NotificationChannel.EMAIL));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testDeletePreference_AllChannels() throws Exception {
        // Test deleting each channel type
        for (NotificationChannel channel : NotificationChannel.values()) {
            doNothing().when(notificationService)
                    .deletePreference(any(User.class), eq(channel));

            mockMvc.perform(delete("/api/v1/notifications/preferences/" + channel.name())
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(notificationService).deletePreference(any(User.class), eq(channel));
        }
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNotificationLogs_ReturnsPaginatedLogs() throws Exception {
        // Given
        Page<NotificationLog> logsPage = new PageImpl<>(
                Collections.singletonList(sentLog),
                PageRequest.of(0, 20),
                1
        );
        when(notificationLogRepository.findByUserOrderByCreatedAtDesc(any(User.class), any()))
                .thenReturn(logsPage);

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/logs")
                        .param("page", "0")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].channel", is("EMAIL")))
                .andExpect(jsonPath("$.content[0].status", is("SENT")))
                .andExpect(jsonPath("$.content[0].subject", notNullValue()))
                .andExpect(jsonPath("$.totalElements", is(1)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNotificationLogs_DefaultPagination() throws Exception {
        // Given
        Page<NotificationLog> emptyPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(0, 20),
                0
        );
        when(notificationLogRepository.findByUserOrderByCreatedAtDesc(any(User.class), any()))
                .thenReturn(emptyPage);

        // When/Then - Should use default page=0, size=20
        mockMvc.perform(get("/api/v1/notifications/logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(0)))
                .andExpect(jsonPath("$.number", is(0)))
                .andExpect(jsonPath("$.size", is(20)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNotificationLogs_CustomPagination() throws Exception {
        // Given
        Page<NotificationLog> customPage = new PageImpl<>(
                Collections.emptyList(),
                PageRequest.of(2, 50),
                100
        );
        when(notificationLogRepository.findByUserOrderByCreatedAtDesc(any(User.class), any()))
                .thenReturn(customPage);

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/logs")
                        .param("page", "2")
                        .param("size", "50"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.number", is(2)))
                .andExpect(jsonPath("$.size", is(50)))
                .andExpect(jsonPath("$.totalElements", is(100)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNotificationStats_ReturnsCorrectCounts() throws Exception {
        // Given
        when(notificationLogRepository.countByUserAndStatus(
                any(User.class), eq(NotificationLog.NotificationStatus.SENT)))
                .thenReturn(45L);
        when(notificationLogRepository.countByUserAndStatus(
                any(User.class), eq(NotificationLog.NotificationStatus.FAILED)))
                .thenReturn(5L);

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(50)))
                .andExpect(jsonPath("$.sent", is(45)))
                .andExpect(jsonPath("$.failed", is(5)));
    }

    @Test
    @WithMockUser(username = "testuser")
    void testGetNotificationStats_NoNotifications() throws Exception {
        // Given
        when(notificationLogRepository.countByUserAndStatus(any(User.class), any()))
                .thenReturn(0L);

        // When/Then
        mockMvc.perform(get("/api/v1/notifications/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total", is(0)))
                .andExpect(jsonPath("$.sent", is(0)))
                .andExpect(jsonPath("$.failed", is(0)));
    }

    @Test
    void testGetPreferences_Unauthorized_Returns401() throws Exception {
        // When/Then - No @WithMockUser annotation
        mockMvc.perform(get("/api/v1/notifications/preferences"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testSavePreference_Unauthorized_Returns401() throws Exception {
        // Given
        NotificationPreferenceRequest request = new NotificationPreferenceRequest(
                NotificationChannel.EMAIL,
                true,
                null,
                AlertSeverity.LOW,
                true,
                null
        );

        // When/Then - No @WithMockUser annotation
        mockMvc.perform(post("/api/v1/notifications/preferences")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    // Helper methods

    private User createTestUser() {
        User user = new User();
        user.setId(1L);
        user.setEmail("test@example.com");
        user.setUsername("testuser");
        return user;
    }

    private UserNotificationPreference createEmailPreference() {
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setId(1L);
        pref.setUser(testUser);
        pref.setChannel(NotificationChannel.EMAIL);
        pref.setEnabled(true);
        pref.setDestination("test@example.com");
        pref.setMinSeverity(AlertSeverity.LOW);
        pref.setImmediate(true);
        pref.setCreatedAt(LocalDateTime.now());
        pref.setUpdatedAt(LocalDateTime.now());
        return pref;
    }

    private UserNotificationPreference createSmsPreference() {
        UserNotificationPreference pref = new UserNotificationPreference();
        pref.setId(2L);
        pref.setUser(testUser);
        pref.setChannel(NotificationChannel.SMS);
        pref.setEnabled(true);
        pref.setDestination("+1234567890");
        pref.setMinSeverity(AlertSeverity.MEDIUM);
        pref.setImmediate(true);
        pref.setCreatedAt(LocalDateTime.now());
        pref.setUpdatedAt(LocalDateTime.now());
        return pref;
    }

    private NotificationLog createSentLog() {
        NotificationLog log = NotificationLog.builder()
                .id(1L)
                .user(testUser)
                .channel(NotificationChannel.EMAIL)
                .destination("test@example.com")
                .subject("Alert: Temperature High")
                .message("Temperature exceeded threshold")
                .status(NotificationLog.NotificationStatus.SENT)
                .sentAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build();
        return log;
    }
}
