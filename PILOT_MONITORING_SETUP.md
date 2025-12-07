# SensorVision Pilot Monitoring and Alerting Setup

## Monitoring Architecture

```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   Application   │───▶│   Prometheus    │───▶│    Grafana      │
│   (Metrics)     │    │   (Collection)  │    │ (Visualization) │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         ▼                       ▼                       ▼
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   CloudWatch    │    │   AlertManager  │    │   PagerDuty     │
│   (AWS Metrics) │    │   (Alerting)    │    │  (Escalation)   │
└─────────────────┘    └─────────────────┘    └─────────────────┘
```

## Application Metrics

### 1. Custom Metrics Configuration
```java
@Component
public class PilotMetricsCollector {
    
    // Business metrics
    private final Counter telemetryMessagesTotal = Counter.builder("sensorvision.telemetry.messages.total")
        .description("Total telemetry messages processed")
        .tag("pilot", "true")
        .register(Metrics.globalRegistry);
    
    private final Timer telemetryProcessingTime = Timer.builder("sensorvision.telemetry.processing.seconds")
        .description("Time to process telemetry messages")
        .register(Metrics.globalRegistry);
    
    private final Gauge activeDevicesGauge = Gauge.builder("sensorvision.devices.active")
        .description("Number of active devices")
        .register(Metrics.globalRegistry, this, PilotMetricsCollector::getActiveDeviceCount);
    
    private final Counter apiRequestsTotal = Counter.builder("sensorvision.api.requests.total")
        .description("Total API requests")
        .register(Metrics.globalRegistry);
    
    private final Timer apiResponseTime = Timer.builder("sensorvision.api.response.seconds")
        .description("API response time")
        .register(Metrics.globalRegistry);
    
    // Pilot-specific metrics
    private final Counter pilotUserLogins = Counter.builder("sensorvision.pilot.user.logins.total")
        .description("Pilot user login count")
        .register(Metrics.globalRegistry);
    
    private final Gauge pilotOrganizationsActive = Gauge.builder("sensorvision.pilot.organizations.active")
        .description("Active pilot organizations")
        .register(Metrics.globalRegistry, this, PilotMetricsCollector::getActivePilotOrganizations);
    
    // Error metrics
    private final Counter errorsTotal = Counter.builder("sensorvision.errors.total")
        .description("Total application errors")
        .register(Metrics.globalRegistry);
    
    public void recordTelemetryMessage(String organizationId, double processingTime) {
        telemetryMessagesTotal.increment(Tags.of("organization", organizationId));
        telemetryProcessingTime.record(processingTime, TimeUnit.MILLISECONDS);
    }
    
    public void recordApiRequest(String endpoint, String method, int statusCode, double responseTime) {
        apiRequestsTotal.increment(Tags.of(
            "endpoint", endpoint,
            "method", method,
            "status", String.valueOf(statusCode)
        ));
        apiResponseTime.record(responseTime, TimeUnit.MILLISECONDS);
    }
    
    private double getActiveDeviceCount() {
        return deviceRepository.countByActiveTrue();
    }
    
    private double getActivePilotOrganizations() {
        return organizationRepository.countPilotOrganizationsWithActiveDevices();
    }
}
```

### 2. Health Check Endpoints
```java
@Component
public class PilotHealthIndicator implements HealthIndicator {
    
    @Override
    public Health health() {
        Health.Builder builder = new Health.Builder();
        
        try {
            // Check database connectivity
            long deviceCount = deviceRepository.count();
            builder.withDetail("database.devices", deviceCount);
            
            // Check MQTT broker
            boolean mqttConnected = mqttService.isConnected();
            builder.withDetail("mqtt.connected", mqttConnected);
            
            // Check Redis cache
            boolean redisConnected = redisTemplate.getConnectionFactory()
                .getConnection().ping() != null;
            builder.withDetail("redis.connected", redisConnected);
            
            // Check external services
            boolean emailServiceUp = emailService.healthCheck();
            builder.withDetail("email.service", emailServiceUp);
            
            if (mqttConnected && redisConnected && emailServiceUp) {
                builder.up();
            } else {
                builder.down();
            }
            
        } catch (Exception e) {
            builder.down().withException(e);
        }
        
        return builder.build();
    }
}
```

