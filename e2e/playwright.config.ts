import { defineConfig, devices } from '@playwright/test';

/**
 * Cấu hình Playwright cho CampusEvent (AEMS).
 *
 * App chạy ở http://localhost:8081/api (server.port=8081, context-path=/api).
 * Đổi bằng biến môi trường BASE_URL nếu bạn deploy nơi khác, ví dụ:
 *   $env:BASE_URL="https://campusevent.onrender.com/api/"  (PowerShell)
 *
 * LƯU Ý: baseURL phải có dấu "/" ở cuối để page.goto('login.html') ghép đúng đường dẫn.
 */
const BASE_URL = process.env.BASE_URL ?? 'http://localhost:8081/api/';

export default defineConfig({
  testDir: './tests',
  // Chờ app khởi động + seed xong dữ liệu demo trước khi chạy test.
  globalSetup: './global-setup.ts',
  fullyParallel: true,
  forbidOnly: !!process.env.CI,
  // App tĩnh + H2 in-memory + scheduler chạy nền -> khi nhiều worker cùng điều
  // hướng lúc đầu, static file có thể phản hồi chậm. Cho retry 1 lần để loại
  // các lần timeout do quá tải tức thời (test logic vẫn ổn khi chạy lại).
  retries: process.env.CI ? 2 : 1,
  // Giảm nhẹ mức song song để server không bị nghẽn cùng lúc (mặc định = số core).
  workers: process.env.CI ? 1 : 4,
  timeout: 45_000,
  expect: { timeout: 10_000 },
  reporter: [['list'], ['html', { open: 'never' }]],
  use: {
    baseURL: BASE_URL,
    actionTimeout: 15_000,
    navigationTimeout: 30_000,
    trace: 'on-first-retry',
    screenshot: 'only-on-failure',
    video: 'retain-on-failure',
    locale: 'vi-VN',
  },
  projects: [
    { name: 'chromium', use: { ...devices['Desktop Chrome'] } },
    // Bỏ comment để chạy thêm trên Firefox/WebKit:
    // { name: 'firefox', use: { ...devices['Desktop Firefox'] } },
    // { name: 'webkit', use: { ...devices['Desktop Safari'] } },
  ],
});
