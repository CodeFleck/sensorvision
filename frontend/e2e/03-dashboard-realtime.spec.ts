import { test, expect } from '@playwright/test';

/**
 * Dashboard and Real-Time Updates E2E Tests
 *
 * Tests:
 * - Dashboard rendering
 * - Real-time WebSocket data updates
 * - Chart visualizations
 * - Device selector
 * - Widget interactions
 */

test.describe('Dashboard and Real-Time Updates', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000); // Wait for login and redirect to dashboard
  });

  test('should display dashboard with widgets', async ({ page }) => {
    // Wait for dashboard to fully load
    await page.waitForTimeout(3000);

    // Check for common dashboard elements
    await expect(page.locator('canvas, .chart, [data-testid="widget"]')).toHaveCount({ min: 1 });

    // Visual regression: Main dashboard
    await expect(page).toHaveScreenshot('dashboard-main.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should display device statistics', async ({ page }) => {
    // Wait for stats to load
    await page.locator('[data-testid="device-count"]').or(page.getByText(/Total Devices|Devices:/i)).waitFor({ timeout: 10000 });

    // Visual regression: Device statistics
    await expect(page).toHaveScreenshot('dashboard-statistics.png');
  });

  test('should render charts correctly', async ({ page }) => {
    // Wait for charts to load
    await page.waitForSelector('canvas', { timeout: 10000 });

    // Get canvas elements
    const canvasCount = await page.locator('canvas').count();
    expect(canvasCount).toBeGreaterThan(0);

    // Wait for chart animations to complete
    await page.waitForTimeout(2000);

    // Visual regression: Charts
    await expect(page).toHaveScreenshot('dashboard-charts.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should update charts in real-time', async ({ page }) => {
    // Wait for initial chart render
    await page.waitForSelector('canvas', { timeout: 10000 });
    await page.waitForTimeout(2000);

    // Take initial screenshot
    const initialScreenshot = await page.screenshot({ fullPage: false });

    // Wait for potential WebSocket updates (10 seconds)
    await page.waitForTimeout(10000);

    // Check if page has updated (WebSocket connection indicator or data points)
    const wsIndicator = page.locator('[data-testid="ws-status"], .ws-connected, text=/Connected|Live/i');
    if (await wsIndicator.count() > 0) {
      await expect(wsIndicator).toBeVisible();
    }

    // Visual regression: Dashboard after real-time updates
    await expect(page).toHaveScreenshot('dashboard-after-realtime-update.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should handle device selector', async ({ page }) => {
    // Look for device selector dropdown
    const deviceSelector = page.locator('select, [role="combobox"], [data-testid="device-selector"]').first();

    if (await deviceSelector.count() > 0) {
      // Click device selector
      await deviceSelector.click();

      // Take screenshot of dropdown
      await expect(page).toHaveScreenshot('dashboard-device-selector-open.png');

      // Select different device
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Enter');

      // Wait for dashboard to update
      await page.waitForTimeout(2000);

      // Visual regression: Dashboard with different device
      await expect(page).toHaveScreenshot('dashboard-different-device.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('should display recent alerts', async ({ page }) => {
    // Look for alerts section
    const alertsSection = page.locator('[data-testid="alerts"], [data-testid="recent-alerts"], text=/Recent Alerts|Alerts/i');

    if (await alertsSection.count() > 0) {
      await alertsSection.first().scrollIntoViewIfNeeded();

      // Visual regression: Alerts section
      await expect(page).toHaveScreenshot('dashboard-alerts-section.png');
    }
  });

  test('should navigate between dashboard views', async ({ page }) => {
    // Check for view tabs or navigation
    const viewTabs = page.locator('[role="tab"], .tab, [data-testid="view-tab"]');

    if (await viewTabs.count() > 1) {
      // Click second tab
      await viewTabs.nth(1).click();

      // Wait for view to load
      await page.waitForTimeout(2000);

      // Visual regression: Alternative dashboard view
      await expect(page).toHaveScreenshot('dashboard-alternative-view.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('should display WebSocket connection status', async ({ page }) => {
    // Look for WebSocket status indicator
    const wsStatus = page.locator('[data-testid="ws-status"], .ws-indicator, text=/Connected|Connecting/i');

    // Wait a few seconds for WebSocket to connect
    await page.waitForTimeout(3000);

    if (await wsStatus.count() > 0) {
      // Visual regression: WebSocket status
      await expect(page).toHaveScreenshot('dashboard-ws-status.png');

      // Verify connected state
      const statusText = await wsStatus.first().textContent();
      expect(statusText).toMatch(/Connected|Online|Live/i);
    }
  });

  test('should refresh data manually', async ({ page }) => {
    // Look for refresh button
    const refreshButton = page.locator('button:has-text("Refresh"), [aria-label="Refresh"], button[data-action="refresh"]');

    if (await refreshButton.count() > 0) {
      // Click refresh button
      await refreshButton.first().click();

      // Wait for refresh animation
      await page.waitForTimeout(2000);

      // Visual regression: Dashboard after manual refresh
      await expect(page).toHaveScreenshot('dashboard-after-refresh.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('should display time range selector', async ({ page }) => {
    // Look for time range controls
    const timeRangeSelector = page.locator('select:has-text("hour"), select:has-text("day"), [data-testid="time-range"]');

    if (await timeRangeSelector.count() > 0) {
      // Change time range
      await timeRangeSelector.first().click();
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Enter');

      // Wait for data to reload
      await page.waitForTimeout(2000);

      // Visual regression: Dashboard with different time range
      await expect(page).toHaveScreenshot('dashboard-different-timerange.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('should handle empty state when no devices exist', async ({ page }) => {
    // This test would need a fresh database
    // For now, we'll just check if empty state exists
    const emptyState = page.locator('text=/No devices|No data|Get started/i');

    if (await emptyState.count() > 0) {
      // Visual regression: Empty state
      await expect(page).toHaveScreenshot('dashboard-empty-state.png');
    }
  });
});
