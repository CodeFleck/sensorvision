# SensorVision Regression Test Suite

**The Most Comprehensive IoT Platform Testing System**

## 📋 Overview

This regression test suite provides 360° coverage of the SensorVision platform:
- **Backend**: Integration tests for all 137 API endpoints
- **Frontend**: E2E tests with visual regression
- **Database**: Migration and data integrity validation
- **MQTT**: Real-time telemetry flow testing
- **WebSocket**: Live data streaming validation
- **Health Monitoring**: Automated system health dashboard

## 🏗️ Test Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Regression Test Suite                     │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌─────────────┐  ┌──────────────┐  ┌──────────────┐       │
│  │   Backend   │  │   Frontend   │  │  Integration │       │
│  │    Tests    │  │   E2E Tests  │  │  Flow Tests  │       │
│  │             │  │              │  │              │       │
│  │ • API Tests │  │ • Playwright │  │ • MQTT→WS    │       │
│  │ • Service   │  │ • Visual     │  │ • Device     │       │
│  │ • Database  │  │   Regression │  │   Lifecycle  │       │
│  │ • Security  │  │ • Navigation │  │ • Rules      │       │
│  └─────────────┘  └──────────────┘  └──────────────┘       │
│                                                               │
│  ┌─────────────────────────────────────────────────────┐   │
│  │          Health Check Dashboard                      │   │
│  │  • Real-time system status                          │   │
│  │  • Test execution history                           │   │
│  │  • Performance metrics                              │   │
│  │  • Visual regression reports                        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## 🧪 Test Categories

### 1. Backend Integration Tests
**Location**: `src/test/java/org/sensorvision/regression/`

- **API Regression Tests**: All REST endpoints
- **Service Layer Tests**: Business logic validation
- **Database Tests**: Migration and query validation
- **Security Tests**: Authentication & authorization
- **Multi-tenancy Tests**: Organization isolation
- **Performance Tests**: Response time benchmarks

### 2. Frontend E2E Tests
**Location**: `frontend/e2e/`

- **User Flow Tests**: Login → Dashboard → Device Management
- **Visual Regression**: Screenshot comparison
- **Component Tests**: All React components
- **Navigation Tests**: Routing validation
- **Form Tests**: Input validation and submission
- **Real-time Tests**: WebSocket data updates

### 3. Integration Flow Tests
**Location**: `regression-tests/flows/`

- **MQTT Ingestion Flow**: Publish → Store → WebSocket → Dashboard
- **Device Lifecycle**: Create → Configure → Monitor → Delete
- **Rules Engine Flow**: Rule → Trigger → Alert → Notification
- **Synthetic Variables**: Expression → Calculate → Display
- **Dashboard Sharing**: Create → Share → View

### 4. Health Check System
**Location**: `regression-tests/health-check/`

- **System Health Dashboard**: Web-based monitoring
- **Automated Health Checks**: Every 5 minutes
- **Alerting**: Email/Slack on failures
- **Historical Tracking**: Trend analysis

## 🚀 Quick Start

### Run All Tests
```bash
./run-regression-tests.sh
```

### Run Specific Test Suites
```bash
# Backend tests only
./gradlew regressionTest

# Frontend E2E tests only
cd frontend && npm run test:e2e

# Integration flow tests
cd regression-tests && npm run test:flows

# Health check
cd regression-tests/health-check && node health-check.js
```

### View Test Results
```bash
# Start health check dashboard
cd regression-tests/health-check && npm run start

# Open browser to http://localhost:3002
```

## 📊 Test Coverage Goals

| Category | Target Coverage | Current |
|----------|----------------|---------|
| Backend Services | 90% | TBD |
| Backend Controllers | 95% | TBD |
| Frontend Components | 80% | TBD |
| E2E Critical Flows | 100% | TBD |
| API Endpoints | 100% | TBD |

## 🎯 Critical Test Scenarios

### Scenario 1: Complete Device Lifecycle
1. Create organization and user
2. Register device with token
3. Send MQTT telemetry
4. Verify data in database
5. Verify WebSocket broadcast
6. Verify dashboard display
7. Screenshot comparison

### Scenario 2: Rules Engine
1. Create device
2. Configure rule (temperature > 80)
3. Send telemetry triggering rule
4. Verify alert created
5. Verify notification sent
6. Verify cooldown period

### Scenario 3: Synthetic Variables
1. Create device with variables (voltage, current)
2. Create synthetic variable (power = voltage * current)
3. Send telemetry
4. Verify synthetic calculation
5. Verify display in dashboard

### Scenario 4: Multi-Tenancy Isolation
1. Create two organizations
2. Create devices in each
3. Verify data isolation
4. Verify API access control
5. Verify dashboard separation

