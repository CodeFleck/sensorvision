import { test, expect } from '@playwright/test';

/**
 * Authentication Flow E2E Tests
 *
 * Tests:
 * - Login functionality
 * - Logout functionality
 * - Invalid credentials handling
 * - Session persistence
 * - Protected route redirection
 */

test.describe('Authentication System', () => {
  test.beforeEach(async ({ page }) => {
    await page.goto('/');
  });

  test('should display login page', async ({ page }) => {
    await expect(page).toHaveTitle(/SensorVision/);

    // Visual regression: Login page
    await expect(page).toHaveScreenshot('login-page.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should login successfully with valid credentials', async ({ page }) => {
    // Fill login form
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');

    // Take screenshot before login
    await expect(page).toHaveScreenshot('login-form-filled.png');

    // Click login button
    await page.click('button[type="submit"]');

    // Wait for successful login (dashboard is at root "/")
    await page.waitForTimeout(2000); // Wait for redirect and auth state

    // Verify we're on dashboard (root path)
    await expect(page).toHaveURL(/^http:\/\/localhost:3001\/\??$/);

    // Visual regression: Dashboard page
    await page.waitForTimeout(2000); // Wait for data to load
    await expect(page).toHaveScreenshot('dashboard-after-login.png', {
      fullPage: true,
      animations: 'disabled',
    });
  });

  test('should show error message with invalid credentials', async ({ page }) => {
    await page.fill('input[name="username"]', 'invalid_user');
    await page.fill('input[name="password"]', 'wrong_password');
    await page.click('button[type="submit"]');

    // Wait for error message
    await page.waitForSelector('text=/Invalid|Error|Failed/', { timeout: 5000 });

    // Visual regression: Error state
    await expect(page).toHaveScreenshot('login-error.png');
  });

  test('should logout successfully', async ({ page }) => {
    // Login first
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000); // Wait for login

    // Find and click logout button (look for user menu dropdown first)
    const userMenu = page.locator('button:has-text("admin"), [data-testid="user-menu"]');
    if (await userMenu.count() > 0) {
      await userMenu.first().click();
      await page.waitForTimeout(500);
    }
    await page.getByRole('button', { name: /logout|sign out/i }).click();

    // Should redirect to login
    await page.waitForURL('**/login', { timeout: 5000 });
    await expect(page).toHaveURL(/login/);
  });

  test('should redirect to login when accessing protected route without auth', async ({ page }) => {
    await page.goto('/dashboard');

    // Should redirect to login
    await page.waitForURL('**/login', { timeout: 5000 });
    await expect(page).toHaveURL(/login/);
  });

  test('should persist session after page reload', async ({ page }) => {
    // Login
    await page.fill('input[name="username"]', 'admin');
    await page.fill('input[name="password"]', 'admin123');
    await page.click('button[type="submit"]');
    await page.waitForTimeout(2000); // Wait for login to complete

    // Reload page
    await page.reload();

    // Should still be logged in (root path)
    await expect(page).toHaveURL(/^http:\/\/localhost:3001\/\??$/);
  });
});
