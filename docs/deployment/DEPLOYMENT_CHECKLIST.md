# Production Deployment Checklist

**Date**: 2025-11-14
**Release**: Q1 2025 - Sprints 4-6
**Version**: 1.6.0

---

## Pre-Deployment

### Code Review & Testing

- [ ] **Sprint 4 Phase 2 PR reviewed and approved**
  - PR: `feature/sprint-4-phase-2-activation`
  - Reviewer: _______________
  - Tests: 69/69 passing ✅

- [ ] **Sprint 6 Plugin Marketplace PR reviewed and approved**
  - PR: `feature/sprint-6-plugin-marketplace`
  - Reviewer: _______________
  - Tests: 39/39 passing ✅

- [ ] **All tests passing on main branch**
  ```bash
  ./gradlew clean test
  # Expected: BUILD SUCCESSFUL, all tests passing
  ```

- [ ] **Frontend build successful**
  ```bash
  cd frontend && npm run build
  # Expected: No errors, dist/ created
  ```

- [ ] **No merge conflicts**
  ```bash
  git checkout main
  git pull
  git merge feature/sprint-4-phase-2-activation --no-commit
  git merge feature/sprint-6-plugin-marketplace --no-commit
  # Expected: No conflicts
  ```

### Database Preparation

- [ ] **Production database backup created**
  ```bash
  pg_dump -h <prod-host> -U <user> -d indcloud > backup_$(date +%Y%m%d_%H%M%S).sql
  ```
  - Backup file: _______________
  - Size: _______________
  - Verified: ☐

- [ ] **Flyway migration test on staging**
  - Staging migrations: V50, V51 applied successfully ☐
  - No migration errors ☐
  - Rollback tested ☐

- [ ] **Database schema verified**
  ```sql
  -- Check tables exist
  SELECT table_name FROM information_schema.tables
  WHERE table_schema = 'public' AND table_name LIKE 'plugin%';

  -- Expected: plugin_registry, installed_plugins, plugin_ratings
  ```

### Environment Configuration

- [ ] **Environment variables configured**
  - `DB_USERNAME` ☐
  - `DB_PASSWORD` ☐
  - `DB_HOST` ☐
  - `SPRING_PROFILES_ACTIVE=prod` ☐
  - `SERVER_PORT=8080` ☐

- [ ] **Secrets management**
  - Database credentials stored securely ☐
  - JWT secret key configured ☐
  - AWS credentials (if applicable) ☐
  - Email service credentials ☐

- [ ] **Infrastructure ready**
  - PostgreSQL 14+ running ☐
  - Sufficient disk space (>10GB free) ☐
  - Memory allocation (>2GB for backend) ☐
  - Nginx configured ☐

---

## Deployment Steps

### 1. Merge Pull Requests

- [ ] **Merge Sprint 4 Phase 2 to main**
  ```bash
  git checkout main
  git pull origin main
  git merge feature/sprint-4-phase-2-activation
  git push origin main
  ```
  - Merged at: _______________
  - Commit SHA: _______________

- [ ] **Merge Sprint 6 Plugin Marketplace to main**
  ```bash
  git merge feature/sprint-6-plugin-marketplace
  git push origin main
  ```
  - Merged at: _______________
  - Commit SHA: _______________

- [ ] **Tag release**
  ```bash
  git tag -a v1.6.0 -m "Q1 2025 Release: Sprint 4 Phase 2 + Sprint 6 Plugin Marketplace"
  git push origin v1.6.0
  ```

### 2. Backend Deployment

- [ ] **Build production JAR**
  ```bash
  git checkout v1.6.0
  ./gradlew clean build -x test
  ```
  - Build time: _______________
  - JAR size: _______________

- [ ] **Copy JAR to production server**
  ```bash
  scp build/libs/indcloud-1.6.0.jar user@prod-server:/opt/indcloud/
  ```

- [ ] **Backup current JAR**
  ```bash
  ssh user@prod-server
  cp /opt/indcloud/current.jar /opt/indcloud/backup_$(date +%Y%m%d).jar
  ```

- [ ] **Update symlink**
  ```bash
  ln -sf /opt/indcloud/indcloud-1.6.0.jar /opt/indcloud/current.jar
  ```

- [ ] **Stop application**
  ```bash
  sudo systemctl stop indcloud
  ```
  - Stopped at: _______________

- [ ] **Verify database migrations ready**
  ```bash
  # Migrations will auto-run on startup
  # Check logs for:
  # "Flyway: Successfully applied 2 migrations"
  ```

