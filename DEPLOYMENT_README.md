# SensorVision AWS Deployment - Files Overview

This document explains all the deployment files that have been created for your AWS deployment.

---

## 📚 Documentation Files (READ THESE FIRST!)

### 🌟 **DEPLOYMENT_WALKTHROUGH.md** ⭐ START HERE!
**Purpose**: Complete beginner-friendly step-by-step guide
**Use this if**: You're deploying to AWS for the first time
**Time**: 2-3 hours to complete
**What it covers**:
- Creating AWS account from scratch
- Installing all required tools
- Creating every AWS resource step-by-step
- First deployment
- Setting up SSL
- Testing everything works

### **AWS_DEPLOYMENT.md**
**Purpose**: Technical reference for AWS infrastructure
**Use this if**: You know AWS basics and want quick commands
**What it covers**:
- Architecture diagrams
- AWS CLI commands for all resources
- Security group configurations
- Troubleshooting infrastructure issues

### **DEPLOYMENT_OPERATIONS.md**
**Purpose**: Day-to-day operations guide
**Use this if**: Your app is already deployed and you need to manage it
**What it covers**:
- Checking logs
- Restarting services
- Database operations
- Backups and recovery
- Monitoring
- Cost optimization
- Troubleshooting common issues

---

## 🔧 Configuration Files

### **docker-compose.production.yml**
**Purpose**: Production Docker Compose configuration
**What it does**:
- Defines all containers (backend, MQTT, nginx)
- Connects to RDS instead of local PostgreSQL
- Pulls images from ECR
- Sets up networking between containers

**When to modify**:
- Adding new environment variables
- Changing container resources
- Adding new services

### **.env.production.template**
**Purpose**: Template for production environment variables
**What it does**:
- Shows all required environment variables
- Provides examples and documentation
- Security checklist

**How to use**:
1. Copy to `.env.production` on EC2
2. Fill in actual values (passwords, endpoints, etc.)
3. Keep it secret - never commit to git!

### **application-prod.yml**
**Purpose**: Spring Boot production configuration
**What it does**:
- Optimizes database connections for production
- Disables development features (like simulator)
- Configures logging levels
- Sets security settings

**When to modify**:
- Changing database pool size
- Adjusting rate limits
- Changing log levels

### **nginx.conf**
**Purpose**: Nginx reverse proxy and SSL configuration
**What it does**:
- Terminates SSL/HTTPS connections
- Routes requests to backend
- Handles WebSocket connections
- Rate limiting
- Security headers

**When to modify**:
- Changing domain name
- Adjusting rate limits
- Modifying SSL settings

---

## 🚀 Deployment Scripts

### **deploy.sh**
**Purpose**: Automated deployment script
**What it does**:
1. Checks prerequisites (Docker, AWS CLI, .env file)
2. Logs into ECR
3. Creates backup of current deployment
4. Pulls latest Docker images
5. Checks database connectivity
6. Deploys new version
7. Runs health checks
8. Automatic rollback if deployment fails

**How to use**:
```bash
# Normal deployment
./deploy.sh

# Rollback to previous version
./deploy.sh --rollback

# Just run health check
./deploy.sh --health-check
```

**When to use**:
- Manual deployments
- Rolling back bad deployments
- Testing health checks

---

## 🤖 GitHub Actions Workflows

### **.github/workflows/ci.yml**
**Purpose**: Continuous Integration - runs tests on every push/PR
**What it does**:
1. Backend tests (Spring Boot)
2. Frontend tests (React/TypeScript)
3. Docker build test
4. Security scanning
5. Code coverage reports

**Triggers**:
- Push to `main`, `develop`, or `feature/*` branches
- Pull requests to `main` or `develop`

**Where to see results**:
- GitHub → Actions tab → CI workflow runs

### **.github/workflows/deploy-production.yml**
**Purpose**: Continuous Deployment - deploys to AWS automatically
**What it does**:
1. Builds Docker image
2. Pushes to ECR
3. SSHs to EC2
4. Creates .env.production with GitHub secrets
5. Runs deployment script
6. Verifies health
7. Sends Slack notifications (optional)

**Triggers**:
- Push to `main` branch
- Manual trigger from GitHub UI

**Requirements**:
- All GitHub secrets configured
- EC2 instance running
- SSH key in GitHub secrets

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
