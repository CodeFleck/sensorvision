package org.sensorvision.model;

import jakarta.persistence.*;

/**
 * Encrypted secret for serverless functions.
 * Secrets are encrypted at rest using AES-256-GCM and injected as environment variables at runtime.
 */
@Entity
@Table(name = "function_secrets")
public class FunctionSecret extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "function_id", nullable = false)
    private ServerlessFunction function;

    /**
     * Environment variable name (uppercase with underscores, e.g., API_KEY, DATABASE_PASSWORD).
     * Must match pattern: ^[A-Z][A-Z0-9_]*$
     */
    @Column(name = "secret_key", nullable = false, length = 100)
    private String secretKey;

    /**
     * AES-256-GCM encrypted secret value (base64 encoded).
     */
    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    /**
     * Encryption algorithm version for key rotation support.
     */
    @Column(name = "encryption_version", nullable = false, length = 10)
    private String encryptionVersion = "v1";

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // Constructors
    public FunctionSecret() {
    }

    public FunctionSecret(ServerlessFunction function, String secretKey, String encryptedValue) {
        this.function = function;
        this.secretKey = secretKey;
        this.encryptedValue = encryptedValue;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ServerlessFunction getFunction() {
        return function;
    }

    public void setFunction(ServerlessFunction function) {
        this.function = function;
    }

    public String getSecretKey() {
        return secretKey;
    }

    public void setSecretKey(String secretKey) {
        this.secretKey = secretKey;
    }

    public String getEncryptedValue() {
        return encryptedValue;
    }

    public void setEncryptedValue(String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }

    public String getEncryptionVersion() {
        return encryptionVersion;
    }

    public void setEncryptionVersion(String encryptionVersion) {
        this.encryptionVersion = encryptionVersion;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User createdBy) {
        this.createdBy = createdBy;
    }
}
