package org.sensorvision.repository;

import org.sensorvision.model.CannedResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CannedResponseRepository extends JpaRepository<CannedResponse, Long> {

    /**
     * Find all active canned responses
     */
    List<CannedResponse> findByActiveTrue();

    /**
     * Find active canned responses by category
     */
    List<CannedResponse> findByActiveTrueAndCategory(String category);

    /**
     * Find canned responses ordered by use count (most used first)
     */
    List<CannedResponse> findByActiveTrueOrderByUseCountDesc();
}
