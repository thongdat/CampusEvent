# E2E Test (Playwright) — CampusEvent / AEMS

Bộ kiểm thử end-to-end bằng [Playwright](https://playwright.dev), tổ chức **theo từng use case**. Test điều khiển trình duyệt thật để chạy qua giao diện web giống người dùng.

## 1. Yêu cầu

- Node.js >= 18 (khuyến nghị LTS). Máy này đã có Node 24.
- **App CampusEvent phải đang chạy** và có dữ liệu demo (tài khoản theo vai trò).
  - Mặc định test trỏ tới `http://localhost:8081/api/`.

## 2. Cài đặt

```bash
cd e2e
npm install
npm run install:browsers   # tải Chromium/Firefox/WebKit cho Playwright
```

## 3. Khởi động app trước khi test

### Cách A (KHUYẾN NGHỊ) — H2 in-memory, KHÔNG cần cài SQL Server

Ở thư mục gốc dự án `D:\fpt\CampusEvent`, chạy với profile `h2`:

```bash
mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
```

Profile này tự tạo schema bằng Hibernate + seed sẵn tài khoản demo (mật khẩu
`Campus@2026`). Đợi tới khi log hiện `Started EventManagementApplication`, rồi mở
`http://localhost:8081/api/login.html` để kiểm tra.

> **QUAN TRỌNG — seed chạy bất đồng bộ:** sau khi thấy log `Started...`, DataSeeder
> vẫn cần thêm ~15-40s để tạo users/events/proposals. Bộ test có `global-setup`
> tự **đợi tới khi seed xong** (poll `/committee/overview` cho tới khi có dữ liệu),
> nên bạn cứ chạy `npm test` là được — nó sẽ chờ hộ. Nếu chạy thủ công thì hãy đợi
> vài chục giây sau log `Started...`.

> Đã kiểm chứng: **39/39 test E2E pass** với cách A.
> Dữ liệu là in-memory nên mỗi lần khởi động sẽ seed lại từ đầu.

### Cách B — SQL Server thật (giống production)

```bash
# PowerShell (thư mục gốc D:\fpt\CampusEvent) — cần SQL Server đang chạy
$env:APP_SEED_ENABLED="true"
$env:DEMO_ADMIN_PASSWORD="Campus@2026"
$env:DEMO_DEPARTMENT_PASSWORD="Campus@2026"
$env:DEMO_COMMITTEE_PASSWORD="Campus@2026"
$env:DEMO_STUDENT_PASSWORD="Campus@2026"
mvn spring-boot:run
```

> Nếu DB của bạn dùng email/mật khẩu khác, hãy override qua biến môi trường
> (xem `.env.example`) hoặc sửa `fixtures/accounts.ts`.
> Lưu ý: màn đăng nhập validate mật khẩu **>= 8 ký tự** ở phía client.

## 4. Chạy test

```bash
cd e2e

npm test                 # chạy toàn bộ (headless)
npm run test:headed      # mở trình duyệt để quan sát
npm run test:ui          # chế độ UI tương tác (rất hợp để demo)
npm run test:auth        # chỉ chạy nhóm đăng nhập/đăng ký
npm run report           # mở báo cáo HTML sau khi chạy

# Trỏ tới môi trường khác:
$env:BASE_URL="https://campusevent.onrender.com/api/"; npm test
```

## 5. Cấu trúc

```
e2e/
├─ playwright.config.ts        # baseURL, reporter, trace/screenshot/video, globalSetup
├─ global-setup.ts             # chờ app khởi động + seed xong trước khi chạy test
├─ fixtures/
│  ├─ accounts.ts              # tài khoản demo (override qua env)
│  └─ auth.ts                  # helper đăng nhập
└─ tests/
   ├─ uc-auth-login.spec.ts        # UC-AUTH-01 · Đăng nhập (mọi vai trò + nhánh lỗi)
   ├─ uc-auth-register.spec.ts     # UC-AUTH-02 · Đăng ký (UI + validate)
   ├─ uc-role-dashboards.spec.ts   # UC-AUTH-03/04 · Trang chủ theo vai trò & bảo vệ
   ├─ uc-student.spec.ts           # UC-STU-01..07 · Sinh viên (xem/tìm/lọc/đăng ký/huỷ/leaderboard)
   ├─ uc-department.spec.ts        # UC-DEP-01..04 · Khoa (dashboard/proposals/lọc/tạo đề xuất)
   ├─ uc-committee.spec.ts         # UC-COM-01..05 · Hội đồng (tổng quan/lọc/duyệt/từ chối/yêu cầu sửa)
   └─ uc-admin.spec.ts             # UC-ADM-01..05 · Admin (dashboard/user/room/report/đăng ký)
```

Tổng cộng **39 test** trên toàn bộ vai trò. Nhóm duyệt/từ chối/yêu cầu sửa của
Hội đồng chạy **tuần tự (serial)** để không tranh nhau cùng một đề xuất đang chờ.

## 6. Thêm use case mới

1. Tạo file `tests/uc-<ten>.spec.ts`.
2. Dùng `loginAs(page, accounts.STUDENT)` để vào đúng vai trò.
3. Dùng `npm run codegen` để Playwright tự sinh selector khi thao tác trên trang.

## 7. Ghi chú

- Một số luồng (đăng ký hoàn tất, duyệt đề xuất tạo Google Form...) cần OTP email
  thật hoặc OAuth Google, nên E2E tập trung phần chạy được không cần thao tác thủ công.
- Khi test fail, xem `playwright-report/` (có ảnh chụp + video + trace để debug).
