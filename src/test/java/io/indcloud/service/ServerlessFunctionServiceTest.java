package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.ServerlessFunctionRequest;
import io.indcloud.dto.ServerlessFunctionResponse;
import io.indcloud.model.*;
import io.indcloud.repository.FunctionTriggerRepository;
import io.indcloud.repository.ServerlessFunctionRepository;
import io.indcloud.security.SecurityUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Tests for ServerlessFunctionService.
 */
@ExtendWith(MockitoExtension.class)
class ServerlessFunctionServiceTest {

    @Mock
    private ServerlessFunctionRepository functionRepository;

    @Mock
    private FunctionTriggerRepository triggerRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ServerlessFunctionService functionService;

    private Organization testOrganization;
    private User testUser;

    @BeforeEach
    void setUp() {
        testOrganization = new Organization();
        testOrganization.setId(1L);
        testOrganization.setName("Test Organization");

        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setOrganization(testOrganization);

        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
        lenient().when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void createFunction_shouldCreateWithDefaults() {
        // Arrange
        String pythonCode = """
            def main(event):
                return {"result": "Hello from Python"}
            """;

        ServerlessFunctionRequest request = new ServerlessFunctionRequest(
            "test-function",
            "Test function",
            FunctionRuntime.PYTHON_3_11,
            pythonCode,
            null,  // handler will default to "main"
            null,  // enabled will default to true
            null,  // timeout will default to 30
            null,  // memory will default to 512
            null   // no env vars
        );

        when(functionRepository.existsByOrganizationAndName(testOrganization, "test-function"))
            .thenReturn(false);
        when(functionRepository.save(any(ServerlessFunction.class)))
            .thenAnswer(invocation -> {
                ServerlessFunction func = invocation.getArgument(0);
                func.setId(100L);
                return func;
            });

        // Act
        ServerlessFunctionResponse response = functionService.createFunction(request);

        // Assert
        ArgumentCaptor<ServerlessFunction> functionCaptor = ArgumentCaptor.forClass(ServerlessFunction.class);
        verify(functionRepository).save(functionCaptor.capture());

        ServerlessFunction savedFunction = functionCaptor.getValue();
        assertThat(savedFunction.getName()).isEqualTo("test-function");
        assertThat(savedFunction.getRuntime()).isEqualTo(FunctionRuntime.PYTHON_3_11);
        assertThat(savedFunction.getCode()).isEqualTo(pythonCode);
        assertThat(savedFunction.getHandler()).isEqualTo("main");
        assertThat(savedFunction.getEnabled()).isTrue();
        assertThat(savedFunction.getTimeoutSeconds()).isEqualTo(30);
        assertThat(savedFunction.getMemoryLimitMb()).isEqualTo(512);
        assertThat(savedFunction.getOrganization()).isEqualTo(testOrganization);
        assertThat(savedFunction.getCreatedBy()).isEqualTo(testUser);
    }

    @Test
    void createFunction_shouldUseCustomValues() {
        // Arrange
        ServerlessFunctionRequest request = new ServerlessFunctionRequest(
            "custom-function",
            "Custom function",
            FunctionRuntime.PYTHON_3_11,
            "def handler(event): return event",
            "handler",  // custom handler
            false,      // disabled
            60,         // 60 second timeout
            1024,       // 1GB memory
            null
        );

        when(functionRepository.existsByOrganizationAndName(testOrganization, "custom-function"))
            .thenReturn(false);
        when(functionRepository.save(any(ServerlessFunction.class)))
            .thenAnswer(invocation -> {
                ServerlessFunction func = invocation.getArgument(0);
                func.setId(101L);
                return func;
            });

        // Act
        ServerlessFunctionResponse response = functionService.createFunction(request);

        // Assert
        ArgumentCaptor<ServerlessFunction> functionCaptor = ArgumentCaptor.forClass(ServerlessFunction.class);
        verify(functionRepository).save(functionCaptor.capture());

        ServerlessFunction savedFunction = functionCaptor.getValue();
        assertThat(savedFunction.getHandler()).isEqualTo("handler");
        assertThat(savedFunction.getEnabled()).isFalse();
        assertThat(savedFunction.getTimeoutSeconds()).isEqualTo(60);
        assertThat(savedFunction.getMemoryLimitMb()).isEqualTo(1024);
    }

    @Test
    void createFunction_shouldRejectDuplicateName() {
        // Arrange
        ServerlessFunctionRequest request = new ServerlessFunctionRequest(
            "duplicate",
            "Duplicate function",
            FunctionRuntime.PYTHON_3_11,
            "def main(event): pass",
            null, null, null, null, null
        );

        when(functionRepository.existsByOrganizationAndName(testOrganization, "duplicate"))
            .thenReturn(true);

        // Act & Assert
        try {
            functionService.createFunction(request);
            assert false : "Should have thrown exception";
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("already exists");
        }

        verify(functionRepository, never()).save(any());
    }

    @Test
    void updateFunction_shouldUpdateCode() {
        // Arrange
        ServerlessFunction existingFunction = new ServerlessFunction();
        existingFunction.setId(100L);
        existingFunction.setOrganization(testOrganization);
        existingFunction.setName("test-function");
        existingFunction.setRuntime(FunctionRuntime.PYTHON_3_11);
        existingFunction.setCode("def main(event): return {}");
        existingFunction.setHandler("main");

        String newCode = "def main(event): return {'updated': True}";

        ServerlessFunctionRequest request = new ServerlessFunctionRequest(
            null, null, null,
            newCode,  // updated code
            null, null, null, null, null
        );

        when(functionRepository.findById(100L)).thenReturn(Optional.of(existingFunction));
        when(functionRepository.save(any(ServerlessFunction.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        ServerlessFunctionResponse response = functionService.updateFunction(100L, request);

        // Assert
        ArgumentCaptor<ServerlessFunction> functionCaptor = ArgumentCaptor.forClass(ServerlessFunction.class);
        verify(functionRepository).save(functionCaptor.capture());

        ServerlessFunction updatedFunction = functionCaptor.getValue();
        assertThat(updatedFunction.getCode()).isEqualTo(newCode);
    }

    @Test
    void deleteFunction_shouldDeleteFunction() {
        // Arrange
        ServerlessFunction function = new ServerlessFunction();
        function.setId(100L);
        function.setOrganization(testOrganization);
        function.setName("to-delete");

        when(functionRepository.findById(100L)).thenReturn(Optional.of(function));

        // Act
        functionService.deleteFunction(100L);

        // Assert
        verify(functionRepository).delete(function);
    }
}
