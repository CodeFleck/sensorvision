package io.indcloud.repository;

import io.indcloud.model.FunctionTrigger;
import io.indcloud.model.FunctionTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FunctionTriggerRepository extends JpaRepository<FunctionTrigger, Long> {

    List<FunctionTrigger> findByEnabledTrue();

    @Query("SELECT t FROM FunctionTrigger t LEFT JOIN FETCH t.function WHERE t.triggerType = :triggerType AND t.enabled = true")
    List<FunctionTrigger> findByTriggerTypeAndEnabledTrue(@Param("triggerType") FunctionTriggerType triggerType);
}
