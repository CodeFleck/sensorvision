package io.indcloud.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import io.indcloud.model.NotificationChannel;
import io.indcloud.model.User;
import io.indcloud.model.UserNotificationPreference;
import io.indcloud.service.WebhookNotificationService;
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
