package org.sensorvision.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.CannedResponseDto;
import org.sensorvision.dto.CannedResponseRequest;
import org.sensorvision.model.CannedResponse;
import org.sensorvision.model.User;
import org.sensorvision.repository.CannedResponseRepository;
import org.sensorvision.security.SecurityUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.lenient;

/**
 * Unit tests for CannedResponseService.
 * Tests template CRUD operations, usage tracking, and filtering.
 */
@ExtendWith(MockitoExtension.class)
class CannedResponseServiceTest {

    @Mock
    private CannedResponseRepository cannedResponseRepository;

    @Mock
    private SecurityUtils securityUtils;

    @InjectMocks
    private CannedResponseService cannedResponseService;

    @Captor
    private ArgumentCaptor<CannedResponse> responseCaptor;

    private User testUser;
    private CannedResponse testResponse;
    private CannedResponse testResponse2;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("admin");
        testUser.setEmail("admin@example.com");

        testResponse = new CannedResponse();
        testResponse.setId(1L);
        testResponse.setTitle("Welcome Message");
        testResponse.setBody("Thank you for contacting support!");
        testResponse.setCategory("GENERAL");
        testResponse.setActive(true);
        testResponse.setUseCount(5);
        testResponse.setCreatedBy(testUser);

        testResponse2 = new CannedResponse();
        testResponse2.setId(2L);
        testResponse2.setTitle("Password Reset");
        testResponse2.setBody("To reset your password...");
        testResponse2.setCategory("AUTHENTICATION");
        testResponse2.setActive(true);
        testResponse2.setUseCount(10);
        testResponse2.setCreatedBy(testUser);

