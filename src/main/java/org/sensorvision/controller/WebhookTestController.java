package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.WebhookTestRequest;
import org.sensorvision.dto.WebhookTestResponse;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.model.WebhookTest;
import org.sensorvision.repository.WebhookTestRepository;
import org.sensorvision.security.SecurityUtils;
import org.sensorvision.service.WebhookTestService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for webhook testing tool
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/webhook-tests")
@RequiredArgsConstructor
public class WebhookTestController {

    private final WebhookTestService webhookTestService;
    private final WebhookTestRepository webhookTestRepository;
    private final SecurityUtils securityUtils;

    /**
     * Execute a webhook test
     */
    @PostMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<WebhookTestResponse> executeTest(
            @Valid @RequestBody WebhookTestRequest request
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();
        User user = securityUtils.getCurrentUser();

        log.info("User {} executing webhook test to {}", user.getUsername(), request.url());

        WebhookTest test = webhookTestService.executeTest(request, org, user);

        return ResponseEntity.ok(toDto(test));
    }

    /**
     * Get webhook test history for the organization
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<Page<WebhookTestResponse>> getHistory(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Organization org = securityUtils.getCurrentUserOrganization();

        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<WebhookTest> tests = webhookTestRepository
                .findByOrganizationIdOrderByCreatedAtDesc(org.getId(), pageable);

        Page<WebhookTestResponse> dtos = tests.map(this::toDto);
        return ResponseEntity.ok(dtos);
    }

    /**
     * Get a specific webhook test by ID
     */
    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public ResponseEntity<WebhookTestResponse> getTest(@PathVariable Long id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return webhookTestRepository.findById(id)
                .filter(test -> test.getOrganization().getId().equals(org.getId()))
                .map(this::toDto)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Delete a webhook test
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Transactional
    public ResponseEntity<Void> deleteTest(@PathVariable Long id) {
        Organization org = securityUtils.getCurrentUserOrganization();

        return webhookTestRepository.findById(id)
                .filter(test -> test.getOrganization().getId().equals(org.getId()))
                .map(test -> {
                    webhookTestRepository.delete(test);
                    return ResponseEntity.noContent().<Void>build();
                })
                .orElse(ResponseEntity.notFound().build());
    }

    private WebhookTestResponse toDto(WebhookTest test) {
        return new WebhookTestResponse(
                test.getId(),
                test.getName(),
                test.getUrl(),
                test.getHttpMethod(),
                test.getHeaders(),
                test.getRequestBody(),
                test.getStatusCode(),
                test.getResponseBody(),
                test.getResponseHeaders(),
                test.getDurationMs(),
                test.getErrorMessage(),
                test.getCreatedBy() != null ? test.getCreatedBy().getUsername() : null,
                test.getCreatedAt()
        );
    }
}
