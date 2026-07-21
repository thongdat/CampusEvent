import { test, expect } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { loginAs } from '../fixtures/auth';

/**
 * UC-ADM: Các use case của Quản trị viên (admin-screen/*.html)
 * Đăng nhập admin rồi điều hướng trực tiếp tới từng trang quản trị.
 */
test.describe('UC-ADM · Quản trị viên', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, accounts.ADMIN);
    await page.waitForURL('**/admin-screen/overview.html', { timeout: 15_000 });
  });

  test('UC-ADM-01: Dashboard hiển thị các chỉ số tổng quan', async ({ page }) => {
    await expect(page.locator('#content .metric-grid')).toBeVisible({ timeout: 15_000 });
    const metrics = page.locator('#content .metric-grid .metric-value');
    expect(await metrics.count()).toBeGreaterThan(0);
  });

  test('UC-ADM-02: Quản lý người dùng - danh sách & tìm kiếm', async ({ page }) => {
    await page.goto('admin-screen/users.html');
    await expect(page.locator('#userSearch')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 15_000 });
    await page.fill('#userSearch', 'admin');
    await page.waitForTimeout(700);
    await expect(page.locator('table tbody tr').first()).toBeVisible();
  });

  test('UC-ADM-03: Quản lý phòng sự kiện', async ({ page }) => {
    await page.goto('admin-screen/rooms.html');
    await expect(page.locator('#roomSearch')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('table tbody tr').first()).toBeVisible({ timeout: 15_000 });
  });

  test('UC-ADM-04: Xem báo cáo', async ({ page }) => {
    await page.goto('admin-screen/reports.html');
    await expect(page.locator('#content')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('h1.page-title')).toBeVisible();
  });

  test('UC-ADM-05: Điều hướng sidebar tới trang Đăng ký', async ({ page }) => {
    await page.goto('admin-screen/registrations.html');
    await expect(page.locator('#content')).toBeVisible({ timeout: 15_000 });
    await expect(page.locator('h1.page-title')).toBeVisible();
  });
});