        // Mock security context (lenient to avoid unnecessary stubbing exceptions)
        lenient().when(securityUtils.getCurrentUser()).thenReturn(testUser);
    }

    @Test
    void getAllActive_shouldReturnAllActiveResponses() {
        // Given
        List<CannedResponse> activeResponses = Arrays.asList(testResponse, testResponse2);
        when(cannedResponseRepository.findByActiveTrue()).thenReturn(activeResponses);

        // When
        List<CannedResponseDto> result = cannedResponseService.getAllActive();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).title()).isEqualTo("Welcome Message");
        assertThat(result.get(1).title()).isEqualTo("Password Reset");
        verify(cannedResponseRepository).findByActiveTrue();
    }

    @Test
    void getAllActiveByPopularity_shouldReturnResponsesOrderedByUseCount() {
        // Given
        List<CannedResponse> popularResponses = Arrays.asList(testResponse2, testResponse);
        when(cannedResponseRepository.findByActiveTrueOrderByUseCountDesc()).thenReturn(popularResponses);

        // When
        List<CannedResponseDto> result = cannedResponseService.getAllActiveByPopularity();

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0).useCount()).isEqualTo(10);
        assertThat(result.get(1).useCount()).isEqualTo(5);
        verify(cannedResponseRepository).findByActiveTrueOrderByUseCountDesc();
    }

    @Test
    void getAllByPopularity_shouldReturnAllResponsesOrderedByUseCount() {
        // Given
        CannedResponse inactiveResponse = new CannedResponse();
        inactiveResponse.setId(3L);
        inactiveResponse.setTitle("Inactive Template");
        inactiveResponse.setActive(false);
        inactiveResponse.setUseCount(7);
        inactiveResponse.setCreatedBy(testUser);

        List<CannedResponse> allByPopularity = Arrays.asList(testResponse2, inactiveResponse, testResponse);
        when(cannedResponseRepository.findAllByOrderByUseCountDesc()).thenReturn(allByPopularity);

        // When
        List<CannedResponseDto> result = cannedResponseService.getAllByPopularity();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.get(0).useCount()).isEqualTo(10);
        assertThat(result.get(1).useCount()).isEqualTo(7);
        assertThat(result.get(1).active()).isFalse();
        assertThat(result.get(2).useCount()).isEqualTo(5);
        verify(cannedResponseRepository).findAllByOrderByUseCountDesc();
    }

    @Test
    void getAll_shouldReturnAllResponsesIncludingInactive() {
        // Given
        CannedResponse inactiveResponse = new CannedResponse();
        inactiveResponse.setId(3L);
        inactiveResponse.setTitle("Inactive Template");
        inactiveResponse.setActive(false);
        inactiveResponse.setCreatedBy(testUser);

        List<CannedResponse> allResponses = Arrays.asList(testResponse, testResponse2, inactiveResponse);
        when(cannedResponseRepository.findAll()).thenReturn(allResponses);

        // When
        List<CannedResponseDto> result = cannedResponseService.getAll();

        // Then
        assertThat(result).hasSize(3);
        assertThat(result.stream().filter(r -> !r.active()).count()).isEqualTo(1);
        verify(cannedResponseRepository).findAll();
    }

    @Test
    void getByCategory_shouldReturnOnlyActiveResponsesWhenIncludeInactiveFalse() {
        // Given
        String category = "AUTHENTICATION";
        when(cannedResponseRepository.findByActiveTrueAndCategory(category))
            .thenReturn(Arrays.asList(testResponse2));

        // When
        List<CannedResponseDto> result = cannedResponseService.getByCategory(category, false);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("AUTHENTICATION");
        assertThat(result.get(0).title()).isEqualTo("Password Reset");
        verify(cannedResponseRepository).findByActiveTrueAndCategory(category);
        verify(cannedResponseRepository, never()).findByCategory(any());
    }

    @Test
    void getByCategory_shouldReturnAllResponsesWhenIncludeInactiveTrue() {
        // Given
        String category = "GENERAL";
        when(cannedResponseRepository.findByCategory(category))
            .thenReturn(Arrays.asList(testResponse));

        // When
        List<CannedResponseDto> result = cannedResponseService.getByCategory(category, true);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).category()).isEqualTo("GENERAL");
        verify(cannedResponseRepository).findByCategory(category);
        verify(cannedResponseRepository, never()).findByActiveTrueAndCategory(any());
    }

    @Test
    void getById_shouldReturnResponse_whenExists() {
        // Given
        Long responseId = 1L;
        when(cannedResponseRepository.findById(responseId)).thenReturn(Optional.of(testResponse));

        // When
        CannedResponseDto result = cannedResponseService.getById(responseId);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Welcome Message");
        assertThat(result.body()).isEqualTo("Thank you for contacting support!");
        verify(cannedResponseRepository).findById(responseId);
    }

    @Test
    void getById_shouldThrowException_whenNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(cannedResponseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cannedResponseService.getById(nonExistentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Canned response not found with id: 999");
        verify(cannedResponseRepository).findById(nonExistentId);
    }

    @Test
    void create_shouldCreateNewResponse_withAllFields() {
        // Given
        CannedResponseRequest request = new CannedResponseRequest(
            "New Template",
            "This is a new template body",
            "BUG",
            true
        );

        CannedResponse savedResponse = new CannedResponse();
        savedResponse.setId(3L);
        savedResponse.setTitle(request.title());
        savedResponse.setBody(request.body());
        savedResponse.setCategory(request.category());
        savedResponse.setActive(request.active());
        savedResponse.setCreatedBy(testUser);

        when(cannedResponseRepository.save(any(CannedResponse.class))).thenReturn(savedResponse);

        // When
        CannedResponseDto result = cannedResponseService.create(request);

        // Then
        verify(cannedResponseRepository).save(responseCaptor.capture());
        CannedResponse capturedResponse = responseCaptor.getValue();

        assertThat(capturedResponse.getTitle()).isEqualTo("New Template");
        assertThat(capturedResponse.getBody()).isEqualTo("This is a new template body");
        assertThat(capturedResponse.getCategory()).isEqualTo("BUG");
        assertThat(capturedResponse.isActive()).isTrue();
        assertThat(capturedResponse.getCreatedBy()).isEqualTo(testUser);

        assertThat(result.title()).isEqualTo("New Template");
    }

    @Test
    void create_shouldDefaultActiveToTrue_whenActiveIsNull() {
        // Given
        CannedResponseRequest request = new CannedResponseRequest(
            "Template",
            "Body",
            "GENERAL",
            null  // active is null
        );

        CannedResponse savedResponse = new CannedResponse();
        savedResponse.setId(4L);
        savedResponse.setTitle(request.title());
        savedResponse.setBody(request.body());
        savedResponse.setCategory(request.category());
        savedResponse.setActive(true);
        savedResponse.setCreatedBy(testUser);

        when(cannedResponseRepository.save(any(CannedResponse.class))).thenReturn(savedResponse);

        // When
        cannedResponseService.create(request);

        // Then
        verify(cannedResponseRepository).save(responseCaptor.capture());
        CannedResponse capturedResponse = responseCaptor.getValue();

        assertThat(capturedResponse.isActive()).isTrue();
    }

    @Test
    void update_shouldUpdateExistingResponse() {
        // Given
        Long responseId = 1L;
        CannedResponseRequest updateRequest = new CannedResponseRequest(
            "Updated Title",
            "Updated body content",
            "FEATURE_REQUEST",
            false
        );

        when(cannedResponseRepository.findById(responseId)).thenReturn(Optional.of(testResponse));
        when(cannedResponseRepository.save(any(CannedResponse.class))).thenReturn(testResponse);

        // When
        CannedResponseDto result = cannedResponseService.update(responseId, updateRequest);

        // Then
        verify(cannedResponseRepository).findById(responseId);
        verify(cannedResponseRepository).save(responseCaptor.capture());
        CannedResponse updatedResponse = responseCaptor.getValue();

        assertThat(updatedResponse.getTitle()).isEqualTo("Updated Title");
        assertThat(updatedResponse.getBody()).isEqualTo("Updated body content");
        assertThat(updatedResponse.getCategory()).isEqualTo("FEATURE_REQUEST");
        assertThat(updatedResponse.isActive()).isFalse();
    }

    @Test
    void update_shouldNotChangeActive_whenActiveIsNull() {
        // Given
        Long responseId = 1L;
        testResponse.setActive(true);  // Initially active

        CannedResponseRequest updateRequest = new CannedResponseRequest(
            "Updated Title",
            "Updated body",
            "GENERAL",
            null  // active is null
        );

        when(cannedResponseRepository.findById(responseId)).thenReturn(Optional.of(testResponse));
        when(cannedResponseRepository.save(any(CannedResponse.class))).thenReturn(testResponse);

        // When
        cannedResponseService.update(responseId, updateRequest);

        // Then
        verify(cannedResponseRepository).save(responseCaptor.capture());
        CannedResponse updatedResponse = responseCaptor.getValue();

        assertThat(updatedResponse.isActive()).isTrue();  // Should remain true
    }

    @Test
    void update_shouldThrowException_whenResponseNotFound() {
        // Given
        Long nonExistentId = 999L;
        CannedResponseRequest request = new CannedResponseRequest(
            "Title",
            "Body",
            "GENERAL",
            true
        );

        when(cannedResponseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cannedResponseService.update(nonExistentId, request))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Canned response not found with id: 999");
        verify(cannedResponseRepository).findById(nonExistentId);
        verify(cannedResponseRepository, never()).save(any());
    }

    @Test
    void delete_shouldDeleteResponse_whenExists() {
        // Given
        Long responseId = 1L;
        when(cannedResponseRepository.findById(responseId)).thenReturn(Optional.of(testResponse));

        // When
        cannedResponseService.delete(responseId);

        // Then
        verify(cannedResponseRepository).findById(responseId);
        verify(cannedResponseRepository).delete(testResponse);
    }

    @Test
    void delete_shouldThrowException_whenResponseNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(cannedResponseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cannedResponseService.delete(nonExistentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Canned response not found with id: 999");
        verify(cannedResponseRepository).findById(nonExistentId);
        verify(cannedResponseRepository, never()).delete(any());
    }

    @Test
    void markAsUsed_shouldIncrementUseCount() {
        // Given
        Long responseId = 1L;
        int initialUseCount = testResponse.getUseCount();
        when(cannedResponseRepository.findById(responseId)).thenReturn(Optional.of(testResponse));
        when(cannedResponseRepository.save(any(CannedResponse.class))).thenReturn(testResponse);

        // When
        cannedResponseService.markAsUsed(responseId);

        // Then
        verify(cannedResponseRepository).findById(responseId);
        verify(cannedResponseRepository).save(responseCaptor.capture());
        CannedResponse savedResponse = responseCaptor.getValue();

        assertThat(savedResponse.getUseCount()).isEqualTo(initialUseCount + 1);
    }

    @Test
    void markAsUsed_shouldThrowException_whenResponseNotFound() {
        // Given
        Long nonExistentId = 999L;
        when(cannedResponseRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> cannedResponseService.markAsUsed(nonExistentId))
            .isInstanceOf(RuntimeException.class)
            .hasMessageContaining("Canned response not found with id: 999");
        verify(cannedResponseRepository).findById(nonExistentId);
        verify(cannedResponseRepository, never()).save(any());
    }
}
