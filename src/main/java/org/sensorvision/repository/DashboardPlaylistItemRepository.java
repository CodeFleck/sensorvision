package org.sensorvision.repository;

import org.sensorvision.model.Dashboard;
import org.sensorvision.model.DashboardPlaylist;
import org.sensorvision.model.DashboardPlaylistItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface DashboardPlaylistItemRepository extends JpaRepository<DashboardPlaylistItem, Long> {

    /**
     * Find all items in a playlist, ordered by position
     */
    List<DashboardPlaylistItem> findByPlaylistOrderByPositionAsc(DashboardPlaylist playlist);

    /**
     * Find all playlists that contain a specific dashboard
     */
    @Query("SELECT pli.playlist FROM DashboardPlaylistItem pli WHERE pli.dashboard = :dashboard")
    List<DashboardPlaylist> findPlaylistsByDashboard(@Param("dashboard") Dashboard dashboard);

    /**
     * Count items in a playlist
     */
    long countByPlaylist(DashboardPlaylist playlist);

    /**
     * Delete all items for a specific playlist
     */
    void deleteByPlaylist(DashboardPlaylist playlist);

    /**
     * Find item by playlist and position
     */
    @Query("SELECT pli FROM DashboardPlaylistItem pli WHERE pli.playlist = :playlist AND pli.position = :position")
    DashboardPlaylistItem findByPlaylistAndPosition(@Param("playlist") DashboardPlaylist playlist, @Param("position") int position);
}
