package org.sensorvision.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "simulator")
public class SimulatorProperties {

    private boolean enabled = true;
    private int deviceCount = 10;
    private int intervalSeconds = 30;
    private String topicPattern = "sensorvision/devices/%s/telemetry";
}
