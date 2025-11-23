package org.sensorvision.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;
import org.sensorvision.service.EmailNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class EmailNotificationStrategy implements NotificationChannelStrategy {

    private final EmailNotificationService emailService;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.EMAIL;
    }

    @Override
    public boolean send(User user, Alert alert, UserNotificationPreference preference) {
        if (!emailEnabled) {
            log.debug("Email notifications are disabled via feature flag. Skipping email to {}", user.getEmail());
            return true; // Return success to avoid logging as failure
        }

        String email = preference.getDestination() != null
                ? preference.getDestination()
                : user.getEmail();
        return emailService.sendAlertEmail(user, alert, email);
    }
}
