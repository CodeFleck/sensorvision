import { test, expect } from '@playwright/test';

/**
 * Rules and Alerts E2E Tests
 *
 * Tests:
 * - Rules creation and management
 * - Alert triggering
 * - Alert viewing and management
 * - Notification settings
 */

test.describe('Rules and Alerts', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL('**/dashboard');

    // Navigate to rules page
    await page.click('a[href="/rules"], text=/Rules/i');
    await page.waitForLoadState('networkidle');
  });

  test('should display rules list', async ({ page }) => {
    // Wait for rules to load
    await page.waitForTimeout(2000);

    // Visual regression: Rules list page
    await expect(page).toHaveScreenshot('rules-list.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should create a new rule', async ({ page }) => {
    // Click create rule button
    await page.click('button:has-text("Add Rule"), button:has-text("Create Rule"), button:has-text("New Rule")');

    // Fill rule form
    await page.fill('input[name="name"]', `E2E Test Rule ${Date.now()}`);

    // Select device
    await page.click('select[name="deviceId"], [data-testid="device-select"]');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');

    // Select variable
    await page.fill('input[name="variableName"], select[name="variableName"]', 'temperature');

    // Select operator
    await page.click('select[name="operator"]');
    await page.selectOption('select[name="operator"]', 'GT');

    // Set threshold
    await page.fill('input[name="threshold"]', '80');

    // Take screenshot of create form
    await expect(page).toHaveScreenshot('rule-create-form.png');

    // Submit form
    await page.click('button[type="submit"]:has-text("Create"), button:has-text("Save")');

    // Wait for success message
    await page.waitForSelector('text=/Success|Created/', { timeout: 10000 });

    // Visual regression: After rule creation
    await expect(page).toHaveScreenshot('rules-list-after-create.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should edit an existing rule', async ({ page }) => {
    // Click edit button on first rule
    await page.click('[aria-label="Edit"], button:has-text("Edit")', { first: true });

    // Modify threshold
    await page.fill('input[name="threshold"]', '90');

    // Take screenshot of edit form
    await expect(page).toHaveScreenshot('rule-edit-form.png');

    // Submit form
    await page.click('button[type="submit"]:has-text("Update"), button:has-text("Save")');

    // Wait for success message
    await page.waitForSelector('text=/Success|Updated/', { timeout: 10000 });
  });

  test('should toggle rule active status', async ({ page }) => {
    // Find active toggle
    const toggle = page.locator('[role="switch"], input[type="checkbox"]').first();

    const initialState = await toggle.isChecked();
    await toggle.click();

    // Wait for update
    await page.waitForTimeout(1000);

    const newState = await toggle.isChecked();
    expect(newState).not.toBe(initialState);
  });

  test('should view alerts page', async ({ page }) => {
    // Navigate to alerts page
    await page.click('a[href="/alerts"], text=/Alerts/i');
    await page.waitForLoadState('networkidle');

    // Wait for alerts to load
    await page.waitForTimeout(2000);

    // Visual regression: Alerts page
    await expect(page).toHaveScreenshot('alerts-list.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should filter alerts by severity', async ({ page }) => {
    // Navigate to alerts
    await page.click('a[href="/alerts"], text=/Alerts/i');
    await page.waitForLoadState('networkidle');

    // Find severity filter
    const severityFilter = page.locator('select:has-text("Severity"), [data-testid="severity-filter"]');

    if (await severityFilter.count() > 0) {
      await severityFilter.click();
      await page.keyboard.press('ArrowDown');
      await page.keyboard.press('Enter');

      await page.waitForTimeout(1000);

      // Visual regression: Filtered alerts
      await expect(page).toHaveScreenshot('alerts-filtered.png', {
        fullPage: true,
        animations: 'disabled',
      });
    }
  });

  test('should acknowledge an alert', async ({ page }) => {
    // Navigate to alerts
    await page.click('a[href="/alerts"], text=/Alerts/i');
    await page.waitForLoadState('networkidle');

    // Find and click acknowledge button
    const ackButton = page.locator('button:has-text("Acknowledge"), [data-action="acknowledge"]').first();

    if (await ackButton.count() > 0) {
      await ackButton.click();

      // Wait for success
      await page.waitForTimeout(1000);

      // Visual regression: After acknowledgment
      await expect(page).toHaveScreenshot('alert-acknowledged.png');
    }
  });

  test('should delete a rule', async ({ page }) => {
    // Create a temporary rule first
    await page.click('button:has-text("Add Rule"), button:has-text("Create Rule")');
    const timestamp = Date.now();
    await page.fill('input[name="name"]', `Delete Me ${timestamp}`);

    // Fill minimum required fields quickly
    await page.click('select[name="deviceId"]');
    await page.keyboard.press('ArrowDown');
    await page.keyboard.press('Enter');
    await page.fill('input[name="variableName"]', 'temp');
    await page.click('select[name="operator"]');
    await page.selectOption('select[name="operator"]', 'GT');
    await page.fill('input[name="threshold"]', '50');
    await page.click('button[type="submit"]');
    await page.waitForSelector('text=/Success/', { timeout: 10000 });

    // Find and click delete button
    await page.click(`[aria-label="Delete"]:near(text="Delete Me ${timestamp}"), button:has-text("Delete"):near(text="Delete Me ${timestamp}")`);

    // Confirm deletion
    await page.click('button:has-text("Confirm"), button:has-text("Delete"), button:has-text("Yes")');

    // Wait for success message
    await page.waitForSelector('text=/Deleted|Removed/', { timeout: 10000 });
  });
});
