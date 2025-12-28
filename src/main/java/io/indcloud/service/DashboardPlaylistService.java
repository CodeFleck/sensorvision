package io.indcloud.service;

import io.indcloud.dto.PlaylistItemResponse;
import io.indcloud.dto.PlaylistResponse;
import io.indcloud.model.Dashboard;
import io.indcloud.model.DashboardPlaylist;
import io.indcloud.model.DashboardPlaylistItem;
import io.indcloud.model.User;
import io.indcloud.repository.DashboardPlaylistItemRepository;
import io.indcloud.repository.DashboardPlaylistRepository;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class DashboardPlaylistService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardPlaylistService.class);

    private final DashboardPlaylistRepository playlistRepository;
    private final DashboardPlaylistItemRepository playlistItemRepository;
    private final DashboardRepository dashboardRepository;
    private final SecurityUtils securityUtils;

    public DashboardPlaylistService(
        DashboardPlaylistRepository playlistRepository,
        DashboardPlaylistItemRepository playlistItemRepository,
        DashboardRepository dashboardRepository,
        SecurityUtils securityUtils
    ) {
        this.playlistRepository = playlistRepository;
        this.playlistItemRepository = playlistItemRepository;
        this.dashboardRepository = dashboardRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all playlists for current user
     */
    @Transactional(readOnly = true)
    public List<PlaylistResponse> getAllPlaylists() {
        User currentUser = securityUtils.getCurrentUser();
        return playlistRepository.findByCreatedByOrderByCreatedAtDesc(currentUser).stream()
            .map(PlaylistResponse::fromEntityWithoutItems)
            .collect(Collectors.toList());
    }

    /**
     * Get public playlists
     */
    @Transactional(readOnly = true)
    public List<PlaylistResponse> getPublicPlaylists() {
        return playlistRepository.findByIsPublicTrueOrderByCreatedAtDesc().stream()
            .map(PlaylistResponse::fromEntityWithoutItems)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific playlist by ID with items
     */
    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylistById(Long id) {
        DashboardPlaylist playlist = playlistRepository.findByIdWithItems(id)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + id));

        User currentUser = securityUtils.getCurrentUser();

        // Check access: user must own the playlist or it must be public
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId()) && !Boolean.TRUE.equals(playlist.getIsPublic())) {
            throw new AccessDeniedException("Access denied to playlist: " + id);
        }

        return PlaylistResponse.fromEntity(playlist);
    }

    /**
     * Get playlist by public share token (no authentication required)
     */
    @Transactional(readOnly = true)
    public PlaylistResponse getPlaylistByToken(String token) {
        DashboardPlaylist playlist = playlistRepository.findByPublicShareToken(token)
            .orElseThrow(() -> new RuntimeException("Playlist not found with token: " + token));

        // Check if token is expired
        if (playlist.getShareExpiresAt() != null && playlist.getShareExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("Playlist share link has expired");
        }

        return PlaylistResponse.fromEntity(playlist);
    }

    /**
     * Create a new playlist
     */
    @Transactional
    public PlaylistResponse createPlaylist(String name, String description, Boolean loopEnabled, String transitionEffect) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Creating new playlist: {} for user: {}", name, currentUser.getEmail());

        DashboardPlaylist playlist = new DashboardPlaylist(name, description);
        playlist.setCreatedBy(currentUser);
        if (loopEnabled != null) {
            playlist.setLoopEnabled(loopEnabled);
        }
        if (transitionEffect != null) {
            playlist.setTransitionEffect(transitionEffect);
        }

        DashboardPlaylist saved = playlistRepository.save(playlist);
        logger.info("Playlist created with id: {}", saved.getId());

        return PlaylistResponse.fromEntity(saved);
    }

    /**
     * Update playlist metadata
     */
    @Transactional
    public PlaylistResponse updatePlaylist(Long id, String name, String description, Boolean loopEnabled, String transitionEffect) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Updating playlist: {}", id);

        DashboardPlaylist playlist = playlistRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + id));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + id);
        }

        if (name != null) {
            playlist.setName(name);
        }
        if (description != null) {
            playlist.setDescription(description);
        }
        if (loopEnabled != null) {
            playlist.setLoopEnabled(loopEnabled);
        }
        if (transitionEffect != null) {
            playlist.setTransitionEffect(transitionEffect);
        }

        return PlaylistResponse.fromEntity(playlistRepository.save(playlist));
    }

    /**
     * Delete a playlist
     */
    @Transactional
    public void deletePlaylist(Long id) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Deleting playlist: {}", id);

        DashboardPlaylist playlist = playlistRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + id));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + id);
        }

        playlistRepository.delete(playlist);
        logger.info("Playlist deleted: {}", id);
    }

    /**
     * Add a dashboard to a playlist
     */
    @Transactional
    public PlaylistItemResponse addDashboardToPlaylist(Long playlistId, Long dashboardId, Integer displayDurationSeconds) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Adding dashboard {} to playlist {}", dashboardId, playlistId);

        DashboardPlaylist playlist = playlistRepository.findByIdWithItems(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + playlistId));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + playlistId);
        }

        Dashboard dashboard = dashboardRepository.findById(dashboardId)
            .orElseThrow(() -> new RuntimeException("Dashboard not found with id: " + dashboardId));

        // Calculate next position
        int nextPosition = playlist.getItems().size();

        // Create playlist item
        DashboardPlaylistItem item = new DashboardPlaylistItem();
        item.setDashboard(dashboard);
        item.setPosition(nextPosition);
        item.setDisplayDurationSeconds(displayDurationSeconds != null ? displayDurationSeconds : 30);

        playlist.addItem(item);
        playlistRepository.save(playlist);

        logger.info("Dashboard added to playlist at position {}", nextPosition);
        return PlaylistItemResponse.fromEntity(item);
    }

    /**
     * Remove a dashboard from a playlist
     */
    @Transactional
    public void removeDashboardFromPlaylist(Long playlistId, Long itemId) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Removing item {} from playlist {}", itemId, playlistId);

        DashboardPlaylist playlist = playlistRepository.findByIdWithItems(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + playlistId));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + playlistId);
        }

        DashboardPlaylistItem item = playlistItemRepository.findById(itemId)
            .orElseThrow(() -> new RuntimeException("Playlist item not found with id: " + itemId));

        playlist.removeItem(item);
        playlist.reorderItems(); // Reorder to maintain continuous positions

        playlistRepository.save(playlist);
        logger.info("Item removed from playlist");
    }

    /**
     * Generate a public share token for a playlist
     */
    @Transactional
    public String generateShareToken(Long playlistId, LocalDateTime expiresAt) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Generating share token for playlist: {}", playlistId);

        DashboardPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + playlistId));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + playlistId);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        playlist.setPublicShareToken(token);
        playlist.setIsPublic(true);
        playlist.setShareExpiresAt(expiresAt);

        playlistRepository.save(playlist);
        logger.info("Share token generated: {}", token);

        return token;
    }

    /**
     * Revoke public sharing for a playlist
     */
    @Transactional
    public void revokeSharing(Long playlistId) {
        User currentUser = securityUtils.getCurrentUser();
        logger.info("Revoking sharing for playlist: {}", playlistId);

        DashboardPlaylist playlist = playlistRepository.findById(playlistId)
            .orElseThrow(() -> new RuntimeException("Playlist not found with id: " + playlistId));

        // Verify ownership
        if (!playlist.getCreatedBy().getId().equals(currentUser.getId())) {
            throw new AccessDeniedException("Access denied to playlist: " + playlistId);
        }

        playlist.setPublicShareToken(null);
        playlist.setIsPublic(false);
        playlist.setShareExpiresAt(null);

        playlistRepository.save(playlist);
        logger.info("Sharing revoked for playlist: {}", playlistId);
    }
}
