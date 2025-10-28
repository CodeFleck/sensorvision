package org.sensorvision.service;

import org.sensorvision.dto.CannedResponseDto;
import org.sensorvision.dto.CannedResponseRequest;
import org.sensorvision.model.CannedResponse;
import org.sensorvision.model.User;
import org.sensorvision.repository.CannedResponseRepository;
import org.sensorvision.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CannedResponseService {

    private static final Logger logger = LoggerFactory.getLogger(CannedResponseService.class);

    private final CannedResponseRepository cannedResponseRepository;
    private final SecurityUtils securityUtils;

    @Autowired
    public CannedResponseService(CannedResponseRepository cannedResponseRepository,
                                SecurityUtils securityUtils) {
        this.cannedResponseRepository = cannedResponseRepository;
        this.securityUtils = securityUtils;
    }

    /**
     * Get all active canned responses
     */
    @Transactional(readOnly = true)
    public List<CannedResponseDto> getAllActive() {
        return cannedResponseRepository.findByActiveTrue().stream()
            .map(CannedResponseDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all canned responses (including inactive ones)
     */
    @Transactional(readOnly = true)
    public List<CannedResponseDto> getAll() {
        return cannedResponseRepository.findAll().stream()
            .map(CannedResponseDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all active canned responses ordered by popularity
     */
    @Transactional(readOnly = true)
    public List<CannedResponseDto> getAllActiveByPopularity() {
        return cannedResponseRepository.findByActiveTrueOrderByUseCountDesc().stream()
            .map(CannedResponseDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get all canned responses (including inactive) ordered by popularity
     */
    @Transactional(readOnly = true)
    public List<CannedResponseDto> getAllByPopularity() {
        return cannedResponseRepository.findAllByOrderByUseCountDesc().stream()
            .map(CannedResponseDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get canned responses by category
     * @param category the category to filter by
     * @param includeInactive if true, includes inactive templates
     */
    @Transactional(readOnly = true)
    public List<CannedResponseDto> getByCategory(String category, boolean includeInactive) {
        if (includeInactive) {
            return cannedResponseRepository.findByCategory(category).stream()
                .map(CannedResponseDto::fromEntity)
                .collect(Collectors.toList());
        }
        return cannedResponseRepository.findByActiveTrueAndCategory(category).stream()
            .map(CannedResponseDto::fromEntity)
            .collect(Collectors.toList());
    }

    /**
     * Get a specific canned response by ID
     */
    @Transactional(readOnly = true)
    public CannedResponseDto getById(Long id) {
        CannedResponse response = cannedResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Canned response not found with id: " + id));
        return CannedResponseDto.fromEntity(response);
    }

    /**
     * Create a new canned response
     */
    @Transactional
    public CannedResponseDto create(CannedResponseRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        CannedResponse response = new CannedResponse();
        response.setTitle(request.title());
        response.setBody(request.body());
        response.setCategory(request.category());
        response.setActive(request.active() != null ? request.active() : true);
        response.setCreatedBy(currentUser);

        CannedResponse saved = cannedResponseRepository.save(response);
        logger.info("Created canned response '{}' by user {}", saved.getTitle(), currentUser.getUsername());

        return CannedResponseDto.fromEntity(saved);
    }

    /**
     * Update an existing canned response
     */
    @Transactional
    public CannedResponseDto update(Long id, CannedResponseRequest request) {
        User currentUser = securityUtils.getCurrentUser();

        CannedResponse response = cannedResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Canned response not found with id: " + id));

        response.setTitle(request.title());
        response.setBody(request.body());
        response.setCategory(request.category());
        if (request.active() != null) {
            response.setActive(request.active());
        }

        CannedResponse updated = cannedResponseRepository.save(response);
        logger.info("Updated canned response '{}' by user {}", updated.getTitle(), currentUser.getUsername());

        return CannedResponseDto.fromEntity(updated);
    }

    /**
     * Delete a canned response
     */
    @Transactional
    public void delete(Long id) {
        User currentUser = securityUtils.getCurrentUser();

        CannedResponse response = cannedResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Canned response not found with id: " + id));

        cannedResponseRepository.delete(response);
        logger.info("Deleted canned response '{}' by user {}", response.getTitle(), currentUser.getUsername());
    }

    /**
     * Mark a canned response as used (increment use count)
     */
    @Transactional
    public void markAsUsed(Long id) {
        CannedResponse response = cannedResponseRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Canned response not found with id: " + id));

        response.incrementUseCount();
        cannedResponseRepository.save(response);

        logger.debug("Incremented use count for canned response '{}'", response.getTitle());
    }
}
