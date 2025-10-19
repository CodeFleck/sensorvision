# AWS Deployment Guide for SensorVision

This guide provides step-by-step instructions for deploying SensorVision to AWS using a hybrid architecture (RDS + EC2 containers) optimized for POC deployment.

## Architecture Overview

```
┌───────────────────────────────────────────────────────────────────┐
│                          AWS Cloud                                 │
│                                                                     │
│  ┌──────────────┐      ┌──────────────────────────────────┐      │
│  │  Route 53    │      │        EC2 Instance              │      │
│  │   (DNS)      │──────│  ┌────────┐    ┌─────────┐      │      │
│  └──────────────┘      │  │Backend │    │Mosquitto│      │      │
│         │              │  │ :8080  │    │  :1883  │      │      │
│         ▼              │  └────────┘    └─────────┘      │      │
│  ┌──────────────┐      │  ┌────────┐                     │      │
│  │   Nginx      │      │  │ Nginx  │                     │      │
│  │(Let's Encrypt│──────│  │  :443  │                     │      │
│  │  or ALB)     │      │  └────────┘                     │      │
│  └──────────────┘      └──────────────────────────────────┘      │
│                                   │                                │
│                                   ▼                                │
│                       ┌────────────────────┐                      │
│                       │  RDS PostgreSQL    │                      │
│                       │  (db.t4g.micro)    │                      │
│                       │   Multi-AZ: No     │                      │
│                       └────────────────────┘                      │
│                                   │                                │
│                                   ▼                                │
│                       ┌────────────────────┐                      │
│                       │  ECR Repository    │                      │
│                       │  Docker Images     │                      │
│                       └────────────────────┘                      │
│                                                                     │
│  ┌──────────────────────────────────────────────────────────┐    │
│  │           CloudWatch Monitoring                           │    │
│  │  - Logs   - Metrics   - Alarms   - Dashboards            │    │
│  └──────────────────────────────────────────────────────────┘    │
└───────────────────────────────────────────────────────────────────┘
```

**Components**:
- EC2 t3.medium: Application server with Docker Compose
- RDS db.t4g.micro: Managed PostgreSQL database
- ECR: Docker image registry
- Nginx: SSL termination and reverse proxy
- CloudWatch: Monitoring and logging

**Estimated Monthly Cost**: $50-80 USD
- EC2 t3.medium: ~$30/month
- RDS db.t4g.micro: ~$15/month
- ECR storage: ~$1/month
- Data transfer: ~$5-10/month
- CloudWatch logs: ~$5/month

---

## Prerequisites

Before starting, ensure you have:

- [ ] AWS Account with admin access
- [ ] AWS CLI installed and configured (`aws configure`)
- [ ] Domain name (optional but recommended for production)
- [ ] SSH key pair for EC2 access
- [ ] Git repository with SensorVision code
- [ ] Basic understanding of Docker and PostgreSQL

---

## Step 1: Configure AWS CLI

```bash
# Install AWS CLI (if not installed)
# Windows: Download from https://aws.amazon.com/cli/
# macOS: brew install awscli
# Linux: sudo apt-get install awscli

# Configure credentials
aws configure
# Enter:
# - AWS Access Key ID
# - AWS Secret Access Key
# - Default region (e.g., us-east-1)
# - Default output format (json)

# Verify configuration
aws sts get-caller-identity
```

---

## Step 2: Create VPC and Network Infrastructure

### Option A: Use Default VPC (Recommended for POC)

```bash
# Check if default VPC exists
aws ec2 describe-vpcs --filters "Name=isDefault,Values=true"

# Note the VPC ID and use it in subsequent steps
```

### Option B: Create Custom VPC (Production-Ready)

```bash
# Create VPC
aws ec2 create-vpc --cidr-block 10.0.0.0/16 --tag-specifications 'ResourceType=vpc,Tags=[{Key=Name,Value=sensorvision-vpc}]'

# Create subnets in different AZs
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.1.0/24 --availability-zone us-east-1a --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=sensorvision-subnet-1a}]'
aws ec2 create-subnet --vpc-id vpc-xxxxx --cidr-block 10.0.2.0/24 --availability-zone us-east-1b --tag-specifications 'ResourceType=subnet,Tags=[{Key=Name,Value=sensorvision-subnet-1b}]'

# Create and attach Internet Gateway
aws ec2 create-internet-gateway --tag-specifications 'ResourceType=internet-gateway,Tags=[{Key=Name,Value=sensorvision-igw}]'
aws ec2 attach-internet-gateway --vpc-id vpc-xxxxx --internet-gateway-id igw-xxxxx

# Update route table
aws ec2 create-route --route-table-id rtb-xxxxx --destination-cidr-block 0.0.0.0/0 --gateway-id igw-xxxxx
```

