package io.indcloud.service.notification;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import io.indcloud.model.Alert;
import io.indcloud.model.NotificationChannel;
import io.indcloud.model.SmsDeliveryLog;
import io.indcloud.model.User;
import io.indcloud.model.UserNotificationPreference;
import io.indcloud.service.SmsNotificationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SmsNotificationStrategy implements NotificationChannelStrategy {

    private final SmsNotificationService smsService;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Override
    public NotificationChannel getSupportedChannel() {
        return NotificationChannel.SMS;
    }

    @Override
    public boolean send(User user, Alert alert, UserNotificationPreference preference) {
        if (!smsEnabled) {
            log.debug("SMS notifications are disabled via feature flag. Skipping SMS to {}",
                    preference.getDestination());
            return true; // Return success to avoid logging as failure
        }

        if (preference.getDestination() == null) {
            log.warn("SMS notification configured but no phone number provided for user: {}",
                    user.getUsername());
            return false;
        }
        String message = smsService.formatAlertMessage(alert);
        SmsDeliveryLog deliveryLog = smsService.sendSms(alert, preference.getDestination(), message);
        return deliveryLog != null && !"FAILED".equals(deliveryLog.getStatus());
    }
}
