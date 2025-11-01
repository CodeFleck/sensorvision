package org.sensorvision.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(
    name = "dashboard_playlist_items",
    uniqueConstraints = @UniqueConstraint(columnNames = {"playlist_id", "position"})
)
public class DashboardPlaylistItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "playlist_id", nullable = false)
    private DashboardPlaylist playlist;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dashboard_id", nullable = false)
    private Dashboard dashboard;

    @Column(nullable = false)
    private Integer position;

    @Column(name = "display_duration_seconds", nullable = false)
    private Integer displayDurationSeconds = 30;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
    }

    // Constructors
    public DashboardPlaylistItem() {
    }

    public DashboardPlaylistItem(Dashboard dashboard, Integer position, Integer displayDurationSeconds) {
        this.dashboard = dashboard;
        this.position = position;
        this.displayDurationSeconds = displayDurationSeconds;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DashboardPlaylist getPlaylist() {
        return playlist;
    }

    public void setPlaylist(DashboardPlaylist playlist) {
        this.playlist = playlist;
    }

    public Dashboard getDashboard() {
        return dashboard;
    }

    public void setDashboard(Dashboard dashboard) {
        this.dashboard = dashboard;
    }

    public Integer getPosition() {
        return position;
    }

    public void setPosition(Integer position) {
        this.position = position;
    }

    public Integer getDisplayDurationSeconds() {
        return displayDurationSeconds;
    }

    public void setDisplayDurationSeconds(Integer displayDurationSeconds) {
        this.displayDurationSeconds = displayDurationSeconds;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
