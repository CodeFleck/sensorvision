package org.sensorvision.repository;

import org.sensorvision.model.NotificationLog;
import org.sensorvision.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {

    Page<NotificationLog> findByUserOrderByCreatedAtDesc(User user, Pageable pageable);

    List<NotificationLog> findByStatusAndCreatedAtBefore(
            NotificationLog.NotificationStatus status,
            LocalDateTime cutoffDate
    );

    long countByUserAndStatus(User user, NotificationLog.NotificationStatus status);
}
