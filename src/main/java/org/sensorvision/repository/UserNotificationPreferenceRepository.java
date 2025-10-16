package org.sensorvision.repository;

import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findByUser(User user);

    List<UserNotificationPreference> findByUserAndEnabledTrue(User user);

    Optional<UserNotificationPreference> findByUserAndChannel(User user, NotificationChannel channel);

    void deleteByUserAndChannel(User user, NotificationChannel channel);
}