---

## Step 3: Create Security Groups

### RDS Security Group

```bash
# Create RDS security group
aws ec2 create-security-group \
  --group-name sensorvision-rds-sg \
  --description "Security group for SensorVision RDS" \
  --vpc-id vpc-xxxxx

# Allow PostgreSQL from EC2 security group
aws ec2 authorize-security-group-ingress \
  --group-id sg-rds-xxxxx \
  --protocol tcp \
  --port 5432 \
  --source-group sg-ec2-xxxxx
```

### EC2 Security Group

```bash
# Create EC2 security group
aws ec2 create-security-group \
  --group-name sensorvision-ec2-sg \
  --description "Security group for SensorVision EC2" \
  --vpc-id vpc-xxxxx

# SSH access (restrict to your IP)
aws ec2 authorize-security-group-ingress \
  --group-id sg-ec2-xxxxx \
  --protocol tcp \
  --port 22 \
  --cidr YOUR_IP_ADDRESS/32

# HTTP access
aws ec2 authorize-security-group-ingress \
  --group-id sg-ec2-xxxxx \
  --protocol tcp \
  --port 80 \
  --cidr 0.0.0.0/0

# HTTPS access
aws ec2 authorize-security-group-ingress \
  --group-id sg-ec2-xxxxx \
  --protocol tcp \
  --port 443 \
  --cidr 0.0.0.0/0

# MQTT access (1883 - restrict if not needed publicly)
aws ec2 authorize-security-group-ingress \
  --group-id sg-ec2-xxxxx \
  --protocol tcp \
  --port 1883 \
  --cidr 0.0.0.0/0

# Application port (for health checks)
aws ec2 authorize-security-group-ingress \
  --group-id sg-ec2-xxxxx \
  --protocol tcp \
  --port 8080 \
  --cidr 0.0.0.0/0
```

---

## Step 4: Create RDS PostgreSQL Instance

```bash
# Create DB subnet group (required for multi-AZ or custom VPC)
aws rds create-db-subnet-group \
  --db-subnet-group-name sensorvision-db-subnet-group \
  --db-subnet-group-description "Subnet group for SensorVision RDS" \
  --subnet-ids subnet-xxxxx subnet-yyyyy

# Create RDS instance
aws rds create-db-instance \
  --db-instance-identifier sensorvision-db \
  --db-instance-class db.t4g.micro \
  --engine postgres \
  --engine-version 15.4 \
  --master-username sensorvision_admin \
  --master-user-password 'CHANGE_ME_STRONG_PASSWORD' \
  --allocated-storage 20 \
  --storage-type gp3 \
  --vpc-security-group-ids sg-rds-xxxxx \
  --db-subnet-group-name sensorvision-db-subnet-group \
  --db-name sensorvision \
  --backup-retention-period 7 \
  --preferred-backup-window "03:00-04:00" \
  --preferred-maintenance-window "mon:04:00-mon:05:00" \
  --storage-encrypted \
  --enable-cloudwatch-logs-exports postgresql \
  --tags Key=Name,Value=sensorvision-production-db

# Wait for RDS to be available (takes 5-10 minutes)
aws rds wait db-instance-available --db-instance-identifier sensorvision-db

# Get RDS endpoint
aws rds describe-db-instances \
  --db-instance-identifier sensorvision-db \
  --query 'DBInstances[0].Endpoint.Address' \
  --output text
```

**Save the RDS endpoint** - you'll need it for configuration:
```
Example: sensorvision-db.c9x7x7x7x7x7.us-east-1.rds.amazonaws.com
```

---

## Step 5: Create ECR Repository

```bash
# Create ECR repository
aws ecr create-repository \
  --repository-name sensorvision-backend \
  --image-scanning-configuration scanOnPush=true \
  --encryption-configuration encryptionType=AES256

# Get repository URI
aws ecr describe-repositories \
  --repository-names sensorvision-backend \
  --query 'repositories[0].repositoryUri' \
  --output text

# Save this URI - you'll need it: 123456789012.dkr.ecr.us-east-1.amazonaws.com/sensorvision-backend
```

