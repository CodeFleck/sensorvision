package io.indcloud.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.indcloud.dto.WidgetCreateRequest;
import io.indcloud.model.*;
import io.indcloud.repository.DashboardRepository;
import io.indcloud.repository.DeviceRepository;
import io.indcloud.repository.WidgetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for automatically generating example widgets when a device sends its first telemetry.
 * Creates a set of widgets (line chart, gauge, metric card) for each variable in the telemetry.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class AutoWidgetGeneratorService {

    private final DashboardRepository dashboardRepository;
    private final WidgetRepository widgetRepository;
    private final DeviceRepository deviceRepository;
    private final DefaultDashboardInitializer defaultDashboardInitializer;
    private final ObjectMapper objectMapper;

    @Value("${auto-widgets.enabled:true}")
    private boolean autoWidgetsEnabled;

    @Value("${auto-widgets.max-widgets-per-variable:3}")
    private int maxWidgetsPerVariable;

    // Grid layout constants
    private static final int GRID_COLS = 12;
    private static final int LINE_CHART_WIDTH = 6;
    private static final int LINE_CHART_HEIGHT = 4;
    private static final int GAUGE_WIDTH = 3;
    private static final int GAUGE_HEIGHT = 4;
    private static final int METRIC_CARD_WIDTH = 3;
    private static final int METRIC_CARD_HEIGHT = 2;

    /**
     * Generate initial widgets for a device when it sends first telemetry.
     * This method runs asynchronously to not block telemetry ingestion.
     *
     * @param device The device that sent telemetry
     * @param variables The variables from the telemetry payload
     */
    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void generateInitialWidgets(Device device, Map<String, BigDecimal> variables) {
        if (!autoWidgetsEnabled) {
            log.debug("Auto widget generation is disabled, skipping for device {}", device.getExternalId());
            return;
        }

        if (variables == null || variables.isEmpty()) {
            log.debug("No variables in telemetry for device {}, skipping widget generation", device.getExternalId());
            return;
        }

        // Acquire pessimistic lock to prevent race conditions
        // Only one thread can hold the lock at a time, ensuring no duplicate widget creation
        Device freshDevice = deviceRepository.findByIdWithLock(device.getId()).orElse(null);
        if (freshDevice == null) {
            log.warn("Device {} not found when trying to generate widgets", device.getExternalId());
            return;
        }

        if (Boolean.TRUE.equals(freshDevice.getInitialWidgetsCreated())) {
            log.debug("Widgets already created for device {}, skipping (detected via lock)", device.getExternalId());
            return;
        }

        try {
            log.info("Generating initial widgets for device {} with {} variables",
                    device.getExternalId(), variables.size());

            Organization org = device.getOrganization();
            if (org == null) {
                log.warn("Device {} has no organization, cannot create widgets", device.getExternalId());
                return;
            }

            // Ensure default dashboard exists
            defaultDashboardInitializer.ensureDefaultDashboardExists(org);

            Dashboard dashboard = dashboardRepository.findByOrganizationAndIsDefaultTrueWithWidgets(org)
                    .orElse(null);

            if (dashboard == null) {
                log.warn("No default dashboard found for organization {}, cannot create widgets", org.getId());
                return;
            }

            // Calculate starting Y position (below existing widgets)
            int startY = calculateNextPositionY(dashboard);
            AtomicInteger currentY = new AtomicInteger(startY);
            AtomicInteger widgetsCreated = new AtomicInteger(0);

            // Create widgets for each variable
            for (Map.Entry<String, BigDecimal> entry : variables.entrySet()) {
                String variableName = entry.getKey();
                String displayName = humanizeVariableName(variableName);

                // Create LINE_CHART for trends
                createWidget(dashboard, device, variableName,
                        displayName + " Trend",
                        WidgetType.LINE_CHART,
                        0, currentY.get(),
                        LINE_CHART_WIDTH, LINE_CHART_HEIGHT);
                widgetsCreated.incrementAndGet();

                // Create GAUGE for current value
                createWidget(dashboard, device, variableName,
                        displayName,
                        WidgetType.GAUGE,
                        LINE_CHART_WIDTH, currentY.get(),
                        GAUGE_WIDTH, GAUGE_HEIGHT);
                widgetsCreated.incrementAndGet();

                // Create METRIC_CARD for simple display
                createWidget(dashboard, device, variableName,
                        displayName + " Value",
                        WidgetType.METRIC_CARD,
                        LINE_CHART_WIDTH + GAUGE_WIDTH, currentY.get(),
                        METRIC_CARD_WIDTH, METRIC_CARD_HEIGHT);
                widgetsCreated.incrementAndGet();

                // Move to next row
                currentY.addAndGet(LINE_CHART_HEIGHT);
            }

            // Mark device as having initial widgets created
            freshDevice.setInitialWidgetsCreated(true);
            deviceRepository.save(freshDevice);

            log.info("Created {} initial widgets for device {} on dashboard {}",
                    widgetsCreated.get(), device.getExternalId(), dashboard.getName());

        } catch (Exception e) {
            log.error("Failed to generate initial widgets for device {}: {}",
                    device.getExternalId(), e.getMessage(), e);
        }
    }

    /**
     * Create a single widget on the dashboard.
     */
    private void createWidget(Dashboard dashboard, Device device, String variableName,
                              String widgetName, WidgetType type,
                              int posX, int posY, int width, int height) {
        Widget widget = new Widget();
        widget.setDashboard(dashboard);
        widget.setName(widgetName);
        widget.setType(type);
        widget.setDeviceId(device.getExternalId());
        widget.setVariableName(variableName);
        widget.setPositionX(posX);
        widget.setPositionY(posY);
        widget.setWidth(width);
        widget.setHeight(height);
        widget.setAggregation(WidgetAggregation.NONE);
        widget.setTimeRangeMinutes(60); // Default to 1 hour
        widget.setConfig(objectMapper.createObjectNode());

        widgetRepository.save(widget);
        log.debug("Created {} widget '{}' for variable {} at ({}, {})",
                type, widgetName, variableName, posX, posY);
    }

    /**
     * Calculate the next Y position for widgets on the dashboard.
     */
    private int calculateNextPositionY(Dashboard dashboard) {
        if (dashboard.getWidgets() == null || dashboard.getWidgets().isEmpty()) {
            return 0;
        }

        return dashboard.getWidgets().stream()
                .mapToInt(w -> w.getPositionY() + w.getHeight())
                .max()
                .orElse(0);
    }

    /**
     * Convert snake_case or camelCase variable names to human-readable format.
     * Examples:
     *   kw_consumption -> Kw Consumption
     *   powerFactor -> Power Factor
     *   temperature -> Temperature
     */
    private String humanizeVariableName(String name) {
        if (name == null || name.isEmpty()) {
            return name;
        }

        // Handle snake_case
        String result = name.replace("_", " ");

        // Handle camelCase
        result = result.replaceAll("([a-z])([A-Z])", "$1 $2");

        // Capitalize each word
        String[] words = result.split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) {
                    sb.append(" ");
                }
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) {
                    sb.append(word.substring(1).toLowerCase());
                }
            }
        }

        return sb.toString();
    }
}
