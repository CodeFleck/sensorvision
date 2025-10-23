package org.sensorvision.testutil;

import org.sensorvision.model.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test fixtures and builders for notification-related test data.
 * Provides reusable factory methods for creating test entities.
 */
public class NotificationTestFixtures {

    /**
     * Builder for creating test User entities
     */
    public static class UserBuilder {
        private Long id = 1L;
        private String email = "test@example.com";
        private String username = "testuser";
        private Organization organization;

        public UserBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public UserBuilder email(String email) {
            this.email = email;
            return this;
        }

        public UserBuilder username(String username) {
            this.username = username;
            return this;
        }

        public UserBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public User build() {
            User user = new User();
            user.setId(id);
            user.setEmail(email);
            user.setUsername(username);
            user.setOrganization(organization);
            return user;
        }
    }

    /**
     * Builder for creating test Organization entities
     */
    public static class OrganizationBuilder {
        private Long id = 1L;
        private String name = "Test Organization";

        public OrganizationBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public OrganizationBuilder name(String name) {
            this.name = name;
            return this;
        }

        public Organization build() {
            Organization org = new Organization();
            org.setId(id);
            org.setName(name);
            return org;
        }
    }

    /**
     * Builder for creating test Device entities
     */
    public static class DeviceBuilder {
        private Long id = 1L;
        private String deviceId = "device-001";
        private String name = "Test Device";
        private String externalId = "EXT-001";
        private Organization organization;

        public DeviceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public DeviceBuilder deviceId(String deviceId) {
            this.deviceId = deviceId;
            return this;
        }

        public DeviceBuilder name(String name) {
            this.name = name;
            return this;
        }

        public DeviceBuilder externalId(String externalId) {
            this.externalId = externalId;
            return this;
        }

        public DeviceBuilder organization(Organization organization) {
            this.organization = organization;
            return this;
        }

        public Device build() {
            Device device = new Device();
            device.setId(UUID.randomUUID());
            device.setExternalId(externalId != null ? externalId : deviceId);
            device.setName(name);
            device.setOrganization(organization);
            return device;
        }
    }

    /**
     * Builder for creating test Rule entities
     */
    public static class RuleBuilder {
        private UUID id = UUID.randomUUID();
        private String name = "Test Rule";
        private String variable = "temperature";
        private RuleOperator operator = RuleOperator.GT;
        private BigDecimal threshold = new BigDecimal("75.0");

        public RuleBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public RuleBuilder name(String name) {
            this.name = name;
            return this;
        }

        public RuleBuilder variable(String variable) {
            this.variable = variable;
            return this;
        }

        public RuleBuilder operator(RuleOperator operator) {
            this.operator = operator;
            return this;
        }

        public RuleBuilder threshold(BigDecimal threshold) {
            this.threshold = threshold;
            return this;
        }

        public Rule build() {
            Rule rule = new Rule();
            rule.setId(id);
            rule.setName(name);
            rule.setVariable(variable);
            rule.setOperator(operator);
            rule.setThreshold(threshold);
            return rule;
        }
    }

    /**
     * Builder for creating test Alert entities
     */
    public static class AlertBuilder {
        private UUID id = UUID.randomUUID();
        private Device device;
        private Rule rule;
        private BigDecimal triggeredValue = new BigDecimal("100.0");
        private AlertSeverity severity = AlertSeverity.CRITICAL;
        private LocalDateTime triggeredAt = LocalDateTime.now();
        private String message = "Alert triggered";

        public AlertBuilder id(UUID id) {
            this.id = id;
            return this;
        }

        public AlertBuilder device(Device device) {
            this.device = device;
            return this;
        }

        public AlertBuilder rule(Rule rule) {
            this.rule = rule;
            return this;
        }

        public AlertBuilder triggeredValue(BigDecimal triggeredValue) {
            this.triggeredValue = triggeredValue;
            return this;
        }

        public AlertBuilder severity(AlertSeverity severity) {
            this.severity = severity;
            return this;
        }

        public AlertBuilder triggeredAt(LocalDateTime triggeredAt) {
            this.triggeredAt = triggeredAt;
            return this;
        }

        public AlertBuilder message(String message) {
            this.message = message;
            return this;
        }

        public Alert build() {
            Alert alert = new Alert();
            alert.setId(id);
            alert.setDevice(device);
            alert.setRule(rule);
            alert.setTriggeredValue(triggeredValue);
            alert.setSeverity(severity);
            alert.setTriggeredAt(triggeredAt);
            alert.setMessage(message);
            return alert;
        }
    }

    /**
     * Builder for creating test UserNotificationPreference entities
     */
    public static class NotificationPreferenceBuilder {
        private Long id;
        private User user;
        private NotificationChannel channel = NotificationChannel.EMAIL;
        private Boolean enabled = true;
        private String destination;
        private AlertSeverity minSeverity = AlertSeverity.LOW;
        private Boolean immediate = true;
        private Integer digestIntervalMinutes;

        public NotificationPreferenceBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public NotificationPreferenceBuilder user(User user) {
            this.user = user;
            return this;
        }

