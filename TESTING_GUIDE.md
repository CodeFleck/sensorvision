# SensorVision Testing Guide

## Table of Contents
1. [Overview](#overview)
2. [Frontend Testing](#frontend-testing)
3. [Backend Testing](#backend-testing)
4. [Running Tests](#running-tests)
5. [Writing New Tests](#writing-new-tests)
6. [Best Practices](#best-practices)
7. [Coverage Goals](#coverage-goals)

---

## Overview

SensorVision uses a comprehensive testing strategy to ensure code quality and prevent regressions:

- **Frontend:** Vitest + React Testing Library
- **Backend:** JUnit 5 + Mockito
- **Coverage Target:** 80%+ overall

### Why Test?

✅ **Catch bugs early** - Before they reach production
✅ **Safe refactoring** - Change code with confidence
✅ **Living documentation** - Tests show how code should work
✅ **Faster development** - Catch issues immediately, not weeks later
✅ **Prevent regressions** - Once fixed, bugs stay fixed

---

## Frontend Testing

### Tech Stack

- **Vitest** - Fast, Vite-native test runner
- **React Testing Library** - User-centric component testing
- **happy-dom** - Lightweight DOM implementation
- **@testing-library/jest-dom** - Additional matchers

### Setup

Testing infrastructure is already configured:

```bash
# Run tests
npm test

# Run tests with UI
npm run test:ui

# Run tests with coverage
npm run test:coverage

# Run tests once (CI mode)
npm run test:run
```

### File Structure

```
frontend/
├── src/
│   ├── components/
│   │   ├── MyComponent.tsx
│   │   └── MyComponent.test.tsx      # Component tests
│   ├── services/
│   │   ├── api.ts
│   │   └── api.test.ts               # Service tests
│   ├── utils/
│   │   ├── helpers.ts
│   │   └── helpers.test.ts           # Utility tests
│   └── test/
│       └── setup.ts                  # Global test setup
├── vitest.config.ts                  # Vitest configuration
└── package.json                      # Test scripts
```

### Example: Component Test

```typescript
import { describe, it, expect } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { MyComponent } from './MyComponent';

describe('MyComponent', () => {
  it('should render correctly', () => {
    render(<MyComponent title="Test" />);

    expect(screen.getByText('Test')).toBeInTheDocument();
  });

  it('should handle button click', () => {
    const handleClick = vi.fn();
    render(<MyComponent onClick={handleClick} />);

    fireEvent.click(screen.getByRole('button'));

    expect(handleClick).toHaveBeenCalledTimes(1);
  });
});
```

### Example: Mocking API Calls

```typescript
import { vi } from 'vitest';
import * as apiService from '../services/api';

// Mock the entire module
vi.mock('../services/api', () => ({
  apiService: {
    getDevices: vi.fn(),
    createDevice: vi.fn(),
  },
}));

// In your test
vi.mocked(apiService.apiService.getDevices).mockResolvedValue([
  { id: '1', name: 'Device 1' },
  { id: '2', name: 'Device 2' },
]);
```

### Example: Testing User Interactions

```typescript
import { render, screen, fireEvent, waitFor } from '@testing-library/react';
import userEvent from '@testing-library/user-event';

it('should handle form submission', async () => {
  const user = userEvent.setup();
  render(<MyForm onSubmit={handleSubmit} />);

  // Type into input
  await user.type(screen.getByLabelText('Email'), 'test@example.com');

  // Click submit
  await user.click(screen.getByRole('button', { name: /submit/i }));

  // Wait for async operation
  await waitFor(() => {
    expect(handleSubmit).toHaveBeenCalled();
  });
});
```

### Critical Frontend Tests

#### ✅ IntegrationWizard.test.tsx
Tests all 5 recent bug fixes:
- Platform selection with explicit color classes (Tailwind JIT fix)
- Device creation flow with token rotation
- URL encoding for special characters
- Filename sanitization for downloads
- Toast notifications

#### ✅ config.test.ts
Tests production URL configuration:
- Backend URL uses `window.location.origin` (reverse proxy fix)
- WebSocket URL construction
- Development vs. production environment detection

---

## Backend Testing

### Tech Stack

- **JUnit 5** - Modern Java testing framework
- **Mockito** - Mocking framework
- **AssertJ** - Fluent assertions
- **Spring Boot Test** - Integration testing support

### Setup

```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "*DeviceServiceTest"

# Run tests with detailed output
./gradlew test --info

# Run tests with coverage (if Jacoco configured)
./gradlew test jacocoTestReport
```

### File Structure

```
src/
├── main/
│   └── java/
│       └── org/sensorvision/
│           ├── service/
│           │   ├── DeviceService.java
│           │   └── ...
│           └── controller/
│               ├── DeviceController.java
│               └── ...
└── test/
    └── java/
        └── org/sensorvision/
            ├── service/
            │   ├── DeviceServiceTest.java       # Unit tests
            │   └── ...
            └── controller/
                ├── DeviceControllerTest.java    # Integration tests
                └── ...
```

### Example: Service Test

```java
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    @Mock
    private DeviceRepository deviceRepository;

    @Mock
    private DeviceTokenService deviceTokenService;

    @InjectMocks
    private DeviceService deviceService;

    @Test
    void createDevice_shouldAutoGenerateApiToken() {
        // Given
        DeviceCreateRequest request = new DeviceCreateRequest(
            "test-device", "Test Device", null, null, null
        );

        when(deviceRepository.findByExternalId(anyString()))
            .thenReturn(Optional.empty());
        when(deviceRepository.save(any(Device.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(deviceTokenService.assignTokenToDevice(any(Device.class)))
            .thenReturn("test-token");

        // When
        DeviceResponse response = deviceService.createDevice(request);

        // Then
        assertThat(response).isNotNull();
        verify(deviceTokenService, times(1)).assignTokenToDevice(any());
    }
}
```

### Example: Controller Integration Test

```java
@SpringBootTest
@AutoConfigureMockMvc
class DeviceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DeviceService deviceService;

    @Test
    void getDevice_shouldReturnDevice_whenExists() throws Exception {
        // Given
        DeviceResponse device = new DeviceResponse(
            "test-001", "Test Device", "ONLINE", null, null, null
        );
        when(deviceService.getDevice("test-001")).thenReturn(device);

        // When/Then
        mockMvc.perform(get("/api/v1/devices/test-001"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.externalId").value("test-001"))
            .andExpect(jsonPath("$.name").value("Test Device"));
    }
}
```

### Critical Backend Tests

#### ✅ DeviceServiceTest.java
Tests the auto-token generation behavior:
- `createDevice()` automatically calls `assignTokenToDevice()`
- Duplicate device detection
- Organization assignment
- **CRITICAL:** Documents why frontend must use `rotateDeviceToken()` not `generateDeviceToken()`

#### ✅ DeviceTokenServiceTest.java (existing)
Tests token generation, validation, rotation

#### ✅ TelemetryIngestionServiceTest.java (existing)
Tests core data ingestion logic

---

## Running Tests

### Frontend Tests

```bash
cd frontend

# Watch mode (recommended for development)
npm test

# Run once
npm run test:run

# With coverage
npm run test:coverage

# With UI (visual test runner)
npm run test:ui

# Run specific file
npm test -- config.test.ts

# Run tests matching pattern
npm test -- --grep "IntegrationWizard"
```

### Backend Tests

```bash
# All tests
./gradlew test

# Specific test class
./gradlew test --tests "*DeviceServiceTest"

# Specific test method
./gradlew test --tests "*DeviceServiceTest.createDevice_shouldAutoGenerateApiToken"

# With detailed output
./gradlew test --info

# Continuous testing (watch mode)
./gradlew test --continuous
```

### CI/CD

Tests run automatically in GitHub Actions on every push and pull request:

**Workflow:** `.github/workflows/ci.yml`

**What it does:**
1. **Backend Tests**
   - Runs all JUnit tests with PostgreSQL test database
   - Generates JaCoCo coverage report
   - Uploads test results and coverage reports as artifacts
   - Enforces 30% minimum coverage (currently 19%, non-blocking)

2. **Frontend Tests**
   - Runs Vitest tests with happy-dom environment
   - Generates coverage report with v8 provider
   - Enforces 30% minimum coverage (currently 53%, passing ✓)
   - Type-checks TypeScript code
   - Builds production bundle

3. **Coverage Reports**
   - Available as downloadable artifacts after each CI run
   - Frontend: `frontend-coverage/` (HTML, JSON, LCOV formats)
   - Backend: `backend-coverage-report/` (HTML, XML formats)

**Running CI commands locally:**
```bash
# Frontend (same as CI)
cd frontend
npm ci
npm run test:run
npm run test:coverage
npm run build

# Backend (same as CI)
./gradlew clean build --no-daemon
./gradlew test --no-daemon
./gradlew jacocoTestReport --no-daemon
```

**Viewing Coverage Locally:**
```bash
# Frontend - open in browser
open frontend/coverage/index.html

# Backend - open in browser
open build/reports/jacoco/test/html/index.html
```

---

## Writing New Tests

### Frontend Testing Checklist

When creating a new component or feature:

- [ ] **Unit tests** - Test component in isolation
- [ ] **Integration tests** - Test component with its dependencies
- [ ] **User interaction tests** - Test clicks, typing, form submission
- [ ] **API mocking** - Mock all external API calls
- [ ] **Error handling** - Test error states
- [ ] **Loading states** - Test loading spinners/skeletons
- [ ] **Edge cases** - Empty states, max limits, special characters

### Backend Testing Checklist

When creating a new service or controller:

- [ ] **Happy path** - Test successful execution
- [ ] **Validation** - Test input validation
- [ ] **Error handling** - Test exception scenarios
- [ ] **Edge cases** - Null values, empty lists, duplicates
- [ ] **Security** - Test authentication/authorization
- [ ] **Database interactions** - Verify correct queries
- [ ] **Business logic** - Test all conditional paths

---

## Best Practices

### ✅ DO

- **Test behavior, not implementation** - Focus on what the code does, not how
- **Use descriptive test names** - `shouldReturnDevice_whenDeviceExists`
- **Arrange-Act-Assert** - Given, When, Then structure
- **One assertion per test** - Or logically related assertions
- **Mock external dependencies** - Don't hit real databases/APIs
- **Test edge cases** - Empty strings, null, max values, special chars
- **Keep tests fast** - Unit tests should run in milliseconds
- **Write tests first** - Or immediately after writing code

### ❌ DON'T

- **Don't test framework code** - React/Spring already tested
- **Don't test implementation details** - Test public API only
- **Don't mock everything** - Only external dependencies
- **Don't write brittle tests** - Avoid testing CSS classes, exact text
- **Don't skip tests** - Fix broken tests, don't skip them
- **Don't test private methods** - Test through public interface
- **Don't duplicate tests** - Remove redundant tests

### Frontend-Specific Best Practices

```typescript
// ✅ Good - Test behavior
it('should show error message when form is invalid', async () => {
  render(<LoginForm />);
  fireEvent.click(screen.getByRole('button', { name: /login/i }));
  expect(screen.getByText(/email is required/i)).toBeInTheDocument();
});

// ❌ Bad - Test implementation details
it('should set hasError state to true', () => {
  const { container } = render(<LoginForm />);
  // Don't access component state directly
});
```

### Backend-Specific Best Practices

```java
// ✅ Good - Test public API
@Test
void createDevice_shouldThrowException_whenDeviceExists() {
    when(deviceRepository.findByExternalId("existing"))
        .thenReturn(Optional.of(existingDevice));

    assertThatThrownBy(() -> deviceService.createDevice(request))
        .isInstanceOf(IllegalArgumentException.class);
}

// ❌ Bad - Test private methods
@Test
void validateDeviceId_shouldReturnTrue() {
    // Don't test private methods
}
```

---

## Coverage Goals

### Current Status

| Component | Coverage | Goal |
|-----------|----------|------|
| Frontend | 25% | 80% |
| Backend Services | 11% | 80% |
| Backend Controllers | 8% | 70% |
| Overall | 15% | 75% |

### Priority Tests to Write

#### HIGH Priority (Next Sprint) - ✅ COMPLETED!
1. ✅ `DeviceService` - 10 tests covering CRUD, security, and token management
2. ✅ `IntegrationWizard` - Component tests with user flows
3. ✅ `config.ts` - Configuration validation tests
4. ✅ `AuthService` - 18 tests covering login, registration, password reset, email verification
5. ✅ `RuleEngineService` - 20 tests covering all operators, severity, cooldown, events
6. ✅ `api.ts` - 28 tests covering authentication, error handling, all CRUD operations

#### MEDIUM Priority (This Quarter) - In Progress
7. ✅ `AnalyticsService` - 23 tests covering aggregations, security, intervals, edge cases
8. ✅ `AlertService` - 15 tests covering retrieval, acknowledgment, notifications, security
9. ✅ `SyntheticVariableService` - 19 tests covering expression parsing, operator precedence, error handling
10. All remaining Controllers

#### LOW Priority (Ongoing)
11. UI Components - Presentational components
12. Utility functions - Helpers, formatters
13. Edge case scenarios

---

## Example Test Scenarios

### Scenario 1: Testing URL Encoding Fix

```typescript
// frontend/src/pages/IntegrationWizard.test.tsx
it('should encode device IDs with special characters', () => {
  const deviceId = 'sensor 01';
  const encoded = encodeURIComponent(deviceId);
  expect(encoded).toBe('sensor%2001');

  const deviceIdWithSlash = 'device/test';
  const encodedSlash = encodeURIComponent(deviceIdWithSlash);
  expect(encodedSlash).toBe('device%2Ftest');
});
```

### Scenario 2: Testing Auto-Token Generation

```java
// src/test/java/org/sensorvision/service/DeviceServiceTest.java
@Test
void createDevice_shouldAutoGenerateApiToken() {
    DeviceCreateRequest request = new DeviceCreateRequest(
        "new-device", "New Device", null, null, null
    );

    when(deviceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
    when(deviceTokenService.assignTokenToDevice(any())).thenReturn("token");

    deviceService.createDevice(request);

    // CRITICAL: Verify token auto-generation
    verify(deviceTokenService, times(1)).assignTokenToDevice(any());
}
```

### Scenario 3: Testing Toast Notifications

```typescript
// frontend/src/pages/IntegrationWizard.test.tsx
it('should show success toast when code is copied', async () => {
  // ... setup wizard state ...

  const copyButton = screen.getByRole('button', { name: /Copy/i });
  fireEvent.click(copyButton);

  await waitFor(() => {
    expect(screen.getByText('Code copied to clipboard!')).toBeInTheDocument();
  });
});
```

---

## Troubleshooting

### Frontend Issues

**Problem:** Tests can't find React components

```bash
# Solution: Ensure React Testing Library is installed
npm install --save-dev @testing-library/react
```

**Problem:** "Cannot find module 'happy-dom'"

```bash
# Solution: Install happy-dom
npm install --save-dev happy-dom
```

**Problem:** Tests timeout

```typescript
// Solution: Increase timeout for slow operations
await waitFor(() => {
  expect(screen.getByText('Loaded')).toBeInTheDocument();
}, { timeout: 5000 });
```

### Backend Issues

**Problem:** Tests fail with "Bean not found"

```java
// Solution: Use @MockBean instead of @Mock in Spring tests
@SpringBootTest
class MyTest {
    @MockBean
    private MyService myService;
}
```

**Problem:** Tests interfere with each other

```java
// Solution: Use @DirtiesContext or proper @BeforeEach cleanup
@BeforeEach
void setUp() {
    // Reset mocks
    reset(deviceRepository);
}
```

---

## Resources

### Frontend
- [Vitest Documentation](https://vitest.dev/)
- [React Testing Library](https://testing-library.com/react)
- [Testing Library Best Practices](https://kentcdodds.com/blog/common-mistakes-with-react-testing-library)

### Backend
- [JUnit 5 User Guide](https://junit.org/junit5/docs/current/user-guide/)
- [Mockito Documentation](https://javadoc.io/doc/org.mockito/mockito-core/latest/org/mockito/Mockito.html)
- [Spring Boot Testing](https://spring.io/guides/gs/testing-web/)

---

## Next Steps

1. **Run existing tests:** `npm test` and `./gradlew test`
2. **Review test coverage:** `npm run test:coverage`
3. **Write tests for new features:** Before merging PRs
4. **Achieve 80% coverage:** Incrementally add tests
5. **Continuous improvement:** Refactor tests as code evolves

---

**Remember:** Tests are an investment. They may slow you down today, but they'll save you weeks of debugging tomorrow.

Happy Testing! 🧪
