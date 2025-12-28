package io.indcloud.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "mqtt")
public class MqttProperties {

    private String clientId = "sensorvision-backend";
    private Broker broker = new Broker();
    private boolean enabled = true;

    @Getter
    @Setter
    public static class Broker {
        private String url;
        private String username;
        private String password;
        private boolean cleanSession = true;
        private int completionTimeout = 5000;
    }
}