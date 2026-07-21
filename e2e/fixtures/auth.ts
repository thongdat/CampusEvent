import { Page, expect } from '@playwright/test';
import { Account } from './accounts';

/**
 * Điền form đăng nhập và bấm "Đăng nhập".
 * Selector khớp login.html: #email, #password, #btnLogin, #formMessage.
 */
export async function fillLogin(page: Page, email: string, password: string): Promise<void> {
  await page.goto('login.html');
  await expect(page.locator('#btnLogin')).toBeVisible();
  await page.fill('#email', email);
  await page.fill('#password', password);
  await page.click('#btnLogin');
}

/**
 * Đăng nhập đầy đủ và đợi tới khi đã vào đúng trang chủ của vai trò.
 * Luồng: login.html -> oauth-success.html?...role=... -> <landing>.
 */
export async function loginAs(page: Page, account: Account): Promise<void> {
  await fillLogin(page, account.email, account.password);
  // oauth-success.html tự chuyển tiếp sang trang landing của vai trò.
  await page.waitForURL(`**/${account.landing}`, { timeout: 15_000 });
}

/** Lấy nội dung thông báo lỗi/thành công hiển thị trên form đăng nhập. */
export async function loginMessage(page: Page): Promise<string> {
  const msg = page.locator('#formMessage');
  await expect(msg).not.toHaveText('', { timeout: 7_000 });
  return (await msg.textContent())?.trim() ?? '';
}
