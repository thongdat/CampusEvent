import { test, expect } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { loginAs } from '../fixtures/auth';

/**
 * UC-STU: Các use case của Sinh viên (screen-student.html)
 * Đăng nhập trước mỗi test bằng tài khoản sinh viên demo.
 *
 * Lưu ý: tab "Sự kiện" mặc định mở view "Đề xuất cho bạn"; lưới đầy đủ (#eventGrid)
 * chỉ hiển thị khi chuyển sang view "Tất cả sự kiện" (data-view="all").
 */
test.describe('UC-STU · Sinh viên', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, accounts.STUDENT);
    await expect(page.locator('#globalSearch')).toBeVisible({ timeout: 15_000 });
  });

  test('UC-STU-01: Xem tất cả sự kiện', async ({ page }) => {
    await page.click('button[data-tab="discover"]');
    await page.click('button[data-view="all"]');
    await expect(page.locator('#eventGrid')).toBeVisible({ timeout: 10_000 });
    await expect(page.locator('#resultCount')).toBeVisible();
  });

  test('UC-STU-02: Mở tab "Đăng ký của tôi"', async ({ page }) => {
    await page.click('button[data-tab="mine"]');
    await expect(page.locator('section[data-tab-panel="mine"]')).toBeVisible();
    await expect(page.locator('section[data-tab-panel="discover"]')).toBeHidden();
  });

  test('UC-STU-03: Tìm kiếm sự kiện theo từ khóa', async ({ page }) => {
    await page.click('button[data-tab="discover"]');
    await page.click('button[data-view="all"]');
    await page.fill('#globalSearch', 'workshop');
    await page.waitForTimeout(500); // debounce 250ms
    await expect(page.locator('#resultCount')).toBeVisible();
    await expect(page.locator('#eventGrid')).toBeVisible();
  });

  test('UC-STU-04: Lọc sự kiện đã diễn ra', async ({ page }) => {
    await page.click('button[data-tab="discover"]');
    await page.click('[data-chip="scope"][data-value="past"]');
    await expect(page.locator('[data-chip="scope"][data-value="past"]')).toHaveClass(/active/);
  });

  test('UC-STU-05: Mở bảng vinh danh (leaderboard)', async ({ page }) => {
    await page.click('button[data-tab="board"]');
    await expect(page.locator('section[data-tab-panel="board"]')).toBeVisible();
  });

  test('UC-STU-06: Đăng ký một sự kiện sắp diễn ra', async ({ page }) => {
    await page.click('button[data-tab="discover"]');
    await page.click('button[data-view="all"]');
    await expect(page.locator('#eventGrid')).toBeVisible({ timeout: 10_000 });
    await page.waitForTimeout(600);
    const registerBtns = page.locator('[data-event-register]');
    const count = await registerBtns.count();
    test.skip(count === 0, 'Không có sự kiện sắp tới nào còn mở đăng ký');

    await registerBtns.first().click();
    await expect(page.locator('#modalBody h3')).toContainText(
      /Đăng ký thành công|danh sách chờ/,
      { timeout: 12_000 },
    );
  });

  test('UC-STU-07: Huỷ một đăng ký sắp diễn ra (nếu có)', async ({ page }) => {
    await page.click('button[data-tab="mine"]');
    await expect(page.locator('section[data-tab-panel="mine"]')).toBeVisible();
    await page.waitForTimeout(800);
    const cancelBtns = page.locator('[data-cancel-reg]');
    const count = await cancelBtns.count();
    test.skip(count === 0, 'Không có đăng ký nào có thể huỷ');

    await cancelBtns.first().click();
    await page.click('#btnConfirmCancelRegistration');
    await expect(page.locator('#toast')).toContainText(/Đã huỷ/, { timeout: 10_000 });
  });
});
