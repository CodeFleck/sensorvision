package org.sensorvision.service.notification;

import org.sensorvision.model.Alert;
import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;

/**
 * Strategy interface for sending notifications via different channels.
 */
public interface NotificationChannelStrategy {

    /**
     * Get the channel supported by this strategy.
     */
    NotificationChannel getSupportedChannel();

    /**
     * Send a notification.
     *
     * @param user       The user to notify
     * @param alert      The alert triggering the notification
     * @param preference The user's preference settings for this channel
     * @return true if notification was sent successfully (or queued), false otherwise
     */
    boolean send(User user, Alert alert, UserNotificationPreference preference);
}
