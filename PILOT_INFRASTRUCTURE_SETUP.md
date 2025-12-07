# SensorVision Pilot Infrastructure Setup

## AWS Infrastructure Requirements

### 1. VPC and Networking
```bash
# Create dedicated VPC for pilot
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=sensorvision-pilot-vpc}]'

# Create public and private subnets
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.1.0/24 --availability-zone us-west-2a
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.2.0/24 --availability-zone us-west-2b
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.10.0/24 --availability-zone us-west-2a
aws ec2 create-subnet --vpc-id vpc-xxx --cidr-block 10.0.11.0/24 --availability-zone us-west-2b
```

### 2. RDS PostgreSQL Setup
```bash
# Create RDS instance for pilot
aws rds create-db-instance \
  --db-instance-identifier sensorvision-pilot-db \
  --db-instance-class db.t3.medium \
  --engine postgres \
  --engine-version 15.5 \
  --allocated-storage 100 \
  --storage-encrypted \
  --master-username sensorvision \
  --master-user-password [SECURE_PASSWORD] \
  --vpc-security-group-ids sg-xxx \
  --db-subnet-group-name sensorvision-pilot-subnet-group \
  --backup-retention-period 7 \
  --multi-az \
  --storage-type gp3
```

### 3. Application Load Balancer
```bash
# Create ALB with SSL certificate
aws elbv2 create-load-balancer \
  --name sensorvision-pilot-alb \
  --subnets subnet-xxx subnet-yyy \
  --security-groups sg-xxx \
  --scheme internet-facing \
  --type application
```

### 4. SSL Certificate
```bash
# Request SSL certificate from ACM
aws acm request-certificate \
  --domain-name pilot.sensorvision.io \
  --subject-alternative-names *.pilot.sensorvision.io \
  --validation-method DNS
```

## Environment Configuration

### Production Environment Variables
```bash
# Database
DB_URL=jdbc:postgresql://sensorvision-pilot-db.xxx.us-west-2.rds.amazonaws.com:5432/sensorvision
DB_USERNAME=sensorvision
DB_PASSWORD=[FROM_SECRETS_MANAGER]

# Security
JWT_SECRET=[GENERATED_256_BIT_KEY]
JWT_EXPIRATION_MS=28800000  # 8 hours for pilot

# Email (AWS SES)
SMTP_HOST=email-smtp.us-west-2.amazonaws.com
SMTP_PORT=587
SMTP_USERNAME=[FROM_SECRETS_MANAGER]
SMTP_PASSWORD=[FROM_SECRETS_MANAGER]
EMAIL_FROM=noreply@pilot.sensorvision.io

# SMS (Twilio)
TWILIO_ACCOUNT_SID=[FROM_SECRETS_MANAGER]
TWILIO_AUTH_TOKEN=[FROM_SECRETS_MANAGER]
SMS_FROM_NUMBER=[PILOT_PHONE_NUMBER]

# Application
APP_BASE_URL=https://pilot.sensorvision.io
SPRING_PROFILES_ACTIVE=prod
```

## Monitoring Setup

### CloudWatch Alarms
```bash
# High CPU utilization
aws cloudwatch put-metric-alarm \
  --alarm-name "SensorVision-High-CPU" \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold

# High memory utilization
aws cloudwatch put-metric-alarm \
  --alarm-name "SensorVision-High-Memory" \
  --alarm-description "Alert when memory exceeds 85%" \
  --metric-name MemoryUtilization \
  --namespace AWS/ECS \
  --statistic Average \
  --period 300 \
  --threshold 85 \
  --comparison-operator GreaterThanThreshold

# Database connections
aws cloudwatch put-metric-alarm \
  --alarm-name "SensorVision-DB-Connections" \
  --alarm-description "Alert when DB connections exceed 80%" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold
```

## Backup Strategy

### Database Backups
- Automated daily backups with 7-day retention
- Point-in-time recovery enabled
- Cross-region backup replication for disaster recovery

### Application Backups
- ECS task definitions versioned
- Docker images tagged and stored in ECR
- Configuration files in version control

## Scaling Configuration

### Auto Scaling
```yaml
# ECS Service Auto Scaling
MinCapacity: 2
MaxCapacity: 10
TargetCPUUtilization: 70%
TargetMemoryUtilization: 80%
ScaleOutCooldown: 300s
ScaleInCooldown: 300s
```

### Database Scaling
- Read replicas for reporting queries
- Connection pooling configured
- Query performance monitoring enabled