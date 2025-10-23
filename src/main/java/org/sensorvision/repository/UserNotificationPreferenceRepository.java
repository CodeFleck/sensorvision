package org.sensorvision.repository;

import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserNotificationPreferenceRepository extends JpaRepository<UserNotificationPreference, Long> {

    List<UserNotificationPreference> findByUser(User user);

    List<UserNotificationPreference> findByUserAndEnabledTrue(User user);

    Optional<UserNotificationPreference> findByUserAndChannel(User user, NotificationChannel channel);

    void deleteByUserAndChannel(User user, NotificationChannel channel);

    @Query("SELECT p FROM UserNotificationPreference p " +
           "WHERE p.user.organization = :organization " +
           "AND p.channel = :channel " +
           "AND p.enabled = true")
    List<UserNotificationPreference> findByUserOrganizationAndChannelAndEnabledTrue(
            @Param("organization") Organization organization,
            @Param("channel") NotificationChannel channel);
}