## Prometheus Configuration

### 1. Prometheus Config (prometheus.yml)
```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

rule_files:
  - "pilot_alerts.yml"

alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093

scrape_configs:
  # SensorVision Application
  - job_name: 'sensorvision-pilot'
    static_configs:
      - targets: ['backend:8080']
    metrics_path: '/actuator/prometheus'
    scrape_interval: 10s
    
  # PostgreSQL Exporter
  - job_name: 'postgres-pilot'
    static_configs:
      - targets: ['postgres-exporter:9187']
    
  # MQTT Broker
  - job_name: 'mosquitto-pilot'
    static_configs:
      - targets: ['mosquitto-exporter:9234']
    
  # Node Exporter (System metrics)
  - job_name: 'node-pilot'
    static_configs:
      - targets: ['node-exporter:9100']
    
  # Redis Exporter
  - job_name: 'redis-pilot'
    static_configs:
      - targets: ['redis-exporter:9121']
```

### 2. Alert Rules (pilot_alerts.yml)
```yaml
groups:
  - name: sensorvision-pilot-alerts
    rules:
      # High-level service alerts
      - alert: ServiceDown
        expr: up{job=~"sensorvision-pilot|postgres-pilot|mosquitto-pilot"} == 0
        for: 1m
        labels:
          severity: critical
          pilot: "true"
        annotations:
          summary: "Service {{ $labels.job }} is down"
          description: "Service {{ $labels.job }} has been down for more than 1 minute"
      
      # Application performance alerts
      - alert: HighAPIResponseTime
        expr: histogram_quantile(0.95, rate(sensorvision_api_response_seconds_bucket[5m])) > 0.5
        for: 2m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High API response time"
          description: "95th percentile API response time is {{ $value }}s"
      
      - alert: HighTelemetryProcessingTime
        expr: histogram_quantile(0.95, rate(sensorvision_telemetry_processing_seconds_bucket[5m])) > 1.0
        for: 2m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High telemetry processing time"
          description: "95th percentile telemetry processing time is {{ $value }}s"
      
      # Resource utilization alerts
      - alert: HighCPUUsage
        expr: rate(process_cpu_seconds_total[5m]) * 100 > 80
        for: 5m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High CPU usage"
          description: "CPU usage is {{ $value }}%"
      
      - alert: HighMemoryUsage
        expr: (process_resident_memory_bytes / 1024 / 1024) > 1024
        for: 5m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High memory usage"
          description: "Memory usage is {{ $value }}MB"
      
      # Database alerts
      - alert: DatabaseConnectionsHigh
        expr: pg_stat_database_numbackends > 80
        for: 2m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High database connections"
          description: "Database has {{ $value }} active connections"
      
      - alert: DatabaseSlowQueries
        expr: rate(pg_stat_database_tup_returned[5m]) / rate(pg_stat_database_tup_fetched[5m]) < 0.1
        for: 5m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "Database slow queries detected"
          description: "Query efficiency is low: {{ $value }}"
      
      # Business logic alerts
      - alert: NoTelemetryData
        expr: increase(sensorvision_telemetry_messages_total[10m]) == 0
        for: 10m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "No telemetry data received"
          description: "No telemetry messages received in the last 10 minutes"
      
      - alert: HighErrorRate
        expr: rate(sensorvision_errors_total[5m]) > 0.1
        for: 2m
        labels:
          severity: critical
          pilot: "true"
        annotations:
          summary: "High error rate"
          description: "Error rate is {{ $value }} errors/second"
      
      # Pilot-specific alerts
      - alert: PilotUserLoginFailures
        expr: increase(sensorvision_pilot_user_login_failures_total[5m]) > 10
        for: 1m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "High pilot user login failures"
          description: "{{ $value }} login failures in the last 5 minutes"
      
      - alert: PilotOrganizationInactive
        expr: sensorvision_pilot_organizations_active < 5
        for: 30m
        labels:
          severity: warning
          pilot: "true"
        annotations:
          summary: "Low pilot organization activity"
          description: "Only {{ $value }} pilot organizations are active"
```

