package org.sensorvision.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
@Configuration
@ConfigurationProperties(prefix = "rate-limit")
public class RateLimitConfig {

    private boolean enabled = true;
    private int defaultCapacity = 100;
    private int defaultRefillTokens = 100;
    private int defaultRefillMinutes = 1;

    // Endpoint-specific limits
    private int dataIngestionCapacity = 1000;
    private int dataIngestionRefillTokens = 1000;
    private int dataIngestionRefillMinutes = 1;

    private int importCapacity = 50;
    private int importRefillTokens = 50;
    private int importRefillMinutes = 1;

    private int exportCapacity = 100;
    private int exportRefillTokens = 100;
    private int exportRefillMinutes = 1;

    // Cache for buckets per user/IP
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    public Bucket resolveBucket(String key, String endpoint) {
        return cache.computeIfAbsent(key, k -> createNewBucket(endpoint));
    }

    private Bucket createNewBucket(String endpoint) {
        Bandwidth limit;

        if (endpoint.contains("/data/ingest") || endpoint.contains("/data/")) {
            limit = Bandwidth.classic(
                dataIngestionCapacity,
                Refill.intervally(dataIngestionRefillTokens, Duration.ofMinutes(dataIngestionRefillMinutes))
            );
        } else if (endpoint.contains("/import/")) {
            limit = Bandwidth.classic(
                importCapacity,
                Refill.intervally(importRefillTokens, Duration.ofMinutes(importRefillMinutes))
            );
        } else if (endpoint.contains("/export/")) {
            limit = Bandwidth.classic(
                exportCapacity,
                Refill.intervally(exportRefillTokens, Duration.ofMinutes(exportRefillMinutes))
            );
        } else {
            limit = Bandwidth.classic(
                defaultCapacity,
                Refill.intervally(defaultRefillTokens, Duration.ofMinutes(defaultRefillMinutes))
            );
        }

        return Bucket.builder()
                .addLimit(limit)
                .build();
    }

    public void clearCache() {
        cache.clear();
    }
}
