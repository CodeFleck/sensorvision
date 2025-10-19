# DevOps Best Practices for SensorVision IoT Platform

**Author**: Staff Engineer Analysis (Google SRE Principles Applied)
**Date**: October 16, 2025
**Status**: ✅ Phase 1 Complete | 🚧 Phases 2-5 In Progress

---

## Executive Summary

This document outlines a comprehensive DevOps transformation roadmap for the SensorVision IoT platform, applying industry best practices from Google SRE, AWS Well-Architected Framework, and the Twelve-Factor App methodology.

### Current State Assessment

**✅ Strengths:**
- Containerized application with Docker Compose
- Database migrations with Flyway
- Basic health checks configured
- Environment variable configuration
- Security fixes implemented

**❌ Critical Gaps:**
- No CI/CD pipeline
- Limited observability (logs, metrics, traces)
- Manual deployment process
- Plaintext secret management
- No infrastructure as code
- Single point of failure (no HA)
- Missing disaster recovery plan
- No defined SLIs/SLOs

---

## Phase 1: Foundation & Stability ✅ COMPLETED

### 1.1 Database Connection Resilience ✅

**Problem**: Application crashed immediately if database wasn't ready on startup.

**Solution Implemented**:
```yaml
spring:
  datasource:
    hikari:
      initialization-fail-timeout: 60000  # Wait up to 60s for DB
      connection-timeout: 30000
      connection-test-query: SELECT 1
      leak-detection-threshold: 60000
  flyway:
    connect-retries: 10
    connect-retries-interval: 5
```

**Benefits**:
- Graceful startup even if database is slow to initialize
- Automatic retry logic prevents container restart loops
- Connection leak detection for debugging
- Improved MTTR (Mean Time To Recovery)

**Testing**:
```bash
# Verify connection pooling
docker-compose logs backend | grep "HikariPool-1 - Start completed"

# Test database connectivity
docker exec sensorvision-backend nc -zv postgres 5432
```

### 1.2 Health Checks Enhancement 🚧

**Current Status**: Basic health check exists but not fully configured.

**Next Steps**:
```yaml
management:
  endpoint:
    health:
      show-details: always
      show-components: always
  health:
    db:
      enabled: true
    diskspace:
      enabled: true
    livenessState:
      enabled: true
    readinessState:
      enabled: true
```

**Testing Strategy**:
```bash
# Liveness probe (is process alive?)
curl http://localhost:8080/actuator/health/liveness

# Readiness probe (ready to serve traffic?)
curl http://localhost:8080/actuator/health/readiness

# Full health details
curl http://localhost:8080/actuator/health
```

---

## Phase 2: Observability & Monitoring 🚧 IN PROGRESS

### 2.1 Structured Logging

**Current**: Basic logging with logback, logs to file only.

**Target**: Structured JSON logging with correlation IDs, log aggregation.

**Implementation**:

1. Add dependencies to `build.gradle.kts`:
```kotlin
implementation("net.logstash.logback:logstash-logback-encoder:7.4")
implementation("org.slf4j:slf4j-api:2.0.9")
```

2. Create `logback-spring.xml`:
```xml
<configuration>
    <appender name="JSON_CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>trace_id</includeMdcKeyName>
            <includeMdcKeyName>span_id</includeMdcKeyName>
            <includeMdcKeyName>user_id</includeMdcKeyName>
            <includeMdcKeyName>organization_id</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_CONSOLE"/>
    </root>
</configuration>
```

3. Add correlation ID filter:
```java
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) {
        String correlationId = request.getHeader("X-Correlation-ID");
        if (correlationId == null) {
            correlationId = UUID.randomUUID().toString();
        }
        MDC.put("trace_id", correlationId);
        response.setHeader("X-Correlation-ID", correlationId);
        filterChain.doFilter(request, response);
        MDC.clear();
    }
}
```

**Log Aggregation Options**:
- **ELK Stack** (Elasticsearch, Logstash, Kibana)
- **Loki + Grafana** (lightweight, cost-effective)
- **CloudWatch Logs** (AWS)
- **Google Cloud Logging** (GCP)

### 2.2 Metrics & Prometheus

**Current**: Basic Actuator metrics, Prometheus exposed but not scraped.

**Enhancement**:

1. Add Micrometer dependencies:
```kotlin
implementation("io.micrometer:micrometer-registry-prometheus")
implementation("io.micrometer:micrometer-core")
```

