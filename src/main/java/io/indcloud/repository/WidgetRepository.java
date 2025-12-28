package io.indcloud.repository;

import io.indcloud.model.Widget;
import io.indcloud.model.WidgetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface WidgetRepository extends JpaRepository<Widget, Long> {

    /**
     * Find all widgets for a specific dashboard
     */
    List<Widget> findByDashboardId(Long dashboardId);

    /**
     * Find all widgets for a specific device across all dashboards
     */
    List<Widget> findByDeviceId(String deviceId);

    /**
     * Find widgets by type
     */
    List<Widget> findByType(WidgetType type);

    /**
     * Count widgets in a dashboard
     */
    long countByDashboardId(Long dashboardId);

    /**
     * Find widgets for a specific device and variable
     */
    @Query("SELECT w FROM Widget w WHERE w.deviceId = :deviceId AND w.variableName = :variableName")
    List<Widget> findByDeviceIdAndVariableName(@Param("deviceId") String deviceId, @Param("variableName") String variableName);
}
