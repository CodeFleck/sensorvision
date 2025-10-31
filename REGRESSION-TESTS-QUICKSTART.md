# Regression Tests Quick Start Guide

## 🚀 Running All Tests (One Command)

### Windows:
```bash
run-regression-tests.bat
```

### Linux/Mac:
```bash
./run-regression-tests.sh
```

## 📋 Prerequisites

Make sure all services are running:

```bash
# 1. Start infrastructure (PostgreSQL, MQTT, Prometheus, Grafana)
docker-compose up -d

# 2. Start backend (in one terminal)
./gradlew bootRun

# 3. Start frontend (in another terminal)
cd frontend && npm run dev
```

## 🧪 Run Individual Test Suites

### Backend Tests Only
```bash
# Windows
run-regression-tests.bat backend

# Linux/Mac
./run-regression-tests.sh backend

# Or directly with Gradle
./gradlew test --tests "org.sensorvision.regression.*"
```

### Frontend E2E Tests Only
```bash
# Windows
run-regression-tests.bat frontend

# Linux/Mac
./run-regression-tests.sh frontend

# Or directly with npm
cd frontend && npm run test:e2e
```

### Integration Flow Tests
```bash
# Windows
run-regression-tests.bat flows

# Linux/Mac
./run-regression-tests.sh flows

# Or directly
cd regression-tests/flows && npm test
```

### Health Check
```bash
# Windows
run-regression-tests.bat health

# Linux/Mac
./run-regression-tests.sh health

# Or directly
cd regression-tests/health-check && npm run check
```

## 📊 Viewing Test Reports

After running tests, open these HTML reports in your browser:

### Backend Test Report
```
build/reports/tests/test/index.html
```

### Frontend E2E Test Report
```
frontend/test-results/html/index.html
```

### Health Check Dashboard
```
regression-tests/health-check/health-report.html
```

### Screenshots & Videos (for failed E2E tests)
```
frontend/test-results/
```

## 🎯 What Gets Tested

### ✅ Backend Integration Tests
- **Authentication System**: Login, logout, session management
- **Device Lifecycle**: Create, read, update, delete operations
- **Telemetry Ingestion**: MQTT data processing and storage
- **Rules Engine**: Alert triggering and evaluation
- **Synthetic Variables**: Expression calculation
- **Analytics**: Data aggregation and reporting
- **Dashboard Management**: CRUD operations
- **Multi-Tenancy**: Organization isolation
- **Device Health Scores**: Health calculation
- **System Health**: Actuator endpoints

### ✅ Frontend E2E Tests (with Visual Regression)
- **Authentication Flow**: Login page, credentials validation
- **Device Management**: List, create, edit, delete, token generation
- **Dashboard**: Real-time updates, charts, widgets
- **Rules & Alerts**: Create rules, view alerts, acknowledge
- **Navigation**: All routes and user flows
- **Visual Regression**: Screenshot comparison for UI changes

### ✅ Integration Flow Tests
- **MQTT → Backend**: Message publishing and reception
- **Backend Processing**: Data transformation and storage
- **Database Storage**: Record creation and integrity
- **WebSocket Broadcasting**: Real-time data streaming
- **Frontend Updates**: Live dashboard updates
- **End-to-End Latency**: Performance measurement

### ✅ Health Check System
- **Backend Health**: Liveness and readiness probes
- **Database**: Connection, schema, data verification
- **MQTT Broker**: Connection validation
- **WebSocket Server**: Connection testing
- **Frontend**: Availability check
- **API Endpoints**: All critical endpoints
- **System Metrics**: Device counts, telemetry records

## 🔄 Continuous Monitoring

Run health checks continuously (every 5 minutes):

```bash
cd regression-tests/health-check
npm run check:continuous
```

## 🐛 Debugging Failed Tests

### View Backend Test Logs
```bash
./gradlew test --tests "org.sensorvision.regression.*" --info
```

### Run Frontend Tests in Debug Mode
```bash
cd frontend
npm run test:e2e:debug
```

### Run Frontend Tests with UI
```bash
cd frontend
npm run test:e2e:ui
```

### View Screenshots from Failed Tests
```bash
# Windows
explorer frontend\test-results

# Linux/Mac
open frontend/test-results/
```

## 📈 Performance Benchmarks

The test suite tracks:
- API response times (p50, p95, p99)
- Database query performance
- WebSocket latency
- Frontend render times
- End-to-end flow duration

All metrics are included in test reports.

## ✨ Visual Regression Testing

### How It Works
1. First test run creates baseline screenshots
2. Subsequent runs compare against baselines
3. Differences are highlighted in reports
4. Manual approval/rejection workflow

### Update Visual Baselines
After intentional UI changes, update baselines:

```bash
cd frontend
npm run test:e2e -- --update-snapshots
```

## 🚨 CI/CD Integration

Tests are designed to run in CI/CD pipelines:

- **Exit Codes**: 0 = all pass, 1 = any failure
- **Retries**: Automatic retry for flaky tests (max 3)
- **Artifacts**: Screenshots, videos, HTML reports
- **Notifications**: Test results can trigger alerts

### GitHub Actions Example
```yaml
- name: Run Regression Tests
  run: ./run-regression-tests.sh

- name: Upload Test Reports
  if: always()
  uses: actions/upload-artifact@v3
  with:
    name: test-reports
    path: |
      build/reports/
      frontend/test-results/
      regression-tests/health-check/health-report.html
```

## 💡 Tips

1. **Run Tests Frequently**: Catch regressions early
2. **Check Screenshots**: Visual regressions are important
3. **Monitor Performance**: Watch for performance degradation
4. **Update Baselines**: After intentional UI changes
5. **Review Health Checks**: Before deployments
6. **Use Specific Tests**: During development to save time

## 📞 Troubleshooting

### "Service not running" errors
- Make sure docker-compose is up
- Verify backend is running on port 8080
- Verify frontend is running on port 3001

### "Connection refused" errors
- Check firewall settings
- Verify ports are not blocked
- Ensure services started successfully

### "Test timeout" errors
- Increase timeout values if needed
- Check system resources (CPU, memory)
- Verify no other processes using ports

### Visual regression failures
- Review screenshots to confirm changes
- Update baselines if changes are intentional
- Check for dynamic content causing false positives

## 🎓 Best Practices

1. **Run Before Committing**: Ensure your changes don't break anything
2. **Run Before Deploying**: Final validation before production
3. **Monitor Health Checks**: Set up continuous monitoring
4. **Review Reports**: Don't just look at pass/fail, review details
5. **Keep Tests Updated**: Update tests when features change
6. **Document Changes**: Note any test updates in commits

---

**Need Help?** Check the comprehensive documentation in [TESTING.md](./TESTING.md)

**Last Updated**: 2025-10-31