---

## Step 6: Create IAM Role for EC2

```bash
# Create trust policy file
cat > ec2-trust-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": {
        "Service": "ec2.amazonaws.com"
      },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create IAM role
aws iam create-role \
  --role-name sensorvision-ec2-role \
  --assume-role-policy-document file://ec2-trust-policy.json

# Create policy for ECR access
cat > ecr-policy.json << EOF
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ecr:GetAuthorizationToken",
        "ecr:BatchCheckLayerAvailability",
        "ecr:GetDownloadUrlForLayer",
        "ecr:BatchGetImage"
      ],
      "Resource": "*"
    },
    {
      "Effect": "Allow",
      "Action": [
        "logs:CreateLogGroup",
        "logs:CreateLogStream",
        "logs:PutLogEvents",
        "logs:DescribeLogStreams"
      ],
      "Resource": "arn:aws:logs:*:*:*"
    }
  ]
}
EOF

# Attach policy to role
aws iam put-role-policy \
  --role-name sensorvision-ec2-role \
  --policy-name sensorvision-ec2-policy \
  --policy-document file://ecr-policy.json

# Create instance profile
aws iam create-instance-profile \
  --instance-profile-name sensorvision-ec2-instance-profile

# Add role to instance profile
aws iam add-role-to-instance-profile \
  --instance-profile-name sensorvision-ec2-instance-profile \
  --role-name sensorvision-ec2-role
```

---

## Step 7: Launch EC2 Instance

```bash
# Create or import SSH key pair
aws ec2 create-key-pair \
  --key-name sensorvision-key \
  --query 'KeyMaterial' \
  --output text > sensorvision-key.pem

# Set permissions
chmod 400 sensorvision-key.pem

# Create user data script
cat > user-data.sh << 'EOF'
#!/bin/bash
set -e

# Update system
apt-get update -y
apt-get upgrade -y

# Install Docker
curl -fsSL https://get.docker.com -o get-docker.sh
sh get-docker.sh

# Install Docker Compose
curl -L "https://github.com/docker/compose/releases/download/v2.24.0/docker-compose-$(uname -s)-$(uname -m)" -o /usr/local/bin/docker-compose
chmod +x /usr/local/bin/docker-compose

# Install AWS CLI
apt-get install -y awscli

# Create application directory
mkdir -p /home/ubuntu/sensorvision
chown ubuntu:ubuntu /home/ubuntu/sensorvision

# Install CloudWatch agent (optional but recommended)
wget https://s3.amazonaws.com/amazoncloudwatch-agent/ubuntu/amd64/latest/amazon-cloudwatch-agent.deb
dpkg -i -E ./amazon-cloudwatch-agent.deb

# Add ubuntu user to docker group
usermod -aG docker ubuntu

# Install certbot for Let's Encrypt (if using nginx)
apt-get install -y certbot python3-certbot-nginx

echo "EC2 initialization complete!"
EOF

# Launch EC2 instance (Ubuntu 22.04 LTS)
aws ec2 run-instances \
  --image-id ami-0c7217cdde317cfec \
  --instance-type t3.medium \
  --key-name sensorvision-key \
  --security-group-ids sg-ec2-xxxxx \
  --subnet-id subnet-xxxxx \
  --iam-instance-profile Name=sensorvision-ec2-instance-profile \
  --block-device-mappings 'DeviceName=/dev/sda1,Ebs={VolumeSize=30,VolumeType=gp3}' \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=sensorvision-production}]' \
  --user-data file://user-data.sh

# Get instance public IP
aws ec2 describe-instances \
  --filters "Name=tag:Name,Values=sensorvision-production" \
  --query 'Reservations[0].Instances[0].PublicIpAddress' \
  --output text
```

---

## Step 8: Configure GitHub Secrets

Go to your GitHub repository → Settings → Secrets and variables → Actions → New repository secret

Add the following secrets:

