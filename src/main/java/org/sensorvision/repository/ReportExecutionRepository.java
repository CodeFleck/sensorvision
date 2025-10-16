package org.sensorvision.repository;

import org.sensorvision.model.ReportExecution;
import org.sensorvision.model.ScheduledReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportExecutionRepository extends JpaRepository<ReportExecution, Long> {

    Page<ReportExecution> findByScheduledReportOrderByStartedAtDesc(
            ScheduledReport scheduledReport,
            Pageable pageable
    );
}
