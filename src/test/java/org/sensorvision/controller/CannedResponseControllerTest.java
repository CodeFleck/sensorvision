package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.CannedResponseDto;
import org.sensorvision.dto.CannedResponseRequest;
import org.sensorvision.service.CannedResponseService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Unit tests for CannedResponseController.
 * Tests REST API endpoints for canned response management.
 */
@ExtendWith(MockitoExtension.class)
class CannedResponseControllerTest {

    @Mock
    private CannedResponseService cannedResponseService;

    @InjectMocks
    private CannedResponseController cannedResponseController;

    private CannedResponseDto testDto1;
    private CannedResponseDto testDto2;
    private CannedResponseRequest testRequest;

    @BeforeEach
    void setUp() {
        testDto1 = new CannedResponseDto(
            1L,
            "Welcome Message",
            "Thank you for contacting support!",
            "GENERAL",
            true,
            5,
            1L,
            "admin",
            Instant.now(),
            Instant.now()
        );

        testDto2 = new CannedResponseDto(
            2L,
            "Password Reset",
            "To reset your password...",
            "AUTHENTICATION",
            true,
            10,
            1L,
            "admin",
            Instant.now(),
            Instant.now()
        );

        testRequest = new CannedResponseRequest(
            "New Template",
            "Template body",
            "BUG",
            true
        );
    }

    @Test
    void getAllActive_withoutParams_shouldReturnAllActiveResponses() {
        // Given
        List<CannedResponseDto> responses = Arrays.asList(testDto1, testDto2);
        when(cannedResponseService.getAllActive()).thenReturn(responses);

        // When
        ResponseEntity<List<CannedResponseDto>> result =
            cannedResponseController.getAllActive(null, false, false);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
        assertThat(result.getBody()).containsExactly(testDto1, testDto2);
        verify(cannedResponseService).getAllActive();
        verify(cannedResponseService, never()).getAllActiveByPopularity();
        verify(cannedResponseService, never()).getByCategory(any(), anyBoolean());
    }

    @Test
    void getAllActive_withSortByPopularity_shouldReturnSortedResponses() {
        // Given
        List<CannedResponseDto> responses = Arrays.asList(testDto2, testDto1); // Sorted by use count
        when(cannedResponseService.getAllActiveByPopularity()).thenReturn(responses);

        // When
        ResponseEntity<List<CannedResponseDto>> result =
            cannedResponseController.getAllActive(null, true, false);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
        assertThat(result.getBody().get(0).useCount()).isEqualTo(10);
        assertThat(result.getBody().get(1).useCount()).isEqualTo(5);
        verify(cannedResponseService).getAllActiveByPopularity();
        verify(cannedResponseService, never()).getAllActive();
    }

    @Test
    void getAllActive_withCategory_shouldReturnFilteredResponses() {
        // Given
        String category = "AUTHENTICATION";
        when(cannedResponseService.getByCategory(category, false)).thenReturn(Arrays.asList(testDto2));

        // When
        ResponseEntity<List<CannedResponseDto>> result =
            cannedResponseController.getAllActive(category, false, false);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(1);
        assertThat(result.getBody().get(0).category()).isEqualTo("AUTHENTICATION");
        verify(cannedResponseService).getByCategory(category, false);
        verify(cannedResponseService, never()).getAllActive();
    }

    @Test
    void getAllActive_withCategoryAndIncludeInactive_shouldReturnAllInCategory() {
        // Given
        String category = "BUG";
        when(cannedResponseService.getByCategory(category, true)).thenReturn(Arrays.asList(testDto1));

        // When
        ResponseEntity<List<CannedResponseDto>> result =
            cannedResponseController.getAllActive(category, false, true);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(cannedResponseService).getByCategory(category, true);
    }

    @Test
    void getAllActive_withIncludeInactive_shouldReturnAllResponses() {
        // Given
        List<CannedResponseDto> responses = Arrays.asList(testDto1, testDto2);
        when(cannedResponseService.getAll()).thenReturn(responses);

        // When
        ResponseEntity<List<CannedResponseDto>> result =
            cannedResponseController.getAllActive(null, false, true);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).hasSize(2);
        verify(cannedResponseService).getAll();
        verify(cannedResponseService, never()).getAllActive();
    }

    @Test
    void getById_shouldReturnResponse_whenExists() {
        // Given
        Long responseId = 1L;
        when(cannedResponseService.getById(responseId)).thenReturn(testDto1);

        // When
        ResponseEntity<CannedResponseDto> result = cannedResponseController.getById(responseId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(1L);
        assertThat(result.getBody().title()).isEqualTo("Welcome Message");
        verify(cannedResponseService).getById(responseId);
    }

    @Test
    void create_shouldCreateAndReturnResponse() {
        // Given
        CannedResponseDto createdDto = new CannedResponseDto(
            3L,
            testRequest.title(),
            testRequest.body(),
            testRequest.category(),
            testRequest.active(),
            0,
            1L,
            "admin",
            Instant.now(),
            Instant.now()
        );
        when(cannedResponseService.create(any(CannedResponseRequest.class))).thenReturn(createdDto);

        // When
        ResponseEntity<CannedResponseDto> result = cannedResponseController.create(testRequest);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(3L);
        assertThat(result.getBody().title()).isEqualTo("New Template");
        verify(cannedResponseService).create(testRequest);
    }

    @Test
    void update_shouldUpdateAndReturnResponse() {
        // Given
        Long responseId = 1L;
        CannedResponseDto updatedDto = new CannedResponseDto(
            responseId,
            "Updated Title",
            "Updated body",
            "GENERAL",
            true,
            5,
            1L,
            "admin",
            Instant.now(),
            Instant.now()
        );
        when(cannedResponseService.update(eq(responseId), any(CannedResponseRequest.class)))
            .thenReturn(updatedDto);

        // When
        ResponseEntity<CannedResponseDto> result = cannedResponseController.update(responseId, testRequest);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNotNull();
        assertThat(result.getBody().id()).isEqualTo(responseId);
        verify(cannedResponseService).update(responseId, testRequest);
    }

    @Test
    void delete_shouldReturnNoContent() {
        // Given
        Long responseId = 1L;
        doNothing().when(cannedResponseService).delete(responseId);

        // When
        ResponseEntity<Void> result = cannedResponseController.delete(responseId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(result.getBody()).isNull();
        verify(cannedResponseService).delete(responseId);
    }

    @Test
    void markAsUsed_shouldReturnOk() {
        // Given
        Long responseId = 1L;
        doNothing().when(cannedResponseService).markAsUsed(responseId);

        // When
        ResponseEntity<Void> result = cannedResponseController.markAsUsed(responseId);

        // Then
        assertThat(result.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(result.getBody()).isNull();
        verify(cannedResponseService).markAsUsed(responseId);
    }

    @Test
    void markAsUsed_shouldHandleServiceException() {
        // Given
        Long responseId = 999L;
        doThrow(new RuntimeException("Canned response not found"))
            .when(cannedResponseService).markAsUsed(responseId);

        // When/Then
        try {
            cannedResponseController.markAsUsed(responseId);
        } catch (RuntimeException e) {
            assertThat(e.getMessage()).contains("Canned response not found");
        }

        verify(cannedResponseService).markAsUsed(responseId);
    }
}
