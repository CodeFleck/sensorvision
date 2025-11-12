package org.sensorvision.repository;

import org.sensorvision.model.Organization;
import org.sensorvision.model.PluginRating;
import org.sensorvision.model.PluginRegistry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface PluginRatingRepository extends JpaRepository<PluginRating, Long> {

    List<PluginRating> findByPluginRegistry(PluginRegistry pluginRegistry);

    Optional<PluginRating> findByPluginRegistryAndOrganization(PluginRegistry pluginRegistry,
                                                               Organization organization);

    @Query("SELECT AVG(pr.rating) FROM PluginRating pr WHERE pr.pluginRegistry = :pluginRegistry")
    BigDecimal calculateAverageRating(@Param("pluginRegistry") PluginRegistry pluginRegistry);

    @Query("SELECT COUNT(pr) FROM PluginRating pr WHERE pr.pluginRegistry = :pluginRegistry")
    Long countRatings(@Param("pluginRegistry") PluginRegistry pluginRegistry);

    boolean existsByPluginRegistryAndOrganization(PluginRegistry pluginRegistry, Organization organization);
}
