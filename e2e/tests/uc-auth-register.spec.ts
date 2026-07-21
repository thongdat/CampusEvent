import { test, expect } from '@playwright/test';

/**
 * UC-AUTH-02: Đăng ký tài khoản sinh viên
 *
 * Việc đăng ký hoàn tất cần OTP gửi qua email thật, nên E2E tập trung kiểm tra
 * giao diện & validate phía client (không tự động lấy được OTP).
 */
test.describe('UC-AUTH-02 · Đăng ký', () => {
  test('trang đăng ký hiển thị đầy đủ các trường bắt buộc', async ({ page }) => {
    await page.goto('register.html');
    await expect(page.locator('#registerForm')).toBeVisible();
    await expect(page.locator('#fullName')).toBeVisible();
    await expect(page.locator('#email')).toBeVisible();
    await expect(page.locator('#phone')).toBeVisible();
    await expect(page.locator('#role')).toBeVisible();
    await expect(page.locator('#password')).toBeVisible();
    await expect(page.locator('#confirmPassword')).toBeVisible();
    await expect(page.locator('#btnSendOtp')).toBeVisible();
  });

  test('chọn vai trò Sinh viên -> hiện thêm các trường học vụ (MSSV, khoa, ngành, kỳ)', async ({ page }) => {
    await page.goto('register.html');
    await page.selectOption('#role', 'STUDENT');
    await expect(page.locator('#studentFields')).toBeVisible();
    await expect(page.locator('#studentCode')).toBeVisible();
    await expect(page.locator('#faculty')).toBeVisible();
    await expect(page.locator('#major')).toBeVisible();
    await expect(page.locator('#semester')).toBeVisible();
  });

  test('bấm gửi OTP khi form trống -> hiện thông báo yêu cầu nhập liệu', async ({ page }) => {
    await page.goto('register.html');
    await page.selectOption('#role', 'STUDENT');
    await page.click('#btnSendOtp');
    const message = page.locator('#message');
    await expect(message).not.toHaveText('', { timeout: 7_000 });
  });

  test('link "Đăng nhập" điều hướng về trang login', async ({ page }) => {
    await page.goto('register.html');
    await page.getByRole('link', { name: /Đăng nhập/i }).first().click();
    await page.waitForURL('**/login.html');
    await expect(page.locator('#btnLogin')).toBeVisible();
  });
});
