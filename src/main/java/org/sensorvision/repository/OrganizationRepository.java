package org.sensorvision.repository;

import org.sensorvision.model.Organization;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {
    Optional<Organization> findByName(String name);

    // Soft delete support
    @Query("SELECT o FROM Organization o WHERE o.deletedAt IS NULL")
    List<Organization> findAllActive();

    @Query("SELECT o FROM Organization o WHERE o.id = :id AND o.deletedAt IS NULL")
    Optional<Organization> findActiveById(@Param("id") Long id);

    @Query("SELECT o FROM Organization o WHERE o.name = :name AND o.deletedAt IS NULL")
    Optional<Organization> findActiveByName(@Param("name") String name);
}
