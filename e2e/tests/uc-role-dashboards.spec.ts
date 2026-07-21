import { test, expect } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { loginAs } from '../fixtures/auth';

/**
 * UC-AUTH-03: Phân quyền truy cập theo vai trò (RBAC)
 *
 * Sau khi đăng nhập, mỗi vai trò phải vào đúng trang chủ tương ứng và trang
 * tải được (không bị đá ngược về login).
 */
test.describe('UC-AUTH-03 · Trang chủ theo vai trò', () => {
  for (const account of Object.values(accounts)) {
    test(`vai trò ${account.role} vào được ${account.landing} và không bị đá về login`, async ({ page }) => {
      await loginAs(page, account);
      expect(page.url()).toContain(account.landing);
      // Trang không được tự chuyển ngược về login (mất session).
      await page.waitForTimeout(1_000);
      expect(page.url()).not.toContain('login.html');
      await expect(page.locator('body')).toBeVisible();
    });
  }
});

/**
 * UC-AUTH-04: Truy cập trực tiếp trang nội bộ khi CHƯA đăng nhập.
 *
 * Các trang Department/Committee gọi API có bảo vệ ngay khi tải; khi không có
 * session hợp lệ, API trả 401 và client tự chuyển về /api/login.html.
 * (screen-student.html chạy ở "chế độ khách" nên không nằm trong nhóm này.)
 */
test.describe('UC-AUTH-04 · Bảo vệ trang khi chưa đăng nhập', () => {
  const protectedPages = ['screen-department.html', 'screen-committee.html'];

  for (const path of protectedPages) {
    test(`mở ${path} khi chưa đăng nhập -> chuyển về login`, async ({ page, context }) => {
      await context.clearCookies();
      await page.goto(path);
      await page.waitForURL('**/login.html', { timeout: 12_000 });
      expect(page.url()).toContain('login.html');
    });
  }
});
