# SensorVision Deployment Documentation

This document tracks all deployment activities, improvements, and issues for the SensorVision platform.

## Production Environment

- **URL**: http://35.88.65.186.nip.io:8080/
- **Backend API**: http://35.88.65.186:8080/api/v1/
- **Health Check**: http://35.88.65.186:8080/actuator/health
- **Infrastructure**: AWS EC2 (us-west-2)
- **Container Registry**: Amazon ECR
- **Database**: PostgreSQL
- **Message Broker**: Mosquitto (MQTT)

## Deployment History

### 2025-10-26 - WebSocket & API Improvements + Toast Notifications

**PR**: [#18](https://github.com/CodeFleck/sensorvision/pull/18)
**Commit**: 34d57798
**Deployed**: 2025-10-26 22:50 UTC
**Status**: ✅ SUCCESS
**Duration**: 3m27s (Build: 2m22s, Deploy: 1m5s)

#### Changes Deployed
1. **WebSocket Connection Improvements**
   - Fixed React Strict Mode race condition
   - Reduced console noise by suppressing expected closure logs
   - Improved error handling to only log errors when socket is not already closed
   - Files: `frontend/src/contexts/WebSocketContext.tsx`

2. **Backend API Type Mismatch Fix**
   - Fixed `InvalidDataAccessApiUsageException` in issue submission endpoint
   - Changed `LocalDateTime` to `Instant` in `IssueSubmissionRepository` and `IssueSubmissionService`
   - Aligned with `AuditableEntity` timestamp types
   - Files:
     - `src/main/java/org/sensorvision/repository/IssueSubmissionRepository.java`
     - `src/main/java/org/sensorvision/service/IssueSubmissionService.java`

3. **User Experience Enhancement**
   - Installed `react-hot-toast` library
   - Replaced browser `alert()` with modern toast notifications
   - Configured global Toaster component with custom styling
   - Success toasts appear in top-right corner with 5s duration
   - Files:
     - `frontend/package.json` (added dependency)
     - `frontend/src/App.tsx` (Toaster configuration)
     - `frontend/src/components/SubmitIssueModal.tsx` (toast implementation)

#### Deployment Process
- **Build**: Docker image built and pushed to ECR
- **Security Scan**: Trivy scan completed (no blocking vulnerabilities)
- **Deployment**: SSH deployment to EC2 instance
- **Verification**: Health check passed successfully

#### Post-Deployment Verification
- ✅ Backend health check: `{"status":"UP"}`
- ✅ Application containers running
- ✅ Database connectivity verified
- ✅ MQTT broker operational
- ✅ WebSocket connections stable
- ✅ Issue submission working without errors
- ✅ Toast notifications displaying correctly

#### Impact
- **Users**: Enhanced UX with modern toast notifications
- **Stability**: Resolved WebSocket console errors in development
- **Reliability**: Fixed 500 error on issue submission endpoint
- **Performance**: No performance impact

---

## Deployment Process

### Automated CI/CD Pipeline
All deployments to production are handled automatically via GitHub Actions when code is merged to the `main` branch.

**Workflow**: `.github/workflows/deploy-production.yml`

**Steps**:
1. **Build Phase**
   - Checkout code
   - Build Docker image
   - Push to Amazon ECR
   - Security scan with Trivy

2. **Deploy Phase**
   - SSH to EC2 instance
   - Copy deployment files
   - Create environment configuration
   - Run deployment script
   - Verify application health

### Manual Deployment (if needed)
```bash
# 1. SSH to production server
ssh -i ~/.ssh/deploy_key ec2-user@35.88.65.186

# 2. Navigate to application directory
cd /home/ec2-user/sensorvision

# 3. Pull latest changes
git pull origin main

# 4. Run deployment script
./deploy.sh

# 5. Verify deployment
docker-compose -f docker-compose.production.yml ps
curl http://localhost:8080/actuator/health
```

## Rollback Procedure

If a deployment fails or causes issues:

```bash
# 1. SSH to production server
ssh -i ~/.ssh/deploy_key ec2-user@35.88.65.186

# 2. Navigate to application directory
cd /home/ec2-user/sensorvision

# 3. Checkout previous stable commit
git checkout <previous-commit-hash>

# 4. Redeploy
./deploy.sh

# 5. Verify
curl http://localhost:8080/actuator/health
```

## Monitoring

### Health Checks
- **Application**: http://35.88.65.186:8080/actuator/health
- **Liveness**: http://35.88.65.186:8080/actuator/health/liveness
- **Readiness**: http://35.88.65.186:8080/actuator/health/readiness

### Logs
```bash
# Backend logs
docker-compose -f docker-compose.production.yml logs -f backend

# PostgreSQL logs
docker-compose -f docker-compose.production.yml logs -f postgres

# MQTT logs
docker-compose -f docker-compose.production.yml logs -f mosquitto

# All services
docker-compose -f docker-compose.production.yml logs -f
```

## Known Issues

None currently. All systems operational.

## Future Improvements

- [ ] Set up automated performance testing after deployments
- [ ] Implement blue-green deployment strategy
- [ ] Add production metrics dashboard
- [ ] Configure automated backups for PostgreSQL
- [ ] Set up alerting for critical failures

---

**Last Updated**: 2025-10-26
**Next Scheduled Deployment**: As needed (continuous deployment enabled)
