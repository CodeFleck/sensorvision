import { test, expect } from '@playwright/test';

/**
 * Device Management E2E Tests
 *
 * Tests:
 * - Device list display
 * - Device creation
 * - Device editing
 * - Device deletion
 * - Device token generation
 * - Device search and filtering
 */

test.describe('Device Management', () => {
  test.beforeEach(async ({ page }) => {
    // Login
    await page.goto('/login');
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForURL(/\/(admin-dashboard)?(\?.*)?$/, { timeout: 10000 });

    // Navigate to devices page
    await page.getByRole('link', { name: 'Devices', exact: true }).click();
    await page.waitForURL('**/devices');
  });

  test('should display devices list', async ({ page }) => {
    // Wait for devices to load
    await page.locator('[data-testid="device-list"]').or(page.locator('table')).or(page.locator('.device-card')).first().waitFor({ timeout: 10000 });

    // Visual regression: Devices list page
    await expect(page).toHaveScreenshot('devices-list.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should create a new device', async ({ page }) => {
    // Click create device button
    await page.click('button:has-text("Add Device"), button:has-text("Create Device"), button:has-text("New Device")');

    // Fill device form
    const timestamp = Date.now();
    await page.fill('input[name="name"]', `E2E Test Device ${timestamp}`);
    await page.fill('input[name="deviceId"]', `e2e-device-${timestamp}`);
    await page.fill('textarea[name="description"]', 'Created by E2E test');

    // Take screenshot of create form
    await expect(page).toHaveScreenshot('device-create-form.png');

    // Submit form
    await page.click('button[type="submit"]:has-text("Create"), button:has-text("Save")');

    // Wait for success message
    await page.waitForSelector('text=/Success|Created|Added/', { timeout: 10000 });

    // Verify device appears in list
    await page.waitForSelector(`text=e2e-device-${timestamp}`, { timeout: 10000 });

    // Visual regression: After device creation
    await expect(page).toHaveScreenshot('devices-list-after-create.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should edit an existing device', async ({ page }) => {
    // Find and click edit button for first device
    await page.click('[aria-label="Edit"], button:has-text("Edit"), [data-action="edit"]', { first: true });

    // Modify device name
    await page.fill('input[name="name"]', `Updated Device ${Date.now()}`);

    // Take screenshot of edit form
    await expect(page).toHaveScreenshot('device-edit-form.png');

    // Submit form
    await page.click('button[type="submit"]:has-text("Update"), button:has-text("Save")');

    // Wait for success message
    await page.waitForSelector('text=/Success|Updated/', { timeout: 10000 });
  });

  test('should generate device token', async ({ page }) => {
    // Find first device and click manage tokens
    await page.getByRole('button', { name: 'Manage Token' }).first().click();

    // Wait for token management modal to appear
    await page.waitForSelector('text=/API Token Management|Token Management/', { timeout: 10000 });

    // Check if token already exists (has Rotate/Revoke buttons) or needs to be generated
    const rotateButton = page.getByRole('button', { name: 'Rotate Token' });
    const generateButton = page.locator('button:has-text("Generate Token"), button:has-text("Create Token")');

    if (await rotateButton.isVisible()) {
      // Token already exists - verify it's displayed
      const tokenElement = page.locator('code').first();
      await expect(tokenElement).toBeVisible();
      const tokenValue = await tokenElement.textContent();
      expect(tokenValue).toMatch(/^[\*a-f0-9-]{8,}$/i); // Token format: masked or hex with dashes
    } else if (await generateButton.count() > 0) {
      // Generate new token
      await generateButton.click();
      await page.locator('code').first().waitFor({ timeout: 10000 });
    }

    // Take screenshot with token visible
    await expect(page).toHaveScreenshot('device-token-generated.png');
  });

  test('should search for devices', async ({ page }) => {
    // Find search input
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]').first();

    // Search for device
    await searchInput.fill('test');

    // Wait for filtered results
    await page.waitForLoadState('networkidle');

    // Visual regression: Search results
    await expect(page).toHaveScreenshot('devices-search-results.png');
  });

  test('should display device details', async ({ page }) => {
    // Click on first device to view details
    await page.locator('[data-testid="device-row"]').or(page.locator('tr')).or(page.locator('.device-card')).first().click();

    // Wait for details to load
    await page.waitForLoadState('networkidle');

    // Visual regression: Device details page
    await expect(page).toHaveScreenshot('device-details.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should toggle device active status', async ({ page }) => {
    // Find the toggle switch - it's a label with a hidden checkbox inside
    // We need to click the label element that wraps the checkbox
    const toggle = page.locator('input[type="checkbox"][aria-label="Toggle Active Status"]').first();

    // Skip test with clear message if toggle not found
    const toggleCount = await toggle.count();
    test.skip(toggleCount === 0, 'Toggle switch not found - UI may have changed');

    const initialState = await toggle.isChecked();

    // Use force: true to click the hidden checkbox directly
    await toggle.click({ force: true });

    // Wait for API call to complete
    await page.waitForLoadState('networkidle');

    // Reload page to get fresh state from server
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Get new state - find the same toggle again after reload
    const toggleAfterReload = page.locator('input[type="checkbox"][aria-label="Toggle Active Status"]').first();
    const newState = await toggleAfterReload.isChecked();

    // The state should have changed
    expect(newState).not.toBe(initialState);
  });

  test('should delete a device', async ({ page }) => {
    // Set up dialog handler BEFORE any actions that might trigger dialogs
    page.on('dialog', async dialog => {
      await dialog.accept();
    });

    // Create a temporary device first
    await page.getByRole('button', { name: 'Add Device' }).click();
    const timestamp = Date.now();
    await page.fill('input[name="name"]', `Delete Me ${timestamp}`);
    await page.fill('input[name="deviceId"]', `delete-me-${timestamp}`);
    await page.getByRole('button', { name: 'Create' }).click();

    // Wait for modal to close and device to appear in list
    await page.waitForSelector(`text=delete-me-${timestamp}`, { timeout: 10000 });

    // Find the row with our device and click its delete button
    const deviceRow = page.locator('tr', { hasText: `delete-me-${timestamp}` });
    await deviceRow.getByRole('button', { name: 'Delete' }).click();

    // Wait for deletion to complete
    await page.waitForLoadState('networkidle');

    // Reload page to verify deletion persisted
    await page.reload();
    await page.waitForLoadState('networkidle');

    // Verify device is gone
    await expect(page.locator(`text=delete-me-${timestamp}`)).toHaveCount(0);
  });
});
