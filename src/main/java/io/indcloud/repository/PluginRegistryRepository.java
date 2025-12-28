package io.indcloud.repository;

import io.indcloud.model.PluginCategory;
import io.indcloud.model.PluginProvider;
import io.indcloud.model.PluginRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRegistryRepository extends JpaRepository<PluginRegistry, Long> {

    Optional<PluginRegistry> findByPluginKey(String pluginKey);

    List<PluginRegistry> findByCategory(PluginCategory category);

    List<PluginRegistry> findByPluginProvider(PluginProvider provider);

    List<PluginRegistry> findByIsOfficial(Boolean isOfficial);

    @Query("SELECT p FROM PluginRegistry p WHERE p.isOfficial = true OR p.isVerified = true ORDER BY p.publishedAt DESC")
    List<PluginRegistry> findOfficialAndVerified();

    @Query("SELECT p FROM PluginRegistry p WHERE " +
           "LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.pluginKey) LIKE LOWER(CONCAT('%', :search, '%'))")
    List<PluginRegistry> searchPlugins(@Param("search") String search);

    @Query("SELECT p FROM PluginRegistry p WHERE " +
           "p.category = :category AND " +
           "(LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')))")
    List<PluginRegistry> searchPluginsByCategory(@Param("category") PluginCategory category,
                                                 @Param("search") String search);

    @Query("SELECT p FROM PluginRegistry p ORDER BY p.installationCount DESC")
    List<PluginRegistry> findMostPopular();

    @Query("SELECT p FROM PluginRegistry p ORDER BY p.ratingAverage DESC, p.ratingCount DESC")
    List<PluginRegistry> findTopRated();

    @Query("SELECT p FROM PluginRegistry p ORDER BY p.publishedAt DESC")
    List<PluginRegistry> findRecent();

    /**
     * Atomically update installation count based on actual count from installed_plugins table
     * This prevents race conditions when multiple users install the same plugin concurrently
     */
    @Modifying
    @Query("UPDATE PluginRegistry p SET p.installationCount = " +
           "(SELECT COUNT(ip) FROM InstalledPlugin ip WHERE ip.pluginKey = :pluginKey) " +
           "WHERE p.pluginKey = :pluginKey")
    int updateInstallationCount(@Param("pluginKey") String pluginKey);
}
