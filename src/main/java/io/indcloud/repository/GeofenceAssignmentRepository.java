package io.indcloud.repository;

import io.indcloud.model.GeofenceAssignment;
import io.indcloud.model.GeofenceAssignmentId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface GeofenceAssignmentRepository extends JpaRepository<GeofenceAssignment, GeofenceAssignmentId> {

    List<GeofenceAssignment> findByGeofenceId(Long geofenceId);

    List<GeofenceAssignment> findByDeviceId(UUID deviceId);

    @Query("SELECT ga FROM GeofenceAssignment ga WHERE ga.geofence.id = :geofenceId AND ga.device.id = :deviceId")
    Optional<GeofenceAssignment> findByGeofenceIdAndDeviceId(
            @Param("geofenceId") Long geofenceId,
            @Param("deviceId") UUID deviceId
    );

    @Query("SELECT ga FROM GeofenceAssignment ga JOIN FETCH ga.geofence g WHERE ga.device.id = :deviceId AND g.enabled = true")
    List<GeofenceAssignment> findActiveAssignmentsByDeviceId(@Param("deviceId") UUID deviceId);

    void deleteByGeofenceIdAndDeviceId(Long geofenceId, UUID deviceId);
}
