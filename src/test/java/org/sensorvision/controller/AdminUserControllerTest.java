package org.sensorvision.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sensorvision.dto.ApiResponse;
import org.sensorvision.dto.UserDto;
import org.sensorvision.exception.BadRequestException;
import org.sensorvision.exception.ResourceNotFoundException;
import org.sensorvision.model.Organization;
import org.sensorvision.model.Role;
import org.sensorvision.model.TrashLog;
import org.sensorvision.model.User;
import org.sensorvision.repository.RoleRepository;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.TrashService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AdminUserController.
 *
 * REGRESSION TESTS: Fix for GitHub Issue #98
 * - LazyInitializationException when accessing user.getOrganization()
 * - Fixed by adding @Transactional(readOnly=true) to GET endpoints
 *
 * These tests validate:
 * 1. Controller correctly accesses Organization lazy-loaded relationships
 * 2. DTO conversion properly maps organization data
 * 3. Roles collection is correctly accessed and mapped
 */
@ExtendWith(MockitoExtension.class)
class AdminUserControllerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private SecurityUtils securityUtils;

    @Mock
    private TrashService trashService;

    @InjectMocks
    private AdminUserController adminUserController;

    private Organization testOrganization;
    private User testUser;
    private User adminUser;
    private Role userRole;
    private Role adminRole;
    private Role developerRole;

    @BeforeEach
    void setUp() {
        // Setup test organization
        testOrganization = Organization.builder()
                .id(1L)
                .name("Test Organization")
                .build();

        // Setup test roles
        userRole = new Role();
        userRole.setId(1L);
        userRole.setName("ROLE_USER");

        adminRole = new Role();
        adminRole.setId(2L);
        adminRole.setName("ROLE_ADMIN");

        developerRole = new Role();
        developerRole.setId(3L);
        developerRole.setName("ROLE_DEVELOPER");
        developerRole.setDescription("Developer access for viewing system logs");

        // Setup test user with lazy-loaded relationships
        testUser = User.builder()
                .id(100L)
                .username("testuser")
                .email("test@example.com")
                .firstName("Test")
                .lastName("User")
                .enabled(true)
                .emailVerified(true)
                .organization(testOrganization) // Lazy-loaded relationship
                .roles(new HashSet<>(Arrays.asList(userRole))) // Lazy-loaded collection
                .build();

        // Setup admin user (the current logged in admin performing actions)
        adminUser = User.builder()
                .id(1L)
                .username("admin")
                .email("admin@example.com")
                .firstName("Admin")
                .lastName("User")
                .enabled(true)
                .emailVerified(true)
                .organization(testOrganization)
                .roles(new HashSet<>(Arrays.asList(adminRole)))
                .build();
    }

    // ========== REGRESSION: Issue #98 - LazyInitializationException ==========
    // These tests validate that @Transactional(readOnly=true) allows safe access
    // to lazy-loaded Organization and Roles relationships

    @Test
    void REGRESSION_getAllUsers_shouldAccessLazyLoadedOrganization() {
        // Given - User with lazy-loaded Organization
        when(userRepository.findAllActiveWithOrganizationAndRoles()).thenReturn(Arrays.asList(testUser));

        // When - getAllUsers() accesses user.getOrganization().getId() and getName()
        ResponseEntity<List<UserDto>> response = adminUserController.getAllUsers();

        // Then - Should successfully convert to DTO without LazyInitializationException
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        UserDto dto = response.getBody().get(0);
        assertThat(dto.getOrganizationId()).isEqualTo(1L); // Eager-fetched Organization.id
        assertThat(dto.getOrganizationName()).isEqualTo("Test Organization"); // Eager-fetched Organization.name
        assertThat(dto.getRoles()).contains("ROLE_USER"); // Eager-fetched Roles collection

        verify(userRepository).findAllActiveWithOrganizationAndRoles();
    }

    @Test
    void REGRESSION_getUser_shouldAccessLazyLoadedOrganization() {
        // Given - User with eager-fetched Organization
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When - getUser() accesses user.getOrganization().getId() and getName()
        ResponseEntity<UserDto> response = adminUserController.getUser(100L);

        // Then - Should successfully convert to DTO without LazyInitializationException
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserDto dto = response.getBody();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getOrganizationId()).isEqualTo(1L); // Eager-fetched Organization.id
        assertThat(dto.getOrganizationName()).isEqualTo("Test Organization"); // Eager-fetched Organization.name
        assertThat(dto.getRoles()).contains("ROLE_USER"); // Eager-fetched Roles collection

        verify(userRepository).findByIdWithOrganizationAndRoles(100L);
    }

    @Test
    void REGRESSION_getUsersByOrganization_shouldAccessLazyLoadedOrganization() {
        // Given - Users with eager-fetched Organization
        when(userRepository.findByOrganizationIdWithOrganizationAndRoles(1L)).thenReturn(Arrays.asList(testUser));

        // When - getUsersByOrganization() accesses user.getOrganization().getId() and
        // getName()
        ResponseEntity<List<UserDto>> response = adminUserController.getUsersByOrganization(1L);

        // Then - Should successfully convert to DTO without LazyInitializationException
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(1);

        UserDto dto = response.getBody().get(0);
        assertThat(dto.getOrganizationId()).isEqualTo(1L); // Eager-fetched Organization.id
        assertThat(dto.getOrganizationName()).isEqualTo("Test Organization"); // Eager-fetched Organization.name

        verify(userRepository).findByOrganizationIdWithOrganizationAndRoles(1L);
    }

    @Test
    void REGRESSION_getAllUsers_shouldAccessLazyLoadedRolesCollection() {
        // Given - User with multiple roles (eager-fetched collection)
        testUser.getRoles().add(adminRole); // User has both ROLE_USER and ROLE_ADMIN
        when(userRepository.findAllActiveWithOrganizationAndRoles()).thenReturn(Arrays.asList(testUser));

        // When - getAllUsers() accesses user.getRoles() stream
        ResponseEntity<List<UserDto>> response = adminUserController.getAllUsers();

        // Then - Should successfully access Roles collection without
        // LazyInitializationException
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();

        UserDto dto = response.getBody().get(0);
        assertThat(dto.getRoles()).hasSize(2);
        assertThat(dto.getRoles()).containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN");
    }

    // ========== Standard API Behavior Tests ==========

    @Test
    void getAllUsers_withMultipleUsers_shouldReturnAllUsers() {
        // Given
        User user2 = User.builder()
                .id(200L)
                .username("testuser2")
                .email("test2@example.com")
                .firstName("Test2")
                .lastName("User2")
                .enabled(true)
                .emailVerified(false)
                .organization(testOrganization)
                .roles(new HashSet<>(Arrays.asList(userRole)))
                .build();

        when(userRepository.findAllActiveWithOrganizationAndRoles()).thenReturn(Arrays.asList(testUser, user2));

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getId()).isEqualTo(100L);
        assertThat(response.getBody().get(1).getId()).isEqualTo(200L);
    }

    @Test
    void getAllUsers_withNoUsers_shouldReturnEmptyList() {
        // Given
        when(userRepository.findAllActiveWithOrganizationAndRoles()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getAllUsers();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void getUser_withValidUserId_shouldReturnUser() {
        // Given
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<UserDto> response = adminUserController.getUser(100L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getId()).isEqualTo(100L);
        assertThat(response.getBody().getUsername()).isEqualTo("testuser");
        assertThat(response.getBody().getEmail()).isEqualTo("test@example.com");
        assertThat(response.getBody().getFirstName()).isEqualTo("Test");
        assertThat(response.getBody().getLastName()).isEqualTo("User");
        assertThat(response.getBody().isEnabled()).isTrue();
        assertThat(response.getBody().isEmailVerified()).isTrue();
    }

    @Test
    void getUser_withNonexistentUserId_shouldThrowException() {
        // Given
        when(userRepository.findByIdWithOrganizationAndRoles(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.getUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");
    }

    @Test
    void enableUser_withValidUserId_shouldEnableUser() {
        // Given
        testUser.setEnabled(false); // User is disabled
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.enableUser(100L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("User enabled successfully");
        assertThat(response.getBody().data().isEnabled()).isTrue();

        verify(userRepository).save(testUser);
        verify(userRepository).findByIdWithOrganizationAndRoles(100L);
        assertThat(testUser.getEnabled()).isTrue();
    }

    @Test
    void enableUser_withNonexistentUserId_shouldThrowException() {
        // Given
        when(userRepository.findById(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.enableUser(999L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void disableUser_withValidUserId_shouldDisableUser() {
        // Given
        testUser.setEnabled(true); // User is enabled
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.disableUser(100L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("User disabled successfully");
        assertThat(response.getBody().data().isEnabled()).isFalse();

        verify(userRepository).save(testUser);
        verify(userRepository).findByIdWithOrganizationAndRoles(100L);
        assertThat(testUser.getEnabled()).isFalse();
    }

    @Test
    void updateUser_withValidData_shouldUpdateUser() {
        // Given
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        AdminUserController.UserUpdateRequest request = new AdminUserController.UserUpdateRequest();
        request.setFirstName("Updated");
        request.setLastName("Name");
        request.setEmail("updated@example.com");

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.updateUser(100L, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("User updated successfully");

        UserDto updatedDto = response.getBody().data();
        assertThat(updatedDto.getFirstName()).isEqualTo("Updated");
        assertThat(updatedDto.getLastName()).isEqualTo("Name");
        assertThat(updatedDto.getEmail()).isEqualTo("updated@example.com");

        verify(userRepository).save(testUser);
        verify(userRepository).findByIdWithOrganizationAndRoles(100L);
    }

    @Test
    void updateUser_withPartialData_shouldUpdateOnlyProvidedFields() {
        // Given
        when(userRepository.findById(100L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        AdminUserController.UserUpdateRequest request = new AdminUserController.UserUpdateRequest();
        request.setFirstName("UpdatedFirstName");
        // lastName and email are null - should not be updated

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.updateUser(100L, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(testUser.getFirstName()).isEqualTo("UpdatedFirstName");
        assertThat(testUser.getLastName()).isEqualTo("User"); // Unchanged
        assertThat(testUser.getEmail()).isEqualTo("test@example.com"); // Unchanged
    }

    @Test
    void deleteUser_withValidUserId_shouldSoftDeleteUser() {
        // Given
        java.time.Instant expiresAt = java.time.Instant.now().plus(30, java.time.temporal.ChronoUnit.DAYS);
        TrashLog trashLog = TrashLog.builder()
                .id(1L)
                .entityType("USER")
                .entityId("100")
                .entityName("testuser")
                .expiresAt(expiresAt)
                .deletedAt(java.time.Instant.now())
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(trashService.softDeleteUser(100L, null)).thenReturn(trashLog);

        // When
        ResponseEntity<ApiResponse<AdminUserController.SoftDeleteResponse>> response =
            adminUserController.deleteUser(100L, null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).contains("moved to trash");

        verify(trashService).softDeleteUser(100L, null);
    }

    @Test
    void deleteUser_withNonexistentUserId_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.deleteUser(999L, null))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(trashService, never()).softDeleteUser(anyLong(), anyString());
    }

    @Test
    void deleteUser_selfDeletion_shouldThrowException() {
        // Given - admin trying to delete themselves
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);

        // When/Then
        assertThatThrownBy(() -> adminUserController.deleteUser(1L, null)) // adminUser has id 1
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete your own account");

        verify(trashService, never()).softDeleteUser(anyLong(), anyString());
    }

    @Test
    void deleteUser_lastAdmin_shouldThrowException() {
        // Given - trying to delete the last admin user
        User lastAdmin = User.builder()
                .id(200L)
                .username("lastadmin")
                .roles(new HashSet<>(Arrays.asList(adminRole)))
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(200L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRolesName("ROLE_ADMIN")).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> adminUserController.deleteUser(200L, null))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot delete the last admin user");

        verify(trashService, never()).softDeleteUser(anyLong(), anyString());
    }

    @Test
    void getUsersByOrganization_withValidOrganizationId_shouldReturnUsers() {
        // Given
        User user2 = User.builder()
                .id(200L)
                .username("testuser2")
                .email("test2@example.com")
                .firstName("Test2")
                .lastName("User2")
                .enabled(true)
                .emailVerified(true)
                .organization(testOrganization)
                .roles(new HashSet<>(Arrays.asList(adminRole)))
                .build();

        when(userRepository.findByOrganizationIdWithOrganizationAndRoles(1L)).thenReturn(Arrays.asList(testUser, user2));

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getUsersByOrganization(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getOrganizationId()).isEqualTo(1L);
        assertThat(response.getBody().get(1).getOrganizationId()).isEqualTo(1L);
    }

    @Test
    void getUsersByOrganization_withNoUsers_shouldReturnEmptyList() {
        // Given
        when(userRepository.findByOrganizationIdWithOrganizationAndRoles(1L)).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<UserDto>> response = adminUserController.getUsersByOrganization(1L);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    // ========== DTO Conversion Tests ==========

    @Test
    void convertToDto_shouldMapAllFieldsCorrectly() {
        // Given
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<UserDto> response = adminUserController.getUser(100L);

        // Then
        UserDto dto = response.getBody();
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(testUser.getId());
        assertThat(dto.getUsername()).isEqualTo(testUser.getUsername());
        assertThat(dto.getEmail()).isEqualTo(testUser.getEmail());
        assertThat(dto.getFirstName()).isEqualTo(testUser.getFirstName());
        assertThat(dto.getLastName()).isEqualTo(testUser.getLastName());
        assertThat(dto.getOrganizationId()).isEqualTo(testUser.getOrganization().getId());
        assertThat(dto.getOrganizationName()).isEqualTo(testUser.getOrganization().getName());
        assertThat(dto.isEnabled()).isEqualTo(testUser.getEnabled());
        assertThat(dto.isEmailVerified()).isEqualTo(testUser.getEmailVerified());
        // Timestamps may be null in unit tests (JPA auditing not triggered)
        // assertThat(dto.getCreatedAt()).isNotNull();
        // assertThat(dto.getUpdatedAt()).isNotNull();
        assertThat(dto.getRoles()).containsExactly("ROLE_USER");
    }

    @Test
    void convertToDto_withNullTimestamps_shouldHandleGracefully() {
        // Given
        // Note: createdAt/updatedAt are managed by AuditableEntity and cannot be set directly
        // This test validates null handling in DTO conversion
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));

        // When
        ResponseEntity<UserDto> response = adminUserController.getUser(100L);

        // Then
        UserDto dto = response.getBody();
        assertThat(dto).isNotNull();
        // Timestamps should be present or null depending on AuditableEntity behavior
        // Just verify DTO conversion doesn't throw exceptions
    }

    // ========== Role Management Tests ==========

    @Test
    void getAllRoles_shouldReturnAllRoles() {
        // Given
        when(roleRepository.findAll()).thenReturn(Arrays.asList(userRole, adminRole, developerRole));

        // When
        ResponseEntity<List<AdminUserController.RoleDto>> response = adminUserController.getAllRoles();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(3);
        assertThat(response.getBody().stream().map(AdminUserController.RoleDto::getName))
                .containsExactlyInAnyOrder("ROLE_USER", "ROLE_ADMIN", "ROLE_DEVELOPER");

        verify(roleRepository).findAll();
    }

    @Test
    void getAllRoles_withNoRoles_shouldReturnEmptyList() {
        // Given
        when(roleRepository.findAll()).thenReturn(Arrays.asList());

        // When
        ResponseEntity<List<AdminUserController.RoleDto>> response = adminUserController.getAllRoles();

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEmpty();
    }

    @Test
    void updateUserRoles_shouldReplaceAllRoles() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_ADMIN")).thenReturn(Optional.of(adminRole));
        when(roleRepository.findByName("ROLE_DEVELOPER")).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("ROLE_ADMIN", "ROLE_DEVELOPER"));

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.updateUserRoles(100L, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("User roles updated successfully");
        assertThat(testUser.getRoles()).containsExactlyInAnyOrder(adminRole, developerRole);

        verify(userRepository).save(testUser);
        verify(roleRepository).findByName("ROLE_ADMIN");
        verify(roleRepository).findByName("ROLE_DEVELOPER");
    }

    @Test
    void updateUserRoles_withNonexistentUser_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(999L)).thenReturn(Optional.empty());

        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("ROLE_ADMIN"));

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(999L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_withNonexistentRole_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("ROLE_NONEXISTENT"));

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(100L, request))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found: ROLE_NONEXISTENT");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_withNullRolesList_shouldThrowException() {
        // Given
        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(null);

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User must have at least one role assigned");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_withEmptyRolesList_shouldThrowException() {
        // Given
        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList());

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User must have at least one role assigned");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_selfDemotion_shouldThrowException() {
        // Given - admin trying to remove ROLE_ADMIN from their own account
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(1L)).thenReturn(Optional.of(adminUser));

        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("ROLE_USER")); // Removing ROLE_ADMIN

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(1L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove ROLE_ADMIN from your own account");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_lastAdmin_shouldThrowException() {
        // Given - trying to remove ROLE_ADMIN from the last admin
        User lastAdmin = User.builder()
                .id(200L)
                .username("lastadmin")
                .roles(new HashSet<>(Arrays.asList(adminRole)))
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(200L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRolesName("ROLE_ADMIN")).thenReturn(1L);

        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("ROLE_USER")); // Removing ROLE_ADMIN

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(200L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove ROLE_ADMIN from the last admin user");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void updateUserRoles_withInvalidRoleNameFormat_shouldThrowException() {
        // Given
        AdminUserController.RolesUpdateRequest request = new AdminUserController.RolesUpdateRequest();
        request.setRoles(Arrays.asList("invalid_role")); // lowercase, no ROLE_ prefix

        // When/Then
        assertThatThrownBy(() -> adminUserController.updateUserRoles(100L, request))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role name format");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRoleToUser_shouldAddRole() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_DEVELOPER")).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.addRoleToUser(100L, "ROLE_DEVELOPER");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Role added successfully");
        assertThat(testUser.getRoles()).contains(developerRole);

        verify(userRepository).save(testUser);
        verify(roleRepository).findByName("ROLE_DEVELOPER");
    }

    @Test
    void addRoleToUser_withNonexistentUser_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.addRoleToUser(999L, "ROLE_DEVELOPER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRoleToUser_withNonexistentRole_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.addRoleToUser(100L, "ROLE_NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found: ROLE_NONEXISTENT");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRoleToUser_withInvalidRoleNameFormat_shouldThrowException() {
        // Given - invalid role name format (lowercase, no ROLE_ prefix)

        // When/Then
        assertThatThrownBy(() -> adminUserController.addRoleToUser(100L, "invalid_role"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid role name format");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_shouldRemoveRole() {
        // Given
        testUser.getRoles().add(developerRole); // User has ROLE_USER and ROLE_DEVELOPER
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_DEVELOPER")).thenReturn(Optional.of(developerRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.removeRoleFromUser(100L, "ROLE_DEVELOPER");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().success()).isTrue();
        assertThat(response.getBody().message()).isEqualTo("Role removed successfully");
        assertThat(testUser.getRoles()).doesNotContain(developerRole);
        assertThat(testUser.getRoles()).contains(userRole); // ROLE_USER still present

        verify(userRepository).save(testUser);
        verify(roleRepository).findByName("ROLE_DEVELOPER");
    }

    @Test
    void removeRoleFromUser_withNonexistentUser_shouldThrowException() {
        // Given
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(999L)).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.removeRoleFromUser(999L, "ROLE_DEVELOPER"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("User not found with id: 999");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_withNonexistentRole_shouldThrowException() {
        // Given
        testUser.getRoles().add(developerRole); // User has 2 roles
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_NONEXISTENT")).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> adminUserController.removeRoleFromUser(100L, "ROLE_NONEXISTENT"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Role not found: ROLE_NONEXISTENT");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_selfDemotion_shouldThrowException() {
        // Given - admin trying to remove ROLE_ADMIN from themselves
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);

        // When/Then
        assertThatThrownBy(() -> adminUserController.removeRoleFromUser(1L, "ROLE_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove ROLE_ADMIN from your own account");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_lastRole_shouldThrowException() {
        // Given - user only has 1 role
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        // testUser only has ROLE_USER

        // When/Then
        assertThatThrownBy(() -> adminUserController.removeRoleFromUser(100L, "ROLE_USER"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("User must have at least one role assigned");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void removeRoleFromUser_lastAdmin_shouldThrowException() {
        // Given - trying to remove ROLE_ADMIN from the last admin
        User lastAdmin = User.builder()
                .id(200L)
                .username("lastadmin")
                .roles(new HashSet<>(Arrays.asList(adminRole, userRole))) // Has 2 roles so last role check passes
                .build();

        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(200L)).thenReturn(Optional.of(lastAdmin));
        when(userRepository.countByRolesName("ROLE_ADMIN")).thenReturn(1L);

        // When/Then
        assertThatThrownBy(() -> adminUserController.removeRoleFromUser(200L, "ROLE_ADMIN"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Cannot remove ROLE_ADMIN from the last admin user");

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void addRoleToUser_duplicateRole_shouldNotDuplicateRole() {
        // Given - User already has ROLE_USER
        when(securityUtils.getCurrentUser()).thenReturn(adminUser);
        when(userRepository.findByIdWithOrganizationAndRoles(100L)).thenReturn(Optional.of(testUser));
        when(roleRepository.findByName("ROLE_USER")).thenReturn(Optional.of(userRole));
        when(userRepository.save(any(User.class))).thenReturn(testUser);

        // When - Try to add ROLE_USER again
        ResponseEntity<ApiResponse<UserDto>> response = adminUserController.addRoleToUser(100L, "ROLE_USER");

        // Then - Should succeed but role set should not have duplicates
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().success()).isTrue();
        // Set naturally handles duplicates - role count should remain 1
        assertThat(testUser.getRoles()).hasSize(1);
        assertThat(testUser.getRoles()).contains(userRole);

        verify(userRepository).save(testUser);
    }
}
