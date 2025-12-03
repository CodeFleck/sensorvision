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
    await page.waitForURL(/\/(admin-dashboard)?(\?.*)?$/, { timeout: 10000 });

    // Navigate directly to rules page (admin users don't see Rules in sidebar)
    await page.goto('/rules');
    await page.waitForLoadState('networkidle');
  });

  test('should display rules list', async ({ page }) => {
    // Verify we're on the rules page
    await expect(page.getByRole('heading', { name: /Rules|Automation/i })).toBeVisible();

    // Visual regression: Rules list page
    await expect(page).toHaveScreenshot('rules-list.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should create a new rule', async ({ page }) => {
    // Click create rule button
    await page.getByRole('button', { name: /Create Rule/i }).click();

    // Wait for modal to appear
    await page.locator('input[name="name"]').waitFor({ state: 'visible' });

    // Fill rule form
    const ruleName = `E2E Test Rule ${Date.now()}`;
    await page.fill('input[name="name"]', ruleName);

    // Select device (if dropdown exists)
    const deviceSelect = page.locator('select[name="deviceId"]');
    if (await deviceSelect.count() > 0) {
      await deviceSelect.selectOption({ index: 1 });
    }

    // Fill variable name
    const variableInput = page.locator('input[name="variable"], input[name="variableName"]');
    if (await variableInput.count() > 0) {
      await variableInput.fill('temperature');
    }

    // Select operator
    const operatorSelect = page.locator('select[name="operator"]');
    if (await operatorSelect.count() > 0) {
      await operatorSelect.selectOption('GT');
    }

    // Set threshold
    const thresholdInput = page.locator('input[name="threshold"]');
    if (await thresholdInput.count() > 0) {
      await thresholdInput.fill('80');
    }

    // Take screenshot of create form
    await expect(page).toHaveScreenshot('rule-create-form.png');

    // Submit form - use type="submit" to distinguish from "Create Rule" button in background
    await page.locator('button[type="submit"]').click();

    // Wait for modal to close
    await page.waitForLoadState('networkidle');

    // Visual regression: After rule creation
    await expect(page).toHaveScreenshot('rules-list-after-create.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should edit an existing rule', async ({ page }) => {
    // Check if there are any rules to edit
    const editButton = page.locator('button:has-text("Edit"), [aria-label="Edit"]').first();

    if (await editButton.count() > 0) {
      await editButton.click();

      // Wait for modal
      await page.locator('input[name="threshold"]').waitFor({ state: 'visible', timeout: 5000 }).catch(() => {});

      // Modify threshold if input exists
      const thresholdInput = page.locator('input[name="threshold"]');
      if (await thresholdInput.count() > 0) {
        await thresholdInput.fill('90');
      }

      // Take screenshot of edit form
      await expect(page).toHaveScreenshot('rule-edit-form.png');

      // Submit form
      await page.getByRole('button', { name: /Update|Save|Submit/i }).click();

      // Wait for modal to close
      await page.waitForLoadState('networkidle');
    }
  });

  test('should toggle rule active status', async ({ page }) => {
    // The toggle is a button with "Enabled" or "Disabled" text
    const toggleButton = page.locator('button:has-text("Enabled"), button:has-text("Disabled")').first();

    if (await toggleButton.count() > 0) {
      // Get initial state
      const initialText = await toggleButton.textContent();

      // Click to toggle
      await toggleButton.click();

      // Wait for update
      await page.waitForLoadState('networkidle');

      // Verify state changed
      const newText = await toggleButton.textContent();
      expect(newText).not.toBe(initialText);
    }
  });

  test('should view alerts page', async ({ page }) => {
    // Navigate directly to alerts page
    await page.goto('/alerts');
    await page.waitForLoadState('networkidle');

    // Visual regression: Alerts page
    await expect(page).toHaveScreenshot('alerts-list.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should filter alerts by severity', async ({ page }) => {
    // Navigate to alerts page
    await page.goto('/alerts');
    await page.waitForLoadState('networkidle');

    // Find severity filter
    const severityFilter = page.locator('select').filter({ hasText: /All|Critical|Warning|Info/i }).first();

    if (await severityFilter.count() > 0) {
      await severityFilter.selectOption({ index: 1 });
      await page.waitForLoadState('networkidle');
    }

    // Visual regression: Alerts page
    await expect(page).toHaveScreenshot('alerts-filtered.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should acknowledge an alert', async ({ page }) => {
    // Navigate to alerts page
    await page.goto('/alerts');
    await page.waitForLoadState('networkidle');

    // Find and click acknowledge button (if any alerts exist)
    const ackButton = page.getByRole('button', { name: /Acknowledge/i }).first();

    if (await ackButton.count() > 0 && await ackButton.isVisible()) {
      await ackButton.click();
      await page.waitForLoadState('networkidle');
    }

    // Visual regression: Alerts page state
    await expect(page).toHaveScreenshot('alert-acknowledged.png');
  });

  test('should delete a rule', async ({ page }) => {
    // Check if there are any rules to delete
    const deleteButton = page.locator('button:has-text("Delete"), [aria-label="Delete"]').first();

    if (await deleteButton.count() > 0) {
      // Set up dialog handler before clicking
      page.on('dialog', async dialog => {
        await dialog.accept();
      });

      await deleteButton.click();

      // Wait for deletion
      await page.waitForLoadState('networkidle');
    }

    // Visual regression: Rules list after deletion
    await expect(page).toHaveScreenshot('rules-after-delete.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });
});
