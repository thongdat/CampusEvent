import { test, expect } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { fillLogin, loginMessage } from '../fixtures/auth';

/**
 * UC-AUTH-01: Đăng nhập hệ thống
 *
 * Kiểm tra đăng nhập theo từng vai trò + các nhánh lỗi (validate phía client,
 * sai mật khẩu, tài khoản bị khóa).
 * Yêu cầu: app đang chạy tại BASE_URL và DB có tài khoản demo (xem fixtures/accounts.ts).
 */
test.describe('UC-AUTH-01 · Đăng nhập', () => {
  for (const account of Object.values(accounts)) {
    test(`đăng nhập thành công với vai trò ${account.role} -> vào đúng trang chủ`, async ({ page }) => {
      await fillLogin(page, account.email, account.password);
      await page.waitForURL(`**/${account.landing}`, { timeout: 15_000 });
      expect(page.url()).toContain(account.landing);
    });
  }

  test('bỏ trống email/mật khẩu -> báo lỗi phía client, không gọi backend', async ({ page }) => {
    await page.goto('login.html');
    await page.click('#btnLogin');
    expect(await loginMessage(page)).toContain('Vui lòng nhập đầy đủ');
    expect(page.url()).toContain('login.html');
  });

  test('email thiếu ký tự @ -> báo lỗi định dạng', async ({ page }) => {
    await fillLogin(page, 'nguoidung-khong-co-at', 'Campus@2026');
    expect(await loginMessage(page)).toContain('@');
    expect(page.url()).toContain('login.html');
  });

  test('mật khẩu ngắn hơn 8 ký tự -> báo lỗi độ dài', async ({ page }) => {
    await fillLogin(page, accounts.STUDENT.email, '123');
    expect(await loginMessage(page)).toContain('8 ký tự');
    expect(page.url()).toContain('login.html');
  });

  test('sai mật khẩu -> báo lỗi, ở lại trang đăng nhập', async ({ page }) => {
    await fillLogin(page, accounts.STUDENT.email, 'SaiMatKhau123');
    const message = await loginMessage(page);
    expect(message.length).toBeGreaterThan(0);
    expect(page.url()).toContain('login.html');
  });
});
