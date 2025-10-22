# SensorVision - AWS Deployment Guide

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Prerequisites](#prerequisites)
4. [Deployment Options](#deployment-options)
5. [Option 1: EC2 with Docker](#option-1-ec2-with-docker-recommended)
6. [Option 2: ECS (Elastic Container Service)](#option-2-ecs-elastic-container-service)
7. [Option 3: Elastic Beanstalk](#option-3-elastic-beanstalk)
8. [Database Setup (RDS)](#database-setup-rds)
9. [Storage and Networking](#storage-and-networking)
10. [Security Configuration](#security-configuration)
11. [Monitoring and Logging](#monitoring-and-logging)
12. [Cost Optimization](#cost-optimization)
13. [Deployment Scripts](#deployment-scripts)

---

## Overview

This guide provides comprehensive instructions for deploying SensorVision to Amazon Web Services (AWS). The platform consists of:

- **Backend**: Spring Boot application (Java)
- **Frontend**: React SPA (served via Nginx or backend static resources)
- **Database**: PostgreSQL
- **Message Broker**: Eclipse Mosquitto (MQTT)
- **Monitoring**: Prometheus + Grafana (optional)

---

## Architecture

### Production Architecture Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         AWS Cloud                               │
│                                                                  │
│  ┌──────────────┐        ┌───────────────────────────────┐     │
│  │   Route 53   │───────▶│  Application Load Balancer     │     │
│  │ (DNS)        │        │  (ALB)                        │     │
│  └──────────────┘        └─────────┬─────────────────────┘     │
│                                    │                            │
│                          ┌─────────┴─────────┐                  │
│                          │                   │                  │
│              ┌───────────▼─────┐  ┌─────────▼────────┐          │
│              │   EC2 Instance  │  │  EC2 Instance    │          │
│              │  (Web + App)    │  │  (Web + App)     │          │
│              │                 │  │                  │          │
│              │  - Spring Boot  │  │  - Spring Boot   │          │
│              │  - React (Nginx)│  │  - React (Nginx) │          │
│              │  - MQTT Broker  │  │  - MQTT Broker   │          │
│              └────────┬────────┘  └────────┬─────────┘          │
│                       │                    │                    │
│                       └─────────┬──────────┘                    │
│                                 │                               │
│                    ┌────────────▼────────────┐                  │
│                    │  Amazon RDS             │                  │
│                    │  (PostgreSQL)           │                  │
│                    │  - Multi-AZ             │                  │
│                    │  - Automated Backups    │                  │
│                    └─────────────────────────┘                  │
│                                                                  │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  Amazon S3  (Static Assets, Backups, Logs)          │       │
│  └──────────────────────────────────────────────────────┘       │
│                                                                  │
│  ┌──────────────────────────────────────────────────────┐       │
│  │  CloudWatch (Logs, Metrics, Alarms)                 │       │
│  └──────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## Prerequisites

### AWS Account Requirements
- Active AWS account with billing enabled
- IAM user with appropriate permissions
- AWS CLI installed and configured

### Local Requirements
- Docker and Docker Compose installed
- AWS CLI v2
- SSH key pair for EC2 access
- Basic knowledge of AWS services

### AWS Services Used
- **EC2**: Virtual servers
- **RDS**: Managed PostgreSQL database
- **ALB**: Load balancing
- **Route 53**: DNS management (optional)
- **S3**: Static assets and backups
- **CloudWatch**: Monitoring and logging
- **VPC**: Network isolation
- **Security Groups**: Firewall rules

---

## Deployment Options

### Comparison Matrix

| Feature | EC2 + Docker | ECS | Elastic Beanstalk |
|---------|-------------|-----|-------------------|
| **Complexity** | Medium | High | Low |
| **Control** | Full | High | Medium |
| **Cost** | $$ | $$$ | $ |
| **Scalability** | Manual/Auto | Automatic | Automatic |
| **Maintenance** | High | Low | Low |
| **Best For** | Custom setups | Microservices | Simple deployments |
| **Recommendation** | ✅ **Recommended** | For large scale | Quick start |

---

## Option 1: EC2 with Docker (Recommended)

This option provides the best balance of control, cost, and simplicity for SensorVision.

### Step 1: Create VPC and Network Infrastructure

```bash
# Create VPC
aws ec2 create-vpc \
  --cidr-block 10.0.0.0/16 \
  --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=sensorvision-vpc}]'

# Note the VPC ID from output
export VPC_ID="vpc-xxxxx"

# Create subnets (public and private)
aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.1.0/24 \
  --availability-zone us-east-1a \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=sensorvision-public-1a}]'

aws ec2 create-subnet \
  --vpc-id $VPC_ID \
  --cidr-block 10.0.2.0/24 \
  --availability-zone us-east-1b \
  --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=sensorvision-public-1b}]'

# Create Internet Gateway
aws ec2 create-internet-gateway \
  --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=sensorvision-igw}]'

export IGW_ID="igw-xxxxx"

# Attach IGW to VPC
aws ec2 attach-internet-gateway \
  --vpc-id $VPC_ID \
  --internet-gateway-id $IGW_ID
```

### Step 2: Create Security Groups

```bash
# Security group for web/app server
aws ec2 create-security-group \
  --group-name sensorvision-web \
  --description "Security group for SensorVision web application" \
  --vpc-id $VPC_ID

export WEB_SG_ID="sg-xxxxx"

# Allow HTTP
aws ec2 authorize-security-group-ingress \
  --group-id $WEB_SG_ID \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# Allow HTTPS
aws ec2 authorize-security-group-ingress \
  --group-id $WEB_SG_ID \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# Allow SSH (restrict to your IP)
aws ec2 authorize-security-group-ingress \
  --group-id $WEB_SG_ID \
  --protocol tcp \
  --port 22 \
  --cidr YOUR_IP/32

# Allow MQTT
aws ec2 authorize-security-group-ingress \
  --group-id $WEB_SG_ID \
  --protocol tcp \
  --port 1883 \
  --cidr 0.0.0.0/0

# Allow MQTT WebSocket (if needed)
aws ec2 authorize-security-group-ingress \
  --group-id $WEB_SG_ID \
  --protocol tcp \
  --port 9001 \
  --cidr 0.0.0.0/0

# Security group for database
aws ec2 create-security-group \
  --group-name sensorvision-db \
  --description "Security group for SensorVision database" \
  --vpc-id $VPC_ID

export DB_SG_ID="sg-xxxxx"

# Allow PostgreSQL from web servers only
aws ec2 authorize-security-group-ingress \
  --group-id $DB_SG_ID \
  --protocol tcp \
  --port 5432 \
  --source-group $WEB_SG_ID
```

### Step 3: Create RDS PostgreSQL Database

```bash
# Create DB subnet group
aws rds create-db-subnet-group \
  --db-subnet-group-name sensorvision-db-subnet \
  --db-subnet-group-description "Subnet group for SensorVision RDS" \
  --subnet-ids subnet-xxxxx subnet-yyyyy

# Create RDS PostgreSQL instance
aws rds create-db-instance \
  --db-instance-identifier sensorvision-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username sensoradmin \
  --master-user-password 'YOUR_SECURE_PASSWORD' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids $DB_SG_ID \
  --db-subnet-group-name sensorvision-db-subnet \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --multi-az \
  --publicly-accessible false \
  --storage-encrypted \
  --db-name sensorvision \
  --tags Key=Name,Value=sensorvision-database

# Wait for database to be available
aws rds wait db-instance-available --db-instance-identifier sensorvision-db

# Get database endpoint
aws rds describe-db-instances \
  --db-instance-identifier sensorvision-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

### Step 4: Launch EC2 Instance

```bash
# Create key pair (if you don't have one)
aws ec2 create-key-pair \
  --key-name sensorvision-key \
  --query 'KeyMaterial' \
  --output text > sensorvision-key.pem

chmod 400 sensorvision-key.pem

# Launch EC2 instance (Amazon Linux 2023)
aws ec2 run-instances \
  --image-id ami-0c55b159cbfafe1f0 \
  --instance-type t3.medium \
  --key-name sensorvision-key \
  --security-group-ids $WEB_SG_ID \
  --subnet-id subnet-xxxxx \
  --associate-public-ip-address \
  --user-data file://ec2-user-data.sh \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=sensorvision-app}]' \
  --block-device-mappings '[{"DeviceName":"/dev/xvda","Ebs":{"VolumeSize":30,"VolumeType":"gp3"}}]'

# Get instance ID
export INSTANCE_ID="i-xxxxx"

# Wait for instance to be running
aws ec2 wait instance-running --instance-ids $INSTANCE_ID

# Get public IP
aws ec2 describe-instances \
  --instance-ids $INSTANCE_ID \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

### Step 5: Configure EC2 Instance

Create `ec2-user-data.sh`:

```bash
#!/bin/bash

# Update system
yum update -y

# Install Docker
yum install -y docker
systemctl start docker
systemctl enable docker

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/latest/download/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Add ec2-user to docker group
usermod -a -G docker ec2-user

# Install Git
yum install -y git

# Install Java 17
yum install -y java-17-amazon-corretto

# Create application directory
mkdir -p /opt/sensorvision
cd /opt/sensorvision

# Clone repository (or copy files)
# git clone https://github.com/yourusername/sensorvision.git .

# Set environment variables
cat > /etc/environment <<EOF
SPRING_DATASOURCE_URL=jdbc:postgresql://YOUR_RDS_ENDPOINT:5432/sensorvision
SPRING_DATASOURCE_USERNAME=sensoradmin
SPRING_DATASOURCE_PASSWORD=YOUR_SECURE_PASSWORD
MQTT_BROKER_URL=tcp://localhost:1883
EOF

echo "EC2 instance setup complete"
```

### Step 6: Deploy Application

SSH into the EC2 instance:

```bash
ssh -i sensorvision-key.pem ec2-user@YOUR_PUBLIC_IP
```

On the EC2 instance:

```bash
cd /opt/sensorvision

# Create production docker-compose file
cat > docker-compose.prod.yml <<EOF
version: '3.8'

services:
  mosquitto:
    image: eclipse-mosquitto:2.0
    container_name: sensorvision-mosquitto
    ports:
      - "1883:1883"
      - "9001:9001"
    volumes:
      - ./mosquitto/config:/mosquitto/config
      - mosquitto-data:/mosquitto/data
      - mosquitto-logs:/mosquitto/log
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "mosquitto_sub", "-t", "$$SYS/#", "-C", "1", "-i", "healthcheck", "-W", "3"]
      interval: 30s
      timeout: 10s
      retries: 3

  app:
    build:
      context: .
      dockerfile: Dockerfile
    container_name: sensorvision-app
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - SPRING_DATASOURCE_URL=\${SPRING_DATASOURCE_URL}
      - SPRING_DATASOURCE_USERNAME=\${SPRING_DATASOURCE_USERNAME}
      - SPRING_DATASOURCE_PASSWORD=\${SPRING_DATASOURCE_PASSWORD}
      - MQTT_BROKER_URL=tcp://mosquitto:1883
    depends_on:
      mosquitto:
        condition: service_healthy
    restart: unless-stopped
    healthcheck:
      test: ["CMD", "curl", "-f", "http://localhost:8080/actuator/health"]
      interval: 30s
      timeout: 10s
      retries: 3

  nginx:
    image: nginx:alpine
    container_name: sensorvision-nginx
    ports:
      - "80:80"
      - "443:443"
    volumes:
      - ./nginx/nginx.conf:/etc/nginx/nginx.conf:ro
      - ./frontend/dist:/usr/share/nginx/html:ro
      - nginx-logs:/var/log/nginx
    depends_on:
      - app
    restart: unless-stopped

volumes:
  mosquitto-data:
  mosquitto-logs:
  nginx-logs:
EOF

# Build and start containers
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f
```

### Step 7: Configure Nginx

Create `nginx/nginx.conf`:

```nginx
events {
    worker_connections 1024;
}

http {
    include /etc/nginx/mime.types;
    default_type application/octet-stream;

    # Logging
    access_log /var/log/nginx/access.log;
    error_log /var/log/nginx/error.log;

    # Gzip compression
    gzip on;
    gzip_vary on;
    gzip_min_length 1024;
    gzip_types text/plain text/css application/json application/javascript text/xml application/xml text/javascript;

    # Upstream backend
    upstream backend {
        server app:8080;
    }

    server {
        listen 80;
        server_name _;

        # Frontend
        location / {
            root /usr/share/nginx/html;
            try_files $uri $uri/ /index.html;
        }

        # Backend API
        location /api/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header X-Forwarded-Proto $scheme;
        }

        # WebSocket for real-time updates
        location /ws/ {
            proxy_pass http://backend;
            proxy_http_version 1.1;
            proxy_set_header Upgrade $http_upgrade;
            proxy_set_header Connection "upgrade";
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_connect_timeout 7d;
            proxy_send_timeout 7d;
            proxy_read_timeout 7d;
        }

        # Actuator endpoints
        location /actuator/ {
            proxy_pass http://backend;
            proxy_set_header Host $host;
            proxy_set_header X-Real-IP $remote_addr;
        }

        # Health check
        location /health {
            access_log off;
            return 200 "healthy\n";
            add_header Content-Type text/plain;
        }
    }
}
```

---

## Option 2: ECS (Elastic Container Service)

### Overview
ECS provides managed container orchestration for larger deployments.

### Step 1: Create ECS Cluster

```bash
aws ecs create-cluster \
  --cluster-name sensorvision-cluster \
  --capacity-providers FARGATE FARGATE_SPOT \
  --default-capacity-provider-strategy \
    capacityProvider=FARGATE,weight=1,base=1 \
    capacityProvider=FARGATE_SPOT,weight=1
```

### Step 2: Create Task Definitions

See `ecs-task-definition.json` in the deployment scripts section.

### Step 3: Create ECS Service

```bash
aws ecs create-service \
  --cluster sensorvision-cluster \
  --service-name sensorvision-app \
  --task-definition sensorvision-task \
  --desired-count 2 \
  --launch-type FARGATE \
  --network-configuration "awsvpcConfiguration={subnets=[subnet-xxxxx],securityGroups=[sg-xxxxx],assignPublicIp=ENABLED}" \
  --load-balancers targetGroupArn=arn:aws:elasticloadbalancing:...,containerName=app,containerPort=8080
```

---

## Option 3: Elastic Beanstalk

### Quick Deployment

```bash
# Install EB CLI
pip install awsebcli

# Initialize Elastic Beanstalk
eb init -p docker sensorvision-app --region us-east-1

# Create environment
eb create sensorvision-prod \
  --instance-type t3.medium \
  --database.engine postgres \
  --database.username sensoradmin

# Deploy
eb deploy

# Open in browser
eb open
```

---

## Database Setup (RDS)

### Configuration Recommendations

**Instance Class Selection:**
- **Development**: db.t3.micro (1 vCPU, 1GB RAM) - $13/month
- **Small Production**: db.t3.small (2 vCPU, 2GB RAM) - $26/month
- **Medium Production**: db.t3.medium (2 vCPU, 4GB RAM) - $52/month
- **Large Production**: db.r6g.large (2 vCPU, 16GB RAM) - $175/month

**Storage:**
- Use gp3 storage type (better performance/cost)
- Start with 20GB, enable autoscaling up to 100GB
- Enable automated backups (7-35 day retention)

**High Availability:**
- Enable Multi-AZ for production
- Use read replicas for heavy read workloads

### Connection String

```
jdbc:postgresql://sensorvision-db.xxxxx.us-east-1.rds.amazonaws.com:5432/sensorvision
```

---

## Storage and Networking

### S3 Buckets

Create S3 buckets for:

```bash
# Application backups
aws s3 mb s3://sensorvision-backups

# Static assets
aws s3 mb s3://sensorvision-static

# Application logs
aws s3 mb s3://sensorvision-logs

# Set lifecycle policies
aws s3api put-bucket-lifecycle-configuration \
  --bucket sensorvision-logs \
  --lifecycle-configuration file://s3-lifecycle-policy.json
```

### CloudFront (Optional)

For global content delivery:

```bash
aws cloudfront create-distribution \
  --origin-domain-name sensorvision-static.s3.amazonaws.com \
  --default-root-object index.html
```

---

## Security Configuration

### SSL/TLS Certificate

Using AWS Certificate Manager (ACM):

```bash
# Request certificate
aws acm request-certificate \
  --domain-name sensorvision.yourdomain.com \
  --validation-method DNS \
  --region us-east-1

# Get certificate ARN
export CERT_ARN="arn:aws:acm:us-east-1:..."

# Update ALB to use HTTPS
aws elbv2 create-listener \
  --load-balancer-arn $ALB_ARN \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=$CERT_ARN \
  --default-actions Type=forward,TargetGroupArn=$TARGET_GROUP_ARN
```

### Secrets Manager

Store sensitive configuration:

```bash
aws secretsmanager create-secret \
  --name sensorvision/prod/database \
  --secret-string '{
    "username":"sensoradmin",
    "password":"YOUR_SECURE_PASSWORD",
    "host":"sensorvision-db.xxxxx.rds.amazonaws.com",
    "port":"5432",
    "dbname":"sensorvision"
  }'
```

Update application to retrieve secrets:

```java
// Add AWS SDK dependency and retrieve secrets at startup
// See application-prod.properties for configuration
```

---

## Monitoring and Logging

### CloudWatch Configuration

```bash
# Create log group
aws logs create-log-group --log-group-name /aws/sensorvision/app

# Create metric alarm
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --evaluation-periods 2 \
  --alarm-actions arn:aws:sns:us-east-1:123456789012:sensorvision-alerts
```

### CloudWatch Dashboard

Create custom dashboard for monitoring:
- CPU and memory usage
- Database connections
- API response times
- MQTT message rates
- Error rates

---

## Cost Optimization

### Monthly Cost Estimate

**Minimal Setup (Single Instance):**
- EC2 t3.medium: ~$30/month
- RDS db.t3.micro (Single-AZ): ~$13/month
- EBS Storage (50GB): ~$5/month
- Data Transfer: ~$5-10/month
- **Total: ~$53-58/month**

**Production Setup (HA):**
- 2x EC2 t3.medium: ~$60/month
- ALB: ~$20/month
- RDS db.t3.small (Multi-AZ): ~$52/month
- EBS Storage (100GB): ~$10/month
- S3 + CloudWatch: ~$5-10/month
- Data Transfer: ~$10-20/month
- **Total: ~$157-172/month**

### Cost Optimization Tips

1. **Use Reserved Instances**: Save up to 75% on EC2/RDS
2. **Right-size instances**: Monitor and adjust based on usage
3. **Use Spot Instances**: For non-critical workloads (ECS)
4. **Enable S3 Lifecycle Policies**: Archive old logs to Glacier
5. **Use CloudWatch Alarms**: Prevent runaway costs
6. **Schedule non-production**: Stop dev/test instances during off-hours

---

## Deployment Scripts

See the following files for automated deployment:

- `deploy-to-aws.sh`: Main deployment script
- `docker-compose.prod.yml`: Production Docker Compose configuration
- `Dockerfile.prod`: Production-optimized Docker image
- `nginx.conf`: Nginx configuration
- `application-prod.properties`: Production Spring Boot configuration

---

## Troubleshooting

### Common Issues

**Cannot connect to RDS:**
- Check security group allows connection from EC2
- Verify RDS is in same VPC
- Check connection string and credentials

**Application won't start:**
- Check CloudWatch logs: `aws logs tail /aws/sensorvision/app --follow`
- Verify environment variables are set
- Check disk space: `df -h`

**High costs:**
- Review AWS Cost Explorer
- Check for idle resources
- Enable AWS Budget alerts

---

## Next Steps

After deployment:

1. ✅ Configure DNS with Route 53
2. ✅ Set up SSL certificate
3. ✅ Configure automated backups
4. ✅ Set up CloudWatch alarms
5. ✅ Configure CI/CD pipeline
6. ✅ Implement auto-scaling
7. ✅ Set up disaster recovery plan
8. ✅ Configure monitoring dashboards

---

**Document Version:** 1.0
**Last Updated:** January 2025
**Compatibility:** AWS, SensorVision v1.0+

For questions or support, refer to the main README.md or SENSOR_INTEGRATION_GUIDE.md.