        public NotificationPreferenceBuilder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public NotificationPreferenceBuilder enabled(Boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public NotificationPreferenceBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public NotificationPreferenceBuilder minSeverity(AlertSeverity minSeverity) {
            this.minSeverity = minSeverity;
            return this;
        }

        public NotificationPreferenceBuilder immediate(Boolean immediate) {
            this.immediate = immediate;
            return this;
        }

        public NotificationPreferenceBuilder digestIntervalMinutes(Integer digestIntervalMinutes) {
            this.digestIntervalMinutes = digestIntervalMinutes;
            return this;
        }

        public UserNotificationPreference build() {
            UserNotificationPreference pref = new UserNotificationPreference();
            pref.setId(id);
            pref.setUser(user);
            pref.setChannel(channel);
            pref.setEnabled(enabled);
            pref.setDestination(destination);
            pref.setMinSeverity(minSeverity);
            pref.setImmediate(immediate);
            pref.setDigestIntervalMinutes(digestIntervalMinutes);
            if (id != null) {
                pref.setCreatedAt(LocalDateTime.now());
                pref.setUpdatedAt(LocalDateTime.now());
            }
            return pref;
        }
    }

    /**
     * Builder for creating test NotificationLog entities
     */
    public static class NotificationLogBuilder {
        private Long id;
        private Alert alert;
        private User user;
        private NotificationChannel channel = NotificationChannel.EMAIL;
        private String destination = "test@example.com";
        private String subject = "Test Alert";
        private String message = "Alert notification";
        private NotificationLog.NotificationStatus status = NotificationLog.NotificationStatus.SENT;
        private String errorMessage;
        private LocalDateTime sentAt;
        private LocalDateTime createdAt = LocalDateTime.now();

        public NotificationLogBuilder id(Long id) {
            this.id = id;
            return this;
        }

        public NotificationLogBuilder alert(Alert alert) {
            this.alert = alert;
            return this;
        }

        public NotificationLogBuilder user(User user) {
            this.user = user;
            return this;
        }

        public NotificationLogBuilder channel(NotificationChannel channel) {
            this.channel = channel;
            return this;
        }

        public NotificationLogBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public NotificationLogBuilder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public NotificationLogBuilder message(String message) {
            this.message = message;
            return this;
        }

        public NotificationLogBuilder status(NotificationLog.NotificationStatus status) {
            this.status = status;
            return this;
        }

        public NotificationLogBuilder errorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
            return this;
        }

        public NotificationLogBuilder sentAt(LocalDateTime sentAt) {
            this.sentAt = sentAt;
            return this;
        }

        public NotificationLogBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }

        public NotificationLog build() {
            return NotificationLog.builder()
                    .id(id)
                    .alert(alert)
                    .user(user)
                    .channel(channel)
                    .destination(destination)
                    .subject(subject)
                    .message(message)
                    .status(status)
                    .errorMessage(errorMessage)
                    .sentAt(sentAt)
                    .createdAt(createdAt)
                    .build();
        }
    }

    // Convenience factory methods

    public static UserBuilder aUser() {
        return new UserBuilder();
    }

    public static OrganizationBuilder anOrganization() {
        return new OrganizationBuilder();
    }

    public static DeviceBuilder aDevice() {
        return new DeviceBuilder();
    }

    public static RuleBuilder aRule() {
        return new RuleBuilder();
    }

    public static AlertBuilder anAlert() {
        return new AlertBuilder();
    }

    public static NotificationPreferenceBuilder aNotificationPreference() {
        return new NotificationPreferenceBuilder();
    }

    public static NotificationLogBuilder aNotificationLog() {
        return new NotificationLogBuilder();
    }

    // Common test scenarios

    public static Alert createCriticalTemperatureAlert() {
        Organization org = anOrganization().build();
        Device device = aDevice()
                .name("Warehouse Sensor")
                .organization(org)
                .build();
        Rule rule = aRule()
                .name("High Temperature Alert")
                .variable("temperature")
                .operator(RuleOperator.GT)
                .threshold(new BigDecimal("75.0"))
                .build();

        return anAlert()
                .device(device)
                .rule(rule)
                .severity(AlertSeverity.CRITICAL)
                .triggeredValue(new BigDecimal("95.5"))
                .message("Temperature critically high")
                .build();
    }

    public static UserNotificationPreference createEmailPreferenceForUser(User user) {
        return aNotificationPreference()
                .user(user)
                .channel(NotificationChannel.EMAIL)
                .enabled(true)
                .destination(user.getEmail())
                .minSeverity(AlertSeverity.LOW)
                .immediate(true)
                .build();
    }

    public static UserNotificationPreference createSmsPreferenceForUser(User user, String phoneNumber) {
        return aNotificationPreference()
                .user(user)
                .channel(NotificationChannel.SMS)
                .enabled(true)
                .destination(phoneNumber)
                .minSeverity(AlertSeverity.MEDIUM)
                .immediate(true)
                .build();
    }
}
