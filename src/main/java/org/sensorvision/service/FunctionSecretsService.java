package org.sensorvision.service;

import lombok.extern.slf4j.Slf4j;
import org.sensorvision.dto.SecretResponse;
import org.sensorvision.model.FunctionSecret;
import org.sensorvision.model.ServerlessFunction;
import org.sensorvision.model.User;
import org.sensorvision.repository.FunctionSecretRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Service for managing encrypted secrets for serverless functions.
 * Uses AES-256-GCM for encryption with a 96-bit IV and 128-bit auth tag.
 */
@Slf4j
@Service
public class FunctionSecretsService {

    private static final String ENCRYPTION_ALGORITHM = "AES/GCM/NoPadding";
    private static final int GCM_IV_LENGTH = 12; // 96 bits
    private static final int GCM_TAG_LENGTH = 128; // 128 bits
    private static final Pattern SECRET_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final FunctionSecretRepository secretRepository;
    private final SecretKey encryptionKey;

    public FunctionSecretsService(
            FunctionSecretRepository secretRepository,
            @Value("${serverless.secrets.encryption-key:}") String encryptionKeyBase64) {
        this.secretRepository = secretRepository;

        // Initialize encryption key
        if (encryptionKeyBase64 == null || encryptionKeyBase64.isEmpty()) {
            log.warn("No encryption key configured! Generating a random key. " +
                    "This key will NOT persist across restarts. " +
                    "Set 'serverless.secrets.encryption-key' in application.properties for production.");
            this.encryptionKey = generateRandomKey();
        } else {
            try {
                byte[] keyBytes = Base64.getDecoder().decode(encryptionKeyBase64);
                this.encryptionKey = new SecretKeySpec(keyBytes, "AES");
                log.info("Loaded encryption key from configuration");
            } catch (Exception e) {
                throw new IllegalStateException("Failed to load encryption key from configuration", e);
            }
        }
    }

    /**
     * Create or update a secret for a function.
     */
    @Transactional
    public FunctionSecret setSecret(ServerlessFunction function, String secretKey, String secretValue, User createdBy) {
        validateSecretKey(secretKey);

        if (secretValue == null || secretValue.isEmpty()) {
            throw new IllegalArgumentException("Secret value cannot be empty");
        }

        try {
            String encryptedValue = encrypt(secretValue);

            FunctionSecret secret = secretRepository
                    .findByFunctionIdAndSecretKey(function.getId(), secretKey)
                    .orElse(new FunctionSecret());

            secret.setFunction(function);
            secret.setSecretKey(secretKey);
            secret.setEncryptedValue(encryptedValue);
            secret.setEncryptionVersion("v1");

            if (secret.getId() == null) {
                secret.setCreatedBy(createdBy);
            }

            return secretRepository.save(secret);
        } catch (Exception e) {
            throw new RuntimeException("Failed to encrypt secret: " + e.getMessage(), e);
        }
    }

    /**
     * Get all secrets for a function (encrypted values NOT included for security).
     * Returns DTOs to avoid exposing entity internals and prevent accidental mutation.
     */
    @Transactional(readOnly = true)
    public List<SecretResponse> getSecretKeys(ServerlessFunction function) {
        List<FunctionSecret> secrets = secretRepository.findByFunctionId(function.getId());

        // Map directly to response DTOs (encrypted values never included)
        return secrets.stream()
                .map(secret -> new SecretResponse(
                        secret.getId(),
                        secret.getSecretKey(),
                        secret.getCreatedAt(),
                        secret.getUpdatedAt()
                ))
                .toList();
    }

