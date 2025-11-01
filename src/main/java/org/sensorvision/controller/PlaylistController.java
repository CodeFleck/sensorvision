package org.sensorvision.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.*;
import org.sensorvision.service.DashboardPlaylistService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/playlists")
@RequiredArgsConstructor
@Tag(name = "Playlists", description = "Dashboard playlist management for production floor displays")
@SecurityRequirement(name = "Bearer Authentication")
public class PlaylistController {

    private final DashboardPlaylistService playlistService;

    /**
     * Get all playlists for current user
     */
    @GetMapping
    @Operation(summary = "Get all playlists", description = "Get all playlists created by the current user")
    public List<PlaylistResponse> getAllPlaylists() {
        return playlistService.getAllPlaylists();
    }

    /**
     * Get public playlists
     */
    @GetMapping("/public")
    @Operation(summary = "Get public playlists", description = "Get all publicly shared playlists")
    public List<PlaylistResponse> getPublicPlaylists() {
        return playlistService.getPublicPlaylists();
    }

    /**
     * Get a specific playlist by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get playlist by ID", description = "Get a specific playlist with all its dashboard items")
    public PlaylistResponse getPlaylist(@PathVariable Long id) {
        return playlistService.getPlaylistById(id);
    }

    /**
     * Get playlist by public share token (no authentication required)
     */
    @GetMapping("/shared/{token}")
    @Operation(summary = "Get shared playlist", description = "Access a playlist via public share token (no authentication)")
    public PlaylistResponse getPlaylistByToken(@PathVariable String token) {
        return playlistService.getPlaylistByToken(token);
    }

    /**
     * Create a new playlist
     */
    @PostMapping
    @Operation(summary = "Create playlist", description = "Create a new dashboard playlist")
    public ResponseEntity<PlaylistResponse> createPlaylist(@Valid @RequestBody PlaylistCreateRequest request) {
        PlaylistResponse response = playlistService.createPlaylist(
            request.name(),
            request.description(),
            request.loopEnabled(),
            request.transitionEffect()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Update playlist metadata
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update playlist", description = "Update playlist name, description, and behavior settings")
    public PlaylistResponse updatePlaylist(
        @PathVariable Long id,
        @Valid @RequestBody PlaylistUpdateRequest request
    ) {
        return playlistService.updatePlaylist(
            id,
            request.name(),
            request.description(),
            request.loopEnabled(),
            request.transitionEffect()
        );
    }

    /**
     * Delete a playlist
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete playlist", description = "Delete a playlist and all its items")
    public void deletePlaylist(@PathVariable Long id) {
        playlistService.deletePlaylist(id);
    }

    /**
     * Add a dashboard to a playlist
     */
    @PostMapping("/{id}/items")
    @Operation(summary = "Add dashboard to playlist", description = "Add a dashboard to a playlist with display duration")
    public ResponseEntity<PlaylistItemResponse> addDashboardToPlaylist(
        @PathVariable Long id,
        @Valid @RequestBody PlaylistItemCreateRequest request
    ) {
        PlaylistItemResponse response = playlistService.addDashboardToPlaylist(
            id,
            request.dashboardId(),
            request.displayDurationSeconds()
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Remove a dashboard from a playlist
     */
    @DeleteMapping("/{playlistId}/items/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove dashboard from playlist", description = "Remove a specific item from a playlist")
    public void removeDashboardFromPlaylist(
        @PathVariable Long playlistId,
        @PathVariable Long itemId
    ) {
        playlistService.removeDashboardFromPlaylist(playlistId, itemId);
    }

    /**
     * Generate public share token
     */
    @PostMapping("/{id}/share")
    @Operation(summary = "Generate share token", description = "Generate a public share token for the playlist")
    public ResponseEntity<Map<String, String>> generateShareToken(
        @PathVariable Long id,
        @Valid @RequestBody(required = false) PlaylistShareRequest request
    ) {
        String token = playlistService.generateShareToken(
            id,
            request != null ? request.expiresAt() : null
        );

        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("shareUrl", "/playlists/shared/" + token);

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke public sharing
     */
    @DeleteMapping("/{id}/share")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Revoke sharing", description = "Revoke public sharing for the playlist")
    public void revokeSharing(@PathVariable Long id) {
        playlistService.revokeSharing(id);
    }
}