- [ ] **Start application**
  ```bash
  sudo systemctl start indcloud
  ```
  - Started at: _______________

- [ ] **Monitor startup logs**
  ```bash
  sudo journalctl -u indcloud -f
  ```
  - Look for:
    - ✅ "Flyway: Successfully applied 2 migrations" ☐
    - ✅ "Started SensorvisionApplication in X seconds" ☐
    - ❌ No ERROR logs ☐

### 3. Database Verification

- [ ] **Verify migrations applied**
  ```sql
  SELECT version, description, success
  FROM flyway_schema_history
  WHERE version IN ('50', '51')
  ORDER BY version;
  ```
  - V50 applied: ☐
  - V51 applied: ☐

- [ ] **Verify plugins seeded**
  ```sql
  SELECT plugin_key, name, version, is_official
  FROM plugin_registry;
  ```
  - Expected: 6 plugins ☐
  - All official: ☐

- [ ] **Verify statistical functions available**
  ```sql
  -- Test in UI or via API
  -- Create synthetic variable with: avg("temperature", "1h")
  ```
  - Functions work: ☐

### 4. Frontend Deployment

- [ ] **Build production frontend**
  ```bash
  cd frontend
  npm install
  npm run build
  ```
  - Build time: _______________
  - Bundle size: _______________

- [ ] **Backup current frontend**
  ```bash
  ssh user@prod-server
  cp -r /var/www/indcloud /var/www/indcloud_backup_$(date +%Y%m%d)
  ```

- [ ] **Deploy frontend**
  ```bash
  scp -r dist/* user@prod-server:/var/www/indcloud/
  ```

- [ ] **Set permissions**
  ```bash
  ssh user@prod-server
  chown -R www-data:www-data /var/www/indcloud
  chmod -R 755 /var/www/indcloud
  ```

- [ ] **Reload nginx**
  ```bash
  sudo nginx -t
  sudo systemctl reload nginx
  ```

### 5. Verification Tests

#### Backend API

- [ ] **Health check**
  ```bash
  curl http://prod-server:8080/actuator/health
  # Expected: {"status":"UP"}
  ```

- [ ] **Plugin marketplace API**
  ```bash
  # Get all plugins
  curl http://prod-server:8080/api/v1/plugins \
    -H "Authorization: Bearer <token>"
  # Expected: 200 OK, 6 plugins
  ```

- [ ] **Statistical functions**
  - Create synthetic variable with avg() ☐
  - Verify calculation works ☐
  - Check data returned ☐

#### Frontend UI

- [ ] **Access application**
  - URL: http://prod-server (or domain)
  - Page loads: ☐
  - No console errors: ☐

- [ ] **Login works**
  - Test user can log in: ☐
  - JWT token received: ☐

- [ ] **Plugin Marketplace accessible**
  - Navigate to Plugin Marketplace: ☐
  - 6 plugins displayed: ☐
  - Search works: ☐
  - Filter works: ☐

- [ ] **Plugin installation workflow**
  - Install a plugin (e.g., Slack): ☐
  - Configure plugin: ☐
  - Activate plugin: ☐
  - Deactivate plugin: ☐
  - Uninstall plugin: ☐

#### Regression Tests

- [ ] **Existing features work**
  - Device CRUD: ☐
  - Telemetry ingestion (MQTT): ☐
  - Dashboard displays: ☐
  - Rules engine triggers alerts: ☐
  - Analytics queries work: ☐
  - Serverless functions execute: ☐

---

## Post-Deployment

### Monitoring

- [ ] **Application health**
  ```bash
  # Monitor for 15 minutes
  sudo journalctl -u indcloud -f
  ```
  - No errors: ☐
  - Normal log patterns: ☐

- [ ] **Database connections**
  ```sql
  SELECT count(*) FROM pg_stat_activity WHERE datname = 'indcloud';
  ```
  - Connection count normal: ☐
  - No connection leaks: ☐

- [ ] **System resources**
  ```bash
  top
  df -h
  free -m
  ```
  - CPU usage normal (<50%): ☐
  - Memory usage normal (<70%): ☐
  - Disk space sufficient (>10GB free): ☐

### Performance Tests

- [ ] **API response times**
  ```bash
  # Test 10 requests
  for i in {1..10}; do
    time curl -s http://prod-server:8080/api/v1/plugins -H "Authorization: Bearer <token>" > /dev/null
  done
  ```
  - Average response time < 200ms: ☐

