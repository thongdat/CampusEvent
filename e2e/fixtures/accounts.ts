/**
 * Tài khoản demo dùng cho E2E.
 *
 * Mặc định khớp bộ dữ liệu do DataSeeder tạo khi bật seed
 * (APP_SEED_ENABLED=true, mật khẩu Campus@2026 — dài >= 8 ký tự nên qua được
 * bước kiểm tra phía client ở màn đăng nhập).
 *
 * Nếu DB của bạn dùng tài khoản/mật khẩu khác, override qua biến môi trường
 * hoặc file e2e/.env (xem .env.example). VD (PowerShell):
 *   $env:E2E_STUDENT_EMAIL="sv001@fpt.edu.vn"; $env:E2E_STUDENT_PASSWORD="Campus@2026"
 */
export type Role = 'ADMIN' | 'DEPARTMENT' | 'COMMITTEE' | 'STUDENT';

export interface Account {
  role: Role;
  email: string;
  password: string;
  /** Trang đích sau khi đăng nhập thành công (khớp oauth-success.html). */
  landing: string;
}

const env = (key: string, fallback: string) => process.env[key]?.trim() || fallback;

export const accounts: Record<Role, Account> = {
  ADMIN: {
    role: 'ADMIN',
    email: env('E2E_ADMIN_EMAIL', 'aems.admin01@uni.edu.vn'),
    password: env('E2E_ADMIN_PASSWORD', 'Campus@2026'),
    landing: 'admin-screen/overview.html',
  },
  DEPARTMENT: {
    role: 'DEPARTMENT',
    email: env('E2E_DEPARTMENT_EMAIL', 'dept01@uni.edu.vn'),
    password: env('E2E_DEPARTMENT_PASSWORD', 'Campus@2026'),
    landing: 'screen-department.html',
  },
  COMMITTEE: {
    role: 'COMMITTEE',
    email: env('E2E_COMMITTEE_EMAIL', 'committee01@uni.edu.vn'),
    password: env('E2E_COMMITTEE_PASSWORD', 'Campus@2026'),
    landing: 'screen-committee.html',
  },
  STUDENT: {
    role: 'STUDENT',
    email: env('E2E_STUDENT_EMAIL', 'student001@uni.edu.vn'),
    password: env('E2E_STUDENT_PASSWORD', 'Campus@2026'),
    landing: 'screen-student.html',
  },
};

/** Email tồn tại nhưng bị khóa (DataSeeder: locked@example.com / locked123). */
export const lockedAccount = {
  email: env('E2E_LOCKED_EMAIL', 'locked@example.com'),
  password: env('E2E_LOCKED_PASSWORD', 'locked123'),
};
