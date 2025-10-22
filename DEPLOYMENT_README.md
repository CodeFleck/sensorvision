# SensorVision Deployment - Complete Documentation

This document provides an overview of all deployment files and documentation created for SensorVision.

---

## 📚 Documentation Files (READ THESE FIRST!)

### 🌟 **SENSOR_INTEGRATION_GUIDE.md** ⭐ FOR END USERS!
**Purpose**: Complete end-user guide for sensor integration
**Use this if**: You need to integrate sensors and set up monitoring
**Time**: 30 minutes to integrate first sensor
**What it covers**:
- Device registration walkthrough
- MQTT and HTTP data ingestion
- Setting up monitoring rules and alerts
- Real-time dashboard usage
- Code examples (Python, JavaScript, Arduino)
- Troubleshooting guide

### **AWS_DEPLOYMENT_GUIDE.md**
**Purpose**: Comprehensive AWS deployment guide
**Use this if**: You're deploying SensorVision to AWS Cloud
**What it covers**:
- Complete architecture diagrams
- Three deployment options (EC2, ECS, Elastic Beanstalk)
- Step-by-step AWS resource creation
- Database setup (RDS)
- Security configuration
- Monitoring and logging
- Cost estimates and optimization

### **SCREENSHOT_GUIDE.md**
**Purpose**: Screenshot capture instructions for documentation
**Use this if**: You need to capture or update UI screenshots
**What it covers**:
- List of all 17 required screenshots
- Specific instructions for each image
- Screenshot specifications
- Tools and setup requirements

---

## 🔧 Configuration Files

### **docker-compose.prod.yml**
**Purpose**: Production Docker Compose configuration
**What it does**:
- Defines all containers (backend, MQTT, nginx, Prometheus, Grafana)
- Configures health checks for all services
- Sets up production logging
- Manages volumes for data persistence
- Optional monitoring stack with profiles

**When to modify**:
- Adding new environment variables
- Changing container resources
- Adding new services
- Adjusting health check parameters

### **Dockerfile.prod**
**Purpose**: Production-optimized multi-stage Docker image
**What it does**:
- Stage 1: Builds Spring Boot application with Gradle
- Stage 2: Builds React frontend with Node.js
- Stage 3: Creates optimized runtime image with Amazon Corretto
- Runs as non-root user for security
- Includes health check configuration

**Features**:
- Multi-stage build for smaller image size
- Optimized JVM parameters for production
- Frontend assets bundled with backend
- Security hardening

### **src/main/resources/application-prod.properties**
**Purpose**: Spring Boot production configuration
**What it does**:
- Optimizes database connection pooling (HikariCP)
- Disables development features (simulator, SQL logging)
- Configures production logging levels
- Sets up Actuator endpoints for monitoring
- Configures MQTT production settings
- Email notification configuration

**When to modify**:
- Changing database pool size
- Adjusting logging levels
- Adding new environment-specific properties
- Configuring OAuth2 providers

### **nginx/nginx.conf**
**Purpose**: Nginx reverse proxy with SSL and caching
**What it does**:
- Terminates SSL/HTTPS connections
- Routes frontend and backend requests
- Handles WebSocket connections for real-time updates
- Implements rate limiting (10 req/s for API, 5 req/m for auth)
- Security headers and gzip compression
- Caches static assets for 1 year

**When to modify**:
- Changing domain name or SSL certificates
- Adjusting rate limits
- Modifying caching policies
- Adding new API routes

---

## 🚀 Deployment Scripts

### **deploy-to-aws.sh**
**Purpose**: Interactive AWS deployment menu system
**What it does**:
- Checks AWS CLI installation and credentials
- Provides deployment options menu:
  1. Deploy to new EC2 instance
  2. Update existing EC2 instance
  3. Deploy using Docker Compose only
  4. Create RDS database
  5. Full AWS infrastructure setup
- Delegates to specific deployment scripts

**How to use**:
```bash
chmod +x deploy-to-aws.sh
./deploy-to-aws.sh
```

### **scripts/deploy-docker-compose.sh**
**Purpose**: Docker Compose deployment automation
**What it does**:
1. Checks prerequisites (Docker, Docker Compose)
2. Creates .env.prod template if needed
3. Builds application with Gradle
4. Builds frontend with npm
5. Builds Docker images
6. Starts containers with health checks
7. Verifies application is running

**How to use**:
```bash
chmod +x scripts/deploy-docker-compose.sh
./deploy-to-aws.sh
# Select option 3
```

**When to use**:
- Local production-like testing
- Standalone server deployment
- Quick deployments without AWS

---

## 📸 Documentation Assets

### **docs/images/*.png** (17 images)
**Purpose**: UI screenshots for SENSOR_INTEGRATION_GUIDE.md
**What's included**:
1. Device Management page
2. Add Device modal (empty and filled)
3. Device list with new device
4. Data Ingestion page and success
5. Rules & Automation page
6. Create Rule modal (empty and filled)
7. Dashboard overview and components
8. Status cards, charts, device cards
9. Alerts page
10. WebSocket connection status

**Status**: Placeholder images generated, ready to be replaced with actual screenshots

**How to update**:
1. Follow SCREENSHOT_GUIDE.md instructions
2. Capture actual screenshots from running application
3. Replace placeholder images in docs/images/
4. Screenshots are already referenced in SENSOR_INTEGRATION_GUIDE.md

###  **capture-screenshots.ps1** & **generate-screenshot-placeholders.ps1**
**Purpose**: PowerShell automation for screenshot capture
**What they do**:
- capture-screenshots.ps1: Opens browser and captures full screenshots
- generate-screenshot-placeholders.ps1: Creates placeholder images with descriptions

