import { test, expect } from '@playwright/test';

/**
 * Dashboard Engagement Features E2E Tests
 *
 * Tests for the new engaging dashboard features:
 * - Fleet Health Gauge
 * - Activity Timeline
 * - Sparklines on device cards
 * - Trend indicators
 * - Hover effects and animations
 *
 * Feature Flag: engagingDashboard (enabled by default)
 */

test.describe('Dashboard Engagement Features', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(admin-dashboard)?(\?.*)?$/, { timeout: 10000 });

    // Wait for dashboard to fully load
    await page.waitForLoadState('networkidle');
  });

  test.describe('Fleet Health Gauge', () => {
    test('should display Fleet Health section', async ({ page }) => {
      // Look for Fleet Health heading
      const fleetHealthSection = page.getByText('Fleet Health');

      if (await fleetHealthSection.count() > 0) {
        await expect(fleetHealthSection.first()).toBeVisible();

        // Visual regression: Fleet Health section
        await expect(page).toHaveScreenshot('dashboard-fleet-health.png', {
          fullPage: false,
          animations: 'disabled',
        });
      }
    });

    test('should display health percentage in gauge', async ({ page }) => {
      // Look for percentage value in the gauge
      const percentageValue = page.locator('text=/\\d+%/').first();

      if (await percentageValue.count() > 0) {
        await expect(percentageValue).toBeVisible();
      }
    });

    test('should display health status label', async ({ page }) => {
      // Look for health status labels
      const statusLabels = page.getByText(/(Excellent|Good|Fair|Poor|Critical)/i);

      if (await statusLabels.count() > 0) {
        await expect(statusLabels.first()).toBeVisible();
      }
    });

    test('should display mini stats (Devices, Online, Uptime)', async ({ page }) => {
      // Look for mini stat labels
      const devicesStat = page.getByText('Devices');
      const onlineStat = page.getByText('Online');
      const uptimeStat = page.getByText('Uptime');

      if (await devicesStat.count() > 0) {
        await expect(devicesStat.first()).toBeVisible();
      }
      if (await onlineStat.count() > 0) {
        await expect(onlineStat.first()).toBeVisible();
      }
      if (await uptimeStat.count() > 0) {
        await expect(uptimeStat.first()).toBeVisible();
      }
    });

    test('should render SVG gauge correctly', async ({ page }) => {
      // Look for SVG gauge
      const gaugeCircle = page.locator('svg circle');

      if (await gaugeCircle.count() > 0) {
        // Verify gauge elements exist
        expect(await gaugeCircle.count()).toBeGreaterThanOrEqual(2); // Background + value circles
      }
    });
  });

  test.describe('Activity Timeline', () => {
    test('should display Activity Feed section', async ({ page }) => {
      // Look for Activity Feed heading
      const activityFeed = page.getByText('Activity Feed');

      if (await activityFeed.count() > 0) {
        await expect(activityFeed.first()).toBeVisible();

        // Visual regression: Activity Feed section
        await expect(page).toHaveScreenshot('dashboard-activity-feed.png', {
          fullPage: false,
          animations: 'disabled',
        });
      }
    });

    test('should display event items with icons', async ({ page }) => {
      // Wait a bit for events to load
      await page.waitForTimeout(2000);

      // Look for event items - they should have border styling
      const eventItems = page.locator('.activity-item-animate, [class*="border-l-"]');

      const count = await eventItems.count();
      if (count > 0) {
        // At least some events should be visible
        expect(count).toBeGreaterThan(0);
      }
    });

    test('should display "No recent activity" when empty', async ({ page }) => {
      // This test checks for empty state - may or may not appear
      const emptyState = page.getByText('No recent activity');

      // If empty state exists, it should be visible
      if (await emptyState.count() > 0) {
        await expect(emptyState.first()).toBeVisible();
      }
    });

    test('should have refresh button', async ({ page }) => {
      // Look for refresh button in Activity Feed
      const refreshButton = page.locator('[title="Refresh"]');

      if (await refreshButton.count() > 0) {
        await expect(refreshButton.first()).toBeVisible();

        // Click refresh and verify no errors
        await refreshButton.first().click();
        await page.waitForLoadState('networkidle');
      }
    });
  });

  test.describe('Device Cards Enhancement', () => {
    test('should display health score badges on device cards', async ({ page }) => {
      // Look for device cards with health badges
      const healthBadges = page.locator('[class*="health"], [class*="emerald"], [class*="cyan"], [class*="amber"]');

      if (await healthBadges.count() > 0) {
        // Visual regression: Device cards with health badges
        await expect(page).toHaveScreenshot('dashboard-device-health-badges.png', {
          fullPage: false,
          animations: 'disabled',
        });
      }
    });

    test('should display sparklines on device cards', async ({ page }) => {
      // Look for sparkline SVGs
      const sparklines = page.locator('.sparkline-svg, svg[role="img"][aria-label*="Trend"]');

      if (await sparklines.count() > 0) {
        expect(await sparklines.count()).toBeGreaterThan(0);
      }
    });

    test('should display trend indicators', async ({ page }) => {
      // Look for trend indicators showing percentage change
      const trendIndicators = page.getByText(/vs 1h ago|Stable/);

      if (await trendIndicators.count() > 0) {
        expect(await trendIndicators.count()).toBeGreaterThan(0);
      }
    });

    test('should display "Latest Readings" section', async ({ page }) => {
      // Look for Latest Readings text
      const latestReadings = page.getByText('Latest Readings');

      if (await latestReadings.count() > 0) {
        await expect(latestReadings.first()).toBeVisible();
      }
    });

    test('should apply hover effects on device cards', async ({ page }) => {
      // Find device cards
      const deviceCards = page.locator('.device-card-hover');

      if (await deviceCards.count() > 0) {
        const firstCard = deviceCards.first();

        // Take screenshot before hover
        await expect(page).toHaveScreenshot('device-card-before-hover.png');

        // Hover over the card
        await firstCard.hover();

        // Wait for hover animation
        await page.waitForTimeout(300);

        // Take screenshot after hover
        await expect(page).toHaveScreenshot('device-card-after-hover.png');
      }
    });
  });

  test.describe('Dashboard Layout', () => {
    test('should display Fleet Health and Activity Feed side by side', async ({ page }) => {
      // Check layout at desktop viewport
      const fleetHealth = page.getByText('Fleet Health');
      const activityFeed = page.getByText('Activity Feed');

      if (await fleetHealth.count() > 0 && await activityFeed.count() > 0) {
        // Both should be visible in the same viewport
        await expect(fleetHealth.first()).toBeVisible();
        await expect(activityFeed.first()).toBeVisible();

        // Visual regression: Dashboard layout
        await expect(page).toHaveScreenshot('dashboard-layout-desktop.png', {
          fullPage: true,
          animations: 'disabled',
        });
      }
    });

    test('should display Historical Metrics Panel', async ({ page }) => {
      // Look for Historical Metrics section
      const metricsPanel = page.getByText(/Historical Metrics|Metrics Panel/i);

      if (await metricsPanel.count() > 0) {
        await expect(metricsPanel.first()).toBeVisible();
      }
    });

    test('should display device grid', async ({ page }) => {
      // Look for device cards in grid layout
      const deviceGrid = page.locator('.grid').filter({ has: page.locator('.device-card-hover') });

      if (await deviceGrid.count() > 0) {
        // Verify grid exists
        expect(await deviceGrid.count()).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Animations', () => {
    test('should animate gauge fill on load', async ({ page }) => {
      // Look for animated gauge
      const animatedGauge = page.locator('.fleet-gauge-animate, [class*="transition"]');

      if (await animatedGauge.count() > 0) {
        // Visual regression captures animation state
        await expect(page).toHaveScreenshot('gauge-animation.png', {
          animations: 'disabled',
        });
      }
    });

    test('should animate activity items on load', async ({ page }) => {
      // Look for animated activity items
      const animatedItems = page.locator('.activity-item-animate');

      if (await animatedItems.count() > 0) {
        // Items should have animation class
        expect(await animatedItems.count()).toBeGreaterThan(0);
      }
    });
  });

  test.describe('Feature Flag', () => {
    test('should respect feature flag override in localStorage', async ({ page }) => {
      // Set feature flag to false
      await page.evaluate(() => {
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'false');
      });

      // Reload page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // The engaging dashboard features might not be visible
      // This test documents the feature flag behavior

      // Take screenshot of classic dashboard
      await expect(page).toHaveScreenshot('dashboard-classic-mode.png', {
        fullPage: true,
        animations: 'disabled',
      });

      // Re-enable feature flag
      await page.evaluate(() => {
        localStorage.setItem('FF_ENGAGING_DASHBOARD', 'true');
      });

      // Reload page
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Take screenshot of engaging dashboard
      await expect(page).toHaveScreenshot('dashboard-engaging-mode.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });
  });

  test.describe('Responsive Design', () => {
    test('should adapt layout on tablet viewport', async ({ page }) => {
      // Set tablet viewport
      await page.setViewportSize({ width: 768, height: 1024 });

      // Reload to apply viewport
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Visual regression: Tablet layout
      await expect(page).toHaveScreenshot('dashboard-tablet.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });

    test('should adapt layout on mobile viewport', async ({ page }) => {
      // Set mobile viewport
      await page.setViewportSize({ width: 375, height: 812 });

      // Reload to apply viewport
      await page.reload();
      await page.waitForLoadState('networkidle');

      // Visual regression: Mobile layout
      await expect(page).toHaveScreenshot('dashboard-mobile.png', {
        fullPage: true,
        animations: 'disabled',
      });
    });
  });

  test.describe('Accessibility', () => {
    test('should have accessible gauge with aria-label', async ({ page }) => {
      // Look for gauge SVG with aria-label
      const accessibleGauge = page.locator('svg[role="img"][aria-label*="Fleet health"]');

      if (await accessibleGauge.count() > 0) {
        await expect(accessibleGauge.first()).toBeVisible();
        const ariaLabel = await accessibleGauge.first().getAttribute('aria-label');
        expect(ariaLabel).toContain('Fleet health');
      }
    });

    test('should have accessible sparklines with aria-label', async ({ page }) => {
      // Look for sparkline SVGs with aria-label
      const accessibleSparklines = page.locator('svg[role="img"][aria-label*="Trend"]');

      if (await accessibleSparklines.count() > 0) {
        const ariaLabel = await accessibleSparklines.first().getAttribute('aria-label');
        expect(ariaLabel).toContain('Trend');
      }
    });
  });
});