## AlertManager Configuration

### 1. AlertManager Config (alertmanager.yml)
```yaml
global:
  smtp_smarthost: 'email-smtp.us-west-2.amazonaws.com:587'
  smtp_from: 'alerts@pilot.sensorvision.io'
  smtp_auth_username: 'SMTP_USERNAME'
  smtp_auth_password: 'SMTP_PASSWORD'

route:
  group_by: ['alertname', 'pilot']
  group_wait: 10s
  group_interval: 10s
  repeat_interval: 1h
  receiver: 'pilot-team'
  routes:
    - match:
        severity: critical
        pilot: "true"
      receiver: 'pilot-critical'
      group_wait: 0s
      repeat_interval: 5m
    
    - match:
        severity: warning
        pilot: "true"
      receiver: 'pilot-warnings'

receivers:
  - name: 'pilot-team'
    email_configs:
      - to: 'pilot-team@sensorvision.io'
        subject: 'SensorVision Pilot Alert: {{ .GroupLabels.alertname }}'
        body: |
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Labels: {{ range .Labels.SortedPairs }}{{ .Name }}={{ .Value }} {{ end }}
          {{ end }}
    
    slack_configs:
      - api_url: 'SLACK_WEBHOOK_URL'
        channel: '#sensorvision-pilot-alerts'
        title: 'SensorVision Pilot Alert'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
  
  - name: 'pilot-critical'
    email_configs:
      - to: 'pilot-oncall@sensorvision.io'
        subject: 'CRITICAL: SensorVision Pilot Alert'
        body: |
          CRITICAL ALERT - Immediate attention required
          
          {{ range .Alerts }}
          Alert: {{ .Annotations.summary }}
          Description: {{ .Annotations.description }}
          Time: {{ .StartsAt }}
          {{ end }}
    
    pagerduty_configs:
      - routing_key: 'PAGERDUTY_INTEGRATION_KEY'
        description: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'
  
  - name: 'pilot-warnings'
    slack_configs:
      - api_url: 'SLACK_WEBHOOK_URL'
        channel: '#sensorvision-pilot-warnings'
        title: 'SensorVision Pilot Warning'
        text: '{{ range .Alerts }}{{ .Annotations.summary }}{{ end }}'

inhibit_rules:
  - source_match:
      severity: 'critical'
    target_match:
      severity: 'warning'
    equal: ['alertname', 'pilot']
```

## Grafana Dashboards

### 1. Pilot Overview Dashboard
```json
{
  "dashboard": {
    "title": "SensorVision Pilot Overview",
    "panels": [
      {
        "title": "Active Organizations",
        "type": "stat",
        "targets": [
          {
            "expr": "sensorvision_pilot_organizations_active",
            "legendFormat": "Active Organizations"
          }
        ]
      },
      {
        "title": "Total Devices",
        "type": "stat",
        "targets": [
          {
            "expr": "sensorvision_devices_active",
            "legendFormat": "Active Devices"
          }
        ]
      },
      {
        "title": "Telemetry Messages/sec",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(sensorvision_telemetry_messages_total[5m])",
            "legendFormat": "Messages/sec"
          }
        ]
      },
      {
        "title": "API Response Time",
        "type": "graph",
        "targets": [
          {
            "expr": "histogram_quantile(0.95, rate(sensorvision_api_response_seconds_bucket[5m]))",
            "legendFormat": "95th percentile"
          },
          {
            "expr": "histogram_quantile(0.50, rate(sensorvision_api_response_seconds_bucket[5m]))",
            "legendFormat": "50th percentile"
          }
        ]
      }
    ]
  }
}
```

