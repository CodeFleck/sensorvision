# Production Deployment Guide

Quick guide for deploying SensorVision to production EC2 server.

## Prerequisites

- SSH access to EC2 instance (35.88.65.186)
- SSH key file (usually `~/.ssh/id_rsa` or `.pem` file)
- Windows machine with SSH client installed

## Option 1: PowerShell Script (Recommended)

The PowerShell script provides detailed output, error handling, and health checks.

### Basic Usage

```powershell
# Using default SSH key location (~/.ssh/id_rsa)
.\deploy-production.ps1

# Using custom SSH key path
.\deploy-production.ps1 -SSHKeyPath "C:\path\to\your-key.pem"

# Specifying different EC2 host
.\deploy-production.ps1 -EC2Host "35.88.65.186" -EC2User "ec2-user"
```

### Full Example

```powershell
# Deploy with custom PEM key
.\deploy-production.ps1 -SSHKeyPath "C:\Users\YourName\Downloads\sensorvision-prod.pem"
```

## Option 2: Batch File

Simple batch script for quick deployments.

### Basic Usage

```batch
REM Using default SSH key location
deploy-production.bat

REM Using custom SSH key path
deploy-production.bat "C:\path\to\your-key.pem"
```

## What Gets Deployed

When you run either script, the following will be deployed:

- ✅ **Integration Wizard** (Phase 6) - Zero-config device setup
- ✅ **Python SDK** (Phase 4) - `sensorvision-sdk`
- ✅ **JavaScript/TypeScript SDK** (Phase 5) - `sensorvision-sdk-js`
- ✅ **WebSocket Fixes** - Subscription replay bug fix
- ✅ **All Bug Fixes** - Latest improvements from main branch

## Deployment Process

The script performs these steps:

1. **Tests SSH Connection** - Verifies access to EC2 server
2. **Checks Current Status** - Shows what's currently deployed
3. **Pulls Latest Code** - Gets latest from `main` branch
4. **Runs Deployment** - Executes `deploy.sh` script
5. **Checks Containers** - Verifies Docker containers are running
6. **Health Checks** - Waits for application to be ready
7. **External Test** - Confirms public accessibility
8. **Summary Report** - Shows deployment results and URLs

**Total Time:** ~3-5 minutes

## After Deployment

### Test the Integration Wizard

1. Open browser: `http://35.88.65.186:3000/integration-wizard`
2. Follow the 5-step wizard
3. Create a test device
4. Verify code generation works
5. Test connection with sample data

### Check Application Health

```powershell
# Backend health
curl http://35.88.65.186:8080/actuator/health

# Should return: {"status":"UP"}
```

### Access Key URLs

- **Frontend Dashboard:** http://35.88.65.186:3000
- **Integration Wizard:** http://35.88.65.186:3000/integration-wizard
- **Backend API:** http://35.88.65.186:8080
- **Health Endpoint:** http://35.88.65.186:8080/actuator/health
- **Prometheus Metrics:** http://35.88.65.186:9090
- **Grafana:** http://35.88.65.186:3001

## Troubleshooting

### SSH Permission Denied

If you get "Permission denied (publickey)":

1. **Check SSH key permissions** (PEM files need restricted access):
   ```powershell
   # PowerShell: Fix permissions
   icacls "C:\path\to\key.pem" /inheritance:r
   icacls "C:\path\to\key.pem" /grant:r "$($env:USERNAME):(R)"
   ```

2. **Verify key format** (should start with `-----BEGIN RSA PRIVATE KEY-----`)

3. **Test connection manually**:
   ```bash
   ssh -i "C:\path\to\key.pem" ec2-user@35.88.65.186
   ```

### Health Check Timeout

If health check times out after deployment:

```powershell
# SSH into server and check logs
ssh -i "C:\path\to\key.pem" ec2-user@35.88.65.186

# View backend logs
cd /home/ec2-user/sensorvision
docker-compose -f docker-compose.production.yml logs backend --tail=100

# Check container status
docker-compose -f docker-compose.production.yml ps

# Restart backend if needed
docker-compose -f docker-compose.production.yml restart backend
```

### Port Not Accessible

If external URLs don't work, check security group:

```bash
# Ensure these ports are open in EC2 security group:
# - 22 (SSH)
# - 8080 (Backend API)
# - 3000 (Frontend)
# - 9090 (Prometheus)
# - 3001 (Grafana)
```

### Docker Issues

```bash
# SSH into server
ssh -i "C:\path\to\key.pem" ec2-user@35.88.65.186
cd /home/ec2-user/sensorvision

# Restart all services
docker-compose -f docker-compose.production.yml restart

# Full rebuild (if needed)
docker-compose -f docker-compose.production.yml down
docker-compose -f docker-compose.production.yml up -d --build

# Check disk space
df -h
```

## Manual Deployment (If Scripts Fail)

If both scripts fail, you can deploy manually:

```bash
# 1. SSH into server
ssh -i "C:\path\to\key.pem" ec2-user@35.88.65.186

# 2. Navigate to project
cd /home/ec2-user/sensorvision

# 3. Pull latest code
git pull origin main

# 4. Run deployment
./deploy.sh

# 5. Check status
docker-compose -f docker-compose.production.yml ps

# 6. Wait for health
curl http://localhost:8080/actuator/health

# 7. Exit
exit
```

## Rollback (If Something Goes Wrong)

```bash
# SSH into server
ssh -i "C:\path\to\key.pem" ec2-user@35.88.65.186
cd /home/ec2-user/sensorvision

# View git history
git log --oneline -10

# Rollback to previous commit
git reset --hard <previous-commit-hash>

# Redeploy
./deploy.sh
```

## Monitoring

### View Live Logs

```bash
# All services
docker-compose -f docker-compose.production.yml logs -f

# Backend only
docker-compose -f docker-compose.production.yml logs -f backend

# Frontend only
docker-compose -f docker-compose.production.yml logs -f frontend

# Last 100 lines
docker-compose -f docker-compose.production.yml logs --tail=100
```

### Check Resource Usage

```bash
# Container stats
docker stats

# Disk usage
docker system df

# Server resources
top
free -h
df -h
```

## Production URLs Summary

| Service | URL | Purpose |
|---------|-----|---------|
| Frontend | http://35.88.65.186:3000 | Main dashboard |
| Integration Wizard | http://35.88.65.186:3000/integration-wizard | Device setup |
| Backend API | http://35.88.65.186:8080 | REST API |
| Health Check | http://35.88.65.186:8080/actuator/health | Status monitoring |
| Prometheus | http://35.88.65.186:9090 | Metrics |
| Grafana | http://35.88.65.186:3001 | Visualization |

## Security Notes

- **Never commit SSH keys** to git repository
- **Restrict PEM file permissions** to current user only
- **Use environment-specific secrets** in `.env.production`
- **Rotate credentials regularly**

## Support

For deployment issues:
1. Check logs first: `docker-compose logs`
2. Verify git status: `git status && git log -3`
3. Check health: `curl http://localhost:8080/actuator/health`
4. Review security group settings in AWS Console

---

**Generated:** 2025-10-24
**Scripts:** `deploy-production.ps1`, `deploy-production.bat`
