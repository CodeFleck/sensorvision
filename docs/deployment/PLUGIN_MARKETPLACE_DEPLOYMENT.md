# Plugin Marketplace Deployment Guide

**Version**: 1.0.0
**Date**: 2025-11-14
**Sprint**: 6 - Plugin Marketplace MVP

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Database Migration](#database-migration)
3. [Backend Deployment](#backend-deployment)
4. [Frontend Deployment](#frontend-deployment)
5. [Verification](#verification)
6. [Rollback Plan](#rollback-plan)
7. [Monitoring](#monitoring)

---

## Prerequisites

### System Requirements
- PostgreSQL 14+ (with JSONB support)
- Java 17+
- Node.js 18+
- npm 9+

### Environment Variables
Ensure these are configured in `application.properties` or environment:

```properties
# Database (must support JSONB for plugin config schemas)
spring.datasource.url=jdbc:postgresql://localhost:5432/indcloud
spring.datasource.username=${DB_USERNAME}
spring.datasource.password=${DB_PASSWORD}

# Flyway migrations
spring.flyway.enabled=true
spring.flyway.baseline-on-migrate=true

# Application
server.port=8080
```

### Pre-Deployment Checklist
- [ ] Database backup completed
- [ ] All tests passing (39/39 plugin marketplace tests)
- [ ] PR reviewed and approved
- [ ] Staging environment tested
- [ ] Rollback plan prepared

---

## Database Migration

The Plugin Marketplace requires two new migrations:

### Migration V50: Plugin Marketplace Schema
Creates core plugin marketplace tables:
- `plugin_registry` - Plugin catalog
- `installed_plugins` - Organization plugin installations
- `plugin_ratings` - Plugin reviews and ratings

```bash
# Migration will run automatically on startup via Flyway
# Location: src/main/resources/db/migration/V50__Create_plugin_marketplace_schema.sql
```

### Migration V51: Seed Plugins
Seeds marketplace with 6 official plugins:
- LoRaWAN TTN Integration
- Slack Notifications
- Discord Notifications
- Sigfox Protocol Parser
- Modbus TCP Integration
- HTTP Webhook Receiver

```bash
# Migration will run automatically on startup via Flyway
# Location: src/main/resources/db/migration/V51__Seed_plugin_marketplace.sql
```

### Manual Migration Verification

```bash
# Connect to PostgreSQL
psql -h localhost -U indcloud_user -d indcloud

# Check migrations applied
SELECT version, description, success
FROM flyway_schema_history
WHERE version IN ('50', '51');

# Expected output:
# version | description                           | success
# --------+---------------------------------------+--------
# 50      | Create plugin marketplace schema      | t
# 51      | Seed plugin marketplace               | t

# Verify tables created
\dt plugin_*

# Expected tables:
# - plugin_registry
# - installed_plugins
# - plugin_ratings

# Check seeded plugins
SELECT plugin_key, name, version, category, is_official
FROM plugin_registry;

# Expected: 6 plugins (lorawan-ttn, slack-notifications, etc.)
```

---

## Backend Deployment

### Step 1: Build Application

```bash
# Clean build with all tests
./gradlew clean build

# Expected output:
# BUILD SUCCESSFUL
# 39 tests passed (PluginRegistryServiceTest: 23, PluginInstallationServiceTest: 16)
```

### Step 2: Deploy JAR

```bash
# Copy JAR to deployment directory
cp build/libs/indcloud-*.jar /opt/indcloud/

# Set permissions
chmod 755 /opt/indcloud/indcloud-*.jar

# Create symlink for easy updates
ln -sf /opt/indcloud/indcloud-*.jar /opt/indcloud/current.jar
```

### Step 3: Restart Application

```bash
# Using systemd
sudo systemctl restart indcloud

# Check status
sudo systemctl status indcloud

# Check logs for migration success
sudo journalctl -u indcloud -f

# Look for:
# "Flyway: Successfully applied 2 migrations"
# "Started SensorvisionApplication in X seconds"
```

### Step 4: Verify Backend API

```bash
# Test plugin registry endpoint
curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer <admin-token>"

# Expected: JSON array with 6 plugins

# Test plugin installation (requires authentication)
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/install \
  -H "Authorization: Bearer <admin-token>" \
  -H "Content-Type: application/json" \
  -d '{
    "applicationId": "test-app",
    "apiKey": "test-key",
    "region": "eu1",
    "webhookEnabled": true
  }'

# Expected: InstalledPlugin JSON with status "INACTIVE"
```

---

## Frontend Deployment

### Step 1: Build Frontend

```bash
cd frontend

# Install dependencies
npm install

# Build production bundle
npm run build

# Expected output:
# vite v5.x.x building for production...
# ✓ built in Xs
# dist/index.html                X kB
# dist/assets/index-XXXXX.js     X kB │ gzip: X kB
# dist/assets/index-XXXXX.css    X kB │ gzip: X kB
```

### Step 2: Deploy Static Assets

```bash
# Copy build to web server
cp -r dist/* /var/www/indcloud/

# Set permissions
chown -R www-data:www-data /var/www/indcloud
chmod -R 755 /var/www/indcloud
```

### Step 3: Configure Nginx (if applicable)

```nginx
# /etc/nginx/sites-available/indcloud

server {
    listen 80;
    server_name indcloud.example.com;

    root /var/www/indcloud;
    index index.html;

    # Frontend routes
    location / {
        try_files $uri $uri/ /index.html;
    }

    # API proxy
    location /api/ {
        proxy_pass http://localhost:8080;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
    }

    # WebSocket proxy
    location /ws/ {
        proxy_pass http://localhost:8080;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $upgrade;
        proxy_set_header Connection "upgrade";
    }
}
```

```bash
# Reload nginx
sudo nginx -t
sudo systemctl reload nginx
```

---

## Verification

### Backend Verification

**1. Database Tables**
```bash
psql -h localhost -U indcloud_user -d indcloud -c "
  SELECT
    (SELECT COUNT(*) FROM plugin_registry) as plugins_count,
    (SELECT COUNT(*) FROM installed_plugins) as installations_count,
    (SELECT COUNT(*) FROM plugin_ratings) as ratings_count;
"
# Expected: plugins_count = 6, installations/ratings may be 0
```

**2. API Endpoints**
```bash
# List all plugins
curl http://localhost:8080/api/v1/plugins

# Get specific plugin
curl http://localhost:8080/api/v1/plugins/lorawan-ttn

# Search plugins
curl "http://localhost:8080/api/v1/plugins/search?query=lorawan"

# Filter by category
curl "http://localhost:8080/api/v1/plugins/category/PROTOCOL_PARSER"

# Get popular plugins
curl http://localhost:8080/api/v1/plugins/popular

# Get top rated
curl http://localhost:8080/api/v1/plugins/top-rated

# Get recent
curl http://localhost:8080/api/v1/plugins/recent
```

**3. Test Installation Workflow**
```bash
# Install plugin (authenticated)
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/install \
  -H "Authorization: Bearer <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://hooks.slack.com/services/XXX",
    "channel": "#alerts"
  }'

# Check installation
curl -X GET http://localhost:8080/api/v1/plugins/slack-notifications/installation \
  -H "Authorization: Bearer <token>"

# Activate plugin
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/activate \
  -H "Authorization: Bearer <token>"

# Deactivate plugin
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/deactivate \
  -H "Authorization: Bearer <token>"

# Uninstall plugin
curl -X DELETE http://localhost:8080/api/v1/plugins/slack-notifications/uninstall \
  -H "Authorization: Bearer <token>"
```

### Frontend Verification

**1. Navigation**
- [ ] Navigate to http://indcloud.example.com
- [ ] Click "Plugin Marketplace" in sidebar
- [ ] Verify marketplace page loads

**2. Plugin Browsing**
- [ ] Verify 6 official plugins displayed
- [ ] Test search functionality (search "lorawan")
- [ ] Test category filter (select "Protocol Parser")
- [ ] Test tabs (switch between "Marketplace" and "My Plugins")

**3. Plugin Details**
- [ ] Click on a plugin card
- [ ] Verify details modal opens
- [ ] Check metadata (author, version, tags)
- [ ] Verify screenshots displayed (if available)
- [ ] Check external links (documentation, repository)

**4. Plugin Installation**
- [ ] Click "Install" on a plugin
- [ ] Verify configuration modal opens
- [ ] Fill in required fields
- [ ] Submit configuration
- [ ] Verify success toast notification
- [ ] Switch to "My Plugins" tab
- [ ] Verify plugin appears as "Inactive"

**5. Plugin Activation**
- [ ] Click "Activate" on installed plugin
- [ ] Verify status changes to "Active"
- [ ] Verify success notification

**6. Plugin Configuration Update**
- [ ] Click "Configure" on installed plugin
- [ ] Modify configuration values
- [ ] Save changes
- [ ] Verify success notification

**7. Plugin Deactivation/Uninstall**
- [ ] Click "Deactivate" on active plugin
- [ ] Verify status changes to "Inactive"
- [ ] Click "Uninstall"
- [ ] Confirm uninstall
- [ ] Verify plugin removed from "My Plugins"

---

## Rollback Plan

### If Migration Fails

```bash
# Rollback Flyway migrations
psql -h localhost -U indcloud_user -d indcloud

# Check current version
SELECT MAX(version) FROM flyway_schema_history;

# Manual rollback (if needed)
DROP TABLE IF EXISTS plugin_ratings CASCADE;
DROP TABLE IF EXISTS installed_plugins CASCADE;
DROP TABLE IF EXISTS plugin_registry CASCADE;

# Remove migration records
DELETE FROM flyway_schema_history WHERE version IN ('50', '51');

# Restart application (will not apply migrations)
sudo systemctl restart indcloud
```

### If Backend Deployment Fails

```bash
# Restore previous JAR
cp /opt/indcloud/backup/indcloud-previous.jar /opt/indcloud/current.jar

# Restart application
sudo systemctl restart indcloud

# Verify application running
curl http://localhost:8080/actuator/health
```

### If Frontend Deployment Fails

```bash
# Restore previous build
cp -r /var/www/indcloud-backup/* /var/www/indcloud/

# Reload nginx
sudo systemctl reload nginx

# Verify frontend accessible
curl http://indcloud.example.com
```

---

## Monitoring

### Health Checks

**Application Health**
```bash
# Spring Boot Actuator
curl http://localhost:8080/actuator/health

# Expected:
# {"status":"UP"}
```

**Database Health**
```bash
# Check plugin registry
psql -h localhost -U indcloud_user -d indcloud \
  -c "SELECT COUNT(*) FROM plugin_registry;"

# Should return 6
```

### Metrics to Monitor

**1. Plugin Installations**
```sql
SELECT
  pr.name,
  COUNT(ip.id) as installation_count,
  SUM(CASE WHEN ip.status = 'ACTIVE' THEN 1 ELSE 0 END) as active_count
FROM plugin_registry pr
LEFT JOIN installed_plugins ip ON pr.plugin_key = ip.plugin_key
GROUP BY pr.id, pr.name
ORDER BY installation_count DESC;
```

**2. Plugin Ratings**
```sql
SELECT
  pr.name,
  pr.rating_average,
  pr.rating_count,
  COUNT(rt.id) as review_count
FROM plugin_registry pr
LEFT JOIN plugin_ratings rt ON pr.id = rt.plugin_registry_id
GROUP BY pr.id, pr.name, pr.rating_average, pr.rating_count
ORDER BY pr.rating_average DESC;
```

**3. Error Monitoring**
```bash
# Check application logs for errors
sudo journalctl -u indcloud --since "10 minutes ago" | grep ERROR

# Check for plugin installation failures
sudo journalctl -u indcloud --since "10 minutes ago" | grep "PluginInstallationException"
```

### Prometheus Metrics (if enabled)

```yaml
# Plugin-specific metrics to add (future enhancement)
- plugin_installations_total{plugin_key}
- plugin_activations_total{plugin_key}
- plugin_execution_duration_seconds{plugin_key}
- plugin_errors_total{plugin_key}
```

---

## Post-Deployment Tasks

### 1. Update Documentation
- [ ] Update API documentation with plugin endpoints
- [ ] Update user guide with plugin marketplace instructions
- [ ] Update admin guide with plugin management

### 2. Announcement
- [ ] Notify users via email/platform announcement
- [ ] Publish blog post about new plugin marketplace
- [ ] Update changelog

### 3. Community Outreach
- [ ] Create GitHub Discussions thread for plugin development
- [ ] Prepare plugin development tutorial video
- [ ] Reach out to potential plugin contributors

### 4. Monitoring Setup
- [ ] Configure alerts for plugin installation errors
- [ ] Set up dashboard for plugin usage metrics
- [ ] Schedule weekly review of plugin ratings/feedback

---

## Troubleshooting

### Common Issues

**Issue 1: Migration Fails - "relation plugin_registry already exists"**
```bash
# Cause: Migration already applied
# Solution: Check flyway_schema_history
SELECT version, description FROM flyway_schema_history WHERE version = '50';

# If already applied, skip migration
# Otherwise, drop tables and re-run
```

**Issue 2: Plugin Installation Fails - "Plugin already installed"**
```bash
# Cause: Plugin already installed for organization
# Solution: Uninstall first or update configuration
curl -X DELETE http://localhost:8080/api/v1/plugins/{pluginKey}/uninstall \
  -H "Authorization: Bearer <token>"
```

**Issue 3: Configuration Validation Fails**
```bash
# Cause: Invalid JSON Schema configuration
# Solution: Verify configuration matches plugin schema
# Check plugin_registry.config_schema for required fields
SELECT config_schema FROM plugin_registry WHERE plugin_key = 'lorawan-ttn';
```

**Issue 4: Frontend Not Loading**
```bash
# Check browser console for errors
# Common causes:
# - API endpoints returning 404 (backend not deployed)
# - CORS issues (check backend CORS configuration)
# - Missing environment variables

# Verify API accessible
curl http://localhost:8080/api/v1/plugins

# Check nginx logs
sudo tail -f /var/log/nginx/error.log
```

---

## Security Considerations

### 1. API Authentication
- All plugin marketplace endpoints require authentication
- Use JWT tokens or session-based auth
- Rate limit plugin installation requests

### 2. Configuration Secrets
- Plugin configurations may contain API keys/tokens
- Stored in database with proper encryption
- Never log sensitive configuration values

### 3. Plugin Execution Isolation
- Plugins execute in sandboxed environment (future)
- Limit plugin access to organization data only
- Monitor plugin execution for anomalies

### 4. Plugin Verification
- Official plugins reviewed and verified by Industrial Cloud team
- Community plugins clearly marked as unverified
- Plugin submission requires code review

---

## Support

### Resources
- **Documentation**: https://github.com/CodeFleck/indcloud/tree/main/docs
- **Issues**: https://github.com/CodeFleck/indcloud/issues
- **Discussions**: https://github.com/CodeFleck/indcloud/discussions

### Contact
- **Email**: support@indcloud.io
- **Slack**: #plugin-marketplace channel

---

**Deployment completed**: _____________
**Deployed by**: _____________
**Version**: 1.0.0
**Status**: ✅ Production Ready
