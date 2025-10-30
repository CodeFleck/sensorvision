package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.EmailTemplateRequest;
import org.sensorvision.dto.EmailTemplateResponse;
import org.sensorvision.model.Organization;
import org.sensorvision.model.User;
import org.sensorvision.service.EmailTemplateService;
import org.sensorvision.security.SecurityUtils;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/email-templates")
@RequiredArgsConstructor
public class EmailTemplateController {

    private final EmailTemplateService emailTemplateService;
    private final SecurityUtils securityUtils;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailTemplateResponse> createTemplate(@Valid @RequestBody EmailTemplateRequest request) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        User user = securityUtils.getCurrentUser();
        EmailTemplateResponse response = emailTemplateService.createTemplate(request, organization, user);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<EmailTemplateResponse>> getTemplates(
            @RequestParam(required = false) String templateType,
            @RequestParam(required = false) Boolean active,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        Page<EmailTemplateResponse> templates = emailTemplateService.getTemplates(
                organization,
                templateType,
                active,
                page,
                size
        );
        return ResponseEntity.ok(templates);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailTemplateResponse> getTemplate(@PathVariable Long id) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        EmailTemplateResponse response = emailTemplateService.getTemplate(id, organization);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EmailTemplateResponse> updateTemplate(
            @PathVariable Long id,
            @Valid @RequestBody EmailTemplateRequest request
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        EmailTemplateResponse response = emailTemplateService.updateTemplate(id, request, organization);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteTemplate(@PathVariable Long id) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        emailTemplateService.deleteTemplate(id, organization);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/preview")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> previewTemplate(
            @PathVariable Long id,
            @RequestBody Map<String, Object> sampleData
    ) {
        Organization organization = securityUtils.getCurrentUserOrganization();
        Map<String, String> preview = emailTemplateService.previewTemplate(id, sampleData, organization);
        return ResponseEntity.ok(preview);
    }
}
