import { test, expect } from '@playwright/test';
import { accounts } from '../fixtures/accounts';
import { loginAs } from '../fixtures/auth';

/**
 * UC-DEP: Các use case của Khoa/Bộ môn (screen-department.html)
 * Tài khoản dept01 là MANAGER -> có quyền Trưởng khoa (HEAD).
 */
test.describe('UC-DEP · Khoa / Bộ môn', () => {
  test.beforeEach(async ({ page }) => {
    await loginAs(page, accounts.DEPARTMENT);
    await expect(page.locator('#content')).toBeVisible({ timeout: 15_000 });
  });

  test('UC-DEP-01: Xem dashboard khoa', async ({ page }) => {
    await page.locator('.nav-link[data-view="dashboard"]').click();
    await expect(page.locator('#pageTitle')).toHaveText('Dashboard');
  });

  test('UC-DEP-02: Xem danh sách đề xuất (Proposals)', async ({ page }) => {
    await page.locator('.nav-link[data-view="proposals"]').click();
    await expect(page.locator('#pageTitle')).toHaveText('Proposals');
    await expect(page.locator('#propSearch')).toBeVisible();
  });

  test('UC-DEP-03: Lọc đề xuất theo trạng thái "Chờ duyệt"', async ({ page }) => {
    await page.locator('.nav-link[data-view="proposals"]').click();
    await expect(page.locator('#pageTitle')).toHaveText('Proposals');
    await page.locator('[data-filter="pending"]').click();
    await expect(page.locator('[data-filter="pending"]')).toBeVisible();
  });

  test('UC-DEP-04: Tạo đề xuất sự kiện mới', async ({ page }) => {
    // Nếu trùng lịch phòng, app hỏi window.confirm -> tự đồng ý để gửi tiếp.
    page.on('dialog', (dialog) => dialog.accept());

    await page.locator('#newProposalBtn').click();
    await expect(page.locator('#proposalModal')).toHaveClass(/open/);

    const unique = Date.now();
    await page.fill('#proposalTitle', `E2E Workshop ${unique}`);
    // Faculty + major: chọn option hợp lệ đầu tiên (bỏ qua placeholder).
    await selectFirstReal(page, '#proposalFaculty');
    await selectFirstReal(page, '#proposalMajor');
    await page.fill('#proposalDate', futureDateTime(10, 9));
    await page.fill('#proposalEndDate', futureDateTime(10, 11));
    await selectFirstReal(page, '#proposalOrganizer');
    await selectFirstReal(page, '#proposalLocation');
    await page.fill('#proposalCapacity', '50');
    await page.fill('#proposalDescription', 'Nội dung workshop kiểm thử E2E tự động.');

    // Bỏ các câu hỏi quiz mặc định (rỗng) để không bị chặn ở bước validate quiz.
    const removeBtns = page.locator('#quizQuestionsList .q-remove');
    for (let i = await removeBtns.count(); i > 0; i--) {
      await page.locator('#quizQuestionsList .q-remove').first().click();
    }

    await page.locator('#saveProposalBtn').click();
    await expect(page.locator('#toast')).toContainText(/Đã gửi proposal|Đã cập nhật/, {
      timeout: 12_000,
    });
  });
});

/** Chọn option "thật" đầu tiên của <select> (bỏ option rỗng/placeholder). */
async function selectFirstReal(page: import('@playwright/test').Page, selector: string) {
  const value = await page.locator(`${selector} option`).evaluateAll((opts) => {
    const real = (opts as HTMLOptionElement[]).find((o) => o.value && o.value.trim() !== '');
    return real ? real.value : '';
  });
  if (value) await page.selectOption(selector, value);
}

/** Trả về chuỗi datetime-local trong tương lai: hôm nay + days, giờ hour. */
function futureDateTime(days: number, hour: number): string {
  const d = new Date();
  d.setDate(d.getDate() + days);
  d.setHours(hour, 0, 0, 0);
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T${pad(d.getHours())}:${pad(d.getMinutes())}`;
}
