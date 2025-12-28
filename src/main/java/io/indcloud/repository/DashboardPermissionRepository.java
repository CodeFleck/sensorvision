package io.indcloud.repository;

import io.indcloud.model.DashboardPermission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DashboardPermissionRepository extends JpaRepository<DashboardPermission, Long> {

    List<DashboardPermission> findByDashboardId(Long dashboardId);

    Optional<DashboardPermission> findByDashboardIdAndUserId(Long dashboardId, Long userId);

    void deleteByDashboardIdAndUserId(Long dashboardId, Long userId);
}
