package org.sensorvision.controller;

import org.sensorvision.dto.ApiResponse;
import org.sensorvision.dto.UserDto;
import org.sensorvision.model.User;
import org.sensorvision.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserRepository userRepository;

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public ResponseEntity<List<UserDto>> getAllUsers() {
        List<User> users = userRepository.findAll();
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserDto> getUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
        return ResponseEntity.ok(convertToDto(user));
    }

    @PutMapping("/{userId}/enable")
    public ResponseEntity<ApiResponse<UserDto>> enableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEnabled(true);
        user = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(user),
                "User enabled successfully"
        ));
    }

    @PutMapping("/{userId}/disable")
    public ResponseEntity<ApiResponse<UserDto>> disableUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        user.setEnabled(false);
        user = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(user),
                "User disabled successfully"
        ));
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable Long userId,
            @RequestBody UserUpdateRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        if (request.getFirstName() != null) {
            user.setFirstName(request.getFirstName());
        }
        if (request.getLastName() != null) {
            user.setLastName(request.getLastName());
        }
        if (request.getEmail() != null) {
            user.setEmail(request.getEmail());
        }

        user = userRepository.save(user);

        return ResponseEntity.ok(ApiResponse.success(
                convertToDto(user),
                "User updated successfully"
        ));
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        userRepository.delete(user);

        return ResponseEntity.ok(ApiResponse.success(
                null,
                "User deleted successfully"
        ));
    }

    @GetMapping("/organization/{organizationId}")
    public ResponseEntity<List<UserDto>> getUsersByOrganization(@PathVariable Long organizationId) {
        List<User> users = userRepository.findByOrganizationId(organizationId);
        List<UserDto> userDtos = users.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(userDtos);
    }

    private UserDto convertToDto(User user) {
        UserDto dto = new UserDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setOrganizationId(user.getOrganization().getId());
        dto.setOrganizationName(user.getOrganization().getName());
        dto.setEnabled(user.getEnabled());
        dto.setEmailVerified(user.getEmailVerified());
        dto.setCreatedAt(user.getCreatedAt() != null ?
            LocalDateTime.ofInstant(user.getCreatedAt(), java.time.ZoneId.systemDefault()) : null);
        dto.setUpdatedAt(user.getUpdatedAt() != null ?
            LocalDateTime.ofInstant(user.getUpdatedAt(), java.time.ZoneId.systemDefault()) : null);
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

        public String getFirstName() { return firstName; }
        public void setFirstName(String firstName) { this.firstName = firstName; }
        public String getLastName() { return lastName; }
        public void setLastName(String lastName) { this.lastName = lastName; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
    }
}
