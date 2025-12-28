package io.indcloud.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration properties for telemetry ingestion settings.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "telemetry")
public class TelemetryConfigurationProperties {

    private AutoProvision autoProvision = new AutoProvision();

    @Data
    public static class AutoProvision {
        /**
         * Whether to automatically create devices on first data send.
         * When enabled, devices sending telemetry data with a valid API token will be auto-created.
         * The device will belong to the same organization as the token used for authentication.
         */
        private boolean enabled = true;
    }
}
