package org.sensorvision.controller;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.exception.FileStorageException;
import org.sensorvision.model.User;
import org.sensorvision.repository.UserRepository;
import org.sensorvision.service.FileStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * REST controller for managing user avatar images.
 * Provides endpoints for uploading, retrieving, and deleting avatar images.
 */
@RestController
@RequestMapping("/api/v1/users")
@CrossOrigin(origins = "*", maxAge = 3600)
@Slf4j
public class AvatarController {

    @Autowired
    private FileStorageService fileStorageService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Upload or update avatar for a user
     *
     * @param userId User ID
     * @param file   Avatar image file
     * @return Upload response with avatar details
     */
    @PostMapping("/{userId}/avatar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> uploadAvatar(
            @PathVariable Long userId,
            @RequestParam("file") MultipartFile file) {

        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            // Find the user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Security check: Users can only update their own avatar unless they're admin
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!user.getUsername().equals(currentUsername) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only update your own avatar"));
            }

            // Delete old avatar if exists
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                try {
                    fileStorageService.deleteAvatar(user.getAvatarUrl());
                } catch (Exception e) {
                    log.warn("Failed to delete old avatar: {}", e.getMessage());
                }
            }

            // Store new avatar
            String filename = fileStorageService.storeAvatar(userId, file);

            // Update user record
            user.setAvatarUrl(filename);
            user.setAvatarVersion(System.currentTimeMillis());
            userRepository.save(user);

            // Return response
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Avatar uploaded successfully");
            response.put("avatarUrl", "/api/v1/users/" + userId + "/avatar");
            response.put("avatarVersion", user.getAvatarVersion());
            response.put("uploadedAt", Instant.now().toString());
            response.put("fileSize", file.getSize());

            log.info("Avatar uploaded successfully for user {}", userId);
            return ResponseEntity.ok(response);

        } catch (FileStorageException e) {
            log.error("File storage error for user {}: {}", userId, e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            log.error("Unexpected error uploading avatar for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to upload avatar: " + e.getMessage()));
        }
    }

    /**
     * Retrieve avatar image for a user
     *
     * @param userId User ID
     * @return Avatar image bytes
     */
    @GetMapping("/{userId}/avatar")
    public ResponseEntity<byte[]> getAvatar(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            if (user.getAvatarUrl() == null || user.getAvatarUrl().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            byte[] avatarBytes = fileStorageService.loadAvatar(user.getAvatarUrl());

            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(avatarBytes);

        } catch (FileStorageException e) {
            log.error("Failed to load avatar for user {}: {}", userId, e.getMessage());
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            log.error("Unexpected error loading avatar for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * Delete avatar for a user
     *
     * @param userId User ID
     * @return Success response
     */
    @DeleteMapping("/{userId}/avatar")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<?> deleteAvatar(@PathVariable Long userId) {
        try {
            // Get current authenticated user
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            String currentUsername = authentication.getName();

            // Find the user
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Security check
            boolean isAdmin = authentication.getAuthorities().stream()
                    .anyMatch(auth -> auth.getAuthority().equals("ROLE_ADMIN"));

            if (!user.getUsername().equals(currentUsername) && !isAdmin) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "You can only delete your own avatar"));
            }

            // Delete avatar file if exists
            if (user.getAvatarUrl() != null && !user.getAvatarUrl().isEmpty()) {
                fileStorageService.deleteAvatar(user.getAvatarUrl());
            }

            // Update user record
            user.setAvatarUrl(null);
            user.setAvatarVersion(System.currentTimeMillis());
            userRepository.save(user);

            log.info("Avatar deleted successfully for user {}", userId);
            return ResponseEntity.ok()
                    .body(Map.of("success", true, "message", "Avatar deleted successfully"));

        } catch (Exception e) {
            log.error("Error deleting avatar for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete avatar: " + e.getMessage()));
        }
    }

    /**
     * Check if user has an avatar
     *
     * @param userId User ID
     * @return Status response
     */
    @GetMapping("/{userId}/avatar/status")
    public ResponseEntity<?> getAvatarStatus(@PathVariable Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            boolean hasAvatar = user.getAvatarUrl() != null &&
                               !user.getAvatarUrl().isEmpty() &&
                               fileStorageService.avatarExists(user.getAvatarUrl());

            Map<String, Object> response = new HashMap<>();
            response.put("hasAvatar", hasAvatar);
            response.put("avatarUrl", hasAvatar ? "/api/v1/users/" + userId + "/avatar" : null);
            response.put("avatarVersion", user.getAvatarVersion());

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error checking avatar status for user {}", userId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to check avatar status"));
        }
    }
}
