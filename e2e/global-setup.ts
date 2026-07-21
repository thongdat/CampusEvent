import { request, FullConfig } from '@playwright/test';
import { accounts } from './fixtures/accounts';

/**
 * Global setup: chờ app SẴN SÀNG và ĐÃ SEED xong dữ liệu demo trước khi chạy test.
 *
 * Lý do: DataSeeder chạy BẤT ĐỒNG BỘ sau khi log "Started EventManagementApplication",
 * mất ~15-40s để tạo users/events/proposals. Nếu test chạy quá sớm, các màn hình
 * (đặc biệt hàng đợi duyệt của Hội đồng) sẽ trống -> test bị skip/thất bại.
 *
 * Ở đây ta đăng nhập Hội đồng và poll /committee/overview cho tới khi tổng số đề xuất > 0.
 */
async function globalSetup(config: FullConfig): Promise<void> {
  const baseURL =
    (config.projects[0]?.use?.baseURL as string) ??
    process.env.BASE_URL ??
    'http://localhost:8081/api/';

  const deadline = Date.now() + 120_000; // tối đa 2 phút
  const ctx = await request.newContext({ baseURL });

  let lastInfo = 'chưa kết nối được app';
  while (Date.now() < deadline) {
    try {
      const login = await ctx.post('auth/login', {
        data: { email: accounts.COMMITTEE.email, password: accounts.COMMITTEE.password },
      });
      if (login.ok()) {
        const res = await ctx.get('committee/overview');
        if (res.ok()) {
          const body = await res.json();
          const counts = body?.counts ?? {};
          const total = Object.values(counts).reduce((sum: number, v: any) => sum + Number(v || 0), 0);
          if (total > 0) {
            // eslint-disable-next-line no-console
            console.log(`[global-setup] App đã seed xong (tổng đề xuất=${total}). Bắt đầu chạy test.`);
            await ctx.dispose();
            return;
          }
          lastInfo = `đã kết nối nhưng chưa có dữ liệu (tổng đề xuất=${total}) — đang chờ seed...`;
        } else {
          lastInfo = `committee/overview trả ${res.status()}`;
        }
      } else {
        lastInfo = `auth/login trả ${login.status()}`;
      }
    } catch (err: any) {
      lastInfo = `chưa kết nối được app (${err?.message ?? err})`;
    }
    await new Promise((r) => setTimeout(r, 3_000));
  }

  await ctx.dispose();
  throw new Error(
    `[global-setup] App chưa sẵn sàng/seed sau 120s tại ${baseURL}. Chi tiết cuối: ${lastInfo}.\n` +
      `Hãy chắc chắn app đang chạy (profile h2) và đợi log "Started EventManagementApplication" + vài chục giây để seed xong.`,
  );
}

export default globalSetup;
