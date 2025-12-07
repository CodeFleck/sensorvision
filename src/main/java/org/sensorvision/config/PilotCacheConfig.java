package org.sensorvision.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * Redis caching configuration optimized for SensorVision Pilot Program
 * Implements intelligent caching strategies for pilot-scale performance
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "pilot.mode", havingValue = "true")
public class PilotCacheConfig {

    private static final Logger logger = LoggerFactory.getLogger(PilotCacheConfig.class);

    @Value("${spring.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.redis.port:6379}")
    private int redisPort;

    @Value("${spring.redis.password:}")
    private String redisPassword;

    @Value("${spring.redis.ssl:false}")
    private boolean redisSsl;

    @Value("${cache.ttl.devices:300}")
    private int deviceCacheTtl;

    @Value("${cache.ttl.telemetry-latest:60}")
    private int telemetryLatestCacheTtl;

    @Value("${cache.ttl.analytics:600}")
    private int analyticsCacheTtl;

    @Value("${cache.ttl.user-sessions:3600}")
    private int userSessionCacheTtl;

    /**
     * Redis connection factory with pilot-optimized settings
     */
    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
        config.setHostName(redisHost);
        config.setPort(redisPort);
        
        if (!redisPassword.isEmpty()) {
            config.setPassword(redisPassword);
        }

        LettuceConnectionFactory factory = new LettuceConnectionFactory(config);
        factory.setUseSsl(redisSsl);
        
        logger.info("Configured Redis connection: {}:{} (SSL: {})", redisHost, redisPort, redisSsl);
        return factory;
    }

    /**
     * Redis template with optimized serialization for pilot program
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        
        // Use String serializer for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());
        
        // Use JSON serializer for values
        GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonSerializer);
        template.setHashValueSerializer(jsonSerializer);
        
        template.setDefaultSerializer(jsonSerializer);
        template.afterPropertiesSet();
        
        return template;
    }

    /**
     * Cache manager with pilot-specific cache configurations
     */
    @Bean
    public CacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(10))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .disableCachingNullValues();

        // Pilot-specific cache configurations
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();
        
        // Device cache - 5 minutes (frequently accessed, changes moderately)
        cacheConfigurations.put("devices", defaultConfig
                .entryTtl(Duration.ofSeconds(deviceCacheTtl)));
        
        // Latest telemetry cache - 1 minute (real-time data, changes frequently)
        cacheConfigurations.put("telemetry-latest", defaultConfig
                .entryTtl(Duration.ofSeconds(telemetryLatestCacheTtl)));
        
        // Analytics cache - 10 minutes (expensive calculations, changes less frequently)
        cacheConfigurations.put("analytics", defaultConfig
                .entryTtl(Duration.ofSeconds(analyticsCacheTtl)));
        
        // User session cache - 1 hour (authentication data, moderate changes)
        cacheConfigurations.put("user-sessions", defaultConfig
                .entryTtl(Duration.ofSeconds(userSessionCacheTtl)));
        
        // Pilot quota cache - 5 minutes (quota checks, moderate frequency)
        cacheConfigurations.put("pilot-quotas", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));
        
        // Dashboard cache - 2 minutes (dashboard data, changes moderately)
        cacheConfigurations.put("dashboards", defaultConfig
                .entryTtl(Duration.ofMinutes(2)));
        
        // Rules cache - 5 minutes (rules evaluation, changes less frequently)
        cacheConfigurations.put("rules", defaultConfig
                .entryTtl(Duration.ofMinutes(5)));
        
        // Plugin marketplace cache - 30 minutes (plugin data, changes infrequently)
        cacheConfigurations.put("plugins", defaultConfig
                .entryTtl(Duration.ofMinutes(30)));
        
        // Organization cache - 15 minutes (org data, changes infrequently)
        cacheConfigurations.put("organizations", defaultConfig
                .entryTtl(Duration.ofMinutes(15)));
        
        // Synthetic variables cache - 3 minutes (calculated values, moderate frequency)
        cacheConfigurations.put("synthetic-variables", defaultConfig
                .entryTtl(Duration.ofMinutes(3)));

        RedisCacheManager cacheManager = RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .transactionAware()
                .build();

        logger.info("Configured Redis cache manager with {} cache configurations", cacheConfigurations.size());
        return cacheManager;
    }

    /**
     * Cache configuration for pilot program statistics
     */
    @Bean
    public RedisCacheConfiguration pilotStatsCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(15))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith("pilot:stats:");
    }

    /**
     * Cache configuration for quota enforcement
     */
    @Bean
    public RedisCacheConfiguration quotaCacheConfig() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofMinutes(5))
                .serializeKeysWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair
                        .fromSerializer(new GenericJackson2JsonRedisSerializer()))
                .prefixCacheNameWith("pilot:quota:");
    }
}