```
AWS_ACCESS_KEY_ID=your-access-key-id
AWS_SECRET_ACCESS_KEY=your-secret-access-key
AWS_REGION=us-east-1
EC2_HOST=your-ec2-public-ip
EC2_USER=ubuntu
EC2_SSH_PRIVATE_KEY=<contents of sensorvision-key.pem>

# Database credentials
DB_URL=jdbc:postgresql://your-rds-endpoint:5432/sensorvision
DB_USERNAME=sensorvision_admin
DB_PASSWORD=your-strong-database-password

# JWT configuration (generate with: openssl rand -hex 64)
JWT_SECRET=your-generated-secret-64-chars-minimum
JWT_EXPIRATION_MS=86400000
JWT_ISSUER=https://your-domain.com

# MQTT credentials
MQTT_USERNAME=sensorvision_mqtt
MQTT_PASSWORD=your-mqtt-password

# Domain configuration
PRODUCTION_DOMAIN=your-domain.com

# Optional: Email configuration
EMAIL_ENABLED=false
EMAIL_FROM=noreply@your-domain.com
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USERNAME=your-email@gmail.com
SMTP_PASSWORD=your-app-password

# Optional: SMS (Twilio)
SMS_ENABLED=false
TWILIO_ACCOUNT_SID=your-sid
TWILIO_AUTH_TOKEN=your-token
SMS_FROM_NUMBER=+1234567890

# Optional: Slack notifications
SLACK_ENABLED=false
SLACK_WEBHOOK_URL=https://hooks.slack.com/services/YOUR/WEBHOOK/URL

# Optional: Teams notifications
TEAMS_ENABLED=false
TEAMS_WEBHOOK_URL=https://outlook.office.com/webhook/YOUR/WEBHOOK/URL

# Support email
SUPPORT_ISSUE_EMAIL=support@your-domain.com
```

---

## Step 9: Initial Deployment

### Manual First Deployment

SSH into your EC2 instance:

```bash
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
```

On the EC2 instance:

```bash
# Clone repository (or copy files)
cd /home/ubuntu/sensorvision
git clone https://github.com/your-username/sensorvision.git .

# Create .env.production from template
cp .env.production.template .env.production

# Edit with your actual values
nano .env.production

# Set file permissions
chmod 600 .env.production

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com
```

### Build and push initial image (from your local machine):

```bash
# On your LOCAL machine:
docker build -t sensorvision-backend:latest .
docker tag sensorvision-backend:latest 123456789012.dkr.ecr.us-east-1.amazonaws.com/sensorvision-backend:latest

# Login to ECR
aws ecr get-login-password --region us-east-1 | docker login --username AWS --password-stdin 123456789012.dkr.ecr.us-east-1.amazonaws.com

# Push image
docker push 123456789012.dkr.ecr.us-east-1.amazonaws.com/sensorvision-backend:latest
```

### Deploy on EC2:

```bash
# Back on EC2, run deployment script
chmod +x deploy.sh
./deploy.sh
```

---

## Step 10: Configure Domain and SSL

### Option A: Using Nginx with Let's Encrypt (Recommended for POC)

On EC2 instance:

```bash
# Update nginx.conf with your domain
sudo nano nginx.conf
# Replace YOUR_DOMAIN_HERE with your actual domain

# Point your domain's DNS A record to EC2 public IP first!

# Get SSL certificate
sudo certbot --nginx -d your-domain.com

# Certbot will automatically configure nginx
# Verify auto-renewal
sudo certbot renew --dry-run
```

### Option B: Using Application Load Balancer (ALB)

```bash
# Create target group
aws elbv2 create-target-group \
  --name sensorvision-tg \
  --protocol HTTP \
  --port 8080 \
  --vpc-id vpc-xxxxx \
  --health-check-path /actuator/health

# Create Application Load Balancer
aws elbv2 create-load-balancer \
  --name sensorvision-alb \
  --subnets subnet-xxxxx subnet-yyyyy \
  --security-groups sg-alb-xxxxx

# Register EC2 instance with target group
aws elbv2 register-targets \
  --target-group-arn arn:aws:... \
  --targets Id=i-xxxxx

# Create HTTPS listener (requires ACM certificate)
aws elbv2 create-listener \
  --load-balancer-arn arn:aws:... \
  --protocol HTTPS \
  --port 443 \
  --certificates CertificateArn=arn:aws:acm:... \
  --default-actions Type=forward,TargetGroupArn=arn:aws:...
```

---

## Step 11: Configure Route 53 (Optional)

