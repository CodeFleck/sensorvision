package org.sensorvision.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;

/**
 * Performance optimization configuration for the pilot program.
 * Configures caching, async processing, and performance monitoring.
 */
@Configuration
@EnableCaching
@EnableAsync
@EnableAspectJAutoProxy
@Slf4j
@ConditionalOnProperty(name = "pilot.performance.enabled", havingValue = "true", matchIfMissing = true)
public class PilotPerformanceConfig {

    /**
     * Redis cache manager with optimized configurations for pilot program
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host")
    public CacheManager redisCacheManager(RedisConnectionFactory redisConnectionFactory) {
        log.info("Configuring Redis cache manager for pilot performance optimization");
        
        // Default cache configuration
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Cache-specific configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Device cache - 5 minutes TTL
        cacheConfigurations.put("pilot-devices", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Telemetry query cache - 1 minute TTL for recent data
        cacheConfigurations.put("telemetry-query", defaultConfig.entryTtl(Duration.ofMinutes(1)));
        
        // Analytics cache - 10 minutes TTL
        cacheConfigurations.put("pilot-analytics", defaultConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Quota cache - 5 minutes TTL
        cacheConfigurations.put("pilot-quotas", defaultConfig.entryTtl(Duration.ofMinutes(5)));
        
        // Performance metrics cache - 2 minutes TTL
        cacheConfigurations.put("performance-metrics", defaultConfig.entryTtl(Duration.ofMinutes(2)));
        
        // Organization cache - 15 minutes TTL (less frequent changes)
        cacheConfigurations.put("organizations", defaultConfig.entryTtl(Duration.ofMinutes(15)));
        
        // User cache - 10 minutes TTL
        cacheConfigurations.put("users", defaultConfig.entryTtl(Duration.ofMinutes(10)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }

    /**
     * Fallback in-memory cache manager when Redis is not available
     */
    @Bean
    @ConditionalOnProperty(name = "spring.data.redis.host", matchIfMissing = true)
    public CacheManager inMemoryCacheManager() {
        log.info("Configuring in-memory cache manager for pilot performance optimization");
        return new ConcurrentMapCacheManager(
                "pilot-devices", 
                "telemetry-query", 
                "pilot-analytics", 
                "pilot-quotas",
                "performance-metrics",
                "organizations",
                "users"
        );
    }

    /**
     * Async executor for performance-optimized operations
     */
    @Bean(name = "pilotAsyncExecutor")
    public Executor pilotAsyncExecutor() {
        log.info("Configuring async executor for pilot performance optimization");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("pilot-async-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return executor;
    }

    /**
     * Telemetry processing executor for high-throughput operations
     */
    @Bean(name = "telemetryProcessingExecutor")
    public Executor telemetryProcessingExecutor() {
        log.info("Configuring telemetry processing executor for pilot performance optimization");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(200);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("telemetry-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return executor;
    }

    /**
     * Analytics processing executor for complex calculations
     */
    @Bean(name = "analyticsProcessingExecutor")
    public Executor analyticsProcessingExecutor() {
        log.info("Configuring analytics processing executor for pilot performance optimization");
        
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(50);
        executor.setKeepAliveSeconds(60);
        executor.setThreadNamePrefix("analytics-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        
        return executor;
    }
}