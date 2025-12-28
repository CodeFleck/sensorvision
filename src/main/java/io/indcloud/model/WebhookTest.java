package io.indcloud.model;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "webhook_tests")
@Getter
@Setter
public class WebhookTest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organization_id", nullable = false)
    private Organization organization;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(length = 200)
    private String name;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "http_method", length = 10, nullable = false)
    private String httpMethod = "POST";

    @Convert(converter = JsonNodeConverter.class)
    @Column(columnDefinition = "jsonb")
    private JsonNode headers;

    @Column(name = "request_body", columnDefinition = "TEXT")
    private String requestBody;

    @Column(name = "status_code")
    private Integer statusCode;

    @Column(name = "response_body", columnDefinition = "TEXT")
    private String responseBody;

    @Convert(converter = JsonNodeConverter.class)
    @Column(name = "response_headers", columnDefinition = "jsonb")
    private JsonNode responseHeaders;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = Instant.now();
    }
}
