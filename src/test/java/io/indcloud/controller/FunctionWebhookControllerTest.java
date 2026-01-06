package io.indcloud.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.indcloud.model.FunctionRuntime;
import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import io.indcloud.model.Organization;
import io.indcloud.model.ServerlessFunction;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.FunctionExecutionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for FunctionWebhookController.
 * Tests authorization, organization filtering, and error message sanitization.
 */
@ExtendWith(MockitoExtension.class)
class FunctionWebhookControllerTest {

    @Mock
    private FunctionTriggerRepository triggerRepository;

    @Mock
    private FunctionExecutionService executionService;

    @Mock
    private SecurityUtils securityUtils;

    private FunctionWebhookController controller;
    private ObjectMapper objectMapper;

    private Organization testOrganization;
    private Organization otherOrganization;
    private ServerlessFunction testFunction;
    private FunctionTrigger testTrigger;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        controller = new FunctionWebhookController(
                triggerRepository, executionService, objectMapper, securityUtils);

        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        otherOrganization = new Organization();
        otherOrganization.setId(2L);
        otherOrganization.setName("Other Organization");

        testFunction = new ServerlessFunction();
        testFunction.setId(1L);
        testFunction.setName("test-function");
        testFunction.setEnabled(true);
        testFunction.setOrganization(testOrganization);
        testFunction.setRuntime(FunctionRuntime.PYTHON_3_11);
        testFunction.setCode("def main(event): return {}");

        // Create trigger config with path
        ObjectNode config = JsonNodeFactory.instance.objectNode();
        config.put("path", "my-function");

        testTrigger = new FunctionTrigger();
        testTrigger.setId(1L);
        testTrigger.setFunction(testFunction);
        testTrigger.setTriggerType(FunctionTriggerType.HTTP);
        testTrigger.setTriggerConfig(config);
        testTrigger.setEnabled(true);
    }

    @Test
    void invokeFunction_withMatchingOrgAndPath_shouldSucceed() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("message")).isEqualTo("Function invoked");
        verify(executionService).executeFunctionAsync(eq(testFunction.getId()), any(), eq(testTrigger));
    }

    @Test
    void invokeFunction_withDifferentOrganization_shouldReturnNotFound() {
        // Given - user belongs to org 2 but function belongs to org 1
        when(securityUtils.getCurrentUserOrganization()).thenReturn(otherOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsKey("error");
        assertThat(response.getBody().get("error")).isEqualTo("Function not found");
        verify(executionService, never()).executeFunctionAsync(any(), any(), any());
    }

    @Test
    void invokeFunction_withNullOrganization_shouldReturnNotFound() {
        // Given - user has no organization
        when(securityUtils.getCurrentUserOrganization()).thenReturn(null);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        verify(executionService, never()).executeFunctionAsync(any(), any(), any());
    }

    @Test
    void invokeFunction_withNonexistentPath_shouldReturnNotFound() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "nonexistent-path", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).containsEntry("error", "Function not found");
    }

    @Test
    void invokeFunction_withDisabledFunction_shouldReturnServiceUnavailable() {
        // Given
        testFunction.setEnabled(false);
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(response.getBody()).containsEntry("error", "Function is disabled");
    }

    @Test
    void invokeFunction_withExecutionError_shouldReturnSanitizedErrorMessage() {
        // Given
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger));

        // Simulate an internal error with sensitive information
        doThrow(new RuntimeException("Database connection failed: password=secret123"))
                .when(executionService).executeFunctionAsync(any(), any(), any());

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().get("error")).isEqualTo("Function invocation failed");
        // CRITICAL: Error message should NOT contain the internal error details
        assertThat(response.getBody().get("message")).isEqualTo("An internal error occurred. Please try again later.");
        assertThat(response.getBody().get("message").toString()).doesNotContain("Database");
        assertThat(response.getBody().get("message").toString()).doesNotContain("password");
        assertThat(response.getBody().get("message").toString()).doesNotContain("secret");
    }

    @Test
    void invokeFunction_shouldOnlyShowFunctionsFromUserOrganization() {
        // Given - set up triggers from different organizations
        ServerlessFunction otherOrgFunction = new ServerlessFunction();
        otherOrgFunction.setId(2L);
        otherOrgFunction.setName("other-org-function");
        otherOrgFunction.setEnabled(true);
        otherOrgFunction.setOrganization(otherOrganization);
        otherOrgFunction.setRuntime(FunctionRuntime.PYTHON_3_11);
        otherOrgFunction.setCode("def main(event): return {}");

        ObjectNode otherConfig = JsonNodeFactory.instance.objectNode();
        otherConfig.put("path", "my-function"); // Same path as testTrigger

        FunctionTrigger otherOrgTrigger = new FunctionTrigger();
        otherOrgTrigger.setId(2L);
        otherOrgTrigger.setFunction(otherOrgFunction);
        otherOrgTrigger.setTriggerType(FunctionTriggerType.HTTP);
        otherOrgTrigger.setTriggerConfig(otherConfig);
        otherOrgTrigger.setEnabled(true);

        // User belongs to testOrganization
        when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        when(triggerRepository.findByTriggerTypeAndEnabledTrue(FunctionTriggerType.HTTP))
                .thenReturn(List.of(testTrigger, otherOrgTrigger));

        Map<String, String> headers = new HashMap<>();

        // When
        ResponseEntity<Map<String, Object>> response = controller.invokeFunction(
                "my-function", null, headers);

        // Then - should invoke the function from user's organization, not the other one
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        verify(executionService).executeFunctionAsync(eq(testFunction.getId()), any(), eq(testTrigger));
        // Verify it did NOT invoke the other organization's function
        verify(executionService, never()).executeFunctionAsync(eq(otherOrgFunction.getId()), any(), any());
    }
}
