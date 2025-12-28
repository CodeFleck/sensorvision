package io.indcloud.service.notification;

import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import io.indcloud.model.NotificationChannel;
import io.indcloud.model.User;
import io.indcloud.model.UserNotificationPreference;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class InAppNotificationStrategy implements NotificationChannelStrategy {

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.IN_APP;
    }

    @Override
    public boolean send(User user, Alert alert, UserNotificationPreference preference) {
        // IN_APP notifications are handled through events system (already triggered in NotificationService)
        // This strategy is a placeholder to satisfy the pattern and return success
        return true;
    }
}
