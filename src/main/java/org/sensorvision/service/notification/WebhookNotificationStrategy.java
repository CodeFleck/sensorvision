package org.sensorvision.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.NotificationChannel;
import org.sensorvision.model.User;
import org.sensorvision.model.UserNotificationPreference;
import org.sensorvision.service.WebhookNotificationService;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class WebhookNotificationStrategy implements NotificationChannelStrategy {

    private final WebhookNotificationService webhookService;

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.WEBHOOK;
    }

    @Override
    public boolean send(User user, Alert alert, UserNotificationPreference preference) {
        if (preference.getDestination() == null) {
            log.warn("Webhook notification configured but no webhook URL provided for user: {}",
                    user.getUsername());
            return false;
        }
        return webhookService.sendAlertWebhook(user, alert, preference.getDestination());
    }
}
