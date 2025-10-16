package org.sensorvision.service;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.model.Alert;
import org.sensorvision.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsNotificationService {

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${notification.sms.from:}")
    private String fromNumber;

    @Value("${notification.sms.twilio.account-sid:}")
    private String twilioAccountSid;

    @Value("${notification.sms.twilio.auth-token:}")
    private String twilioAuthToken;

    @PostConstruct
    public void init() {
        if (smsEnabled && twilioAccountSid != null && !twilioAccountSid.isEmpty()
            && twilioAuthToken != null && !twilioAuthToken.isEmpty()) {
            try {
                Twilio.init(twilioAccountSid, twilioAuthToken);
                log.info("Twilio SMS service initialized successfully");
            } catch (Exception e) {
                log.error("Failed to initialize Twilio SMS service", e);
            }
        }
    }

    /**
     * Send SMS notification for an alert using Twilio
     */
    public boolean sendAlertSms(User user, Alert alert, String phoneNumber) {
        if (!smsEnabled) {
            log.info("SMS notifications disabled. Would have sent SMS to: {}", phoneNumber);
            return false;
        }

        if (twilioAccountSid == null || twilioAccountSid.isEmpty() ||
            twilioAuthToken == null || twilioAuthToken.isEmpty()) {
            log.warn("Twilio credentials not configured. Cannot send SMS to: {}", phoneNumber);
            return false;
        }

        try {
            String message = generateSmsMessage(alert);

            log.info("Sending SMS notification to: {}", phoneNumber);

            // Send SMS using Twilio
            Message twilioMessage = Message.creator(
                new PhoneNumber(phoneNumber),
                new PhoneNumber(fromNumber),
                message
            ).create();

            log.info("SMS sent successfully to {} with SID: {}", phoneNumber, twilioMessage.getSid());
            return true;
        } catch (Exception e) {
            log.error("Failed to send SMS notification to {}", phoneNumber, e);
            return false;
        }
    }

    private String generateSmsMessage(Alert alert) {
        // Keep SMS messages short (160 characters for single SMS)
        return String.format("[SensorVision] %s Alert: %s on %s. Value: %s",
                alert.getSeverity(),
                alert.getRule().getName(),
                alert.getDevice().getName(),
                alert.getTriggeredValue());
    }
}
