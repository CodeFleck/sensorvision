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
    await page.waitForTimeout(2000); // Wait for login

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
    await page.click('[aria-label="Manage Token"], button:has-text("Token"), [data-action="token"]', { first: true });

    // Click generate token button
    await page.click('button:has-text("Generate Token"), button:has-text("Create Token")');

    // Wait for token to be displayed
    await page.locator('[data-testid="device-token"]').or(page.locator('input[readonly]')).or(page.locator('code')).first().waitFor({ timeout: 10000 });

    // Take screenshot with token visible
    await expect(page).toHaveScreenshot('device-token-generated.png');

    // Verify token format (UUID)
    const tokenElement = page.locator('[data-testid="device-token"], input[readonly], code').first();
    const tokenValue = await tokenElement.textContent() || await tokenElement.inputValue();
    expect(tokenValue).toMatch(/[a-f0-9-]{36}/i);
  });

  test('should search for devices', async ({ page }) => {
    // Find search input
    const searchInput = page.locator('input[placeholder*="Search"], input[type="search"]').first();

    // Search for device
    await searchInput.fill('test');

    // Wait for filtered results
    await page.waitForTimeout(1000);

    // Visual regression: Search results
    await expect(page).toHaveScreenshot('devices-search-results.png');
  });

  test('should display device details', async ({ page }) => {
    // Click on first device to view details
    await page.locator('[data-testid="device-row"]').or(page.locator('tr')).or(page.locator('.device-card')).first().click();

    // Wait for details to load
    await page.waitForTimeout(2000);

    // Visual regression: Device details page
    await expect(page).toHaveScreenshot('device-details.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should toggle device active status', async ({ page }) => {
    // Find and click active/inactive toggle
    const toggle = page.locator('[role="switch"], input[type="checkbox"]').first();
    const initialState = await toggle.isChecked();

    await toggle.click();

    // Wait for update
    await page.waitForTimeout(1000);

    // Verify state changed
    const newState = await toggle.isChecked();
    expect(newState).not.toBe(initialState);
  });

  test('should delete a device', async ({ page }) => {
    // Create a temporary device first
    await page.click('button:has-text("Add Device"), button:has-text("Create Device")');
    const timestamp = Date.now();
    await page.fill('input[name="name"]', `Delete Me ${timestamp}`);
    await page.fill('input[name="deviceId"]', `delete-me-${timestamp}`);
    await page.click('button[type="submit"]:has-text("Create")');
    await page.waitForSelector('text=/Success/', { timeout: 10000 });

    // Find and click delete button
    await page.click(`[aria-label="Delete"]:near(text="delete-me-${timestamp}"), button:has-text("Delete"):near(text="delete-me-${timestamp}")`);

    // Confirm deletion
    await page.click('button:has-text("Confirm"), button:has-text("Delete"), button:has-text("Yes")');

    // Wait for success message
    await page.waitForSelector('text=/Deleted|Removed/', { timeout: 10000 });

    // Verify device is gone
    await expect(page.locator(`text=delete-me-${timestamp}`)).toHaveCount(0);
  });
});
