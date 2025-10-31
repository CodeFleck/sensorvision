import { defineConfig, devices } from '@playwright/test';

/**
 * Comprehensive Playwright E2E Test Configuration
 *
 * Features:
 * - Visual regression testing with screenshot comparison
 * - Video recording on failure
 * - Trace collection for debugging
 * - Multi-browser testing (Chromium, Firefox, WebKit)
 * - Retries for flaky tests
 * - Parallelization for speed
 */
export default defineConfig({
  testDir: './e2e',

  // Maximum time one test can run
  timeout: 60 * 1000,

  // Test configuration
  fullyParallel: true,
  forbidOnly: !!process.env.CI,

  // Retry failed tests
  retries: process.env.CI ? 2 : 1,

  // Parallel workers
  workers: process.env.CI ? 1 : undefined,

  // Reporter configuration
  reporter: [
    ['html', { outputFolder: 'test-results/html' }],
    ['json', { outputFile: 'test-results/results.json' }],
    ['junit', { outputFile: 'test-results/junit.xml' }],
    ['list'],
  ],

  use: {
    // Base URL for tests
    baseURL: 'http://localhost:3001',

    // Collect trace on failure
    trace: 'on-first-retry',

    // Take screenshot on failure
    screenshot: 'only-on-failure',

    // Record video on failure
    video: 'retain-on-failure',

    // Browser viewport
    viewport: { width: 1920, height: 1080 },

    // Ignore HTTPS errors
    ignoreHTTPSErrors: true,
  },

  // Configure projects for different browsers
  projects: [
    {
      name: 'chromium',
      use: { ...devices['Desktop Chrome'] },
    },
    {
      name: 'firefox',
      use: { ...devices['Desktop Firefox'] },
    },
    {
      name: 'webkit',
      use: { ...devices['Desktop Safari'] },
    },
    // Mobile browsers
    {
      name: 'mobile-chrome',
      use: { ...devices['Pixel 5'] },
    },
    {
      name: 'mobile-safari',
      use: { ...devices['iPhone 13'] },
    },
  ],

  // Web server configuration
  webServer: {
    command: 'npm run dev',
    url: 'http://localhost:3001',
    reuseExistingServer: !process.env.CI,
    timeout: 120 * 1000,
  },
});
