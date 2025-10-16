package org.sensorvision.repository;

import org.sensorvision.model.Variable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface VariableRepository extends JpaRepository<Variable, Long> {

    List<Variable> findByOrganizationId(Long organizationId);

    Optional<Variable> findByOrganizationIdAndName(Long organizationId, String name);

    boolean existsByOrganizationIdAndName(Long organizationId, String name);
}
