package org.sensorvision.controller;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.AlertResponse;
import org.sensorvision.service.AlertService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService alertService;

    @GetMapping
    public List<AlertResponse> getAlerts(@RequestParam(defaultValue = "false") boolean unacknowledgedOnly) {
        if (unacknowledgedOnly) {
            return alertService.getUnacknowledgedAlerts();
        }
        return alertService.getAllAlerts();
    }

    @PostMapping("/{id}/acknowledge")
    public ResponseEntity<Void> acknowledgeAlert(@PathVariable UUID id) {
        alertService.acknowledgeAlert(id);
        return ResponseEntity.ok().build();
    }
}