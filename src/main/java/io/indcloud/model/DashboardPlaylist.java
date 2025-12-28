package io.indcloud.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dashboard_playlists")
public class DashboardPlaylist extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id")
    private User createdBy;

    // Sharing capabilities
    @Column(name = "is_public")
    private Boolean isPublic = false;

    @Column(name = "public_share_token", unique = true)
    private String publicShareToken;

    @Column(name = "share_expires_at")
    private LocalDateTime shareExpiresAt;

    // Playlist behavior
    @Column(name = "loop_enabled")
    private Boolean loopEnabled = true;

    @Column(name = "transition_effect")
    private String transitionEffect = "fade"; // fade, slide, none

    @OneToMany(mappedBy = "playlist", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("position ASC")
    private List<DashboardPlaylistItem> items = new ArrayList<>();

    // Constructors
    public DashboardPlaylist() {
    }

    public DashboardPlaylist(String name, String description) {
        this.name = name;
        this.description = description;
    }

    // Getters and Setters
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

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }

    public Boolean getIsPublic() {
        return isPublic;
    }

    public void setIsPublic(Boolean isPublic) {
        this.isPublic = isPublic;
    }

    public String getPublicShareToken() {
        return publicShareToken;
    }

    public void setPublicShareToken(String publicShareToken) {
        this.publicShareToken = publicShareToken;
    }

    public LocalDateTime getShareExpiresAt() {
        return shareExpiresAt;
    }

    public void setShareExpiresAt(LocalDateTime shareExpiresAt) {
        this.shareExpiresAt = shareExpiresAt;
    }

    public Boolean getLoopEnabled() {
        return loopEnabled;
    }

    public void setLoopEnabled(Boolean loopEnabled) {
        this.loopEnabled = loopEnabled;
    }

    public String getTransitionEffect() {
        return transitionEffect;
    }

    public void setTransitionEffect(String transitionEffect) {
        this.transitionEffect = transitionEffect;
    }

    public List<DashboardPlaylistItem> getItems() {
        return items;
    }

    /**
     * IMPORTANT: With orphanRemoval=true, we must NEVER replace the collection instance.
     * This method clears the existing collection and adds all new items properly.
     */
    public void setItems(List<DashboardPlaylistItem> newItems) {
        if (newItems == null) {
            this.items.clear();
            return;
        }

        // Remove items that are no longer in the new list
        this.items.removeIf(existingItem ->
            newItems.stream().noneMatch(i -> i.getId() != null && i.getId().equals(existingItem.getId()))
        );

        // Add or update items
        for (DashboardPlaylistItem newItem : newItems) {
            if (newItem.getId() == null) {
                // New item - add it
                addItem(newItem);
            } else {
                // Existing item - check if it's already in the collection
                boolean exists = this.items.stream()
                    .anyMatch(i -> i.getId().equals(newItem.getId()));
                if (!exists) {
                    addItem(newItem);
                }
            }
        }
    }

    // Helper methods
    public void addItem(DashboardPlaylistItem item) {
        items.add(item);
        item.setPlaylist(this);
    }

    public void removeItem(DashboardPlaylistItem item) {
        items.remove(item);
        item.setPlaylist(null);
    }

    /**
     * Reorders playlist items to maintain continuous positions (0, 1, 2, ...)
     * Call this after removing items to prevent gaps in positions.
     */
    public void reorderItems() {
        for (int i = 0; i < items.size(); i++) {
            items.get(i).setPosition(i);
        }
    }
}
