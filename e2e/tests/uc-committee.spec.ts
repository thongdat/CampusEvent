import { test, expect, Page } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { loginAs } from '../fixtures/auth';

/**
 * UC-COM: Các use case của Hội đồng duyệt (screen-committee.html)
 *
 * Các test có thao tác thay đổi dữ liệu (duyệt/từ chối/yêu cầu sửa) chạy TUẦN TỰ
 * (serial) để không tranh nhau cùng một đề xuất PENDING.
 */
test.describe.configure({ mode: 'serial' });

test.describe('UC-COM · Hội đồng duyệt', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, accounts.COMMITTEE);
    await expect(page.locator('#overviewView')).toBeVisible({ timeout: 15_000 });
  });

  test('UC-COM-01: Xem tổng quan và bộ đếm theo trạng thái', async ({ page }) => {
    await expect(page.locator('[data-stat-card="PENDING"]')).toBeVisible();
    await expect(page.locator('[data-stat-card="APPROVED"]')).toBeVisible();
    await expect(page.locator('#queueCount')).toBeVisible();
  });

  test('UC-COM-02: Lọc danh sách theo "Chờ duyệt"', async ({ page }) => {
    await page.locator('[data-nav-filter="PENDING"]').click();
    await page.waitForTimeout(600);
    await expect(page.locator('#activeFilterLabel')).toBeVisible();
  });

  test('UC-COM-03: Duyệt một đề xuất đang chờ', async ({ page }) => {
    const opened = await openFirstPending(page);
    test.skip(!opened, 'Không có đề xuất PENDING nào để duyệt');

    await page.locator('#detail [data-action="approve"]').click();
    await expect(page.locator('#actionForm')).toBeVisible();
    await page.fill('#actionForm input[name="startTime"]', futureDateTime(14, 9));
    await page.fill('#actionForm input[name="endTime"]', futureDateTime(14, 11));
    await selectFirstReal(page, '#actionForm select[name="roomId"]');
    await page.locator('#actionForm button[type="submit"]').click();
    await expect(page.locator('#toast')).toContainText(/Đã phê duyệt/, { timeout: 12_000 });
  });

  test('UC-COM-04: Từ chối một đề xuất đang chờ', async ({ page }) => {
    const opened = await openFirstPending(page);
    test.skip(!opened, 'Không có đề xuất PENDING nào để từ chối');

    await page.locator('#detail [data-action="reject"]').click();
    await expect(page.locator('#reasonForm')).toBeVisible();
    await page.fill('#reasonForm textarea[name="reason"]', 'Từ chối tự động bởi kiểm thử E2E.');
    await page.locator('#reasonForm button[type="submit"]').click();
    await expect(page.locator('#toast')).toContainText(/Đã từ chối/, { timeout: 12_000 });
  });

  test('UC-COM-05: Yêu cầu chỉnh sửa một đề xuất đang chờ', async ({ page }) => {
    const opened = await openFirstPending(page);
    test.skip(!opened, 'Không có đề xuất PENDING nào để yêu cầu sửa');

    await page.locator('#detail [data-action="revise"]').click();
    await expect(page.locator('#reasonForm')).toBeVisible();
    await page.fill('#reasonForm textarea[name="request"]', 'Đề nghị bổ sung dự toán kinh phí chi tiết.');
    await page.locator('#reasonForm button[type="submit"]').click();
    await expect(page.locator('#toast')).toContainText(/Đã gửi yêu cầu chỉnh sửa/, { timeout: 12_000 });
  });
});

/** Đảm bảo đang lọc PENDING, chọn đề xuất đầu tiên, mở chi tiết và chờ nút hành động. */
async function openFirstPending(page: Page): Promise<boolean> {
  await page.locator('[data-nav-filter="PENDING"]').click();
  const items = page.locator('li.queue-item');
  try {
    await items.first().waitFor({ state: 'visible', timeout: 12_000 });
  } catch {
    return false;
  }
  await items.first().click();
  try {
    await page.locator('#detail [data-action="approve"]').waitFor({ state: 'visible', timeout: 12_000 });
    return true;
  } catch {
    return false;
  }
}

async function selectFirstReal(page: Page, selector: string) {
  const value = await page.locator(`${selector} option`).evaluateAll((opts) => {
    const real = (opts as HTMLOptionElement[]).find((o) => o.value && o.value.trim() !== '');
    return real ? real.value : '';
  });
  if (value) await page.selectOption(selector, value);
}

function futureDateTime(days: number, hour: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  d.setHours(hour, 0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