- [ ] **Page load times**
  - Homepage < 2s: ☐
  - Dashboard < 3s: ☐
  - Plugin Marketplace < 2s: ☐

### Documentation

- [ ] **Update production docs**
  - [ ] Update CHANGELOG.md
  - [ ] Update version in README.md
  - [ ] Update API docs (if needed)
  - [ ] Update user guide (if needed)

- [ ] **Internal documentation**
  - [ ] Update deployment runbook
  - [ ] Update architecture diagrams
  - [ ] Document any manual steps taken

### Communication

- [ ] **Team notification**
  - Slack/Discord message sent: ☐
  - Deployment notes shared: ☐
  - Known issues documented: ☐

- [ ] **User announcement**
  - Email sent to users: ☐
  - In-app notification (if applicable): ☐
  - Blog post published: ☐
  - Release notes: ☐

---

## Rollback Plan (If Issues Occur)

### Immediate Rollback (Critical Issues)

If deployment fails or critical issues found:

- [ ] **Stop application**
  ```bash
  sudo systemctl stop indcloud
  ```

- [ ] **Restore previous JAR**
  ```bash
  ln -sf /opt/indcloud/backup_<date>.jar /opt/indcloud/current.jar
  ```

- [ ] **Rollback database migrations**
  ```sql
  -- Only if data corruption occurred
  DROP TABLE IF EXISTS plugin_ratings CASCADE;
  DROP TABLE IF EXISTS installed_plugins CASCADE;
  DROP TABLE IF EXISTS plugin_registry CASCADE;
  DELETE FROM flyway_schema_history WHERE version IN ('50', '51');

  -- Restore from backup
  psql -h <host> -U <user> -d indcloud < backup_<timestamp>.sql
  ```

- [ ] **Restore frontend**
  ```bash
  rm -rf /var/www/indcloud/*
  cp -r /var/www/indcloud_backup_<date>/* /var/www/indcloud/
  ```

- [ ] **Restart application**
  ```bash
  sudo systemctl start indcloud
  sudo systemctl reload nginx
  ```

- [ ] **Verify rollback**
  - Application starts: ☐
  - No migration errors: ☐
  - Previous version running: ☐
  - Existing features work: ☐

### Partial Rollback (Minor Issues)

If only specific features have issues:

- [ ] **Disable problematic plugins**
  ```sql
  UPDATE plugin_registry
  SET is_official = false, is_verified = false
  WHERE plugin_key IN ('problem-plugin');
  ```

- [ ] **Feature flags** (if applicable)
  - Disable Plugin Marketplace UI: ☐
  - Disable Statistical Functions: ☐

---

## Known Issues & Workarounds

### Issue Template

**Issue**: Description of the issue
**Severity**: Critical / High / Medium / Low
**Workaround**: Temporary solution
**Fix ETA**: Expected resolution time

---

## Sign-Off

### Deployment Team

- [ ] **DevOps Lead**: _____________ Date: _____________
- [ ] **Backend Developer**: _____________ Date: _____________
- [ ] **Frontend Developer**: _____________ Date: _____________
- [ ] **QA Lead**: _____________ Date: _____________

### Approval

- [ ] **Technical Lead**: _____________ Date: _____________
- [ ] **Product Owner**: _____________ Date: _____________

---

## Deployment Summary

**Deployment Date**: _____________
**Deployment Start Time**: _____________
**Deployment End Time**: _____________
**Total Duration**: _____________
**Status**: ☐ Success ☐ Success with Issues ☐ Failed/Rolled Back

**Features Deployed**:
- ✅ Sprint 4 Phase 2: Statistical Time-Series Functions (10 functions)
- ✅ Sprint 6: Plugin Marketplace MVP (6 official plugins)

**Database Changes**:
- V50: Plugin marketplace schema
- V51: 6 official plugins seeded

**Code Changes**:
- Backend: 1,168 lines (Sprint 4) + 3,402 lines (Sprint 6)
- Frontend: 1,400+ lines
- Documentation: 6,000+ lines
- Tests: 39 new tests

**Performance Metrics**:
- API response time: _____ ms
- Page load time: _____ s
- Memory usage: _____ MB
- CPU usage: _____ %

**Issues Encountered**:
1. _____________________________________________
2. _____________________________________________

**Resolutions**:
1. _____________________________________________
2. _____________________________________________

**Next Steps**:
1. _____________________________________________
2. _____________________________________________

**Notes**:
_____________________________________________
_____________________________________________
_____________________________________________

---

**Checklist Version**: 1.0.0
**Last Updated**: 2025-11-14
