package io.indcloud.repository;

import io.indcloud.model.Geofence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<Geofence, Long> {

    List<Geofence> findByOrganizationId(Long organizationId);

    @Query("SELECT g FROM Geofence g WHERE g.organization.id = :organizationId AND g.id = :id")
    Optional<Geofence> findByIdAndOrganizationId(@Param("id") Long id, @Param("organizationId") Long organizationId);

    List<Geofence> findByOrganizationIdAndEnabledTrue(Long organizationId);

    @Query("SELECT g FROM Geofence g LEFT JOIN FETCH g.assignments WHERE g.id = :id")
    Optional<Geofence> findByIdWithAssignments(@Param("id") Long id);

    @Query("SELECT COUNT(g) FROM Geofence g WHERE g.organization.id = :organizationId")
    long countByOrganizationId(@Param("organizationId") Long organizationId);
}
