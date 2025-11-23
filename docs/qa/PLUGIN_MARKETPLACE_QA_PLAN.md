# Plugin Marketplace QA Test Plan

**Version**: 1.0.0
**Date**: 2025-11-14
**Sprint**: 6 - Plugin Marketplace MVP
**Target Release**: Q1 2025

---

## Table of Contents

1. [Overview](#overview)
2. [Test Environment](#test-environment)
3. [Test Strategy](#test-strategy)
4. [Functional Tests](#functional-tests)
5. [Integration Tests](#integration-tests)
6. [UI/UX Tests](#uiux-tests)
7. [Security Tests](#security-tests)
8. [Performance Tests](#performance-tests)
9. [Regression Tests](#regression-tests)
10. [Acceptance Criteria](#acceptance-criteria)
11. [Test Execution Log](#test-execution-log)

---

## Overview

### Scope

This QA plan covers testing for Sprint 6: Plugin Marketplace MVP, including:
- Backend API endpoints (16 endpoints)
- Frontend UI components (marketplace, modals, workflows)
- Plugin lifecycle (install, activate, configure, deactivate, uninstall)
- 6 official plugins
- Rating and review system
- Search and filtering functionality

### Objectives

- Verify all 16 API endpoints work correctly
- Ensure plugin installation workflows are intuitive and error-free
- Validate plugin configurations against JSON Schema
- Test security (authentication, authorization, input validation)
- Verify performance with multiple plugins and installations
- Ensure backward compatibility with existing features

### Success Criteria

- 100% of functional test cases pass
- 100% of API tests pass (39/39 unit tests already passing)
- Zero critical/blocker bugs
- Performance meets targets (API <200ms, UI <2s load)
- All 6 official plugins install and activate successfully

---

## Test Environment

### Required Infrastructure

**Backend:**
- Spring Boot 3.2+ application running
- PostgreSQL 14+ with migrations V50 and V51 applied
- Java 17+

**Frontend:**
- React 18+ application served via Vite dev server or production build
- Modern browser (Chrome 90+, Firefox 88+, Safari 14+)

**Test Data:**
- Fresh database with Flyway migrations applied
- At least 2 test organizations
- At least 2 test users per organization
- 6 official plugins seeded via V51 migration

### Environment Setup

```bash
# 1. Start infrastructure
docker-compose up -d

# 2. Build and start backend
./gradlew clean build
./gradlew bootRun

# 3. Verify migrations
psql -h localhost -U sensorvision_user -d sensorvision \
  -c "SELECT COUNT(*) FROM plugin_registry;"
# Expected: 6

# 4. Start frontend
cd frontend
npm install
npm run dev

# 5. Create test users
# Use Integration Wizard or API to create:
# - admin@test.com (organization: "Test Org 1")
# - user@test.com (organization: "Test Org 2")
```

---

## Test Strategy

### Test Levels

1. **Unit Tests** (Already implemented)
   - PluginRegistryServiceTest: 23 tests
   - PluginInstallationServiceTest: 16 tests
   - Total: 39 tests, 100% passing

2. **Integration Tests** (This plan)
   - API endpoint tests
   - Database integration tests
   - Plugin activation tests

3. **UI Tests** (This plan)
   - Component rendering
   - User workflows
   - Error handling

4. **End-to-End Tests** (This plan)
   - Complete user journeys
   - Cross-browser testing
   - Real plugin execution

### Testing Tools

- **Backend**: JUnit 5, Mockito, Spring Boot Test, RestAssured
- **Frontend**: Jest, React Testing Library, Cypress (E2E)
- **API**: Postman, curl, HTTPie
- **Database**: psql, DBeaver
- **Performance**: JMeter, Chrome DevTools
- **Manual**: Browser DevTools, logging

---

## Functional Tests

### FT-01: Plugin Registry - Browse Plugins

**Objective**: Verify users can browse all available plugins

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-01.1 | GET /api/v1/plugins (no auth) | 401 Unauthorized | ☐ |
| FT-01.2 | GET /api/v1/plugins (with auth) | 200 OK, returns 6 plugins | ☐ |
| FT-01.3 | Verify each plugin has required fields | All fields present (id, pluginKey, name, etc.) | ☐ |
| FT-01.4 | Verify official badges | All 6 plugins have isOfficial=true | ☐ |
| FT-01.5 | Verify verified badges | All 6 plugins have isVerified=true | ☐ |

**Test Script:**
```bash
# FT-01.1: No auth
curl -X GET http://localhost:8080/api/v1/plugins
# Expected: 401

# FT-01.2: With auth
TOKEN="<get-from-login>"
curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200, JSON array with 6 items

# FT-01.3: Check fields
curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.[0] | keys'
# Expected: [id, pluginKey, name, description, category, version, ...]
```

---

### FT-02: Plugin Registry - Search and Filter

**Objective**: Verify search and filter functionality

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-02.1 | Search for "lorawan" | Returns 1 plugin (LoRaWAN TTN) | ☐ |
| FT-02.2 | Search for "slack" | Returns 1 plugin (Slack Notifications) | ☐ |
| FT-02.3 | Search for "nonexistent" | Returns empty array | ☐ |
| FT-02.4 | Filter by category PROTOCOL_PARSER | Returns 3 plugins (LoRaWAN, Modbus, Sigfox) | ☐ |
| FT-02.5 | Filter by category NOTIFICATION | Returns 2 plugins (Slack, Discord) | ☐ |
| FT-02.6 | Filter official=true | Returns all 6 plugins | ☐ |
| FT-02.7 | Combine search + category filter | Correctly filters results | ☐ |

**Test Script:**
```bash
# FT-02.1: Search
curl -X GET "http://localhost:8080/api/v1/plugins?search=lorawan" \
  -H "Authorization: Bearer $TOKEN" \
  | jq 'length'
# Expected: 1

# FT-02.4: Filter by category
curl -X GET http://localhost:8080/api/v1/plugins/category/PROTOCOL_PARSER \
  -H "Authorization: Bearer $TOKEN" \
  | jq 'length'
# Expected: 3
```

---

### FT-03: Plugin Installation

**Objective**: Verify plugin installation workflow

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-03.1 | Install plugin without auth | 401 Unauthorized | ☐ |
| FT-03.2 | Install non-existent plugin | 404 Not Found | ☐ |
| FT-03.3 | Install plugin with invalid config | 400 Bad Request, validation error | ☐ |
| FT-03.4 | Install plugin with missing required field | 400 Bad Request, "apiKey is required" | ☐ |
| FT-03.5 | Install plugin with valid config | 200 OK, status=INACTIVE | ☐ |
| FT-03.6 | Install same plugin twice | 409 Conflict, "already installed" | ☐ |
| FT-03.7 | Verify installation count incremented | plugin_registry.installation_count increased | ☐ |
| FT-03.8 | Verify configuration stored | installed_plugins.configuration matches input | ☐ |

**Test Script:**
```bash
# FT-03.5: Valid installation
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/install \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://hooks.slack.com/services/TEST/TEST/TEST",
    "channel": "#alerts",
    "username": "SensorVision"
  }'
# Expected: 200, status: "INACTIVE"

# FT-03.6: Install again
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/install \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{ ... }'
# Expected: 409
```

---

### FT-04: Plugin Activation

**Objective**: Verify plugin activation/deactivation

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-04.1 | Activate plugin without auth | 401 Unauthorized | ☐ |
| FT-04.2 | Activate non-installed plugin | 404 Not Found | ☐ |
| FT-04.3 | Activate installed plugin | 200 OK, status=ACTIVE, activatedAt set | ☐ |
| FT-04.4 | Activate already active plugin | 200 OK, no error (idempotent) | ☐ |
| FT-04.5 | Deactivate active plugin | 200 OK, status=INACTIVE | ☐ |
| FT-04.6 | Deactivate inactive plugin | 200 OK, no error (idempotent) | ☐ |
| FT-04.7 | Verify data_plugin record created | data_plugin table has entry with correct plugin_key | ☐ |

**Test Script:**
```bash
# FT-04.3: Activate
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/activate \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200, status: "ACTIVE", activatedAt: "<timestamp>"

# FT-04.5: Deactivate
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/deactivate \
  -H "Authorization: Bearer $TOKEN"
# Expected: 200, status: "INACTIVE"
```

---

### FT-05: Plugin Configuration

**Objective**: Verify configuration updates

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-05.1 | Update config without auth | 401 Unauthorized | ☐ |
| FT-05.2 | Update config for non-installed plugin | 404 Not Found | ☐ |
| FT-05.3 | Update with valid config | 200 OK, configuration updated | ☐ |
| FT-05.4 | Update with invalid config | 400 Bad Request, validation error | ☐ |
| FT-05.5 | Update while plugin is active | 200 OK, plugin reloads with new config | ☐ |
| FT-05.6 | Verify sensitive fields encrypted | Database has encrypted apiKey/webhookUrl | ☐ |

**Test Script:**
```bash
# FT-05.3: Update config
curl -X PUT http://localhost:8080/api/v1/plugins/slack-notifications/configuration \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "webhookUrl": "https://hooks.slack.com/services/NEW/WEBHOOK/URL",
    "channel": "#critical-alerts",
    "username": "SensorVision Bot"
  }'
# Expected: 200, configuration updated
```

---

### FT-06: Plugin Uninstallation

**Objective**: Verify plugin uninstallation

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-06.1 | Uninstall without auth | 401 Unauthorized | ☐ |
| FT-06.2 | Uninstall non-installed plugin | 404 Not Found | ☐ |
| FT-06.3 | Uninstall active plugin | 204 No Content, plugin deactivated and removed | ☐ |
| FT-06.4 | Uninstall inactive plugin | 204 No Content, plugin removed | ☐ |
| FT-06.5 | Verify installation count decremented | plugin_registry.installation_count decreased | ☐ |
| FT-06.6 | Verify installed_plugins record deleted | Database record removed | ☐ |
| FT-06.7 | Verify data_plugin record deleted | data_plugin record removed | ☐ |

**Test Script:**
```bash
# FT-06.3: Uninstall
curl -X DELETE http://localhost:8080/api/v1/plugins/slack-notifications/uninstall \
  -H "Authorization: Bearer $TOKEN"
# Expected: 204

# Verify removed
curl -X GET http://localhost:8080/api/v1/plugins/slack-notifications/installation \
  -H "Authorization: Bearer $TOKEN"
# Expected: 404
```

---

### FT-07: Plugin Ratings

**Objective**: Verify rating and review functionality

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-07.1 | Rate plugin without auth | 401 Unauthorized | ☐ |
| FT-07.2 | Rate non-existent plugin | 404 Not Found | ☐ |
| FT-07.3 | Rate with invalid rating (0) | 400 Bad Request, "must be 1-5" | ☐ |
| FT-07.4 | Rate with invalid rating (6) | 400 Bad Request, "must be 1-5" | ☐ |
| FT-07.5 | Rate plugin (1-5 stars) | 200 OK, rating saved | ☐ |
| FT-07.6 | Rate plugin with review text | 200 OK, rating and review saved | ☐ |
| FT-07.7 | Update existing rating | 200 OK, rating updated (not duplicated) | ☐ |
| FT-07.8 | Verify ratingAverage updated | plugin_registry.rating_average recalculated | ☐ |
| FT-07.9 | Verify ratingCount updated | plugin_registry.rating_count incremented | ☐ |

**Test Script:**
```bash
# FT-07.5: Rate plugin
curl -X POST http://localhost:8080/api/v1/plugins/lorawan-ttn/rate \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "rating": 5,
    "reviewText": "Excellent plugin! Very easy to set up."
  }'
# Expected: 200

# FT-07.8: Verify average updated
curl -X GET http://localhost:8080/api/v1/plugins/lorawan-ttn \
  -H "Authorization: Bearer $TOKEN" \
  | jq '.ratingAverage'
# Expected: Updated average
```

---

### FT-08: Multi-Organization Isolation

**Objective**: Verify plugin installations are organization-scoped

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| FT-08.1 | Org1 installs plugin | Plugin installed for Org1 only | ☐ |
| FT-08.2 | Org2 lists installed plugins | Empty list (Org1's plugin not visible) | ☐ |
| FT-08.3 | Org2 installs same plugin | Plugin installed for Org2 (separate installation) | ☐ |
| FT-08.4 | Org1 activates plugin | Only Org1's instance activated | ☐ |
| FT-08.5 | Org2 sees plugin as inactive | Org2's plugin still inactive | ☐ |
| FT-08.6 | Org1 uninstalls plugin | Only Org1's installation removed | ☐ |
| FT-08.7 | Org2 plugin still exists | Org2's installation unaffected | ☐ |

**Test Script:**
```bash
# Login as Org1 user
TOKEN_ORG1="<org1-token>"

# FT-08.1: Install for Org1
curl -X POST http://localhost:8080/api/v1/plugins/slack-notifications/install \
  -H "Authorization: Bearer $TOKEN_ORG1" \
  -d '{ ... }'

# Login as Org2 user
TOKEN_ORG2="<org2-token>"

# FT-08.2: Check Org2 installations
curl -X GET http://localhost:8080/api/v1/plugins/installed \
  -H "Authorization: Bearer $TOKEN_ORG2"
# Expected: []
```

---

## Integration Tests

### IT-01: Database Integration

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| IT-01.1 | Verify V50 migration applied | Tables exist: plugin_registry, installed_plugins, plugin_ratings | ☐ |
| IT-01.2 | Verify V51 migration applied | 6 plugins seeded in plugin_registry | ☐ |
| IT-01.3 | Test foreign key constraints | Cannot delete plugin_registry if installed_plugins exist | ☐ |
| IT-01.4 | Test JSON Schema validation | Invalid configSchema is rejected | ☐ |
| IT-01.5 | Test JSONB query performance | Filter by config properties performs well | ☐ |

**Test Script:**
```sql
-- IT-01.1: Check tables
SELECT table_name FROM information_schema.tables
WHERE table_schema = 'public' AND table_name LIKE 'plugin%';

-- IT-01.2: Count plugins
SELECT COUNT(*) FROM plugin_registry;
-- Expected: 6

-- IT-01.3: Test constraint
INSERT INTO installed_plugins (plugin_key, organization_id, status, configuration)
VALUES ('nonexistent-key', 1, 'INACTIVE', '{}');
-- Expected: Foreign key violation
```

---

### IT-02: Plugin Execution Integration

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| IT-02.1 | Activate LoRaWAN plugin | data_plugin record created | ☐ |
| IT-02.2 | Send test TTN webhook | Device data parsed and stored (future) | ☐ |
| IT-02.3 | Activate Slack plugin | Notification channel registered | ☐ |
| IT-02.4 | Trigger alert with Slack active | Slack receives notification (future) | ☐ |
| IT-02.5 | Deactivate plugin mid-processing | Graceful shutdown, no errors | ☐ |

---

## UI/UX Tests

### UI-01: Plugin Marketplace Page

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-01.1 | Navigate to /plugin-marketplace | Page loads within 2 seconds | ☐ |
| UI-01.2 | Verify page layout | Header, tabs, search bar, category filter, plugin grid | ☐ |
| UI-01.3 | Verify 6 plugin cards displayed | All 6 official plugins shown | ☐ |
| UI-01.4 | Check plugin card elements | Icon, name, description, author, version, badges, rating | ☐ |
| UI-01.5 | Verify official badges displayed | Blue "Official" badge on all plugins | ☐ |
| UI-01.6 | Verify verified badges displayed | Green checkmark on all plugins | ☐ |
| UI-01.7 | Check responsive design | Layout adapts on mobile, tablet, desktop | ☐ |

---

### UI-02: Search and Filter

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-02.1 | Type "lorawan" in search | Filters to 1 plugin in real-time | ☐ |
| UI-02.2 | Clear search | All 6 plugins reappear | ☐ |
| UI-02.3 | Select "Protocol Parser" category | Shows 3 plugins | ☐ |
| UI-02.4 | Select "Notification" category | Shows 2 plugins | ☐ |
| UI-02.5 | Select "All" category | Shows all 6 plugins | ☐ |
| UI-02.6 | Combine search + filter | Both filters apply correctly | ☐ |
| UI-02.7 | Search with no results | Shows "No plugins found" message | ☐ |

---

### UI-03: Plugin Details Modal

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-03.1 | Click on plugin card | Details modal opens | ☐ |
| UI-03.2 | Verify modal content | Icon, name, description, version, author, tags | ☐ |
| UI-03.3 | Verify external links | Documentation, repository, website links present | ☐ |
| UI-03.4 | Click external link | Opens in new tab | ☐ |
| UI-03.5 | View screenshots (if available) | Screenshot gallery displays | ☐ |
| UI-03.6 | Click "Install" button | Configuration modal opens | ☐ |
| UI-03.7 | Click "Close" or press Esc | Modal closes | ☐ |
| UI-03.8 | Click outside modal | Modal closes (backdrop click) | ☐ |

---

### UI-04: Plugin Configuration Modal

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-04.1 | Open configuration modal | Form generated from JSON Schema | ☐ |
| UI-04.2 | Verify required fields marked | Asterisk (*) shown for required fields | ☐ |
| UI-04.3 | Test text input | Can type and edit text | ☐ |
| UI-04.4 | Test password input | Input masked with dots/asterisks | ☐ |
| UI-04.5 | Test number input | Only accepts numbers | ☐ |
| UI-04.6 | Test boolean checkbox | Can check/uncheck | ☐ |
| UI-04.7 | Test dropdown select | Can select from enum values | ☐ |
| UI-04.8 | Test textarea | Multi-line text input works | ☐ |
| UI-04.9 | Submit with empty required field | Validation error shown | ☐ |
| UI-04.10 | Submit with invalid format | Validation error shown | ☐ |
| UI-04.11 | Submit with valid data | Success toast, modal closes, plugin installed | ☐ |

---

### UI-05: My Plugins Tab

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-05.1 | Switch to "My Plugins" tab | Tab switches smoothly | ☐ |
| UI-05.2 | Initially empty | "No plugins installed" message shown | ☐ |
| UI-05.3 | After installation | Plugin appears in list | ☐ |
| UI-05.4 | Verify plugin card | Name, status badge, action buttons | ☐ |
| UI-05.5 | Status badge for inactive | Gray "Inactive" badge | ☐ |
| UI-05.6 | Status badge for active | Green "Active" badge | ☐ |
| UI-05.7 | Action buttons present | Activate/Deactivate, Configure, Uninstall | ☐ |
| UI-05.8 | Multiple plugins displayed | All installed plugins listed | ☐ |

---

### UI-06: Plugin Lifecycle Actions

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-06.1 | Click "Activate" | Status changes to "Active", success toast | ☐ |
| UI-06.2 | Click "Deactivate" | Status changes to "Inactive", success toast | ☐ |
| UI-06.3 | Click "Configure" | Configuration modal opens with current values | ☐ |
| UI-06.4 | Update configuration | Changes saved, success toast | ☐ |
| UI-06.5 | Click "Uninstall" | Confirmation dialog appears | ☐ |
| UI-06.6 | Confirm uninstall | Plugin removed from list, success toast | ☐ |
| UI-06.7 | Cancel uninstall | Plugin remains in list | ☐ |
| UI-06.8 | Error during action | Error toast with message | ☐ |

---

### UI-07: Notifications and Feedback

| Test Case | Steps | Expected Result | Status |
|-----------|-------|-----------------|--------|
| UI-07.1 | Successful installation | Green success toast with "Plugin installed" | ☐ |
| UI-07.2 | Successful activation | Green success toast with "Plugin activated" | ☐ |
| UI-07.3 | Successful deactivation | Green success toast with "Plugin deactivated" | ☐ |
| UI-07.4 | Successful uninstall | Green success toast with "Plugin uninstalled" | ☐ |
| UI-07.5 | Configuration error | Red error toast with error message | ☐ |
| UI-07.6 | Network error | Red error toast with "Network error" | ☐ |
| UI-07.7 | Toast auto-dismiss | Toast disappears after 3-5 seconds | ☐ |
| UI-07.8 | Toast manual dismiss | Can click X to close toast | ☐ |

---

## Security Tests

### SEC-01: Authentication

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| SEC-01.1 | Access plugin API without token | 401 Unauthorized | ☐ |
| SEC-01.2 | Access plugin API with invalid token | 401 Unauthorized | ☐ |
| SEC-01.3 | Access plugin API with expired token | 401 Unauthorized | ☐ |
| SEC-01.4 | Access plugin UI without login | Redirect to login page | ☐ |

---

### SEC-02: Authorization

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| SEC-02.1 | User from Org1 accesses Org2's plugins | 403 Forbidden or empty results | ☐ |
| SEC-02.2 | User tries to activate another org's plugin | 403 Forbidden | ☐ |
| SEC-02.3 | User tries to view another org's config | 403 Forbidden or null | ☐ |
| SEC-02.4 | Regular user vs admin permissions | All users can install plugins (no restriction) | ☐ |

---

### SEC-03: Input Validation

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| SEC-03.1 | SQL injection in search query | Sanitized, no SQL executed | ☐ |
| SEC-03.2 | XSS in plugin configuration | Sanitized, no script executed | ☐ |
| SEC-03.3 | Very long plugin key (>1000 chars) | 400 Bad Request | ☐ |
| SEC-03.4 | Invalid JSON in configuration | 400 Bad Request, JSON parse error | ☐ |
| SEC-03.5 | Configuration exceeding size limit | 400 Bad Request | ☐ |

---

### SEC-04: Sensitive Data Protection

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| SEC-04.1 | View plugin configuration via API | Sensitive fields (apiKey) encrypted/masked | ☐ |
| SEC-04.2 | Inspect database directly | Sensitive fields encrypted in DB | ☐ |
| SEC-04.3 | Check API response logs | No sensitive data logged | ☐ |
| SEC-04.4 | Check backend application logs | No API keys/passwords logged | ☐ |

---

## Performance Tests

### PERF-01: API Performance

| Test Case | Metric | Target | Actual | Status |
|-----------|--------|--------|--------|--------|
| PERF-01.1 | GET /api/v1/plugins | <200ms | ___ms | ☐ |
| PERF-01.2 | POST /install | <500ms | ___ms | ☐ |
| PERF-01.3 | POST /activate | <300ms | ___ms | ☐ |
| PERF-01.4 | PUT /configuration | <400ms | ___ms | ☐ |
| PERF-01.5 | DELETE /uninstall | <300ms | ___ms | ☐ |

**Test Script:**
```bash
# Measure API latency
time curl -X GET http://localhost:8080/api/v1/plugins \
  -H "Authorization: Bearer $TOKEN"
```

---

### PERF-02: UI Performance

| Test Case | Metric | Target | Actual | Status |
|-----------|--------|--------|--------|--------|
| PERF-02.1 | Initial page load | <2s | ___s | ☐ |
| PERF-02.2 | Tab switch | <100ms | ___ms | ☐ |
| PERF-02.3 | Search/filter | <200ms | ___ms | ☐ |
| PERF-02.4 | Modal open | <100ms | ___ms | ☐ |
| PERF-02.5 | Form submit | <1s | ___s | ☐ |

---

### PERF-03: Scalability

| Test Case | Description | Target | Actual | Status |
|-----------|-------------|--------|--------|--------|
| PERF-03.1 | 100 concurrent users browsing | <2s response | ___s | ☐ |
| PERF-03.2 | 50 concurrent installations | <5s response | ___s | ☐ |
| PERF-03.3 | 1000 plugins in registry | <500ms list query | ___ms | ☐ |
| PERF-03.4 | 100 installations per plugin | <200ms install query | ___ms | ☐ |

---

## Regression Tests

### REG-01: Existing Features

| Test Case | Description | Expected Result | Status |
|-----------|-------------|-----------------|--------|
| REG-01.1 | Device CRUD still works | Can create, read, update, delete devices | ☐ |
| REG-01.2 | Telemetry ingestion works | MQTT messages still processed | ☐ |
| REG-01.3 | Rules engine works | Alerts still triggered | ☐ |
| REG-01.4 | Dashboard loads | Real-time charts display | ☐ |
| REG-01.5 | Serverless functions work | Functions execute correctly | ☐ |
| REG-01.6 | Analytics queries work | Historical data retrieved | ☐ |

---

## Acceptance Criteria

### Must-Have (Blocker if failing)

- [ ] All 39 unit tests passing
- [ ] All 6 official plugins install successfully
- [ ] Plugin activation/deactivation works
- [ ] Configuration updates work
- [ ] Search and filter work
- [ ] No authentication/authorization bypass vulnerabilities
- [ ] No critical UI bugs (crashes, blank screens)

### Should-Have (High priority)

- [ ] All API endpoints return correct status codes
- [ ] All validation errors display helpful messages
- [ ] UI is responsive on mobile/tablet/desktop
- [ ] Performance targets met (API <200ms, UI <2s)
- [ ] Multi-organization isolation works correctly

### Nice-to-Have (Medium priority)

- [ ] Rating system works
- [ ] External links open correctly
- [ ] Screenshots display properly
- [ ] Keyboard shortcuts work
- [ ] Accessibility (WCAG 2.1 AA)

---

## Test Execution Log

### Test Execution #1

**Date**: _____________
**Tester**: _____________
**Environment**: _____________

| Category | Total | Passed | Failed | Blocked | Pass Rate |
|----------|-------|--------|--------|---------|-----------|
| Functional | 60 | ___ | ___ | ___ | ___% |
| Integration | 10 | ___ | ___ | ___ | ___% |
| UI/UX | 45 | ___ | ___ | ___ | ___% |
| Security | 12 | ___ | ___ | ___ | ___% |
| Performance | 13 | ___ | ___ | ___ | ___% |
| Regression | 6 | ___ | ___ | ___ | ___% |
| **TOTAL** | **146** | **___** | **___** | **___** | **___%** |

**Blocker Issues**:
1. _____________
2. _____________

**Critical Issues**:
1. _____________
2. _____________

**Notes**:
_____________________________________________

---

## Defect Tracking

### Template

**Bug ID**: BUG-XXX
**Severity**: Critical / High / Medium / Low
**Priority**: P1 / P2 / P3 / P4
**Status**: Open / In Progress / Fixed / Verified / Closed

**Description**:
_____________________________________________

**Steps to Reproduce**:
1. _____________
2. _____________
3. _____________

**Expected Result**:
_____________________________________________

**Actual Result**:
_____________________________________________

**Environment**:
- Browser: _____________
- OS: _____________
- Backend version: _____________

**Screenshots/Logs**:
_____________________________________________

---

## Sign-Off

### QA Approval

**QA Lead**: _____________ **Date**: _____________
**Signature**: _____________

**Comments**:
_____________________________________________

### Product Owner Approval

**Product Owner**: _____________ **Date**: _____________
**Signature**: _____________

**Comments**:
_____________________________________________

---

**Test Plan Version**: 1.0.0
**Last Updated**: 2025-11-14
**Status**: Ready for Execution