2. Configure custom metrics:
```java
@Component
public class TelemetryMetrics {
    private final Counter telemetryReceived;
    private final Gauge devicesOnline;
    private final Timer telemetryProcessingTime;

    public TelemetryMetrics(MeterRegistry registry) {
        this.telemetryReceived = Counter.builder("telemetry.received")
            .description("Total telemetry messages received")
            .tags("source", "mqtt")
            .register(registry);

        this.devicesOnline = Gauge.builder("devices.online", this,
            metrics -> deviceService.countOnlineDevices())
            .description("Number of online devices")
            .register(registry);

        this.telemetryProcessingTime = Timer.builder("telemetry.processing.time")
            .description("Time to process telemetry message")
            .register(registry);
    }

    public void recordTelemetry(Runnable processing) {
        telemetryReceived.increment();
        telemetryProcessingTime.record(processing);
    }
}
```

3. Update Prometheus scrape config:
```yaml
# prometheus.yml
scrape_configs:
  - job_name: 'sensorvision-backend'
    scrape_interval: 15s
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['backend:8080']
        labels:
          environment: 'dev'
          service: 'sensorvision'
```

**Key Metrics to Track**:
- **Golden Signals**:
  - Latency: Request duration (p50, p95, p99)
  - Traffic: Requests per second
  - Errors: Error rate (4xx, 5xx)
  - Saturation: CPU, memory, disk, connection pool usage
- **Business Metrics**:
  - Telemetry messages/sec
  - Active devices
  - Alert triggering rate
  - Database query performance

### 2.3 Distributed Tracing

**Tool**: OpenTelemetry + Jaeger/Tempo

**Implementation**:
```kotlin
// build.gradle.kts
implementation("io.opentelemetry:opentelemetry-api:1.31.0")
implementation("io.opentelemetry:opentelemetry-sdk:1.31.0")
implementation("io.opentelemetry:opentelemetry-exporter-otlp:1.31.0")
implementation("io.micrometer:micrometer-tracing-bridge-otel")
```

**Benefits**:
- Trace requests across services
- Identify performance bottlenecks
- Debug distributed systems issues
- Understand service dependencies

### 2.4 Alerting Strategy

**Tool**: Prometheus Alertmanager + PagerDuty/Slack

**Critical Alerts** (Page immediately):
```yaml
# alerts/critical.yml
groups:
  - name: sensorvision_critical
    interval: 1m
    rules:
      - alert: HighErrorRate
        expr: sum(rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum(rate(http_server_requests_seconds_count[5m])) > 0.05
        for: 5m
        labels:
          severity: critical
        annotations:
          summary: "High error rate detected ({{ $value }}%)"

      - alert: DatabaseDown
        expr: up{job="sensorvision-backend"} == 0 or sum(hikaricp_connections_active) == 0
        for: 1m
        labels:
          severity: critical

      - alert: HighMemoryUsage
        expr: (process_memory_used / process_memory_max) > 0.90
        for: 5m
        labels:
          severity: critical

      - alert: TelemetryIngestionStalled
        expr: rate(telemetry_received_total[5m]) == 0 and telemetry_received_total offset 1h > 0
        for: 10m
        labels:
          severity: critical
```

