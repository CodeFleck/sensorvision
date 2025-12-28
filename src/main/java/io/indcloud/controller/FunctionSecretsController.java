package io.indcloud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import io.indcloud.dto.SecretCreateRequest;
import io.indcloud.dto.SecretResponse;
import io.indcloud.model.FunctionSecret;
import io.indcloud.model.ServerlessFunction;
import io.indcloud.model.User;
import io.indcloud.service.FunctionSecretsService;
import io.indcloud.service.ServerlessFunctionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        // Service returns DTOs directly
        return secretsService.getSecretKeys(function);
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
