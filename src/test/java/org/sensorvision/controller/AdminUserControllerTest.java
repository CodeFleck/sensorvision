package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.UserDto;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.User;
import org.sensorvision.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/**
 * Unit tests for AdminUserController.
 * Tests REST API endpoints for admin user management.
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AdminUserController adminUserController;

    private Organization testOrganization;
    private User testUser1;
    private User testUser2;
    private Role userRole;

    @BeforeEach
    void setUp() {
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        testUser1 = User.builder()
                .id(1L)
                .username("testuser1")
                .email("testuser1@example.com")
                .firstName("Test")
                .lastName("User One")
                .organization(testOrganization)
                .enabled(true)
                .emailVerified(true)
                .roles(roles)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        testUser2 = User.builder()
                .id(2L)
                .username("testuser2")
                .email("testuser2@example.com")
                .firstName("Test")
                .lastName("User Two")
                .organization(testOrganization)
                .enabled(true)
                .emailVerified(false)
                .roles(roles)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();
    }

    @Test
    void getAllUsers_shouldReturnUsersWithOrganizationData() {
        // Given
        List<User> users = Arrays.asList(testUser1, testUser2);
        when(userRepository.findAll()).thenReturn(users);

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        UserDto dto1 = response.getBody().get(0);
        assertThat(dto1.getId()).isEqualTo(testUser1.getId());
        assertThat(dto1.getUsername()).isEqualTo("testuser1");
        assertThat(dto1.getEmail()).isEqualTo("testuser1@example.com");
        assertThat(dto1.getOrganizationId()).isEqualTo(testOrganization.getId());
        assertThat(dto1.getOrganizationName()).isEqualTo(testOrganization.getName());
        assertThat(dto1.getEnabled()).isTrue();
        assertThat(dto1.getEmailVerified()).isTrue();
        assertThat(dto1.getRoles()).contains("ROLE_USER");

        UserDto dto2 = response.getBody().get(1);
        assertThat(dto2.getId()).isEqualTo(testUser2.getId());
        assertThat(dto2.getUsername()).isEqualTo("testuser2");
        assertThat(dto2.getOrganizationId()).isEqualTo(testOrganization.getId());
        assertThat(dto2.getOrganizationName()).isEqualTo(testOrganization.getName());
    }

    @Test
    void getUser_shouldReturnSingleUserWithOrganization() {
        // Given
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.of(testUser1));

        // When
        ResponseEntity<UserDto> response = adminUserController.getUser(userId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserDto dto = response.getBody();
        assertThat(dto.getId()).isEqualTo(testUser1.getId());
        assertThat(dto.getUsername()).isEqualTo("testuser1");
        assertThat(dto.getEmail()).isEqualTo("testuser1@example.com");
        assertThat(dto.getFirstName()).isEqualTo("Test");
        assertThat(dto.getLastName()).isEqualTo("User One");
        assertThat(dto.getOrganizationId()).isEqualTo(testOrganization.getId());
        assertThat(dto.getOrganizationName()).isEqualTo(testOrganization.getName());
        assertThat(dto.getEnabled()).isTrue();
        assertThat(dto.getRoles()).contains("ROLE_USER");
    }

    @Test
    void getUser_withNonexistentUser_shouldThrowException() {
        // Given
        Long userId = 999L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.getUser(userId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("User not found with id: " + userId);
    }

    @Test
    void getUsersByOrganization_shouldReturnFilteredUsers() {
        // Given
        Long organizationId = 1L;
        List<User> users = Arrays.asList(testUser1, testUser2);
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(users);

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getUsersByOrganization(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);

        // Verify all users belong to the same organization
        response.getBody().forEach(dto -> {
            assertThat(dto.getOrganizationId()).isEqualTo(organizationId);
            assertThat(dto.getOrganizationName()).isEqualTo(testOrganization.getName());
        });
    }

    @Test
    void getUsersByOrganization_withNoUsers_shouldReturnEmptyList() {
        // Given
        Long organizationId = 999L;
        when(userRepository.findByOrganizationId(organizationId)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getUsersByOrganization(organizationId);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void convertToDto_shouldMapAllFieldsCorrectly() {
        // This test verifies that the convertToDto method properly accesses
        // lazy-loaded Organization fields without throwing LazyInitializationException
        // when @Transactional annotation is present

        // Given
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser1));

        // When
        ResponseEntity<UserDto> response = adminUserController.getUser(1L);

        // Then
        UserDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(testUser1.getId());
        assertThat(dto.getUsername()).isEqualTo(testUser1.getUsername());
        assertThat(dto.getEmail()).isEqualTo(testUser1.getEmail());
        assertThat(dto.getFirstName()).isEqualTo(testUser1.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(testUser1.getLastName());

        // Critical: These fields access lazy-loaded Organization entity
        assertThat(dto.getOrganizationId()).isEqualTo(testOrganization.getId());
        assertThat(dto.getOrganizationName()).isEqualTo(testOrganization.getName());

        assertThat(dto.getEnabled()).isEqualTo(testUser1.getEnabled());
        assertThat(dto.getEmailVerified()).isEqualTo(testUser1.getEmailVerified());
        assertThat(dto.getCreatedAt()).isNotNull();
        assertThat(dto.getUpdatedAt()).isNotNull();
        assertThat(dto.getRoles()).isNotEmpty();
    }
}
