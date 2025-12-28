package io.indcloud.repository;

import io.indcloud.model.Organization;
import io.indcloud.model.ScheduledReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ScheduledReportRepository extends JpaRepository<ScheduledReport, Long> {

    List<ScheduledReport> findByOrganization(Organization organization);

    List<ScheduledReport> findByOrganizationAndEnabledTrue(Organization organization);

    @Query("SELECT sr FROM ScheduledReport sr WHERE sr.enabled = true AND sr.nextRunAt <= :now")
    List<ScheduledReport> findDueReports(LocalDateTime now);
}
