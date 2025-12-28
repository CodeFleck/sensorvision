package io.indcloud.repository;

import io.indcloud.model.DashboardPlaylist;
import io.indcloud.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DashboardPlaylistRepository extends JpaRepository<DashboardPlaylist, Long> {

    /**
     * Find all playlists created by a specific user
     */
    List<DashboardPlaylist> findByCreatedByOrderByCreatedAtDesc(User user);

    /**
     * Find public playlists
     */
    List<DashboardPlaylist> findByIsPublicTrueOrderByCreatedAtDesc();

    /**
     * Find playlist by public share token (with items eagerly loaded)
     */
    @Query("SELECT p FROM DashboardPlaylist p LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.dashboard WHERE p.publicShareToken = :token")
    Optional<DashboardPlaylist> findByPublicShareToken(@Param("token") String token);

    /**
     * Find playlist with items eagerly loaded
     */
    @Query("SELECT p FROM DashboardPlaylist p LEFT JOIN FETCH p.items i LEFT JOIN FETCH i.dashboard WHERE p.id = :id")
    Optional<DashboardPlaylist> findByIdWithItems(@Param("id") Long id);

    /**
     * Check if a playlist name already exists for a user
     */
    boolean existsByNameAndCreatedBy(String name, User user);

    /**
     * Find all playlists (with items count)
     */
    @Query("SELECT p FROM DashboardPlaylist p ORDER BY p.createdAt DESC")
    List<DashboardPlaylist> findAllOrderByCreatedAtDesc();
}
