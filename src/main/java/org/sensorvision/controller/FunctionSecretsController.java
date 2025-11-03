package org.sensorvision.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.sensorvision.dto.SecretCreateRequest;
import org.sensorvision.dto.SecretResponse;
import org.sensorvision.model.FunctionSecret;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.model.User;
import org.sensorvision.service.FunctionSecretsService;
import org.sensorvision.service.ServerlessFunctionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST API for managing encrypted secrets for serverless functions.
 */
@RestController
@RequestMapping("/api/v1/functions/{functionId}/secrets")
@RequiredArgsConstructor
public class FunctionSecretsController {

    private final FunctionSecretsService secretsService;
    private final ServerlessFunctionService functionService;

    /**
     * Get all secret keys for a function (values are not returned for security).
     */
    @GetMapping
    public List<SecretResponse> getSecrets(@PathVariable Long functionId) {
        // Validate function exists and user has access (enforces org scoping)
        ServerlessFunction function = functionService.getFunctionEntity(functionId);

        List<FunctionSecret> secrets = secretsService.getSecretKeys(function);
        return secrets.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Create or update a secret for a function.
     */
    @PutMapping("/{secretKey}")
    public ResponseEntity<SecretResponse> setSecret(
            @PathVariable Long functionId,
            @PathVariable String secretKey,
            @Valid @RequestBody SecretCreateRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        // Validate function exists and user has access
        ServerlessFunction function = functionService.getFunctionEntity(functionId);

        // Create/update secret
        FunctionSecret secret = secretsService.setSecret(
                function,
                secretKey,
                request.secretValue(),
                currentUser
        );

        return ResponseEntity.ok(toResponse(secret));
    }

    /**
     * Delete a secret.
     */
    @DeleteMapping("/{secretKey}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSecret(
            @PathVariable Long functionId,
            @PathVariable String secretKey
    ) {
        // Validate function exists and user has access (enforces org scoping)
        ServerlessFunction function = functionService.getFunctionEntity(functionId);

        secretsService.deleteSecret(function, secretKey);
    }

    /**
     * Check if a secret exists.
     */
    @GetMapping("/{secretKey}/exists")
    public ResponseEntity<Boolean> secretExists(
            @PathVariable Long functionId,
            @PathVariable String secretKey
    ) {
        // Validate function exists and user has access (enforces org scoping)
        ServerlessFunction function = functionService.getFunctionEntity(functionId);

        boolean exists = secretsService.secretExists(function, secretKey);
        return ResponseEntity.ok(exists);
    }

    /**
     * Convert entity to response DTO.
     */
    private SecretResponse toResponse(FunctionSecret secret) {
        return new SecretResponse(
                secret.getId(),
                secret.getSecretKey(),
                secret.getCreatedAt(),
                secret.getUpdatedAt()
        );
    }
}
