package org.sensorvision.repository;

import org.sensorvision.model.FunctionExecution;
import org.sensorvision.model.ServerlessFunction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

@Repository
public interface FunctionExecutionRepository extends JpaRepository<FunctionExecution, Long> {

    Page<FunctionExecution> findByFunctionOrderByStartedAtDesc(ServerlessFunction function, Pageable pageable);

    List<FunctionExecution> findByFunctionAndStartedAtAfterOrderByStartedAtDesc(
        ServerlessFunction function,
        Instant startedAfter
    );

    @Query("SELECT COUNT(e) FROM FunctionExecution e WHERE e.function = :function AND e.startedAt >= :since")
    long countRecentExecutions(@Param("function") ServerlessFunction function, @Param("since") Instant since);

    @Query("SELECT AVG(e.durationMs) FROM FunctionExecution e WHERE e.function = :function AND e.status = 'SUCCESS'")
    Double getAverageDuration(@Param("function") ServerlessFunction function);
}