```bash
# Create hosted zone (if you don't have one)
aws route53 create-hosted-zone \
  --name your-domain.com \
  --caller-reference $(date +%s)

# Create A record pointing to ALB or EC2
# Get your zone ID first
aws route53 list-hosted-zones

# Create DNS record (example for EC2 Elastic IP)
cat > dns-record.json << EOF
{
  "Changes": [{
    "Action": "CREATE",
    "ResourceRecordSet": {
      "Name": "your-domain.com",
      "Type": "A",
      "TTL": 300,
      "ResourceRecords": [{"Value": "YOUR-EC2-IP"}]
    }
  }]
}
EOF

aws route53 change-resource-record-sets \
  --hosted-zone-id Z123456789 \
  --change-batch file://dns-record.json
```

---

## Step 12: Configure CloudWatch Monitoring

```bash
# Create CloudWatch alarm for EC2 CPU
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-high-cpu \
  --alarm-description "Alert when CPU exceeds 80%" \
  --metric-name CPUUtilization \
  --namespace AWS/EC2 \
  --statistic Average \
  --period 300 \
  --threshold 80 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=InstanceId,Value=i-xxxxx \
  --evaluation-periods 2

# Create alarm for RDS connections
aws cloudwatch put-metric-alarm \
  --alarm-name sensorvision-rds-connections \
  --alarm-description "Alert when RDS connections exceed 80% of max" \
  --metric-name DatabaseConnections \
  --namespace AWS/RDS \
  --statistic Average \
  --period 300 \
  --threshold 16 \
  --comparison-operator GreaterThanThreshold \
  --dimensions Name=DBInstanceIdentifier,Value=sensorvision-db \
  --evaluation-periods 2
```

---

## Step 13: Test Deployment

```bash
# Test health endpoint
curl https://your-domain.com/actuator/health

# Test application
curl https://your-domain.com/api/v1/devices

# Check logs
ssh -i sensorvision-key.pem ubuntu@your-ec2-ip
cd /home/ubuntu/sensorvision
docker-compose -f docker-compose.production.yml logs -f
```

---

## Step 14: Enable Automated Deployments

Once manual deployment works:

1. Push to `main` branch
2. GitHub Actions will automatically:
   - Run tests
   - Build Docker image
   - Push to ECR
   - Deploy to EC2
   - Run health checks

Monitor deployment: https://github.com/your-repo/actions

---

## Troubleshooting

### Cannot connect to RDS
- Check security group rules
- Verify RDS is in the same VPC as EC2
- Test connection: `psql -h rds-endpoint -U sensorvision_admin -d sensorvision`

### Docker image pull fails
- Check IAM role attached to EC2
- Verify ECR permissions
- Test: `aws ecr get-login-password | docker login --username AWS --password-stdin <ecr-url>`

### Application won't start
- Check logs: `docker-compose -f docker-compose.production.yml logs backend`
- Verify .env.production has all required variables
- Check database migrations: `docker-compose -f docker-compose.production.yml exec backend bash`

### SSL certificate issues
- Verify domain DNS points to EC2/ALB
- Check certbot logs: `sudo journalctl -u certbot`
- Manually renew: `sudo certbot renew`

---

## Cost Optimization Tips

1. **Use Reserved Instances**: Save 30-60% if committed for 1-3 years
2. **RDS Snapshot**: Take manual snapshots, delete instance when not needed for testing
3. **CloudWatch Log Retention**: Set to 7 days instead of indefinite
4. **EC2 Instance Scheduler**: Stop instance during non-business hours (dev/test only)
5. **Elastic IP**: Only allocate if needed; unused EIPs cost money

---

## Security Best Practices

- [ ] Enable MFA on AWS root account
- [ ] Use least-privilege IAM policies
- [ ] Rotate credentials regularly
- [ ] Enable RDS encryption at rest
- [ ] Enable VPC Flow Logs
- [ ] Set up AWS Config for compliance
- [ ] Use AWS Secrets Manager for sensitive data (alternative to .env files)
- [ ] Enable CloudTrail for audit logging
- [ ] Restrict SSH access to known IPs
- [ ] Keep Docker images updated

---

## Next Steps

1. Set up automated backups for RDS
2. Configure CloudWatch Alarms with SNS notifications
3. Implement CI/CD pipeline with GitHub Actions (already configured)
4. Set up staging environment for testing
5. Consider migrating to ECS Fargate for better scalability
6. Implement Infrastructure as Code with Terraform

---

## Support

For issues or questions:
- Review logs in CloudWatch
- Check GitHub Actions workflow runs
- Consult `DEPLOYMENT_OPERATIONS.md` for common operations
- Contact AWS Support if infrastructure issues

**Deployment complete!** Your SensorVision application is now running in production on AWS.
