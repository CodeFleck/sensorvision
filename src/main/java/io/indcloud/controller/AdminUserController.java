package io.indcloud.controller;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.dto.ApiResponse;
import io.indcloud.dto.UserDto;
import io.indcloud.exception.BadRequestException;
import io.indcloud.exception.ResourceNotFoundException;
import io.indcloud.model.Role;
import io.indcloud.model.TrashLog;
import io.indcloud.model.User;
import io.indcloud.repository.RoleRepository;
import io.indcloud.repository.UserRepository;
import io.indcloud.security.SecurityUtils;
import io.indcloud.service.TrashService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";
    private static final Pattern ROLE_NAME_PATTERN = Pattern.compile("^ROLE_[A-Z_]{1,50}$");

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SecurityUtils securityUtils;
    private final TrashService trashService;

    public AdminUserController(UserRepository userRepository, RoleRepository roleRepository,
                               SecurityUtils securityUtils, TrashService trashService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.securityUtils = securityUtils;
        this.trashService = trashService;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        // Only return non-deleted users
        List<User> users = userRepository.findAllActiveWithOrganizationAndRoles();
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        User user = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));
        return ResponseEntity.ok(convertToDto(user));
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<ApiResponse<UserDto>> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(true);
        userRepository.save(user);

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "User enabled successfully"));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<ApiResponse<UserDto>> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        user.setEnabled(false);
        userRepository.save(user);

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "User disabled successfully"));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        userRepository.save(user);

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "User updated successfully"));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<SoftDeleteResponse>> deleteUser(
            @PathVariable Long userId,
            @RequestParam(required = false) String reason) {
        User currentUser = securityUtils.getCurrentUser();

        // Prevent self-deletion
        if (userId.equals(currentUser.getId())) {
            throw new BadRequestException("Cannot delete your own account");
        }

        User user = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Don't allow deleting already deleted users
        if (user.isDeleted()) {
            throw new BadRequestException("User is already deleted");
        }

        // Check if this is the last admin user
        boolean userIsAdmin = user.getRoles().stream()
                .anyMatch(r -> ROLE_ADMIN.equals(r.getName()));

        if (userIsAdmin) {
            long adminCount = userRepository.countByRolesName(ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot delete the last admin user");
            }
        }

        // Use soft delete instead of hard delete
        TrashLog trashLog = trashService.softDeleteUser(userId, reason);

        SoftDeleteResponse response = new SoftDeleteResponse(
                trashLog.getId(),
                trashLog.getEntityType(),
                trashLog.getEntityName(),
                trashLog.getExpiresAt().toString(),
                trashLog.getDaysRemaining()
        );

        return ResponseEntity.ok(ApiResponse.success(
                response,
                "User moved to trash. You can restore it within " + TrashLog.RETENTION_DAYS + " days."));
    }

    /**
     * Response for soft delete operations, including undo info.
     */
    public record SoftDeleteResponse(
            Long trashId,
            String entityType,
            String entityName,
            String expiresAt,
            long daysRemaining
    ) {}

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<UserDto>> getUsersByOrganization(@PathVariable Long organizationId) {
        List<User> users = userRepository.findByOrganizationIdWithOrganizationAndRoles(organizationId);
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    /**
     * Get all available roles in the system.
     */
    @GetMapping("/roles")
    public ResponseEntity<List<RoleDto>> getAllRoles() {
        List<Role> roles = roleRepository.findAll();
        List<RoleDto> roleDtos = roles.stream()
                .map(role -> new RoleDto(role.getId(), role.getName(), role.getDescription()))
                .collect(Collectors.toList());
        return ResponseEntity.ok(roleDtos);
    }

    /**
     * Update roles for a specific user.
     * Replaces all existing roles with the provided list.
     */
    @PutMapping("/{userId}/roles")
    public ResponseEntity<ApiResponse<UserDto>> updateUserRoles(
            @PathVariable Long userId,
            @RequestBody RolesUpdateRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        // Validate request
        if (request.getRoles() == null || request.getRoles().isEmpty()) {
            throw new BadRequestException("User must have at least one role assigned");
        }

        // Validate role name format
        for (String roleName : request.getRoles()) {
            validateRoleName(roleName);
        }

        User user = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Get current roles for logging
        List<String> oldRoles = user.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toList());

        // Prevent self-demotion from admin role
        boolean currentUserIsTarget = userId.equals(currentUser.getId());
        boolean wouldRemoveAdminFromSelf = currentUserIsTarget && !request.getRoles().contains(ROLE_ADMIN);

        if (wouldRemoveAdminFromSelf) {
            throw new BadRequestException("Cannot remove ROLE_ADMIN from your own account");
        }

        // Check if removing admin from user would leave no admins
        boolean userCurrentlyHasAdmin = user.getRoles().stream()
                .anyMatch(r -> ROLE_ADMIN.equals(r.getName()));
        boolean newRolesContainAdmin = request.getRoles().contains(ROLE_ADMIN);

        if (userCurrentlyHasAdmin && !newRolesContainAdmin) {
            long adminCount = userRepository.countByRolesName(ROLE_ADMIN);
            if (adminCount <= 1) {
                throw new BadRequestException("Cannot remove ROLE_ADMIN from the last admin user");
            }
        }

        // Validate and collect new roles
        Set<Role> newRoles = new HashSet<>();
        for (String roleName : request.getRoles()) {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));
            newRoles.add(role);
        }

        // Update user's roles
        user.setRoles(newRoles);
        userRepository.save(user);

        log.info("ROLE_CHANGE: Admin {} updated roles for user {} (id: {}) from {} to {}",
                currentUser.getUsername(), user.getUsername(), userId, oldRoles, request.getRoles());

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "User roles updated successfully"));
    }

    /**
     * Add a single role to a user.
     */
    @PostMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDto>> addRoleToUser(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        User currentUser = securityUtils.getCurrentUser();

        // Validate role name format
        validateRoleName(roleName);

        User user = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        user.getRoles().add(role);
        userRepository.save(user);

        log.info("ROLE_CHANGE: Admin {} added role {} to user {} (id: {})",
                currentUser.getUsername(), roleName, user.getUsername(), userId);

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "Role added successfully"));
    }

    /**
     * Remove a single role from a user.
     */
    @DeleteMapping("/{userId}/roles/{roleName}")
    public ResponseEntity<ApiResponse<UserDto>> removeRoleFromUser(
            @PathVariable Long userId,
            @PathVariable String roleName) {
        User currentUser = securityUtils.getCurrentUser();

        // Validate role name format
        validateRoleName(roleName);

        // Prevent self-demotion from admin role (check early before DB lookup)
        if (userId.equals(currentUser.getId()) && ROLE_ADMIN.equals(roleName)) {
            throw new BadRequestException("Cannot remove ROLE_ADMIN from your own account");
        }

        User user = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + userId));

        // Check if removing admin would leave no admins
        if (ROLE_ADMIN.equals(roleName)) {
            boolean userHasAdmin = user.getRoles().stream()
                    .anyMatch(r -> ROLE_ADMIN.equals(r.getName()));

            if (userHasAdmin) {
                long adminCount = userRepository.countByRolesName(ROLE_ADMIN);
                if (adminCount <= 1) {
                    throw new BadRequestException("Cannot remove ROLE_ADMIN from the last admin user");
                }
            }
        }

        // Prevent removing the last role
        if (user.getRoles().size() <= 1) {
            throw new BadRequestException("User must have at least one role assigned");
        }

        Role role = roleRepository.findByName(roleName)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + roleName));

        user.getRoles().remove(role);
        userRepository.save(user);

        log.info("ROLE_CHANGE: Admin {} removed role {} from user {} (id: {})",
                currentUser.getUsername(), roleName, user.getUsername(), userId);

        // Re-fetch with eager loading for DTO conversion
        User savedUser = userRepository.findByIdWithOrganizationAndRoles(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found after save"));

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(savedUser),
                "Role removed successfully"));
    }

    /**
     * Validates that a role name follows the expected format: ROLE_[A-Z_]{1,50}
     */
    private void validateRoleName(String roleName) {
        if (roleName == null || !ROLE_NAME_PATTERN.matcher(roleName).matches()) {
            throw new BadRequestException("Invalid role name format: " + roleName);
        }
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        if (user.getOrganization() != null) {
            dto.setOrganizationId(user.getOrganization().getId());
            dto.setOrganizationName(user.getOrganization().getName());
        }
        dto.setEnabled(user.getEnabled());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setCreatedAt(user.getCreatedAt() != null
                ? LocalDateTime.ofInstant(user.getCreatedAt(), java.time.ZoneId.systemDefault())
                : null);
        dto.setUpdatedAt(user.getUpdatedAt() != null
                ? LocalDateTime.ofInstant(user.getUpdatedAt(), java.time.ZoneId.systemDefault())
                : null);
        dto.setLastLoginAt(null); // lastLoginAt field doesn't exist in User entity yet
        dto.setRoles(user.getRoles().stream()
                .map(role -> role.getName())
                .collect(Collectors.toList()));
        return dto;
    }

    // Inner class for update request
    public static class UserUpdateRequest {
        private String firstName;
        private String lastName;
        private String email;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(String firstName) {
            this.firstName = firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public void setLastName(String lastName) {
            this.lastName = lastName;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }
    }

    // Inner class for roles update request
    public static class RolesUpdateRequest {
        private List<String> roles;

        public List<String> getRoles() {
            return roles;
        }

        public void setRoles(List<String> roles) {
            this.roles = roles;
        }
    }

    // Inner class for role DTO
    public static class RoleDto {
        private Long id;
        private String name;
        private String description;

        public RoleDto(Long id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }

        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }
}
