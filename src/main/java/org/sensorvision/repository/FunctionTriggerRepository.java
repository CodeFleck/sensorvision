package org.sensorvision.repository;

import org.sensorvision.model.FunctionTrigger;
import org.sensorvision.model.FunctionTriggerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FunctionTriggerRepository extends JpaRepository<FunctionTrigger, Long> {

    List<FunctionTrigger> findByEnabledTrue();

    List<FunctionTrigger> findByTriggerTypeAndEnabledTrue(FunctionTriggerType triggerType);
}
