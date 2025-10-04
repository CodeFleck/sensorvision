package org.sensorvision.controller;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.LatestTelemetryResponse;
import org.sensorvision.dto.TelemetryPointDto;
import org.sensorvision.service.TelemetryService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/data")
@RequiredArgsConstructor
public class TelemetryController {

    private final TelemetryService telemetryService;

    @GetMapping("/query")
    public List<TelemetryPointDto> queryTelemetry(@RequestParam String deviceId,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return telemetryService.queryTelemetry(deviceId, from, to);
    }

    @GetMapping("/latest")
    public List<LatestTelemetryResponse> latestForDevices(@RequestParam(name = "deviceIds") List<String> deviceIds) {
        List<String> ids = deviceIds.stream()
                .flatMap(id -> Arrays.stream(id.split(",")))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        return telemetryService.getLatest(ids);
    }

    @GetMapping("/latest/{deviceId}")
    public TelemetryPointDto latestForDevice(@PathVariable String deviceId) {
        return telemetryService.getLatest(deviceId);
    }
}
