package org.sensorvision.controller;

import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.RuleCreateRequest;
import org.sensorvision.dto.RuleResponse;
import org.sensorvision.service.RuleService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/rules")
@RequiredArgsConstructor
public class RuleController {

    private final RuleService ruleService;

    @GetMapping
    public List<RuleResponse> getRules() {
        return ruleService.getAllRules();
    }

    @GetMapping("/{id}")
    public RuleResponse getRule(@PathVariable UUID id) {
        return ruleService.getRule(id);
    }

    @PostMapping
    public ResponseEntity<RuleResponse> createRule(@Valid @RequestBody RuleCreateRequest request) {
        RuleResponse response = ruleService.createRule(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    public RuleResponse updateRule(@PathVariable UUID id, @Valid @RequestBody RuleCreateRequest request) {
        return ruleService.updateRule(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRule(@PathVariable UUID id) {
        ruleService.deleteRule(id);
    }
}