package org.sensorvision.controller;

import jakarta.validation.Valid;
import org.sensorvision.dto.CannedResponseDto;
import org.sensorvision.dto.CannedResponseRequest;
import org.sensorvision.service.CannedResponseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/canned-responses")
@PreAuthorize("hasRole('ADMIN')")
public class CannedResponseController {

    private final CannedResponseService cannedResponseService;

    @Autowired
    public CannedResponseController(CannedResponseService cannedResponseService) {
        this.cannedResponseService = cannedResponseService;
    }

    /**
     * Get canned responses with optional filters
     * @param category optional category filter
     * @param sortByPopularity if true, sort by use count descending
     * @param includeInactive if true, includes inactive templates (admin UI only)
     */
    @GetMapping
    public ResponseEntity<List<CannedResponseDto>> getAllActive(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "false") boolean sortByPopularity,
            @RequestParam(required = false, defaultValue = "false") boolean includeInactive
    ) {
        List<CannedResponseDto> responses;

        if (category != null && !category.isBlank()) {
            responses = cannedResponseService.getByCategory(category, includeInactive);
        } else if (sortByPopularity) {
            responses = cannedResponseService.getAllActiveByPopularity();
        } else if (includeInactive) {
            responses = cannedResponseService.getAll();
        } else {
            responses = cannedResponseService.getAllActive();
        }

        return ResponseEntity.ok(responses);
    }

    /**
     * Get a specific canned response
     */
    @GetMapping("/{id}")
    public ResponseEntity<CannedResponseDto> getById(@PathVariable Long id) {
        CannedResponseDto response = cannedResponseService.getById(id);
        return ResponseEntity.ok(response);
    }

    /**
     * Create a new canned response
     */
    @PostMapping
    public ResponseEntity<CannedResponseDto> create(@Valid @RequestBody CannedResponseRequest request) {
        CannedResponseDto created = cannedResponseService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update an existing canned response
     */
    @PutMapping("/{id}")
    public ResponseEntity<CannedResponseDto> update(
            @PathVariable Long id,
            @Valid @RequestBody CannedResponseRequest request
    ) {
        CannedResponseDto updated = cannedResponseService.update(id, request);
        return ResponseEntity.ok(updated);
    }

    /**
     * Delete a canned response
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        cannedResponseService.delete(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Mark a canned response as used (increment use count)
     */
    @PostMapping("/{id}/use")
    public ResponseEntity<Void> markAsUsed(@PathVariable Long id) {
        cannedResponseService.markAsUsed(id);
        return ResponseEntity.ok().build();
    }
}