## 🔧 Test Tools & Technologies

- **Backend**: JUnit 5, Spring Boot Test, Testcontainers
- **Frontend**: Playwright, Vitest, Testing Library
- **E2E**: Playwright with video recording
- **Visual Regression**: Playwright screenshots + pixelmatch
- **MQTT Testing**: Eclipse Paho Java Client
- **Database**: Testcontainers PostgreSQL
- **Reporting**: Allure Reports, Custom HTML dashboard

## 📈 Performance Benchmarks

Tests track performance over time:
- API response times (p50, p95, p99)
- Database query performance
- WebSocket latency
- Frontend render times
- MQTT ingestion throughput

## 🐛 Debugging Failed Tests

### View Test Logs
```bash
# Backend test logs
./gradlew regressionTest --info

# Frontend test logs
cd frontend && npm run test:e2e -- --debug

# View screenshots from failed tests
open frontend/test-results/
```

### Run Tests in Debug Mode
```bash
# Backend with debugger
./gradlew regressionTest --debug-jvm

# Frontend with UI
cd frontend && npm run test:e2e -- --ui
```

## 🔄 CI/CD Integration

### GitHub Actions Workflow
Tests run automatically on:
- Every push to main
- Every pull request
- Nightly at 2 AM UTC
- On-demand via workflow_dispatch

### Test Failure Handling
- Failed tests block PR merges
- Screenshots/videos attached to failed test reports
- Slack notifications on failures
- Auto-retry flaky tests (max 3 attempts)

## 📝 Writing New Tests

### Backend Test Template
```java
@SpringBootTest
@AutoConfigureMockMvc
class MyFeatureRegressionTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldTestFeature() {
        // Test implementation
    }
}
```

### Frontend E2E Test Template
```typescript
import { test, expect } from '@playwright/test';

test('feature should work correctly', async ({ page }) => {
    await page.goto('/feature');

    // Take screenshot for visual regression
    await expect(page).toHaveScreenshot('feature-page.png');
});
```

## 🎬 Visual Regression Testing

### How It Works
1. First run creates baseline screenshots
2. Subsequent runs compare against baseline
3. Differences highlighted in report
4. Review and approve/reject changes

### Managing Baselines
```bash
# Update baselines after UI changes
cd frontend && npm run test:e2e -- --update-snapshots

# View visual diff
open frontend/test-results/visual-diff.html
```

## 🚨 Test Failure Triage

### Priority Levels
1. **P0 - Critical**: System down, data loss, security breach
2. **P1 - High**: Major feature broken, API failures
3. **P2 - Medium**: Minor feature issues, visual regressions
4. **P3 - Low**: Cosmetic issues, non-critical paths

### Escalation Path
1. Test fails → Check logs and screenshots
2. If P0/P1 → Slack alert + Email
3. If persistent → Create GitHub issue
4. If blocking → Rollback deployment

## 📊 Metrics & Reporting

### Key Metrics Tracked
- Test execution time trends
- Flaky test detection
- Coverage trends over time
- Performance regression detection
- API response time percentiles

### Reports Generated
- **HTML Report**: Interactive test results
- **JUnit XML**: CI/CD integration
- **Allure Report**: Comprehensive test documentation
- **Visual Diff Report**: Screenshot comparisons
- **Health Dashboard**: Real-time system status

## 🔐 Security Testing

- Authentication bypass attempts
- Authorization boundary testing
- SQL injection prevention
- XSS prevention
- CSRF token validation
- Rate limiting validation
- Device token security

## ⚡ Performance Testing

- Load testing (100 concurrent users)
- Stress testing (1000 devices sending data)
- Endurance testing (24-hour run)
- Spike testing (sudden traffic burst)
- Database query optimization validation

## 🎓 Best Practices

1. **Atomic Tests**: Each test is independent
2. **Fast Feedback**: Critical tests run first
3. **Clear Naming**: Test name describes scenario
4. **Comprehensive Logging**: Debug information on failure
5. **Data Cleanup**: Tests clean up after themselves
6. **Idempotent**: Tests can run multiple times
7. **Deterministic**: No random failures

## 📚 Resources

- [Backend Testing Guide](./docs/testing/backend-testing.md)
- [Frontend Testing Guide](./docs/testing/frontend-testing.md)
- [E2E Testing Guide](./docs/testing/e2e-testing.md)
- [Visual Regression Guide](./docs/testing/visual-regression.md)
- [Performance Testing Guide](./docs/testing/performance-testing.md)

---

**Last Updated**: 2025-10-31
**Maintained By**: SensorVision Team
**Status**: 🟢 Active Development