**How to use**:
```powershell
# Generate placeholders
powershell -ExecutionPolicy Bypass -File generate-screenshot-placeholders.ps1

# Or capture actual screenshots (requires running app)
powershell -ExecutionPolicy Bypass -File capture-screenshots.ps1
```

---

## 📁 Supporting Files

### **.gitignore** (updated)
**Purpose**: Prevent sensitive files from being committed
**Added entries**:
- `.env.production` - Production secrets
- `*.pem` - SSH keys
- `ssl/` - SSL certificates
- `deploy_key*` - Deployment keys

### **ec2-trust-policy.json** (created during setup)
**Purpose**: IAM trust policy for EC2 role
**Use**: Allows EC2 instances to assume the IAM role

### **ecr-policy.json** (created during setup)
**Purpose**: IAM permissions for ECR access
**Use**: Allows EC2 to pull Docker images from ECR

### **user-data.sh** (created during setup)
**Purpose**: EC2 initialization script
**What it does**:
- Installs Docker and Docker Compose
- Installs AWS CLI
- Creates application directory
- Installs certbot for SSL

---

## 🗺️ Deployment Flow Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    LOCAL DEVELOPMENT                         │
│                                                              │
│  1. Write code                                              │
│  2. Test locally with docker-compose.yml                   │
│  3. Commit and push to GitHub                               │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                   GITHUB ACTIONS                             │
│                                                              │
│  1. Run tests (ci.yml)                                      │
│  2. Build Docker image                                      │
│  3. Push to ECR                                             │
│  4. SSH to EC2 and deploy (deploy-production.yml)          │
└──────────────────────┬───────────────────────────────────────┘
                       │
                       ▼
┌─────────────────────────────────────────────────────────────┐
│                     AWS CLOUD                                │
│                                                              │
│  ┌──────────┐    ┌──────────┐    ┌──────────┐             │
│  │   ECR    │───>│   EC2    │───>│   RDS    │             │
│  │ (Images) │    │(Containers)│   │(Database)│             │
│  └──────────┘    └──────────┘    └──────────┘             │
│                        │                                     │
│                        ▼                                     │
│                  ┌──────────┐                               │
│                  │  Users   │                               │
│                  │(Internet)│                               │
│                  └──────────┘                               │
└─────────────────────────────────────────────────────────────┘
```

---

## 🎯 Quick Start Guide

### First Time Setup (Use DEPLOYMENT_WALKTHROUGH.md)
1. Create AWS account
2. Install AWS CLI and configure
3. Create all AWS resources
4. Configure GitHub secrets
5. Run first deployment manually
6. Enable automated deployments

### Daily Development Workflow
1. Make code changes locally
2. Test with `docker-compose up`
3. Commit and push to GitHub
4. GitHub Actions automatically tests and deploys
5. Verify at https://your-domain.com

### Manual Deployment
1. SSH to EC2: `ssh -i sensorvision-key.pem ubuntu@YOUR_EC2_IP`
2. Navigate: `cd /home/ubuntu/sensorvision`
3. Deploy: `./deploy.sh`
4. Check logs: `docker-compose -f docker-compose.production.yml logs -f`

### Troubleshooting
1. Check DEPLOYMENT_OPERATIONS.md
2. View logs on EC2
3. Check GitHub Actions logs
4. Verify AWS resources in console

---

## 📞 Support Resources

- **Beginner Guide**: DEPLOYMENT_WALKTHROUGH.md
- **Infrastructure Guide**: AWS_DEPLOYMENT.md
- **Operations Guide**: DEPLOYMENT_OPERATIONS.md
- **AWS Console**: https://console.aws.amazon.com/
- **GitHub Actions**: https://github.com/your-repo/actions

---

## 🔐 Security Checklist

Before going live:
- [ ] Changed all default passwords
- [ ] Generated strong JWT secret
- [ ] Enabled MFA on AWS root account
- [ ] Restricted SSH to your IP only
- [ ] Set up SSL certificate
- [ ] Configured database backups
- [ ] Set up CloudWatch alarms
- [ ] Reviewed security group rules
- [ ] `.env.production` has correct permissions (600)
- [ ] All secrets stored in GitHub Secrets, not in code

---

## 💡 Pro Tips

1. **Always test locally first** with `docker-compose up` before deploying
2. **Check GitHub Actions** before manually deploying - it might already be running
3. **Keep backups** - deploy.sh automatically creates them, but verify they exist
4. **Monitor costs** - check AWS billing dashboard weekly
5. **Update regularly** - keep Docker images and system packages updated
6. **Use staging** - consider creating a staging environment for testing

---

## 📊 File Summary

| File | Type | Purpose | Edit Frequency |
|------|------|---------|---------------|
| DEPLOYMENT_WALKTHROUGH.md | Docs | Beginner setup guide | Never |
| AWS_DEPLOYMENT.md | Docs | Infrastructure reference | Rarely |
| DEPLOYMENT_OPERATIONS.md | Docs | Operations guide | Rarely |
| docker-compose.production.yml | Config | Container orchestration | Occasionally |
| .env.production.template | Template | Environment variables | Rarely |
| application-prod.yml | Config | Spring Boot settings | Occasionally |
| nginx.conf | Config | Reverse proxy | Rarely |
| deploy.sh | Script | Deployment automation | Rarely |
| .github/workflows/ci.yml | CI/CD | Testing pipeline | Occasionally |
| .github/workflows/deploy-production.yml | CI/CD | Deployment pipeline | Occasionally |

---

**Remember**: Start with DEPLOYMENT_WALKTHROUGH.md if this is your first time!

Good luck with your deployment! 🚀
