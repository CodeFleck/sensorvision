package io.indcloud.controller;

import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.AggregationResponse;
import io.indcloud.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    @GetMapping("/aggregate")
    public List<AggregationResponse> aggregateData(
            @RequestParam String deviceId,
            @RequestParam String variable,
            @RequestParam String aggregation,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(required = false) String interval
    ) {
        return analyticsService.aggregateData(deviceId, variable, aggregation, from, to, interval);
    }
}