    /**
     * Get decrypted secrets for function execution.
     * Returns a map of secret key -> decrypted value.
     */
    @Transactional(readOnly = true)
    public Map<String, String> getDecryptedSecrets(Long functionId) {
        List<FunctionSecret> secrets = secretRepository.findByFunctionId(functionId);
        Map<String, String> decryptedSecrets = new HashMap<>();

        for (FunctionSecret secret : secrets) {
            try {
                String decryptedValue = decrypt(secret.getEncryptedValue());
                decryptedSecrets.put(secret.getSecretKey(), decryptedValue);
            } catch (Exception e) {
                log.error("Failed to decrypt secret {} for function {}: {}",
                        secret.getSecretKey(), functionId, e.getMessage());
                // Skip this secret but continue with others
            }
        }

        return decryptedSecrets;
    }

    /**
     * Delete a secret.
     */
    @Transactional
    public void deleteSecret(ServerlessFunction function, String secretKey) {
        secretRepository.deleteByFunctionIdAndSecretKey(function.getId(), secretKey);
    }

    /**
     * Check if a secret exists.
     */
    @Transactional(readOnly = true)
    public boolean secretExists(ServerlessFunction function, String secretKey) {
        return secretRepository.existsByFunctionIdAndSecretKey(function.getId(), secretKey);
    }

    /**
     * Encrypt a secret value using AES-256-GCM.
     */
    private String encrypt(String plaintext) throws Exception {
        // Generate random IV
        byte[] iv = new byte[GCM_IV_LENGTH];
        SecureRandom random = new SecureRandom();
        random.nextBytes(iv);

        // Encrypt
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, spec);
        byte[] ciphertext = cipher.doFinal(plaintext.getBytes("UTF-8"));

        // Combine IV and ciphertext
        ByteBuffer byteBuffer = ByteBuffer.allocate(iv.length + ciphertext.length);
        byteBuffer.put(iv);
        byteBuffer.put(ciphertext);

        // Return as base64
        return Base64.getEncoder().encodeToString(byteBuffer.array());
    }

    /**
     * Decrypt a secret value using AES-256-GCM.
     */
    private String decrypt(String encryptedBase64) throws Exception {
        byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

        // Extract IV and ciphertext
        ByteBuffer byteBuffer = ByteBuffer.wrap(encrypted);
        byte[] iv = new byte[GCM_IV_LENGTH];
        byteBuffer.get(iv);
        byte[] ciphertext = new byte[byteBuffer.remaining()];
        byteBuffer.get(ciphertext);

        // Decrypt
        Cipher cipher = Cipher.getInstance(ENCRYPTION_ALGORITHM);
        GCMParameterSpec spec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
        cipher.init(Cipher.DECRYPT_MODE, encryptionKey, spec);
        byte[] plaintext = cipher.doFinal(ciphertext);

        return new String(plaintext, "UTF-8");
    }

    /**
     * Validate secret key format (uppercase with underscores).
     */
    private void validateSecretKey(String secretKey) {
        if (secretKey == null || secretKey.isEmpty()) {
            throw new IllegalArgumentException("Secret key cannot be empty");
        }

        if (secretKey.length() < 2 || secretKey.length() > 100) {
            throw new IllegalArgumentException(
                    "Secret key must be between 2 and 100 characters");
        }

        if (!SECRET_KEY_PATTERN.matcher(secretKey).matches()) {
            throw new IllegalArgumentException(
                    "Secret key must start with uppercase letter and contain only uppercase letters, numbers, and underscores (e.g., API_KEY, DATABASE_URL)");
        }
    }

    /**
     * Generate a random 256-bit AES key.
     * SECURITY NOTE: The generated key is NOT persisted and will be lost on restart.
     * For production use, generate a key externally and set 'serverless.secrets.encryption-key'.
     */
    private SecretKey generateRandomKey() {
        try {
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(256);
            SecretKey key = keyGenerator.generateKey();

            // DO NOT log the actual key - security vulnerability
            log.warn("Generated ephemeral encryption key. This key will NOT persist across restarts. " +
                    "Generate a production key with: openssl rand -base64 32");

            return key;
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate encryption key", e);
        }
    }
}
