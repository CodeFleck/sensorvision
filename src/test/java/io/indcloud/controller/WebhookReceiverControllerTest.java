package io.indcloud.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.model.Organization;
import io.indcloud.model.PluginExecution;
import io.indcloud.model.PluginExecutionStatus;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.DataPluginService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for WebhookReceiverController.
 * Tests authorization and organization validation.
 */
@ExtendWith(MockitoExtension.class)
class WebhookReceiverControllerTest {

    @Mock
    private DataPluginService pluginService;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private WebhookReceiverController controller;

    private Organization testOrganization;
    private Organization otherOrganization;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        otherOrganization = new Organization();
        otherOrganization.setId(2L);
        otherOrganization.setName("Other Organization");
    }

    @Test
    void receiveWebhook_withMatchingOrganization_shouldSucceed() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);

        PluginExecution execution = new PluginExecution();
        execution.setId(1L);
        execution.setStatus(PluginExecutionStatus.SUCCESS);
        execution.setRecordsProcessed(5);
        execution.setDurationMs(100L);

        when(pluginService.executePluginByName(eq(1L), eq("test-plugin"), any()))
                .thenReturn(execution);

        // When
        ResponseEntity<Map<String, Object>> response = controller.receiveWebhook(
                1L, "test-plugin", "{\"data\": \"test\"}");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
        verify(pluginService).executePluginByName(eq(1L), eq("test-plugin"), any());
    }

    @Test
    void receiveWebhook_withMismatchedOrganization_shouldThrowAccessDenied() {
        // Given - user belongs to org 2 but tries to access org 1
        when(securityUtils.getCurrentUserOrganization()).thenReturn(otherOrganization);

        // When/Then
        assertThatThrownBy(() -> controller.receiveWebhook(1L, "test-plugin", "{}"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to organization: 1");

        verify(pluginService, never()).executePluginByName(any(), any(), any());
    }

    @Test
    void receiveWebhook_withNullOrganization_shouldThrowAccessDenied() {
        // Given - user has no organization
        when(securityUtils.getCurrentUserOrganization()).thenReturn(null);

        // When/Then
        assertThatThrownBy(() -> controller.receiveWebhook(1L, "test-plugin", "{}"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to organization: 1");

        verify(pluginService, never()).executePluginByName(any(), any(), any());
    }

    @Test
    void testWebhook_withMatchingOrganization_shouldSucceed() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);

        PluginExecution execution = new PluginExecution();
        execution.setId(1L);
        execution.setStatus(PluginExecutionStatus.SUCCESS);
        execution.setRecordsProcessed(0);
        execution.setDurationMs(50L);

        when(pluginService.executePluginByName(eq(1L), eq("test-plugin"), eq("{}")))
                .thenReturn(execution);

        // When
        ResponseEntity<Map<String, Object>> response = controller.testWebhook(1L, "test-plugin");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(true);
    }

    @Test
    void testWebhook_withMismatchedOrganization_shouldThrowAccessDenied() {
        // Given - user belongs to org 2 but tries to test plugin in org 1
        when(securityUtils.getCurrentUserOrganization()).thenReturn(otherOrganization);

        // When/Then
        assertThatThrownBy(() -> controller.testWebhook(1L, "test-plugin"))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied to organization: 1");

        verify(pluginService, never()).executePluginByName(any(), any(), any());
    }

    @Test
    void receiveWebhook_withPluginNotFound_shouldReturnNotFound() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(pluginService.executePluginByName(eq(1L), eq("nonexistent"), any()))
                .thenThrow(new IllegalArgumentException("Plugin not found"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.receiveWebhook(
                1L, "nonexistent", "{}");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void receiveWebhook_withPluginError_shouldReturnSanitizedError() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(pluginService.executePluginByName(eq(1L), eq("broken"), any()))
                .thenThrow(new IllegalStateException("Database connection failed: password=secret"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.receiveWebhook(
                1L, "broken", "{}");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsKey("error");
        // CRITICAL: Error message should be sanitized, not exposing internal details
        assertThat(response.getBody().get("error")).isEqualTo("Plugin execution failed");
        assertThat(response.getBody().get("error").toString()).doesNotContain("Database");
        assertThat(response.getBody().get("error").toString()).doesNotContain("password");
    }

    @Test
    void receiveWebhook_withUnexpectedError_shouldReturnInternalError() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(pluginService.executePluginByName(eq(1L), eq("test-plugin"), any()))
                .thenThrow(new RuntimeException("Database connection failed"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.receiveWebhook(
                1L, "test-plugin", "{}");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        // Error message should be generic, not exposing internal details
        assertThat(response.getBody().get("error")).isEqualTo("Internal server error");
    }

    @Test
    void testWebhook_withExecutionError_shouldNotLeakDetails() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(pluginService.executePluginByName(eq(1L), eq("test-plugin"), eq("{}")))
                .thenThrow(new RuntimeException("Database connection failed: password=secret123"));

        // When
        ResponseEntity<Map<String, Object>> response = controller.testWebhook(1L, "test-plugin");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("success")).isEqualTo(false);
        assertThat(response.getBody().get("message")).isEqualTo("Plugin exists but configuration may need adjustment");
        // CRITICAL: Response should NOT contain the error key with internal details
        assertThat(response.getBody()).doesNotContainKey("error");
    }
}
