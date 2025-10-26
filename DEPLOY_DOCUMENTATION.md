# SensorVision Deployment Documentation

This document tracks all successful deployments, improvements, and issues encountered during the deployment process.

---

## Deployment Log

### 2025-10-26 - Integration Wizard Connection Test Fix

**Type:** Bug Fix
**Status:** ✅ Deployed Successfully
**Deployment Method:** GitHub Actions CI/CD
**Deployment Time:** 4m 5s
**Git Commit:** `5cd273ce`

**Issue:**
- Integration Wizard connection test was failing with error: "Unsupported data type for field 'test': Boolean (expected number or numeric string)"
- The test connection function was sending `test: true` in the payload, but SensorVision backend only accepts numeric values for telemetry variables

**Fix Applied:**
- Removed the boolean `test: true` field from the connection test payload in `frontend/src/pages/IntegrationWizard.tsx:575`
- Connection test now only sends numeric values: `temperature: 23.5` and `humidity: 65.2`

**Files Changed:**
- `frontend/src/pages/IntegrationWizard.tsx` (1 deletion)

**Deployment Steps:**
1. Built frontend with fix: `npm run build` (completed in 4.02s)
2. Committed changes to branch `fix/clipboard-copy-integration-wizard`
3. Pushed to remote repository
4. Merged PR #17 to main branch
5. GitHub Actions workflow triggered automatically
6. Docker image built and pushed to ECR (2m 38s)
7. Deployed to EC2 instance at 35.88.65.186
8. Health check verified: Backend UP, Frontend Online

**Verification:**
- Backend health: http://35.88.65.186:8080/actuator/health (Status: UP ✅)
- Frontend: http://35.88.65.186:3000/ (Status: Online ✅)
- Integration Wizard: http://35.88.65.186:3000/integration-wizard (Status: Online ✅)

**Impact:**
- Users can now successfully test device connections in Integration Wizard
- No breaking changes to existing functionality
- Backward compatible

---

## Deployment Template

Use this template for documenting future deployments:

```
### YYYY-MM-DD - [Deployment Title]

**Type:** [Feature / Bug Fix / Enhancement / Hotfix]
**Status:** [✅ Deployed Successfully / ❌ Failed / 🔄 In Progress]
**Deployment Method:** [GitHub Actions CI/CD / Manual / Other]
**Deployment Time:** [Duration]
**Git Commit:** [Commit SHA]

**Issue:**
[Description of the problem or feature request]

**Fix/Implementation:**
[Description of changes made]

**Files Changed:**
- [List of modified files with brief description]

**Deployment Steps:**
1. [Step 1]
2. [Step 2]
...

**Verification:**
[How the deployment was verified]

**Impact:**
[Impact on users, system, or other components]

**Rollback Plan (if applicable):**
[Steps to rollback if issues arise]
```

---

## Error Tracking & Automatic Ticket Creation

When deployment errors occur, tickets are automatically created in GitHub Issues with the following information:

**Automatic Ticket Template:**
- **Title:** `[DEPLOYMENT ERROR] - [Date] - [Brief Description]`
- **Labels:** `deployment-error`, `bug`, `needs-investigation`
- **Priority:** Based on error severity
- **Content:**
  - Error message and stack trace
  - Deployment step that failed
  - Environment information
  - Relevant logs
  - Suggested remediation steps

**Manual Ticket Creation:**
If automatic creation fails, use the following command:
```bash
gh issue create --title "[DEPLOYMENT ERROR] - Description" \
  --label "deployment-error,bug,needs-investigation" \
  --body "Error details here"
```

---

## Deployment Best Practices

1. **Always use feature branches** - Never commit directly to main
2. **Test locally first** - Run `npm run build` and verify no errors
3. **Monitor the deployment** - Use `gh run watch` to track GitHub Actions
4. **Verify health checks** - Check backend `/actuator/health` endpoint
5. **Document everything** - Update this file immediately after deployment
6. **Have a rollback plan** - Know how to revert if issues arise

---

## Quick Reference

### Common Deployment Commands

```bash
# Build frontend locally
cd frontend && npm run build

# Check git status
git status

# Commit changes
git add <files>
git commit -m "type: description"

# Push to remote
git push origin <branch-name>

# Create PR
gh pr create --title "Title" --body "Description"

# Merge PR (auto-deploy to production)
gh pr merge <PR-number> --squash --delete-branch

# Monitor deployment
gh run watch <run-id>

# Check production health
curl http://35.88.65.186:8080/actuator/health

# Create issue for error
gh issue create --title "[ERROR] Description" --label "bug" --body "Details"
```

### Environment Information

**Production Server:**
- IP: 35.88.65.186
- Backend Port: 8080
- Frontend Port: 3000
- Region: us-west-2

**GitHub Actions Workflows:**
- Deploy to Production: `.github/workflows/deploy-production.yml`
- Triggers: Push to main, Manual dispatch

**Deployment Targets:**
- ECR Repository: sensorvision-backend
- EC2 Instance: Production instance in us-west-2

---

## Rollback Procedures

### Quick Rollback (Recommended)
```bash
# 1. Identify the last working commit
git log --oneline -10

# 2. Create rollback branch from working commit
git checkout -b rollback/<issue-description> <working-commit-sha>

# 3. Push and create PR
git push origin rollback/<issue-description>
gh pr create --title "Rollback: <reason>" --body "Rolling back to commit <sha>"

# 4. Merge to trigger deployment
gh pr merge --squash
```

### Emergency Rollback (EC2 Direct)
```bash
# SSH into EC2 and redeploy previous image
ssh -i ~/.ssh/key.pem ec2-user@35.88.65.186
cd /home/ec2-user/sensorvision
docker-compose pull
docker-compose up -d
```

---

## Issue Labels Reference

- `deployment-error` - Errors during deployment
- `deployment-success` - Successful deployment tracking
- `rollback-required` - Needs rollback
- `needs-investigation` - Requires investigation
- `critical` - Critical priority
- `high-priority` - High priority
- `bug` - Bug fix
- `feature` - New feature
- `enhancement` - Improvement

---

## Contacts & Resources

**Documentation:**
- Main README: `README.md`
- Claude Instructions: `CLAUDE.md`
- AWS Deployment Guide: `AWS_DEPLOYMENT_GUIDE.md`

**Monitoring:**
- GitHub Actions: https://github.com/CodeFleck/sensorvision/actions
- Production Dashboard: http://35.88.65.186:3000/
- Backend Health: http://35.88.65.186:8080/actuator/health

---

*Last Updated: 2025-10-26*
*Maintained by: Development Team*
*Auto-generated deployment documentation*
