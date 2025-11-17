package org.sensorvision.demo.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;

/**
 * Demo Mode Spring Configuration.
 *
 * This configuration is only active when demo.mode-enabled=true.
 * It enables component scanning for demo-specific beans and activates
 * scheduling for telemetry generation.
 *
 * IMPORTANT: Demo Mode should NEVER be enabled in production environments.
 * Use the 'demo' Spring profile to activate this feature.
 */
@Slf4j
@Configuration
@EnableScheduling
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "demo", name = "mode-enabled", havingValue = "true")
@ComponentScan(basePackages = "org.sensorvision.demo")
public class DemoModeConfig {

    private final DemoModeProperties properties;

    @PostConstruct
    public void init() {
        log.info("╔════════════════════════════════════════════════════════════════╗");
        log.info("║              🎬 DEMO MODE ACTIVATED 🎬                         ║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ Organization: {}", String.format("%-44s", properties.getOrganizationName()) + "║");
        log.info("║ Devices:      {}", String.format("%-44s", properties.getDeviceCount() + " manufacturing machines") + "║");
        log.info("║ Frequency:    {}", String.format("%-44s", properties.getGenerationIntervalMs() + "ms (" + (1000.0 / properties.getGenerationIntervalMs()) + " samples/sec)") + "║");
        log.info("║ Anomaly Rate: {}", String.format("%-44s", (properties.getAnomalyProbability() * 100) + "%") + "║");
        log.info("║ Cache Window: {}", String.format("%-44s", properties.getRollingWindowMinutes() + " minutes") + "║");
        log.info("╠════════════════════════════════════════════════════════════════╣");
        log.info("║ ⚠️  WARNING: Demo Mode is for demonstration only              ║");
        log.info("║ ⚠️  Do NOT enable in production environments                  ║");
        log.info("╚════════════════════════════════════════════════════════════════╝");
    }
}