### 2. System Health Dashboard
```json
{
  "dashboard": {
    "title": "SensorVision Pilot System Health",
    "panels": [
      {
        "title": "Service Uptime",
        "type": "stat",
        "targets": [
          {
            "expr": "up{job=\"sensorvision-pilot\"}",
            "legendFormat": "Application"
          },
          {
            "expr": "up{job=\"postgres-pilot\"}",
            "legendFormat": "Database"
          },
          {
            "expr": "up{job=\"mosquitto-pilot\"}",
            "legendFormat": "MQTT"
          }
        ]
      },
      {
        "title": "CPU Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "rate(process_cpu_seconds_total[5m]) * 100",
            "legendFormat": "CPU %"
          }
        ]
      },
      {
        "title": "Memory Usage",
        "type": "graph",
        "targets": [
          {
            "expr": "process_resident_memory_bytes / 1024 / 1024",
            "legendFormat": "Memory MB"
          }
        ]
      },
      {
        "title": "Database Connections",
        "type": "graph",
        "targets": [
          {
            "expr": "pg_stat_database_numbackends",
            "legendFormat": "Active Connections"
          }
        ]
      }
    ]
  }
}
```

## Log Aggregation

### 1. Structured Logging Configuration
```yaml
# logback-spring.xml for pilot
<configuration>
    <appender name="STDOUT" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <appender name="FILE" class="ch.qos.logback.core.rolling.RollingFileAppender">
        <file>logs/sensorvision-pilot.log</file>
        <rollingPolicy class="ch.qos.logback.core.rolling.TimeBasedRollingPolicy">
            <fileNamePattern>logs/sensorvision-pilot.%d{yyyy-MM-dd}.%i.gz</fileNamePattern>
            <maxFileSize>100MB</maxFileSize>
            <maxHistory>30</maxHistory>
            <totalSizeCap>3GB</totalSizeCap>
        </rollingPolicy>
        <encoder class="net.logstash.logback.encoder.LoggingEventCompositeJsonEncoder">
            <providers>
                <timestamp/>
                <logLevel/>
                <loggerName/>
                <message/>
                <mdc/>
                <arguments/>
                <stackTrace/>
            </providers>
        </encoder>
    </appender>
    
    <logger name="org.sensorvision" level="INFO"/>
    <logger name="org.sensorvision.pilot" level="DEBUG"/>
    
    <root level="INFO">
        <appender-ref ref="STDOUT"/>
        <appender-ref ref="FILE"/>
    </root>
</configuration>
```

### 2. CloudWatch Logs Integration
```yaml
# Docker Compose logging configuration
services:
  backend:
    logging:
      driver: awslogs
      options:
        awslogs-group: /aws/ecs/sensorvision-pilot
        awslogs-region: us-west-2
        awslogs-stream-prefix: backend
        
  postgres:
    logging:
      driver: awslogs
      options:
        awslogs-group: /aws/ecs/sensorvision-pilot
        awslogs-region: us-west-2
        awslogs-stream-prefix: postgres
        
  mosquitto:
    logging:
      driver: awslogs
      options:
        awslogs-group: /aws/ecs/sensorvision-pilot
        awslogs-region: us-west-2
        awslogs-stream-prefix: mosquitto
```

## Monitoring Checklist

### Pre-Pilot Launch
- [ ] Prometheus and Grafana deployed and configured
- [ ] AlertManager rules configured and tested
- [ ] PagerDuty integration set up for critical alerts
- [ ] Slack notifications configured for team alerts
- [ ] CloudWatch logs aggregation enabled
- [ ] Custom application metrics implemented
- [ ] Health check endpoints verified
- [ ] Dashboard access configured for pilot team

### During Pilot
- [ ] Daily monitoring dashboard reviews
- [ ] Weekly performance trend analysis
- [ ] Monthly capacity planning reviews
- [ ] Incident response procedures tested
- [ ] Alert fatigue monitoring and tuning
- [ ] User feedback correlation with metrics
- [ ] Performance baseline establishment

### Post-Pilot
- [ ] Monitoring effectiveness assessment
- [ ] Alert accuracy and relevance review
- [ ] Performance benchmark documentation
- [ ] Monitoring infrastructure scaling plan
- [ ] Lessons learned documentation