package io.indcloud.repository;

import io.indcloud.model.Organization;
import io.indcloud.model.ServerlessFunction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ServerlessFunctionRepository extends JpaRepository<ServerlessFunction, Long> {

    List<ServerlessFunction> findByOrganization(Organization organization);

    List<ServerlessFunction> findByOrganizationAndEnabledTrue(Organization organization);

    Optional<ServerlessFunction> findByOrganizationAndName(Organization organization, String name);

    @Query("SELECT f FROM ServerlessFunction f LEFT JOIN FETCH f.triggers WHERE f.id = :id")
    Optional<ServerlessFunction> findByIdWithTriggers(@Param("id") Long id);

    boolean existsByOrganizationAndName(Organization organization, String name);
}
