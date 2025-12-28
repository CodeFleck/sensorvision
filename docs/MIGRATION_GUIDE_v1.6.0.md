# Migration Guide: Upgrading to Industrial Cloud v1.6.0

**Target Audience**: Existing Industrial Cloud users upgrading from v1.5.0 or earlier
**Release Date**: November 14, 2025
**Estimated Migration Time**: 15-30 minutes

---

## Table of Contents

1. [Overview](#overview)
2. [Pre-Migration Checklist](#pre-migration-checklist)
3. [Backup Procedures](#backup-procedures)
4. [Migration Steps](#migration-steps)
5. [Post-Migration Verification](#post-migration-verification)
6. [New Features Guide](#new-features-guide)
7. [Troubleshooting](#troubleshooting)
8. [Rollback Procedures](#rollback-procedures)

---

## Overview

### What's Changing

Industrial Cloud v1.6.0 introduces two major features:
1. **Plugin Marketplace** - Extensible plugin system with 6 official plugins
2. **Statistical Time-Series Functions** - 10 new functions for advanced analytics

### Compatibility

- ✅ **Fully Backward Compatible** - No breaking changes
- ✅ **Automatic Migrations** - Database schema updated automatically
- ✅ **Zero Downtime** - Can upgrade with minimal service interruption
- ✅ **Preserves Data** - All existing devices, rules, alerts, and dashboards retained

### What You Get

**New Capabilities**:
- Install plugins for LoRaWAN, Modbus, Sigfox, Slack, Discord, and HTTP webhooks
- Use statistical functions like `avg()`, `stddev()`, `percentChange()` in synthetic variables
- Detect anomalies and spikes with time-series analysis
- Extend platform with custom plugins

**No Impact On**:
- Existing devices and telemetry data
- Current rules and alerts
- Dashboards and widgets
- User accounts and permissions
- API integrations

---

## Pre-Migration Checklist

### System Requirements

Verify your system meets requirements:

- [ ] **Database**: PostgreSQL 14+ (check version: `psql --version`)
- [ ] **Java**: Java 17+ (check version: `java -version`)
- [ ] **Node.js**: Node 18+ for frontend (check version: `node --version`)
- [ ] **Disk Space**: At least 10GB free for backups and new data
- [ ] **Memory**: 2GB+ RAM available for backend

### Pre-Upgrade Checks

- [ ] **Backup created** (see [Backup Procedures](#backup-procedures))
- [ ] **Current version verified**: Check `/actuator/info` or logs
- [ ] **Active users notified**: Schedule maintenance window if needed
- [ ] **Test environment validated**: If you have staging, test there first
- [ ] **Rollback plan ready**: Know how to restore from backup

---

## Backup Procedures

### 1. Database Backup

**PostgreSQL Backup** (REQUIRED):
```bash
# Full database backup
pg_dump -h localhost -U indcloud_user -d indcloud \
  --no-owner --no-acl \
  > indcloud_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup file created
ls -lh indcloud_backup_*.sql
```

**Expected output**: Backup file should be several MB to GB depending on data volume.

### 2. Application Backup

**Backend JAR**:
```bash
# Copy current JAR
cp /opt/indcloud/current.jar \
   /opt/indcloud/backup/indcloud_v1.5.0_$(date +%Y%m%d).jar
```

**Frontend Static Files** (if deployed separately):
```bash
# Backup frontend
cp -r /var/www/indcloud \
      /var/www/indcloud_backup_$(date +%Y%m%d)
```

### 3. Configuration Backup

```bash
# Backup application.yml (if customized)
cp /opt/indcloud/config/application.yml \
   /opt/indcloud/backup/application.yml.backup
```

### 4. Verify Backups

```bash
# Check backup file integrity
psql -h localhost -U indcloud_user -d indcloud_test \
  < indcloud_backup_*.sql

# If successful, you have a valid backup
# Clean up test database
dropdb -h localhost -U indcloud_user indcloud_test
```

---

## Migration Steps

### Step 1: Stop Application (Optional)

**For Zero-Downtime Upgrade**: Skip this step and proceed to Step 2
**For Safe Upgrade**: Stop application

```bash
# Using systemd
sudo systemctl stop indcloud

# Using Docker
docker-compose down

# Verify stopped
curl http://localhost:8080/actuator/health
# Should fail with connection error
```

### Step 2: Update Code

**From Git Repository**:
```bash
cd /path/to/indcloud
git checkout main
git pull origin main
git checkout v1.6.0  # Or specific commit/tag
```

**From Release Archive**:
```bash
# Download v1.6.0 from GitHub releases
wget https://github.com/CodeFleck/indcloud/releases/download/v1.6.0/indcloud-1.6.0.tar.gz
tar -xzf indcloud-1.6.0.tar.gz
cd indcloud-1.6.0
```

### Step 3: Build Application

**Backend**:
```bash
# Clean build
./gradlew clean build

# Expected output: BUILD SUCCESSFUL
# JAR location: build/libs/indcloud-1.6.0.jar
```

**Frontend**:
```bash
cd frontend
npm install  # Update dependencies
npm run build  # Production build

# Expected output: dist/ folder created
# Verify: ls -la dist/
```

### Step 4: Deploy Backend

**Copy New JAR**:
```bash
# Copy to deployment directory
cp build/libs/indcloud-1.6.0.jar /opt/indcloud/

# Update symlink
ln -sf /opt/indcloud/indcloud-1.6.0.jar \
       /opt/indcloud/current.jar
```

### Step 5: Start Application

**Start Backend**:
```bash
# Using systemd
sudo systemctl start indcloud

# Using Docker
docker-compose up -d

# Using manual start
java -jar /opt/indcloud/current.jar
```

**Monitor Startup**:
```bash
# Watch logs
sudo journalctl -u indcloud -f

# Or with Docker
docker-compose logs -f indcloud

# Look for these key messages:
# ✅ "Flyway: Successfully applied 2 migrations"
# ✅ "Started IndcloudApplication in X seconds"
# ❌ No ERROR logs
```

**Expected Flyway Output**:
```
Flyway: Migrating schema "public" to version "50 - Create plugin marketplace schema"
Flyway: Migrating schema "public" to version "51 - Seed plugin marketplace"
Flyway: Successfully applied 2 migrations to schema "public"
```

### Step 6: Deploy Frontend

**Copy Built Files**:
```bash
# Backup current frontend (already done in pre-migration)
# Copy new frontend
cp -r frontend/dist/* /var/www/indcloud/

# Set permissions
chown -R www-data:www-data /var/www/indcloud
chmod -R 755 /var/www/indcloud
```

**Reload Web Server**:
```bash
# Nginx
sudo nginx -t  # Test configuration
sudo systemctl reload nginx

# Apache
sudo apachectl configtest
sudo systemctl reload apache2
```

---

## Post-Migration Verification

### 1. Database Verification

**Check Migrations**:
```sql
-- Connect to database
psql -h localhost -U indcloud_user -d indcloud

-- Verify migrations applied
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('50', '51')
ORDER BY version;

-- Expected output:
-- version |           description              | success
-- --------|------------------------------------|---------
--   50    | Create plugin marketplace schema   |    t
--   51    | Seed plugin marketplace            |    t
```

**Check Tables Created**:
```sql
-- Verify new tables exist
\dt plugin*

-- Expected output:
--  plugin_ratings
--  plugin_registry
--  installed_plugins
```

**Verify Plugins Seeded**:
```sql
-- Count seeded plugins
SELECT COUNT(*) FROM plugin_registry;
-- Expected: 6

-- List plugins
SELECT plugin_key, name, version, is_official
FROM plugin_registry
ORDER BY plugin_key;

-- Expected plugins:
-- discord-notifications, Dis cord Notifications, 1.0.0, true
-- http-webhook, HTTP Webhook Receiver, 1.0.0, true
-- lorawan-ttn, LoRaWAN TTN Integration, 1.0.0, true
-- modbus-tcp, Modbus TCP Integration, 1.0.0, true
-- sigfox-parser, Sigfox Protocol Parser, 1.0.0, true
-- slack-notifications, Slack Notifications, 1.0.0, true
```

### 2. Backend Verification

**Health Check**:
```bash
curl http://localhost:8080/actuator/health

# Expected: {"status":"UP"}
```

**Plugin Marketplace API**:
```bash
# Get all plugins
curl http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer <your-jwt-token>"

# Expected: JSON array with 6 plugins
```

**Statistical Functions**:
```bash
# Test in synthetic variable (via UI or API)
# Create synthetic variable with expression: avg("temperature", "1h")
# Verify it calculates without error
```

### 3. Frontend Verification

**Access Application**:
- Navigate to: `http://your-instance:3001` (or your domain)
- Login with existing credentials
- Verify dashboard loads

**Plugin Marketplace UI**:
- Click "Plugin Marketplace" in sidebar (Store icon)
- Verify 6 plugins displayed
- Search for "slack"
- Filter by "Notification" category
- Click on a plugin to view details

**Existing Features**:
- [ ] Dashboard displays correctly
- [ ] Devices list loads
- [ ] Telemetry data visible
- [ ] Rules and alerts work
- [ ] Analytics page loads
- [ ] User settings accessible

### 4. Regression Testing

**Test Core Functionality**:
- [ ] Can create a new device
- [ ] Can send telemetry via MQTT or HTTP
- [ ] Real-time data appears on dashboard
- [ ] Can create a new rule
- [ ] Alerts trigger correctly
- [ ] Notifications send successfully
- [ ] Can create synthetic variables
- [ ] Analytics queries work

---

## New Features Guide

### Using Plugin Marketplace

**Install Your First Plugin**:

1. **Navigate to Marketplace**
   - Click "Plugin Marketplace" in sidebar
   - Or go to: `http://your-instance/plugin-marketplace`

2. **Browse Plugins**
   - View all 6 official plugins
   - Use search: Try searching "slack"
   - Filter by category: Select "Notification"

3. **Install Slack Plugin** (Example)
   - Click on "Slack Notifications" card
   - Click "Install" button
   - Fill in configuration:
     - Webhook URL: `https://hooks.slack.com/services/YOUR/WEBHOOK/URL`
     - Channel: `#alerts`
     - Username: `Industrial Cloud`
     - Icon Emoji: `:robot_face:`
   - Click "Save"

4. **Activate Plugin**
   - Switch to "My Plugins" tab
   - Find "Slack Notifications"
   - Click "Activate"
   - Status changes to "Active" ✅

5. **Test Integration**
   - Create a test alert (trigger a rule)
   - Check Slack channel for notification

### Using Statistical Functions

**Create Spike Detection Rule**:

1. **Navigate to Synthetic Variables**
   - Go to a device page
   - Find "Synthetic Variables" section

2. **Create Average Calculation**
   - Click "Add Synthetic Variable"
   - Name: `avg_consumption_1h`
   - Expression: `avg("kwConsumption", "1h")`
   - Unit: `kW`
   - Save

3. **Create Spike Detector**
   - Add another synthetic variable
   - Name: `consumption_spike`
   - Expression: `if(kwConsumption > avg("kwConsumption", "1h") * 1.5, 1, 0)`
   - Description: "1 if current consumption exceeds 150% of hourly average"
   - Save

4. **Create Alert Rule**
   - Go to Rules page
   - Create new rule
   - Condition: `consumption_spike == 1`
   - Alert: "High consumption spike detected!"
   - Severity: HIGH
   - Save

**More Examples**:

```javascript
// Anomaly detection (2 standard deviations)
if(abs(voltage - avg("voltage", "1h")) > stddev("voltage", "1h") * 2, 1, 0)

// Daily energy total
sum("kwConsumption", "24h")

// Week-over-week growth
percentChange("kwConsumption", "7d")

// Smooth noisy sensor
movingAvg("temperature", "15m")

// Check if value is below median
if(pressure < median("pressure", "1h"), 1, 0)
```

---

## Troubleshooting

### Migration Issues

**Issue: Migrations Don't Run**

Symptoms: Application starts but plugins not available

Solution:
```bash
# Check Flyway status
psql -h localhost -U indcloud_user -d indcloud \
  -c "SELECT * FROM flyway_schema_history ORDER BY installed_rank DESC LIMIT 5;"

# If V50/V51 missing, manually apply:
psql -h localhost -U indcloud_user -d indcloud \
  < src/main/resources/db/migration/V50__Create_plugin_marketplace_schema.sql

psql -h localhost -U indcloud_user -d indcloud \
  < src/main/resources/db/migration/V51__Seed_plugin_marketplace.sql
```

---

**Issue: Plugin Marketplace Page Not Loading**

Symptoms: 404 error or blank page

Solutions:
1. Clear browser cache: `Ctrl+Shift+R` or `Cmd+Shift+R`
2. Verify frontend build deployed correctly
3. Check nginx/Apache configuration
4. Verify backend is running: `curl http://localhost:8080/api/v1/plugins`

---

**Issue: Statistical Functions Return Errors**

Symptoms: Synthetic variables with statistical functions fail to calculate

Solutions:
1. Check time window syntax: Use quotes `avg("temp", "1h")` not `avg(temp, 1h)`
2. Verify variable names exist: Must match telemetry variable names exactly
3. Check sufficient data: Need data within time window for calculation
4. Review logs for specific error messages

---

**Issue: "Table Already Exists" Error**

Symptoms: Migration fails with duplicate table error

Solution:
```sql
-- Check if tables exist
\dt plugin*

-- If tables exist but Flyway didn't record migration:
INSERT INTO flyway_schema_history (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
  (50, '50', 'Create plugin marketplace schema', 'SQL', 'V50__Create_plugin_marketplace_schema.sql', 0, 'indcloud_user', NOW(), 1000, true),
  (51, '51', 'Seed plugin marketplace', 'SQL', 'V51__Seed_plugin_marketplace.sql', 0, 'indcloud_user', NOW(), 500, true);
```

---

**Issue: Performance Degradation**

Symptoms: Slow queries after upgrade

Solutions:
1. Analyze database:
   ```sql
   ANALYZE plugin_registry;
   ANALYZE installed_plugins;
   ANALYZE plugin_ratings;
   ```

2. Create indexes (if not exist):
   ```sql
   CREATE INDEX IF NOT EXISTS idx_plugin_registry_category
   ON plugin_registry(category);

   CREATE INDEX IF NOT EXISTS idx_installed_plugins_org
   ON installed_plugins(organization_id);
   ```

3. Restart database to clear cache

---

### Getting Help

If issues persist:

1. **Check Logs**:
   ```bash
   # Backend logs
   sudo journalctl -u indcloud -n 100

   # Database logs
   sudo tail -f /var/log/postgresql/postgresql-*.log
   ```

2. **Check GitHub Issues**:
   - https://github.com/CodeFleck/indcloud/issues
   - Search for similar problems

3. **Create Issue**:
   - Include Industrial Cloud version (1.6.0)
   - Attach relevant log snippets
   - Describe steps to reproduce
   - Note migration step where issue occurred

---

## Rollback Procedures

If you need to rollback to v1.5.0:

### 1. Stop Application

```bash
sudo systemctl stop indcloud
```

### 2. Restore Database

```bash
# Drop new tables (ONLY if needed)
psql -h localhost -U indcloud_user -d indcloud <<EOF
DROP TABLE IF EXISTS plugin_ratings CASCADE;
DROP TABLE IF EXISTS installed_plugins CASCADE;
DROP TABLE IF EXISTS plugin_registry CASCADE;
EOF

# Restore from backup
psql -h localhost -U indcloud_user -d indcloud \
  < indcloud_backup_YYYYMMDD_HHMMSS.sql

# Remove migration records
psql -h localhost -U indcloud_user -d indcloud <<EOF
DELETE FROM flyway_schema_history WHERE version IN ('50', '51');
EOF
```

### 3. Restore Application

```bash
# Restore previous JAR
ln -sf /opt/indcloud/backup/indcloud_v1.5.0_YYYYMMDD.jar \
       /opt/indcloud/current.jar

# Restore previous frontend
rm -rf /var/www/indcloud/*
cp -r /var/www/indcloud_backup_YYYYMMDD/* /var/www/indcloud/
```

### 4. Start Application

```bash
sudo systemctl start indcloud
sudo systemctl reload nginx
```

### 5. Verify Rollback

```bash
# Check health
curl http://localhost:8080/actuator/health

# Verify version (should be 1.5.0)
curl http://localhost:8080/actuator/info

# Test existing features work
```

---

## Best Practices

### Before Upgrading

- ✅ Always test in staging environment first
- ✅ Create comprehensive backups
- ✅ Schedule during low-traffic period
- ✅ Notify users of maintenance window
- ✅ Have rollback plan ready

### During Upgrade

- ✅ Monitor logs continuously
- ✅ Verify each step completes successfully
- ✅ Don't skip verification steps
- ✅ Document any issues encountered

### After Upgrade

- ✅ Test all critical workflows
- ✅ Monitor application for 24-48 hours
- ✅ Keep backups for at least 7 days
- ✅ Update documentation with new features
- ✅ Train users on new capabilities

---

## FAQ

**Q: Will my existing data be affected?**
A: No. All devices, telemetry, rules, alerts, and dashboards are preserved. New tables are added, existing tables unchanged.

**Q: Do I need to reconfigure anything?**
A: No. All existing configuration is retained. New features are opt-in.

**Q: Can I skip this upgrade?**
A: Yes, but you'll miss powerful new features. v1.6.0 is fully compatible with v1.5.0 infrastructure.

**Q: How long does migration take?**
A: Typically 15-30 minutes including backup, depending on database size.

**Q: Can I upgrade with zero downtime?**
A: Yes, if you have a load-balanced setup. Migrations run automatically on first startup.

**Q: What if I encounter errors?**
A: Follow the rollback procedure to restore to v1.5.0. Report issues on GitHub.

**Q: Are there any performance impacts?**
A: Minimal. Plugin Marketplace adds new tables but doesn't affect existing queries. Statistical functions are opt-in.

---

## Summary Checklist

### Pre-Migration
- [ ] Backups created (database, application, config)
- [ ] Backups verified
- [ ] System requirements met
- [ ] Maintenance window scheduled

### Migration
- [ ] Application stopped (if needed)
- [ ] Code updated to v1.6.0
- [ ] Application built successfully
- [ ] Backend deployed
- [ ] Frontend deployed
- [ ] Application started

### Verification
- [ ] Migrations applied (V50, V51)
- [ ] 6 plugins seeded in database
- [ ] Health endpoint returns UP
- [ ] Plugin Marketplace UI accessible
- [ ] Statistical functions work
- [ ] Existing features functional

### Post-Migration
- [ ] Users notified of completion
- [ ] New features documented
- [ ] Backups retained for 7+ days
- [ ] Monitoring enabled

---

**Need Help?**

- **Documentation**: [docs/](../docs/)
- **GitHub Issues**: https://github.com/CodeFleck/indcloud/issues
- **Discussions**: https://github.com/CodeFleck/indcloud/discussions

---

**Migration Guide Version**: 1.0
**Last Updated**: November 14, 2025
**Target Version**: Industrial Cloud v1.6.0
