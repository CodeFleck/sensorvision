package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import io.indcloud.dto.DashboardShareRequest;
import io.indcloud.dto.DashboardShareResponse;
import io.indcloud.model.Dashboard;
import io.indcloud.model.Organization;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.security.SecurityUtils;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for Dashboard Sharing functionality
 */
@ExtendWith(MockitoExtension.class)
class DashboardSharingServiceTest {

    @Mock
    private DashboardRepository dashboardRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private DefaultDashboardInitializer defaultDashboardInitializer;

    @Mock
    private ObjectMapper objectMapper;

    @Captor
    private ArgumentCaptor<Dashboard> dashboardCaptor;

    private DashboardService dashboardService;

    private Organization testOrganization;
    private Dashboard testDashboard;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        testDashboard = new Dashboard();
        testDashboard.setId(1L);
        testDashboard.setName("Test Dashboard");
        testDashboard.setOrganization(testOrganization);
        testDashboard.setIsPublic(false);
        testDashboard.setPublicShareToken(null);

        dashboardService = new DashboardService(
                dashboardRepository,
                null, // widgetRepository not needed for these tests
                objectMapper,
                defaultDashboardInitializer,
                securityUtils,
                passwordEncoder
        );

        // Lenient stubbing since not all tests need security context
        lenient().when(securityUtils.getCurrentUserOrganization()).thenReturn(testOrganization);
    }

    @Test
    void configureDashboardSharing_shouldEnablePublicSharing() {
        // Given
        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .allowAnonymousView(true)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DashboardShareResponse response = dashboardService.configureDashboardSharing(1L, request);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getIsPublic()).isTrue();
        assertThat(savedDashboard.getAllowAnonymousView()).isTrue();
        assertThat(savedDashboard.getPublicShareToken()).isNotNull();
        assertThat(savedDashboard.getPublicShareToken()).hasSize(36); // UUID length

        assertThat(response.getIsPublic()).isTrue();
        assertThat(response.getShareUrl()).isNotNull();
        assertThat(response.getShareToken()).isNotNull();
        assertThat(response.getMessage()).contains("enabled");
    }

    @Test
    void configureDashboardSharing_shouldSetPasswordHash() {
        // Given
        String plainPassword = "secure123";
        String hashedPassword = "$2a$10$hashedPassword";

        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .password(plainPassword)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        when(passwordEncoder.encode(plainPassword)).thenReturn(hashedPassword);
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DashboardShareResponse response = dashboardService.configureDashboardSharing(1L, request);

        // Then
        verify(passwordEncoder).encode(plainPassword);
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getSharePasswordHash()).isEqualTo(hashedPassword);
        assertThat(response.getIsPasswordProtected()).isTrue();
    }

    @Test
    void configureDashboardSharing_shouldSetExpirationDate() {
        // Given
        LocalDateTime expirationDate = LocalDateTime.now().plusDays(30);

        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .expiresAt(expirationDate)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DashboardShareResponse response = dashboardService.configureDashboardSharing(1L, request);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getShareExpiresAt()).isEqualTo(expirationDate);
        assertThat(response.getExpiresAt()).isEqualTo(expirationDate);
    }

    @Test
    void configureDashboardSharing_shouldReuseExistingToken() {
        // Given
        String existingToken = UUID.randomUUID().toString();
        testDashboard.setPublicShareToken(existingToken);
        testDashboard.setIsPublic(false);

        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DashboardShareResponse response = dashboardService.configureDashboardSharing(1L, request);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getPublicShareToken()).isEqualTo(existingToken);
        assertThat(response.getShareToken()).isEqualTo(existingToken);
    }

    @Test
    void configureDashboardSharing_shouldDisableSharing() {
        // Given
        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(UUID.randomUUID().toString());
        testDashboard.setSharePasswordHash("hashed");

        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(false)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));
        when(dashboardRepository.save(any(Dashboard.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // When
        DashboardShareResponse response = dashboardService.configureDashboardSharing(1L, request);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getIsPublic()).isFalse();
        assertThat(savedDashboard.getPublicShareToken()).isNull();
        assertThat(savedDashboard.getSharePasswordHash()).isNull();
        assertThat(response.getMessage()).contains("disabled");
    }

    @Test
    void configureDashboardSharing_shouldThrowException_whenDashboardNotFound() {
        // Given
        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .build();

        when(dashboardRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dashboardService.configureDashboardSharing(999L, request))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dashboard not found");
    }

    @Test
    void configureDashboardSharing_shouldThrowException_whenWrongOrganization() {
        // Given
        Organization otherOrg = Organization.builder()
                .id(2L)
                .name("Other Organization")
                .build();

        testDashboard.setOrganization(otherOrg);

        DashboardShareRequest request = DashboardShareRequest.builder()
                .isPublic(true)
                .build();

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));

        // When & Then
        assertThatThrownBy(() -> dashboardService.configureDashboardSharing(1L, request))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }

    @Test
    void getPublicDashboard_shouldReturnDashboard_whenValidToken() {
        // Given
        String shareToken = UUID.randomUUID().toString();
        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(shareToken);

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.of(testDashboard));

        // When
        var response = dashboardService.getPublicDashboard(shareToken, null);

        // Then
        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(testDashboard.getId());
        verify(dashboardRepository).findByPublicShareToken(shareToken);
    }

    @Test
    void getPublicDashboard_shouldValidatePassword_whenPasswordProtected() {
        // Given
        String shareToken = UUID.randomUUID().toString();
        String correctPassword = "secure123";
        String hashedPassword = "$2a$10$hashedPassword";

        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(shareToken);
        testDashboard.setSharePasswordHash(hashedPassword);

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.of(testDashboard));
        when(passwordEncoder.matches(correctPassword, hashedPassword)).thenReturn(true);

        // When
        var response = dashboardService.getPublicDashboard(shareToken, correctPassword);

        // Then
        assertThat(response).isNotNull();
        verify(passwordEncoder).matches(correctPassword, hashedPassword);
    }

    @Test
    void getPublicDashboard_shouldThrowException_whenWrongPassword() {
        // Given
        String shareToken = UUID.randomUUID().toString();
        String wrongPassword = "wrong";
        String hashedPassword = "$2a$10$hashedPassword";

        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(shareToken);
        testDashboard.setSharePasswordHash(hashedPassword);

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.of(testDashboard));
        when(passwordEncoder.matches(wrongPassword, hashedPassword)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> dashboardService.getPublicDashboard(shareToken, wrongPassword))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Invalid password");
    }

    @Test
    void getPublicDashboard_shouldThrowException_whenExpired() {
        // Given
        String shareToken = UUID.randomUUID().toString();
        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(shareToken);
        testDashboard.setShareExpiresAt(LocalDateTime.now().minusDays(1)); // Expired yesterday

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.of(testDashboard));

        // When & Then
        assertThatThrownBy(() -> dashboardService.getPublicDashboard(shareToken, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void getPublicDashboard_shouldThrowException_whenNotPublic() {
        // Given
        String shareToken = UUID.randomUUID().toString();
        testDashboard.setIsPublic(false);
        testDashboard.setPublicShareToken(shareToken);

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.of(testDashboard));

        // When & Then
        assertThatThrownBy(() -> dashboardService.getPublicDashboard(shareToken, null))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("not publicly shared");
    }

    @Test
    void getPublicDashboard_shouldThrowException_whenTokenNotFound() {
        // Given
        String shareToken = UUID.randomUUID().toString();

        when(dashboardRepository.findByPublicShareToken(shareToken)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> dashboardService.getPublicDashboard(shareToken, null))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Dashboard not found");
    }

    @Test
    void disableDashboardSharing_shouldClearSharingData() {
        // Given
        testDashboard.setIsPublic(true);
        testDashboard.setPublicShareToken(UUID.randomUUID().toString());
        testDashboard.setSharePasswordHash("hashed");
        testDashboard.setShareExpiresAt(LocalDateTime.now().plusDays(30));

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));

        // When
        dashboardService.disableDashboardSharing(1L);

        // Then
        verify(dashboardRepository).save(dashboardCaptor.capture());
        Dashboard savedDashboard = dashboardCaptor.getValue();

        assertThat(savedDashboard.getIsPublic()).isFalse();
        assertThat(savedDashboard.getPublicShareToken()).isNull();
        assertThat(savedDashboard.getSharePasswordHash()).isNull();
        assertThat(savedDashboard.getShareExpiresAt()).isNull();
    }

    @Test
    void disableDashboardSharing_shouldThrowException_whenWrongOrganization() {
        // Given
        Organization otherOrg = Organization.builder()
                .id(2L)
                .name("Other Organization")
                .build();

        testDashboard.setOrganization(otherOrg);

        when(dashboardRepository.findById(1L)).thenReturn(Optional.of(testDashboard));

        // When & Then
        assertThatThrownBy(() -> dashboardService.disableDashboardSharing(1L))
                .isInstanceOf(AccessDeniedException.class)
                .hasMessageContaining("Access denied");
    }
}
