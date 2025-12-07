# SensorVision Pilot Performance Optimization

## Database Optimization

### 1. Index Analysis and Creation
```sql
-- Analyze current index usage
SELECT schemaname, tablename, attname, n_distinct, correlation 
FROM pg_stats 
WHERE schemaname = 'public' 
ORDER BY n_distinct DESC;

-- Add missing indexes for pilot workload
CREATE INDEX CONCURRENTLY idx_telemetry_device_timestamp 
ON telemetry_records(device_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_telemetry_variable_timestamp 
ON telemetry_records(variable_name, timestamp DESC) 
WHERE timestamp > NOW() - INTERVAL '30 days';

CREATE INDEX CONCURRENTLY idx_alerts_unacknowledged 
ON alerts(organization_id, acknowledged) 
WHERE acknowledged = false;

CREATE INDEX CONCURRENTLY idx_devices_active_org 
ON devices(organization_id, active) 
WHERE active = true;
```

### 2. Query Optimization
```sql
-- Optimize telemetry queries with partitioning
CREATE TABLE telemetry_records_y2025m12 PARTITION OF telemetry_records
FOR VALUES FROM ('2025-12-01') TO ('2026-01-01');

-- Add partial indexes for common queries
CREATE INDEX idx_recent_telemetry 
ON telemetry_records(device_id, timestamp DESC) 
WHERE timestamp > NOW() - INTERVAL '7 days';
```

### 3. Connection Pool Configuration
```yaml
# HikariCP settings for pilot load
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 5
      idle-timeout: 300000
      max-lifetime: 1800000
      connection-timeout: 20000
      leak-detection-threshold: 60000
```

## Application Performance

### 1. Caching Strategy
```java
// Redis configuration for pilot
@Configuration
@EnableCaching
public class CacheConfig {
    
    @Bean
    public CacheManager cacheManager() {
        RedisCacheManager.Builder builder = RedisCacheManager
            .RedisCacheManagerBuilder
            .fromConnectionFactory(redisConnectionFactory())
            .cacheDefaults(cacheConfiguration());
        return builder.build();
    }
    
    private RedisCacheConfiguration cacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair
                .fromSerializer(new GenericJackson2JsonRedisSerializer()));
    }
}
```

### 2. Async Processing
```java
// Async configuration for heavy operations
@Configuration
@EnableAsync
public class AsyncConfig {
    
    @Bean(name = "telemetryExecutor")
    public Executor telemetryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("telemetry-");
        executor.initialize();
        return executor;
    }
    
    @Bean(name = "alertExecutor")
    public Executor alertExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("alert-");
        executor.initialize();
        return executor;
    }
}
```

## Frontend Optimization

### 1. Bundle Optimization
```typescript
// Vite configuration for production
export default defineConfig({
  build: {
    rollupOptions: {
      output: {
        manualChunks: {
          vendor: ['react', 'react-dom'],
          charts: ['chart.js', 'react-chartjs-2'],
          ui: ['@headlessui/react', '@heroicons/react']
        }
      }
    },
    chunkSizeWarningLimit: 1000
  }
});
```

### 2. Lazy Loading
```typescript
// Implement route-based code splitting
const Dashboard = lazy(() => import('./pages/Dashboard'));
const Devices = lazy(() => import('./pages/Devices'));
const Analytics = lazy(() => import('./pages/Analytics'));

// Lazy load heavy components
const ChartComponent = lazy(() => import('./components/ChartComponent'));
```

## MQTT Optimization

### 1. Connection Management
```yaml
# MQTT broker configuration for pilot
mqtt:
  broker:
    max-connections: 1000
    max-message-size: 1048576  # 1MB
    keepalive: 60
    clean-session: true
  
  # Connection pooling
  client:
    pool-size: 10
    connection-timeout: 30
    reconnect-delay: 5000
```

### 2. Message Processing
```java
@Service
public class OptimizedTelemetryProcessor {
    
    @Async("telemetryExecutor")
    public CompletableFuture<Void> processTelemetryBatch(List<TelemetryMessage> messages) {
        // Batch process messages for better performance
        return CompletableFuture.runAsync(() -> {
            telemetryService.saveBatch(messages);
            webSocketService.broadcastBatch(messages);
        });
    }
}
```

## Monitoring and Metrics

### 1. Application Metrics
```java
// Custom metrics for pilot monitoring
@Component
public class PilotMetrics {
    
    private final Counter telemetryCounter = Counter.builder("telemetry.messages.total")
        .description("Total telemetry messages processed")
        .register(Metrics.globalRegistry);
    
    private final Timer processingTimer = Timer.builder("telemetry.processing.time")
        .description("Time to process telemetry messages")
        .register(Metrics.globalRegistry);
    
    private final Gauge activeDevices = Gauge.builder("devices.active.count")
        .description("Number of active devices")
        .register(Metrics.globalRegistry, this, PilotMetrics::getActiveDeviceCount);
}
```

### 2. Performance Thresholds
```yaml
# Performance targets for pilot
targets:
  api_response_time_p95: 500ms
  telemetry_ingestion_rate: 1000/second
  websocket_latency_p95: 100ms
  database_query_time_p95: 200ms
  concurrent_users: 100
  concurrent_devices: 500
```

## Load Testing Configuration

### 1. JMeter Test Plan
```xml
<!-- API Load Test -->
<TestPlan>
  <ThreadGroup>
    <stringProp name="ThreadGroup.num_threads">50</stringProp>
    <stringProp name="ThreadGroup.ramp_time">300</stringProp>
    <stringProp name="ThreadGroup.duration">1800</stringProp>
  </ThreadGroup>
</TestPlan>
```

### 2. Device Simulation
```python
# MQTT load testing script
import paho.mqtt.client as mqtt
import json
import time
import random
from concurrent.futures import ThreadPoolExecutor

def simulate_device(device_id):
    client = mqtt.Client()
    client.connect("pilot.sensorvision.io", 1883, 60)
    
    while True:
        payload = {
            "deviceId": device_id,
            "timestamp": int(time.time() * 1000),
            "variables": {
                "temperature": random.uniform(20, 30),
                "humidity": random.uniform(40, 80),
                "pressure": random.uniform(1000, 1020)
            }
        }
        
        client.publish(f"sensorvision/devices/{device_id}/telemetry", 
                      json.dumps(payload))
        time.sleep(30)  # Send every 30 seconds

# Simulate 100 devices
with ThreadPoolExecutor(max_workers=100) as executor:
    for i in range(100):
        executor.submit(simulate_device, f"pilot-device-{i:03d}")
```