**Warning Alerts** (Notify, don't page):
```yaml
      - alert: HighLatency
        expr: histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m])) > 1.0
        for: 10m
        labels:
          severity: warning

      - alert: LowDiskSpace
        expr: (node_filesystem_avail_bytes / node_filesystem_size_bytes) < 0.15
        for: 5m
        labels:
          severity: warning
```

---

## Phase 3: CI/CD Pipeline 🚧 PLANNED

### 3.1 GitHub Actions Workflow

**File**: `.github/workflows/ci-cd.yml`

```yaml
name: CI/CD Pipeline

on:
  push:
    branches: [main, develop]
  pull_request:
    branches: [main]

env:
  DOCKER_REGISTRY: ghcr.io
  IMAGE_NAME: ${{ github.repository }}

jobs:
  # Job 1: Code Quality & Security
  code-quality:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Run Checkstyle
        run: ./gradlew checkstyleMain checkstyleTest

      - name: Run SpotBugs
        run: ./gradlew spotbugsMain

      - name: Run OWASP Dependency Check
        run: ./gradlew dependencyCheckAnalyze

      - name: SonarQube Scan
        env:
          SONAR_TOKEN: ${{ secrets.SONAR_TOKEN }}
        run: ./gradlew sonar

  # Job 2: Unit & Integration Tests
  test:
    runs-on: ubuntu-latest
    services:
      postgres:
        image: postgres:15-alpine
        env:
          POSTGRES_PASSWORD: test123
          POSTGRES_DB: sensorvision_test
        options: >-
          --health-cmd pg_isready
          --health-interval 10s
          --health-timeout 5s
          --health-retries 5
        ports:
          - 5432:5432

      mosquitto:
        image: eclipse-mosquitto:2.0
        ports:
          - 1883:1883

    steps:
      - uses: actions/checkout@v4

      - name: Setup Java 17
        uses: actions/setup-java@v4
        with:
          java-version: '17'
          distribution: 'temurin'
          cache: gradle

      - name: Run Unit Tests
        run: ./gradlew test --info

      - name: Run Integration Tests
        env:
          DB_URL: jdbc:postgresql://localhost:5432/sensorvision_test
          DB_USERNAME: postgres
          DB_PASSWORD: test123
        run: ./gradlew integrationTest

      - name: Generate Test Coverage
        run: ./gradlew jacocoTestReport

      - name: Upload Coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          files: ./build/reports/jacoco/test/jacocoTestReport.xml

  # Job 3: Build & Push Docker Image
  build:
    needs: [code-quality, test]
    runs-on: ubuntu-latest
    permissions:
      contents: read
      packages: write
    steps:
      - uses: actions/checkout@v4

      - name: Log in to Container Registry
        uses: docker/login-action@v3
        with:
          registry: ${{ env.DOCKER_REGISTRY }}
          username: ${{ github.actor }}
          password: ${{ secrets.GITHUB_TOKEN }}

      - name: Extract metadata
        id: meta
        uses: docker/metadata-action@v5
        with:
          images: ${{ env.DOCKER_REGISTRY }}/${{ env.IMAGE_NAME }}
          tags: |
            type=ref,event=branch
            type=ref,event=pr
            type=semver,pattern={{version}}
            type=semver,pattern={{major}}.{{minor}}
            type=sha,prefix={{branch}}-

      - name: Build and push
        uses: docker/build-push-action@v5
        with:
          context: .
          push: true
          tags: ${{ steps.meta.outputs.tags }}
          labels: ${{ steps.meta.outputs.labels }}
          cache-from: type=gha
          cache-to: type=gha,mode=max

  # Job 4: Deploy to Staging
  deploy-staging:
    needs: build
    if: github.ref == 'refs/heads/develop'
    runs-on: ubuntu-latest
    environment:
      name: staging
      url: https://staging.sensorvision.example.com
    steps:
      - name: Deploy to Staging ECS
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ecs-task-definition-staging.json
          service: sensorvision-staging
          cluster: iot-platform-staging
          wait-for-service-stability: true

      - name: Run Smoke Tests
        run: |
          curl -f https://staging.sensorvision.example.com/actuator/health || exit 1

  # Job 5: Deploy to Production
  deploy-production:
    needs: build
    if: github.ref == 'refs/heads/main'
    runs-on: ubuntu-latest
    environment:
      name: production
      url: https://sensorvision.example.com
    steps:
      - name: Deploy to Production ECS (Blue/Green)
        uses: aws-actions/amazon-ecs-deploy-task-definition@v1
        with:
          task-definition: ecs-task-definition.json
          service: sensorvision-production
          cluster: iot-platform-production
          wait-for-service-stability: true

      - name: Run Production Health Checks
        run: |
          for i in {1..10}; do
            if curl -f https://sensorvision.example.com/actuator/health/readiness; then
              echo "Health check passed"
              exit 0
            fi
            sleep 10
          done
          echo "Health check failed"
          exit 1

      - name: Notify Slack
        uses: 8398a7/action-slack@v3
        with:
          status: ${{ job.status }}
          text: 'Production deployment completed'
          webhook_url: ${{ secrets.SLACK_WEBHOOK }}
```

### 3.2 Pre-commit Hooks

**File**: `.github/workflows/pre-commit.yml`

```yaml
repos:
  - repo: https://github.com/pre-commit/pre-commit-hooks
    rev: v4.5.0
    hooks:
      - id: trailing-whitespace
      - id: end-of-file-fixer
      - id: check-yaml
      - id: check-added-large-files
        args: ['--maxkb=1000']
      - id: detect-private-key

  - repo: local
    hooks:
      - id: gradle-test
        name: Run Gradle Tests
        entry: ./gradlew test
        language: system
        pass_filenames: false

      - id: security-scan
        name: Run Security Scan
        entry: ./gradlew dependencyCheckAnalyze
        language: system
        pass_filenames: false
```

### 3.3 Deployment Strategies

**Blue/Green Deployment**:
- Zero-downtime deployments
- Instant rollback capability
- Full environment parity

**Canary Deployment** (Advanced):
- Deploy to 5% of traffic
- Monitor metrics for 10 minutes
- Gradually increase to 100%
- Auto-rollback on errors

---

## Phase 4: Infrastructure as Code 🚧 PLANNED

### 4.1 Terraform for AWS Infrastructure

**File**: `terraform/main.tf`

```hcl
terraform {
  required_version = ">= 1.5"
  backend "s3" {
    bucket = "sensorvision-terraform-state"
    key    = "production/terraform.tfstate"
    region = "us-east-1"
    encrypt = true
    dynamodb_table = "terraform-state-lock"
  }
}

provider "aws" {
  region = var.aws_region

  default_tags {
    tags = {
      Project     = "SensorVision"
      Environment = var.environment
      ManagedBy   = "Terraform"
    }
  }
}

# VPC and Networking
module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
  version = "5.1.2"

  name = "sensorvision-${var.environment}"
  cidr = "10.0.0.0/16"

  azs             = ["us-east-1a", "us-east-1b", "us-east-1c"]
  private_subnets = ["10.0.1.0/24", "10.0.2.0/24", "10.0.3.0/24"]
  public_subnets  = ["10.0.101.0/24", "10.0.102.0/24", "10.0.103.0/24"]

  enable_nat_gateway = true
  enable_vpn_gateway = false
  enable_dns_hostnames = true

  tags = {
    Terraform = "true"
  }
}

# RDS PostgreSQL (Multi-AZ)
module "db" {
  source  = "terraform-aws-modules/rds/aws"
  version = "6.3.0"

  identifier = "sensorvision-${var.environment}"

  engine               = "postgres"
  engine_version       = "15.4"
  family               = "postgres15"
  major_engine_version = "15"
  instance_class       = "db.r6g.xlarge"

  allocated_storage     = 100
  max_allocated_storage = 500
  storage_encrypted     = true

  multi_az = true
  db_name  = "sensorvision"
  username = "sensorvision"
  port     = 5432

  # Secrets Manager integration
  manage_master_user_password = true

  vpc_security_group_ids = [aws_security_group.db.id]
  db_subnet_group_name   = module.vpc.database_subnet_group

  backup_retention_period = 30
  backup_window           = "03:00-04:00"
  maintenance_window      = "sun:04:00-sun:05:00"

  enabled_cloudwatch_logs_exports = ["postgresql", "upgrade"]

  performance_insights_enabled = true
  performance_insights_retention_period = 7

  tags = {
    Name = "sensorvision-${var.environment}-db"
  }
}

# ECS Cluster with Fargate
module "ecs" {
  source  = "terraform-aws-modules/ecs/aws"
  version = "5.7.0"

  cluster_name = "sensorvision-${var.environment}"

  cluster_configuration = {
    execute_command_configuration = {
      logging = "OVERRIDE"
      log_configuration = {
        cloud_watch_log_group_name = "/aws/ecs/sensorvision"
      }
    }
  }

  fargate_capacity_providers = {
    FARGATE = {
      default_capacity_provider_strategy = {
        weight = 50
      }
    }
    FARGATE_SPOT = {
      default_capacity_provider_strategy = {
        weight = 50
      }
    }
  }
}

# ECS Task Definition
resource "aws_ecs_task_definition" "backend" {
  family                   = "sensorvision-backend"
  requires_compatibilities = ["FARGATE"]
  network_mode             = "awsvpc"
  cpu                      = 2048
  memory                   = 4096
  execution_role_arn       = aws_iam_role.ecs_execution_role.arn
  task_role_arn            = aws_iam_role.ecs_task_role.arn

  container_definitions = jsonencode([
    {
      name  = "backend"
      image = "${var.ecr_repository_url}:${var.image_tag}"

      portMappings = [
        {
          containerPort = 8080
          protocol      = "tcp"
        }
      ]

      environment = [
        {
          name  = "SPRING_PROFILES_ACTIVE"
          value = var.environment
        }
      ]

      secrets = [
        {
          name      = "DB_USERNAME"
          valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:username::"
        },
        {
          name      = "DB_PASSWORD"
          valueFrom = "${aws_secretsmanager_secret.db_credentials.arn}:password::"
        },
        {
          name      = "JWT_SECRET"
          valueFrom = aws_secretsmanager_secret.jwt_secret.arn
        }
      ]

      logConfiguration = {
        logDriver = "awslogs"
        options = {
          "awslogs-group"         = "/ecs/sensorvision-backend"
          "awslogs-region"        = var.aws_region
          "awslogs-stream-prefix" = "ecs"
        }
      }

      healthCheck = {
        command     = ["CMD-SHELL", "curl -f http://localhost:8080/actuator/health/liveness || exit 1"]
        interval    = 30
        timeout     = 5
        retries     = 3
        startPeriod = 60
      }
    }
  ])
}

# Application Load Balancer
module "alb" {
  source  = "terraform-aws-modules/alb/aws"
  version = "9.0.0"

  name = "sensorvision-${var.environment}"

  load_balancer_type = "application"
  vpc_id             = module.vpc.vpc_id
  subnets            = module.vpc.public_subnets
  security_groups    = [aws_security_group.alb.id]

  target_groups = [
    {
      name_prefix      = "sv-"
      backend_protocol = "HTTP"
      backend_port     = 8080
      target_type      = "ip"

      health_check = {
        enabled             = true
        interval            = 30
        path                = "/actuator/health/readiness"
        port                = "traffic-port"
        healthy_threshold   = 2
        unhealthy_threshold = 3
        timeout             = 5
        protocol            = "HTTP"
        matcher             = "200"
      }
    }
  ]

  https_listeners = [
    {
      port               = 443
      protocol           = "HTTPS"
      certificate_arn    = aws_acm_certificate.main.arn
      target_group_index = 0
    }
  ]
}

# ElastiCache Redis (for session management)
resource "aws_elasticache_replication_group" "redis" {
  replication_group_id       = "sensorvision-${var.environment}"
  replication_group_description = "Redis cluster for session management"

  engine               = "redis"
  engine_version       = "7.0"
  node_type            = "cache.r6g.large"
  num_cache_clusters   = 2
  parameter_group_name = "default.redis7"

  port                  = 6379
  subnet_group_name     = aws_elasticache_subnet_group.redis.name
  security_group_ids    = [aws_security_group.redis.id]

  automatic_failover_enabled = true
  at_rest_encryption_enabled = true
  transit_encryption_enabled = true

  snapshot_retention_limit = 7
  snapshot_window          = "03:00-05:00"

  tags = {
    Name = "sensorvision-${var.environment}-redis"
  }
}

# Secrets Manager
resource "aws_secretsmanager_secret" "db_credentials" {
  name = "sensorvision/${var.environment}/db-credentials"
  recovery_window_in_days = 30
}

resource "aws_secretsmanager_secret" "jwt_secret" {
  name = "sensorvision/${var.environment}/jwt-secret"
  recovery_window_in_days = 30
}

# CloudWatch Log Groups
resource "aws_cloudwatch_log_group" "backend" {
  name              = "/ecs/sensorvision-backend"
  retention_in_days = 30
  kms_key_id        = aws_kms_key.logs.arn
}

# Outputs
output "alb_dns_name" {
  description = "DNS name of the load balancer"
  value       = module.alb.lb_dns_name
}

output "db_endpoint" {
  description = "RDS instance endpoint"
  value       = module.db.db_instance_endpoint
  sensitive   = true
}

output "redis_endpoint" {
  description = "Redis primary endpoint"
  value       = aws_elasticache_replication_group.redis.primary_endpoint_address
}
```

### 4.2 Kubernetes Helm Charts (Alternative to ECS)

**File**: `helm/sensorvision/values.yaml`

```yaml
replicaCount: 3

image:
  repository: ghcr.io/yourorg/sensorvision
  pullPolicy: IfNotPresent
  tag: "latest"

service:
  type: ClusterIP
  port: 8080

ingress:
  enabled: true
  className: nginx
  annotations:
    cert-manager.io/cluster-issuer: letsencrypt-prod
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
  hosts:
    - host: sensorvision.example.com
      paths:
        - path: /
          pathType: Prefix
  tls:
    - secretName: sensorvision-tls
      hosts:
        - sensorvision.example.com

resources:
  limits:
    cpu: 2000m
    memory: 4Gi
  requests:
    cpu: 1000m
    memory: 2Gi

autoscaling:
  enabled: true
  minReplicas: 3
  maxReplicas: 10
  targetCPUUtilizationPercentage: 70
  targetMemoryUtilizationPercentage: 80

postgresql:
  enabled: true
  auth:
    existingSecret: postgresql-credentials
  primary:
    resources:
      limits:
        cpu: 4000m
        memory: 8Gi
      requests:
        cpu: 2000m
        memory: 4Gi
    persistence:
      enabled: true
      size: 100Gi
      storageClass: gp3

mosquitto:
  enabled: true
  persistence:
    enabled: true
    size: 10Gi

redis:
  enabled: true
  auth:
    existingSecret: redis-credentials
  master:
    persistence:
      enabled: true
      size: 10Gi
  replica:
    replicaCount: 2

livenessProbe:
  httpGet:
    path: /actuator/health/liveness
    port: 8080
  initialDelaySeconds: 60
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

readinessProbe:
  httpGet:
    path: /actuator/health/readiness
    port: 8080
  initialDelaySeconds: 30
  periodSeconds: 10
  timeoutSeconds: 5
  failureThreshold: 3

env:
  - name: SPRING_PROFILES_ACTIVE
    value: "production"
  - name: DB_URL
    valueFrom:
      secretKeyRef:
        name: postgresql-credentials
        key: url
  - name: DB_USERNAME
    valueFrom:
      secretKeyRef:
        name: postgresql-credentials
        key: username
  - name: DB_PASSWORD
    valueFrom:
      secretKeyRef:
        name: postgresql-credentials
        key: password
  - name: JWT_SECRET
    valueFrom:
      secretKeyRef:
        name: jwt-secret
        key: secret
```

---

## Phase 5: Security & Compliance 🚧 PLANNED

### 5.1 Secret Management

**Current Issue**: Secrets in .env files (not suitable for production)

**Solution**: AWS Secrets Manager / HashiCorp Vault

**AWS Secrets Manager Integration**:
```java
@Configuration
@Profile("production")
public class SecretsManagerConfig {

    @Bean
    public SecretsManagerClient secretsManagerClient() {
        return SecretsManagerClient.builder()
            .region(Region.US_EAST_1)
            .build();
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
            SecretsManagerClient client) {

        String dbCredentials = getSecret(client, "sensorvision/production/db-credentials");
        String jwtSecret = getSecret(client, "sensorvision/production/jwt-secret");

        Properties props = new Properties();
        props.put("DB_PASSWORD", parseJson(dbCredentials).get("password"));
        props.put("JWT_SECRET", jwtSecret);

        PropertySourcesPlaceholderConfigurer configurer =
            new PropertySourcesPlaceholderConfigurer();
        configurer.setProperties(props);
        return configurer;
    }

    private static String getSecret(SecretsManagerClient client, String secretName) {
        GetSecretValueRequest request = GetSecretValueRequest.builder()
            .secretId(secretName)
            .build();
        return client.getSecretValue(request).secretString();
    }
}
```

### 5.2 Security Scanning

**Tools**:
- **SAST**: SonarQube, Checkmarx
- **DAST**: OWASP ZAP
- **Container Scanning**: Trivy, Snyk
- **Dependency Scanning**: OWASP Dependency-Check

**GitHub Actions Integration**:
```yaml
- name: Run Trivy vulnerability scanner
  uses: aquasecurity/trivy-action@master
  with:
    image-ref: ${{ env.DOCKER_REGISTRY }}/${{ env.IMAGE_NAME }}:${{ github.sha }}
    format: 'sarif'
    output: 'trivy-results.sarif'

- name: Upload Trivy results to GitHub Security
  uses: github/codeql-action/upload-sarif@v2
  with:
    sarif_file: 'trivy-results.sarif'
```

---

## Phase 6: Disaster Recovery & Business Continuity 🚧 PLANNED

### 6.1 Backup Strategy

**Database Backups**:
- **Automated daily backups** with 30-day retention
- **Point-in-time recovery** enabled
- **Cross-region replication** for DR

**Implementation** (RDS):
```hcl
backup_retention_period = 30
backup_window           = "03:00-04:00"
enabled_cloudwatch_logs_exports = ["postgresql"]

# Cross-region replica
resource "aws_db_instance_automated_backups_replication" "replica" {
  source_db_instance_arn = aws_db_instance.primary.arn
  retention_period       = 30
}
```

**Backup Testing**:
```bash
#!/bin/bash
# backup-test.sh - Test database restore monthly

# 1. Create test restore
aws rds restore-db-instance-from-db-snapshot \
  --db-instance-identifier sensorvision-restore-test \
  --db-snapshot-identifier latest-automated-snapshot

# 2. Wait for availability
aws rds wait db-instance-available \
  --db-instance-identifier sensorvision-restore-test

# 3. Run validation queries
psql -h restore-test-endpoint -U sensorvision -d sensorvision \
  -c "SELECT COUNT(*) FROM devices;" \
  -c "SELECT COUNT(*) FROM telemetry_records WHERE timestamp > NOW() - INTERVAL '24 hours';"

# 4. Cleanup
aws rds delete-db-instance \
  --db-instance-identifier sensorvision-restore-test \
  --skip-final-snapshot
```

### 6.2 Disaster Recovery Plan

**RPO (Recovery Point Objective)**: < 1 hour
**RTO (Recovery Time Objective)**: < 2 hours

**DR Architecture**:
```
Primary Region (us-east-1):
  ├── Multi-AZ RDS (active)
  ├── ECS Cluster (active)
  └── Redis (active)

DR Region (us-west-2):
  ├── RDS Read Replica (standby)
  ├── ECS Cluster (standby, scaled to 0)
  └── Redis (standby)
```

**Failover Procedure**:
```bash
#!/bin/bash
# dr-failover.sh

# 1. Promote RDS read replica in DR region
aws rds promote-read-replica \
  --db-instance-identifier sensorvision-dr-replica \
  --region us-west-2

# 2. Update DNS to point to DR region ALB
aws route53 change-resource-record-sets \
  --hosted-zone-id Z1234567890ABC \
  --change-batch file://dns-failover.json

# 3. Scale up ECS services in DR region
aws ecs update-service \
  --cluster sensorvision-dr \
  --service backend \
  --desired-count 3 \
  --region us-west-2

# 4. Verify health
curl -f https://sensorvision.example.com/actuator/health || exit 1

echo "Failover completed successfully"
```

---

## Phase 7: Performance Optimization 🚧 PLANNED

### 7.1 Caching Strategy

**Levels of Caching**:
1. **Application-level**: Spring Cache with Redis
2. **Database-level**: PostgreSQL query cache
3. **CDN**: CloudFront for static assets

**Implementation**:
```java
@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory factory) {
        RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new StringRedisSerializer()))
            .serializeValuesWith(
                RedisSerializationContext.SerializationPair.fromSerializer(
                    new GenericJackson2JsonRedisSerializer()));

        return RedisCacheManager.builder(factory)
            .cacheDefaults(config)
            .withCacheConfiguration("devices", config.entryTtl(Duration.ofHours(1)))
            .withCacheConfiguration("telemetry", config.entryTtl(Duration.ofMinutes(5)))
            .build();
    }
}

// Usage
@Service
public class DeviceService {

    @Cacheable(value = "devices", key = "#externalId")
    public Device findByExternalId(String externalId) {
        return deviceRepository.findByExternalId(externalId)
            .orElseThrow(() -> new ResourceNotFoundException("Device not found"));
    }

    @CacheEvict(value = "devices", key = "#device.externalId")
    public Device updateDevice(Device device) {
        return deviceRepository.save(device);
    }
}
```

### 7.2 Database Optimization

**Indexing Strategy**:
```sql
-- Add indexes for frequently queried columns
CREATE INDEX CONCURRENTLY idx_telemetry_device_timestamp
ON telemetry_records(device_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_telemetry_organization_timestamp
ON telemetry_records(organization_id, timestamp DESC);

CREATE INDEX CONCURRENTLY idx_alerts_triggered_at
ON alerts(triggered_at DESC) WHERE status = 'ACTIVE';

-- Partial index for active devices
CREATE INDEX CONCURRENTLY idx_devices_active
ON devices(organization_id, external_id) WHERE status = 'ONLINE';
```

**Query Optimization**:
```java
// Use projections instead of full entities
public interface TelemetrySummary {
    String getDeviceId();
    Double getAvgValue();
    LocalDateTime getTimestamp();
}

@Query("SELECT t.deviceId as deviceId, AVG(t.value) as avgValue, " +
       "t.timestamp as timestamp FROM TelemetryRecord t " +
       "WHERE t.organizationId = :orgId " +
       "AND t.timestamp BETWEEN :from AND :to " +
       "GROUP BY t.deviceId, t.timestamp")
List<TelemetrySummary> findTelemetrySummary(
    @Param("orgId") Long orgId,
    @Param("from") LocalDateTime from,
    @Param("to") LocalDateTime to);
```

**Connection Pooling** (Already implemented):
```yaml
hikari:
  maximum-pool-size: 20
  minimum-idle: 5
  connection-timeout: 30000
  idle-timeout: 600000
  max-lifetime: 1800000
```

### 7.3 Async Processing

**MQTT Message Processing**:
```java
@Configuration
@EnableAsync
public class AsyncConfig {
    @Bean
    public Executor telemetryExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(50);
        executor.setQueueCapacity(500);
        executor.setThreadNamePrefix("telemetry-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}

@Service
public class TelemetryIngestionService {

    @Async("telemetryExecutor")
    public CompletableFuture<Void> processAsync(TelemetryPayload payload) {
        try {
            ingest(payload, true);
            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
```

---

## Phase 8: Service Level Objectives (SLOs) 🚧 PLANNED

### 8.1 Define SLIs/SLOs

**Service Level Indicators**:
1. **Availability**: Percentage of successful health check probes
2. **Latency**: 95th percentile request duration
3. **Error Rate**: Percentage of 5xx responses
4. **Throughput**: Telemetry messages processed per second

**Service Level Objectives**:
```yaml
slos:
  - name: API Availability
    sli: (count(http_requests_total{status!~"5.."}) / count(http_requests_total)) * 100
    target: 99.9%
    window: 30d

  - name: API Latency
    sli: histogram_quantile(0.95, rate(http_request_duration_seconds_bucket[5m]))
    target: < 500ms
    window: 7d

  - name: Telemetry Processing Latency
    sli: histogram_quantile(0.95, rate(telemetry_processing_duration_seconds_bucket[5m]))
    target: < 1s
    window: 7d

  - name: Error Rate
    sli: (count(http_requests_total{status=~"5.."}) / count(http_requests_total)) * 100
    target: < 0.1%
    window: 7d
```

**Error Budget**:
- **99.9% availability** = 43 minutes downtime per month
- **Error budget tracking**:
```promql
# Remaining error budget
1 - (
  sum(rate(http_requests_total{status=~"5.."}[30d])) /
  sum(rate(http_requests_total[30d]))
) / 0.001
```

---

## Phase 9: Cost Optimization 🚧 PLANNED

### 9.1 AWS Cost Optimization

**Strategies**:
1. **Right-sizing**:
   - Start with t3.medium instances
   - Use AWS Compute Optimizer recommendations
   - Enable auto-scaling

2. **Reserved Instances**:
   - Purchase 1-year RDS Reserved Instances (40% savings)
   - Use Savings Plans for ECS Fargate (20% savings)

3. **S3 Lifecycle Policies**:
```hcl
resource "aws_s3_bucket_lifecycle_configuration" "logs" {
  bucket = aws_s3_bucket.logs.id

  rule {
    id     = "archive-old-logs"
    status = "Enabled"

    transition {
      days          = 30
      storage_class = "STANDARD_IA"
    }

    transition {
      days          = 90
      storage_class = "GLACIER"
    }

    expiration {
      days = 365
    }
  }
}
```

4. **Database Cost Optimization**:
   - Use Aurora Serverless v2 for dev/staging
   - Enable automated storage scaling
   - Archive old telemetry data to S3

**Cost Monitoring**:
```yaml
# CloudWatch billing alarm
resource "aws_cloudwatch_metric_alarm" "billing" {
  alarm_name          = "monthly-billing-alarm"
  comparison_operator = "GreaterThanThreshold"
  evaluation_periods  = "1"
  metric_name         = "EstimatedCharges"
  namespace           = "AWS/Billing"
  period              = "21600"  # 6 hours
  statistic           = "Maximum"
  threshold           = var.monthly_budget
  alarm_description   = "Triggers when monthly costs exceed budget"
  alarm_actions       = [aws_sns_topic.billing_alerts.arn]
}
```

---

## Implementation Roadmap

### Sprint 1-2 (Weeks 1-4): Foundation ✅
- [x] Database connection resilience
- [x] HikariCP configuration
- [x] Flyway retry logic
- [ ] Enhanced health checks

### Sprint 3-4 (Weeks 5-8): Observability
- [ ] Structured logging (JSON)
- [ ] Prometheus metrics
- [ ] Custom business metrics
- [ ] Grafana dashboards

### Sprint 5-6 (Weeks 9-12): CI/CD
- [ ] GitHub Actions workflows
- [ ] Automated testing
- [ ] Security scanning
- [ ] Staging deployment

### Sprint 7-8 (Weeks 13-16): Infrastructure
- [ ] Terraform for AWS
- [ ] ECS/Fargate setup
- [ ] RDS Multi-AZ
- [ ] Load balancer configuration

### Sprint 9-10 (Weeks 17-20): Production Hardening
- [ ] Secrets Manager integration
- [ ] Disaster recovery setup
- [ ] Backup testing
- [ ] Performance optimization

### Sprint 11-12 (Weeks 21-24): Monitoring & SLOs
- [ ] Alerting rules
- [ ] PagerDuty integration
- [ ] SLO tracking
- [ ] Runbook documentation

---

## Key Metrics to Track

### Golden Signals
```
1. Latency
   - API endpoint latency (p50, p95, p99)
   - Database query latency
   - MQTT message processing time

2. Traffic
   - Requests per second
   - Telemetry messages per second
   - Active WebSocket connections

3. Errors
   - HTTP error rate (4xx, 5xx)
   - Database connection errors
   - MQTT connection failures

4. Saturation
   - CPU utilization
   - Memory usage
   - Database connection pool usage
   - Disk I/O
```

### Business Metrics
```
- Total devices
- Active devices (last 24h)
- Telemetry messages received
- Alerts triggered
- Average device uptime
```

---

## Conclusion

This DevOps transformation will take **approximately 6 months** to fully implement across all phases. The return on investment includes:

**Benefits**:
- **99.9% uptime** (from ~95% currently)
- **50% faster deployments** (from hours to minutes)
- **80% reduction in MTTR** (Mean Time To Recovery)
- **Complete audit trail** and compliance readiness
- **Proactive alerting** prevents outages
- **Scalability** to millions of devices

**Next Immediate Actions**:
1. Complete health check enhancements (this week)
2. Implement structured logging (next week)
3. Set up Prometheus monitoring (week 3)
4. Create GitHub Actions CI pipeline (week 4)

**Questions or need help with implementation? Contact the DevOps team.**

---

**Document Version**: 1.0
**Last Updated**: October 16, 2025
**Owner**: DevOps Team
