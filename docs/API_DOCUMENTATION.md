# CampusEvent - Tài liệu API

> Tài liệu mô tả chi tiết toàn bộ REST API của hệ thống **CampusEvent** (Academic Event
> Management System - AEMS): quy ước chung, cơ chế xác thực, phân quyền, danh sách endpoint
> theo nhóm chức năng, ví dụ request/response, mã lỗi và ghi chú bảo mật.
>
> Tài liệu này được biên soạn bám sát mã nguồn hiện tại trong thư mục `src/main/java/com/example/controller`.
> Khi mã nguồn thay đổi, vui lòng cập nhật lại tài liệu để tránh sai lệch.

---

## Mục lục

1. [Giới thiệu API](#1-giới-thiệu-api)
2. [Thông tin chung và quy ước](#2-thông-tin-chung-và-quy-ước)
3. [Quy ước response](#3-quy-ước-response)
4. [Xác thực (Authentication)](#4-xác-thực-authentication)
5. [Phân quyền (Authorization)](#5-phân-quyền-authorization)
6. [Nhóm API: Xác thực tài khoản (`/auth`)](#6-nhóm-api-xác-thực-tài-khoản-auth)
7. [Nhóm API: Công khai (`/public`)](#7-nhóm-api-công-khai-public)
8. [Nhóm API: Sinh viên (`/student`)](#8-nhóm-api-sinh-viên-student)
9. [Nhóm API: Điểm danh sinh viên (`/student/attendance`)](#9-nhóm-api-điểm-danh-sinh-viên-studentattendance)
10. [Nhóm API: Quiz sinh viên (`/student/quiz`)](#10-nhóm-api-quiz-sinh-viên-studentquiz)
11. [Nhóm API: Check-in QR (`/checkin`)](#11-nhóm-api-check-in-qr-checkin)
12. [Nhóm API: Hội đồng duyệt đề xuất (`/committee`)](#12-nhóm-api-hội-đồng-duyệt-đề-xuất-committee)
13. [Nhóm API: Khoa - Điểm danh (`/department/attendance`)](#13-nhóm-api-khoa---điểm-danh-departmentattendance)
14. [Nhóm API: Khoa - Phản hồi (`/department/feedback`)](#14-nhóm-api-khoa---phản-hồi-departmentfeedback)
15. [Nhóm API: Khoa - Quiz (`/department/quiz`)](#15-nhóm-api-khoa---quiz-departmentquiz)
16. [Nhóm API: Quản trị (`/admin`)](#16-nhóm-api-quản-trị-admin)
17. [Nhóm API: Tải ảnh (`/admin/uploads`)](#17-nhóm-api-tải-ảnh-adminuploads)
18. [Mã lỗi thường gặp](#18-mã-lỗi-thường-gặp)
19. [Quy ước trạng thái (status enum)](#19-quy-ước-trạng-thái-status-enum)
20. [Ghi chú bảo mật](#20-ghi-chú-bảo-mật)
21. [Phụ lục: Bảng tổng hợp endpoint](#21-phụ-lục-bảng-tổng-hợp-endpoint)

---

## 1. Giới thiệu API

### 1.1. CampusEvent là gì?

CampusEvent là hệ thống quản lý sự kiện trong khuôn viên trường học, được xây dựng bằng
Spring Boot. Hệ thống số hóa toàn bộ vòng đời của một sự kiện học thuật / ngoại khóa:

- Đề xuất sự kiện (do khoa hoặc ban tổ chức soạn thảo).
- Duyệt đề xuất (do hội đồng và quản trị viên thực hiện).
- Công bố sự kiện cho sinh viên đăng ký.
- Đăng ký tham gia có cơ chế xếp hạng ưu tiên (priority ranking).
- Điểm danh bằng mã QR động (check-in, mid-session, check-out).
- Làm bài quiz / khảo sát sau sự kiện.
- Gửi phản hồi (feedback) và phân tích phản hồi bằng AI.
- Tính điểm hoạt động, xếp hạng (leaderboard).

### 1.2. Phong cách API

API của CampusEvent là REST trên nền HTTP. Một số đặc điểm quan trọng:

- Phần lớn endpoint trả về **JSON** (`application/json;charset=UTF-8`).
- Một số endpoint thuộc nhóm `/student/attendance` và `/department/*` trả về **trang HTML**
  (render bằng Thymeleaf) vì chúng phục vụ trực tiếp giao diện máy chủ.
- Hệ thống không sử dụng JWT trong luồng nội bộ; thay vào đó dùng **phiên đăng nhập phía
  server** (`SessionAuth`) kết hợp với header `X-User-Email` cho một số endpoint sinh viên.
- Đăng nhập bằng Google OAuth2 được hỗ trợ song song với đăng nhập bằng email/mật khẩu.

### 1.3. Đối tượng đọc tài liệu

- Lập trình viên frontend cần gọi API.
- Lập trình viên backend mới gia nhập dự án.
- Kỹ sư kiểm thử (QA) cần viết test case cho API.
- Người vận hành cần hiểu hành vi hệ thống.

### 1.4. Phạm vi tài liệu

Tài liệu này chỉ mô tả lớp API (controller). Logic nghiệp vụ chi tiết nằm trong lớp service,
truy vấn dữ liệu nằm trong lớp repository — hai phần này được tham chiếu khi cần nhưng không
mô tả đầy đủ ở đây. Để hiểu mô hình dữ liệu, xem `docs/DATABASE_DESIGN.md`. Để hiểu cách
kiểm thử, xem `docs/TESTING_GUIDE.md`.

---

## 2. Thông tin chung và quy ước

### 2.1. Base URL

Ứng dụng cấu hình `server.servlet.context-path=/api`, nghĩa là **mọi endpoint đều có tiền tố
`/api`**. Cổng mặc định là `8081` (có thể bị ghi đè bởi biến môi trường `PORT` hoặc
`SERVER_PORT`).

| Môi trường   | Base URL ví dụ                          |
|--------------|------------------------------------------|
| Local        | `http://localhost:8081/api`              |
| Production    | `https://<ten-mien-trien-khai>/api`     |

Ví dụ: controller khai báo `@RequestMapping("/auth")` với phương thức `@PostMapping("/login")`
sẽ tạo ra đường dẫn đầy đủ là:

```
POST http://localhost:8081/api/auth/login
```

> Trong toàn bộ tài liệu này, để ngắn gọn, các endpoint được viết kèm tiền tố `/api`.

### 2.2. Phương thức HTTP

| Phương thức | Ý nghĩa sử dụng trong dự án                              |
|-------------|----------------------------------------------------------|
| `GET`       | Lấy dữ liệu, không thay đổi trạng thái máy chủ.           |
| `POST`      | Tạo mới tài nguyên hoặc thực thi hành động (duyệt, gửi).  |
| `PUT`       | Cập nhật tài nguyên đã có.                                |
| `DELETE`    | Xóa tài nguyên hoặc hủy đăng ký.                          |

### 2.3. Định dạng dữ liệu

- Request body: JSON (`Content-Type: application/json`), trừ endpoint tải ảnh dùng
  `multipart/form-data`.
- Response body: JSON, trừ các trang HTML render server-side.
- Mã hóa ký tự: UTF-8 (hỗ trợ tiếng Việt có dấu).

### 2.4. Định dạng ngày giờ

Hệ thống dùng `LocalDateTime` của Java và định dạng **ISO-8601 cục bộ** (không kèm múi giờ):

```
yyyy-MM-ddTHH:mm:ss
```

Ví dụ: `2026-06-29T14:30:00`.

Một số endpoint chấp nhận đầu vào dạng `yyyy-MM-ddTHH:mm` hoặc `yyyy-MM-dd HH:mm` và tự
chuẩn hóa (ví dụ endpoint duyệt đề xuất). Khi gửi giờ, nên gửi đúng định dạng ISO để tránh
sai lệch.

### 2.5. Header thường dùng

| Header           | Bắt buộc?         | Mô tả                                                      |
|------------------|-------------------|-----------------------------------------------------------|
| `Content-Type`   | Khi có body       | `application/json` cho hầu hết request.                    |
| `Accept`         | Tùy chọn          | `application/json` hoặc `text/html`.                       |
| `X-User-Email`   | Một số endpoint    | Email của sinh viên đang đăng nhập (xem mục Xác thực).     |
| `Cookie`         | Có (phiên)        | Cookie phiên `JSESSIONID` để duy trì đăng nhập.            |

---

## 3. Quy ước response

### 3.1. Cấu trúc response thành công

Dự án không áp đặt một "vỏ bọc" (envelope) thống nhất duy nhất cho mọi response. Tùy nhóm
endpoint, có ba dạng phổ biến:

**Dạng A — Đối tượng nghiệp vụ trực tiếp** (phổ biến ở nhóm sinh viên, admin):

```json
{
  "items": [ ... ],
  "total": 12
}
```

**Dạng B — Trạng thái thao tác** (đăng ký, duyệt, gửi phản hồi):

```json
{
  "registrationId": 105,
  "status": "REGISTERED",
  "priorityScore": 87.5
}
```

**Dạng C — Cờ thành công + thông điệp** (auth, OTP):

```json
{
  "success": true,
  "message": "Đăng ký thành công."
}
```

### 3.2. Cấu trúc response lỗi

Có hai kiểu lỗi tùy theo nơi phát sinh:

**Kiểu 1 — Lỗi do `ResponseStatusException`** (phát sinh trong controller nghiệp vụ). Spring
trả về cấu trúc lỗi mặc định:

```json
{
  "timestamp": "2026-06-29T12:00:00.000+00:00",
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy sự kiện",
  "path": "/api/student/events/999"
}
```

**Kiểu 2 — Lỗi do interceptor phân quyền** (`AuthorizationInterceptor`). Trả về JSON ngắn
gọn do interceptor tự ghi:

```json
{
  "success": false,
  "error": "Bạn không có quyền truy cập chức năng này.",
  "message": "Bạn không có quyền truy cập chức năng này."
}
```

**Kiểu 3 — Lỗi nghiệp vụ trả về cờ `success=false`** (nhóm auth/OTP), HTTP vẫn có thể là
`200` hoặc `400`:

```json
{
  "success": false,
  "message": "Email đã được sử dụng."
}
```

### 3.3. Mã trạng thái HTTP sử dụng

| Mã  | Ý nghĩa trong dự án                                                       |
|-----|--------------------------------------------------------------------------|
| 200 | Thành công (lấy dữ liệu, thao tác hoàn tất).                              |
| 201 | Tạo mới thành công (đăng ký tài khoản).                                   |
| 400 | Dữ liệu đầu vào sai / vi phạm điều kiện nghiệp vụ.                        |
| 401 | Chưa đăng nhập / phiên không hợp lệ.                                      |
| 403 | Đã đăng nhập nhưng không đủ quyền / sai danh tính / tài khoản bị khóa.    |
| 404 | Không tìm thấy tài nguyên.                                                |
| 409 | Xung đột dữ liệu (ví dụ trùng).                                           |
| 500 | Lỗi máy chủ ngoài dự kiến.                                                |

### 3.4. Phân trang

Một số endpoint danh sách lớn dùng phân trang theo kiểu Spring Data:

- Tham số query: `page` (bắt đầu từ 0), `size` (số bản ghi mỗi trang).
- Response chứa: `items`, `page`, `size`, `totalItems`, `totalPages`.

Ví dụ `/api/admin/activity-logs?page=0&size=80`:

```json
{
  "items": [ ... ],
  "page": 0,
  "size": 80,
  "totalItems": 540,
  "totalPages": 7
}
```

---

## 4. Xác thực (Authentication)

### 4.1. Tổng quan cơ chế

CampusEvent dùng **phiên đăng nhập phía server** làm danh tính tin cậy cho phân quyền. Quá
trình:

1. Người dùng gọi `POST /api/auth/login` với email + mật khẩu.
2. Nếu hợp lệ, server mở một phiên (`SessionAuth.set(...)`) lưu `userId`, `email`, `role`
   gắn với `HttpSession`. Trình duyệt nhận cookie phiên (`JSESSIONID`).
3. Các request sau gửi kèm cookie phiên này để được nhận diện.

> **Lưu ý quan trọng:** Mặc dù tài liệu tổng quan dự án có nhắc tới JWT, luồng nội bộ hiện
> tại dựa trên **session** chứ không phải JWT bearer token. Đây là điểm cần nắm khi tích hợp.

### 4.2. Vai trò của header `X-User-Email`

Các màn hình sinh viên (`/student/**`) gửi thêm header `X-User-Email` chứa email sinh viên
(frontend lưu trong `sessionStorage` sau khi đăng nhập). Backend dùng email này để xác định
sinh viên hiện tại.

Để chống mạo danh, `AuthorizationInterceptor.assertOwnIdentity(...)` kiểm tra: nếu
`X-User-Email` khác với email trong phiên (và người gọi không phải ADMIN) thì trả về **403**.

```
X-User-Email: sv001@fpt.edu.vn
```

### 4.3. Đăng nhập bằng Google OAuth2

Hệ thống hỗ trợ đăng nhập Google:

- Endpoint khởi tạo do Spring Security cung cấp (ví dụ `/oauth2/authorization/google`),
  nằm ngoài context-path nội bộ.
- Sau khi Google xác thực, `OAuth2LoginSuccessHandler` xử lý, ánh xạ email Google sang tài
  khoản người dùng nội bộ qua `StudentIdentityService`.
- Có endpoint `GET /api/auth/oauth-status` để frontend kiểm tra OAuth có được cấu hình không.

### 4.4. Đăng xuất

Đăng xuất thực hiện qua `/logout` (do Spring Security xử lý) để hủy phiên. Đường dẫn này nằm
trong danh sách công khai, không bị interceptor chặn.

### 4.5. Giới hạn số lần đăng nhập sai

`AttemptLimiter` giới hạn số lần đăng nhập sai nhằm chống dò mật khẩu (brute-force). Khi tài
khoản bị khóa tạm thời, `POST /api/auth/login` trả về **403** với `errorType = "ACCOUNT_LOCKED"`.

---

## 5. Phân quyền (Authorization)

### 5.1. Các vai trò

| Vai trò      | Mô tả ngắn                                                           |
|--------------|----------------------------------------------------------------------|
| `STUDENT`    | Sinh viên: đăng ký, điểm danh, làm quiz, gửi feedback.               |
| `COMMITTEE`  | Hội đồng: duyệt / từ chối / yêu cầu chỉnh sửa đề xuất sự kiện.       |
| `DEPARTMENT` | Khoa: quản lý điểm danh, xem phản hồi, quản lý quiz của sự kiện.     |
| `MANAGER`    | Quản lý cấp khoa: truy cập một phần namespace `/admin` và `/department`. |
| `ADMIN`      | Quản trị viên: toàn quyền quản lý hệ thống.                          |

### 5.2. Phạm vi áp dụng interceptor

`WebSecurityMvcConfig` chỉ gắn `AuthorizationInterceptor` cho các nhóm:

```
/admin/**, /committee/**, /department/**, /student/**
```

Các nhóm khác (`/auth/**`, `/public/**`, `/checkin/**`, `/oauth2/**`, `/logout`, file tĩnh)
**không bị interceptor chặn** (công khai hoặc tự xử lý bảo mật bên trong).

### 5.3. Quy tắc phân quyền theo nhóm

| Nhóm path            | Vai trò được phép                          | Ghi chú                                  |
|----------------------|--------------------------------------------|------------------------------------------|
| `/student/**`        | `STUDENT`, `ADMIN`                          | Kiểm tra thêm danh tính qua `X-User-Email`. |
| `/committee/**`      | `COMMITTEE`, `ADMIN`                        | Kiểm tra thêm danh tính.                  |
| `/department/**`     | `DEPARTMENT`, `MANAGER`, `ADMIN`           | —                                        |
| `/admin/**` (nhạy cảm)| `ADMIN`                                    | users, roles, email-logs, activity-logs, registrations, feedback, overview, reports, và ghi departments. |
| `/admin/**` (còn lại) | `ADMIN`, `MANAGER`, `DEPARTMENT`          | events, proposals, dashboard, đọc departments. |

### 5.4. Phản hồi khi không đủ quyền

- Chưa đăng nhập (không có phiên) → **401**. Nếu là điều hướng HTML (GET, `Accept: text/html`)
  thì server **redirect** về `/api/login.html` thay vì trả JSON.
- Đã đăng nhập nhưng sai vai trò → **403**.
- Gửi `X-User-Email` khác phiên (không phải ADMIN) → **403**.

---

## 6. Nhóm API: Xác thực tài khoản (`/auth`)

Controller: `AuthController`, `OAuthConfigController`. Base: `/api/auth`. **Công khai** (không
bị interceptor chặn).

### 6.1. Bảng tổng hợp

| Method | Endpoint                       | Mô tả                                  | Quyền      |
|--------|--------------------------------|----------------------------------------|------------|
| GET    | `/api/auth/test`               | Kiểm tra sống (health) đơn giản.        | Công khai  |
| GET    | `/api/auth/department-structure`| Lấy cấu trúc khoa/ngành.                | Công khai  |
| POST   | `/api/auth/login`              | Đăng nhập, mở phiên.                     | Công khai  |
| POST   | `/api/auth/register/send-otp`  | Gửi OTP xác minh email khi đăng ký.     | Công khai  |
| POST   | `/api/auth/register`           | Đăng ký tài khoản mới (cần OTP).        | Công khai  |
| POST   | `/api/auth/forgot-password`    | Gửi OTP đặt lại mật khẩu.               | Công khai  |
| POST   | `/api/auth/verify-otp`         | Xác minh OTP.                           | Công khai  |
| POST   | `/api/auth/reset-password`     | Đặt lại mật khẩu mới.                    | Công khai  |
| GET    | `/api/auth/oauth-status`       | Kiểm tra cấu hình OAuth2 Google.        | Công khai  |

### 6.2. `GET /api/auth/test`

Trả về chuỗi `Hello World`. Dùng để kiểm tra ứng dụng đang chạy.

**Response (200):**

```
Hello World
```

### 6.3. `GET /api/auth/department-structure`

Trả về cấu trúc học thuật (danh sách khoa và ngành) dùng để đổ dropdown khi đăng ký.

**Response (200):**

```json
{
  "items": [
    {
      "faculty": "Công nghệ thông tin",
      "majors": ["Kỹ thuật phần mềm", "An toàn thông tin", "Trí tuệ nhân tạo"]
    },
    {
      "faculty": "Quản trị kinh doanh",
      "majors": ["Marketing", "Kinh doanh quốc tế"]
    }
  ]
}
```

### 6.4. `POST /api/auth/login`

Đăng nhập bằng email và mật khẩu. Khi thành công, server mở phiên đăng nhập.

**Request body:**

```json
{
  "username": "sv001@fpt.edu.vn",
  "password": "matkhau123"
}
```

> Trường `username` chứa email. Cả hai trường được validate (`@Valid`); để trống sẽ bị từ chối.

**Response thành công (200):**

```json
{
  "success": true,
  "message": "Đăng nhập thành công.",
  "user": {
    "id": 12,
    "email": "sv001@fpt.edu.vn",
    "fullName": "Nguyễn Văn A",
    "role": "STUDENT"
  }
}
```

**Response sai mật khẩu (401):**

```json
{
  "success": false,
  "message": "Email hoặc mật khẩu không đúng."
}
```

**Response tài khoản bị khóa (403):**

```json
{
  "success": false,
  "errorType": "ACCOUNT_LOCKED",
  "message": "Tài khoản tạm thời bị khóa do đăng nhập sai nhiều lần."
}
```

### 6.5. `POST /api/auth/register/send-otp`

Bước 1 trong quy trình đăng ký: gửi OTP tới email để xác minh.

**Request body:**

```json
{
  "email": "sv999@fpt.edu.vn"
}
```

**Response (200):**

```json
{
  "success": true,
  "message": "Mã OTP đã được gửi tới email của bạn."
}
```

**Response thiếu email (400):**

```json
{
  "success": false,
  "message": "Vui lòng nhập email."
}
```

### 6.6. `POST /api/auth/register`

Bước 2: tạo tài khoản mới sau khi đã có OTP xác minh.

**Request body (ví dụ):**

```json
{
  "fullName": "Trần Thị B",
  "email": "sv999@fpt.edu.vn",
  "password": "matkhauMoi123",
  "phone": "0900000000",
  "major": "Kỹ thuật phần mềm",
  "semester": 3,
  "otp": "123456"
}
```

**Response thành công (201):**

```json
{
  "success": true,
  "message": "Đăng ký thành công.",
  "userId": 120
}
```

**Response thất bại (400):**

```json
{
  "success": false,
  "message": "Email đã được sử dụng."
}
```

### 6.7. `POST /api/auth/forgot-password`

Gửi OTP để đặt lại mật khẩu. Nếu người dùng đang đăng nhập, email gửi lên phải khớp với email
trong phiên (nếu không sẽ bị từ chối với thông điệp "Email không khớp...").

**Request body:**

```json
{
  "email": "sv001@fpt.edu.vn"
}
```

**Response (200):**

```json
{
  "success": true,
  "message": "Mã OTP đã được gửi nếu email tồn tại."
}
```

### 6.8. `POST /api/auth/verify-otp`

Xác minh mã OTP.

**Request body:**

```json
{
  "email": "sv001@fpt.edu.vn",
  "otp": "123456"
}
```

**Response (200):**

```json
{
  "success": true,
  "message": "Xác minh OTP thành công."
}
```

**Response thiếu dữ liệu (400):**

```json
{
  "success": false,
  "message": "Vui lòng nhập email và mã OTP."
}
```

### 6.9. `POST /api/auth/reset-password`

Đặt lại mật khẩu mới. Mật khẩu mới phải có **ít nhất 8 ký tự**.

**Request body:**

```json
{
  "email": "sv001@fpt.edu.vn",
  "otp": "123456",
  "newPassword": "matKhauMoi2026"
}
```

**Response (200):**

```json
{
  "success": true,
  "message": "Đặt lại mật khẩu thành công."
}
```

**Response mật khẩu quá ngắn (400):**

```json
{
  "success": false,
  "message": "Mật khẩu mới phải có ít nhất 8 ký tự."
}
```

### 6.10. `GET /api/auth/oauth-status`

Cho frontend biết OAuth2 Google có được cấu hình hay không (để hiện/ẩn nút đăng nhập Google).

**Response (200):**

```json
{
  "enabled": true,
  "provider": "google"
}
```

---

## 7. Nhóm API: Công khai (`/public`)

Controller: `PublicController`. Base: `/api/public`. **Công khai**.

### 7.1. `GET /api/public/landing`

Trả về dữ liệu cho trang chủ (landing page): các con số tổng quan, sự kiện nổi bật... Kết quả
được cache để giảm tải.

**Response (200) (ví dụ rút gọn):**

```json
{
  "stats": {
    "totalEvents": 124,
    "totalStudents": 3200,
    "totalDepartments": 8
  },
  "featuredEvents": [
    {
      "id": 45,
      "title": "Ngày hội Công nghệ 2026",
      "startTime": "2026-07-10T08:00:00",
      "location": "Hội trường A",
      "imageUrl": "/uploads/tech-day.jpg"
    }
  ]
}
```

---

## 8. Nhóm API: Sinh viên (`/student`)

Controller: `StudentController`. Base: `/api/student`. Quyền: `STUDENT`, `ADMIN`. Hầu hết
endpoint yêu cầu header `X-User-Email`.

### 8.1. Bảng tổng hợp

| Method | Endpoint                                  | Mô tả                              |
|--------|-------------------------------------------|------------------------------------|
| GET    | `/api/student/me`                         | Hồ sơ + thống kê cá nhân.          |
| GET    | `/api/student/events`                     | Danh sách sự kiện (lọc, sắp xếp).  |
| GET    | `/api/student/events/{id}`                | Chi tiết một sự kiện.              |
| POST   | `/api/student/events/{id}/register`       | Đăng ký tham gia sự kiện.          |
| DELETE | `/api/student/registrations/{id}`         | Hủy đăng ký.                       |
| GET    | `/api/student/my-registrations`           | Danh sách đăng ký của tôi.         |
| POST   | `/api/student/events/{id}/feedback`       | Gửi feedback cho sự kiện.          |
| GET    | `/api/student/leaderboard`                | Bảng xếp hạng điểm hoạt động.      |

### 8.2. `GET /api/student/me`

Lấy hồ sơ sinh viên và các chỉ số tổng quan (số sự kiện đã đăng ký, danh sách chờ, đã tham
gia, số feedback, hạng, điểm).

**Headers:** `X-User-Email: sv001@fpt.edu.vn`

**Response (200):**

```json
{
  "profile": {
    "studentId": 12,
    "studentCode": "SV00012",
    "fullName": "Nguyễn Văn A",
    "email": "sv001@fpt.edu.vn",
    "phone": "0900000001",
    "major": "Kỹ thuật phần mềm",
    "faculty": "Công nghệ thông tin",
    "semester": 5,
    "year": 3,
    "totalPoints": 145
  },
  "stats": {
    "registered": 6,
    "waitlist": 1,
    "attended": 4,
    "feedback": 3,
    "upcoming": 2,
    "rank": 18,
    "totalPoints": 145
  },
  "rank": 18
}
```

**Response chưa đăng nhập (401):**

```json
{
  "status": 401,
  "error": "Unauthorized",
  "message": "Yêu cầu đăng nhập (X-User-Email)"
}
```

### 8.3. `GET /api/student/events`

Danh sách sự kiện đang mở / sắp diễn ra / đã diễn ra, kèm điểm ưu tiên ước tính cho sinh viên
hiện tại.

**Query params:**

| Tham số  | Mặc định    | Mô tả                                                         |
|----------|-------------|--------------------------------------------------------------|
| `q`      | —           | Từ khóa tìm kiếm (tiêu đề, mô tả, địa điểm, khoa).            |
| `faculty`| `all`       | Lọc theo khoa.                                               |
| `scope`  | `all`       | `all`, `upcoming`, `past`, `today`, `recommended`.           |
| `sort`   | `priority`  | `priority` (hoặc `rbl`), `date`.                              |

**Ghi chú scope:**

- `upcoming`: sự kiện bắt đầu sau thời điểm hiện tại.
- `past`: sự kiện đã diễn ra trong vòng 8 tháng gần đây.
- `today`: sự kiện diễn ra trong ngày hôm nay.
- `recommended`: sự kiện thuộc khoa của sinh viên (cần đăng nhập).

**Response (200) (rút gọn):**

```json
{
  "items": [
    {
      "id": 45,
      "title": "Ngày hội Công nghệ 2026",
      "description": "Sự kiện công nghệ thường niên...",
      "location": "Hội trường A",
      "startTime": "2026-07-10T08:00:00",
      "endTime": "2026-07-10T11:00:00",
      "capacity": 200,
      "imageUrl": "/uploads/tech-day.jpg",
      "status": "PUBLISHED",
      "budget": 5000000,
      "speakers": "TS. Nguyễn Văn C",
      "organizer": "CLB Lập trình",
      "department": {
        "id": 1,
        "name": "Công nghệ thông tin",
        "faculty": "Công nghệ thông tin"
      },
      "registeredCount": 150,
      "waitlistCount": 5,
      "seatsLeft": 50,
      "fillRate": 75,
      "priorityPreview": 82.5,
      "priorityMajor": 30,
      "prioritySemester": 20,
      "priorityPoints": 22.5,
      "priorityTime": 10,
      "myRegistrationId": 301,
      "myStatus": "REGISTERED",
      "myPriorityScore": 82.5
    }
  ],
  "total": 1,
  "facultyOptions": ["Công nghệ thông tin", "Quản trị kinh doanh"]
}
```

### 8.4. `GET /api/student/events/{id}`

Chi tiết một sự kiện, kèm phân tích điểm ưu tiên, hàng đợi ưu tiên (top 8) và các feedback
nổi bật.

**Response (200) (rút gọn):**

```json
{
  "id": 45,
  "title": "Ngày hội Công nghệ 2026",
  "status": "PUBLISHED",
  "priorityBreakdown": {
    "total": 82.5,
    "majorScore": 30,
    "semesterScore": 20,
    "pointsScore": 22.5,
    "timeScore": 10
  },
  "queue": [
    {
      "name": "Nguyễn Văn A.",
      "studentCode": "SV00012",
      "major": "Kỹ thuật phần mềm",
      "priority": 87.5,
      "status": "REGISTERED"
    }
  ],
  "feedbacks": [
    {
      "rating": 5,
      "comment": "Sự kiện rất bổ ích!",
      "author": "Trần Thị B.",
      "createdAt": "2026-06-01T10:00:00"
    }
  ]
}
```

**Response không tìm thấy (404):**

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Không tìm thấy sự kiện"
}
```

### 8.5. `POST /api/student/events/{id}/register`

Đăng ký tham gia sự kiện. Hệ thống tính điểm ưu tiên và áp dụng cơ chế xếp hàng:

- Nếu còn slot → trạng thái `REGISTERED`, cấp vé (ticket), cộng điểm hoạt động.
- Nếu hết slot nhưng điểm ưu tiên cao hơn người thấp nhất → người đó bị đẩy xuống `WAITLIST`,
  sinh viên hiện tại được `REGISTERED`.
- Nếu hết slot và điểm thấp hơn → sinh viên vào `WAITLIST`.
- Nếu đã đăng ký trước đó (chưa hủy) → trả về thông tin đăng ký hiện tại với
  `alreadyRegistered = true`.

**Headers:** `X-User-Email: sv001@fpt.edu.vn`

**Response đăng ký thành công (200):**

```json
{
  "registrationId": 301,
  "status": "REGISTERED",
  "priorityScore": 82.5,
  "ticketCode": "AEMS-1A2B3C4D",
  "invitationEmailQueued": false,
  "priorityBreakdown": {
    "total": 82.5,
    "majorScore": 30,
    "semesterScore": 20,
    "pointsScore": 22.5,
    "timeScore": 10
  },
  "notificationEmail": "sv001@fpt.edu.vn",
  "emailStatus": "SCHEDULED",
  "emailMessage": "Thư mời sẽ được gửi tới email đăng ký khoảng 7 ngày trước sự kiện.",
  "nextSteps": [
    "Mở mục Đăng ký của tôi để xem trạng thái và mã vé.",
    "Kiểm tra cả hộp thư Spam/Quảng cáo nếu chưa thấy email.",
    "Đến sớm và chuẩn bị quét QR check-in tại sự kiện."
  ]
}
```

**Response vào danh sách chờ (200):**

```json
{
  "registrationId": 302,
  "status": "WAITLIST",
  "priorityScore": 60.0,
  "ticketCode": null,
  "emailStatus": "WAITLIST",
  "emailMessage": "Bạn đang ở danh sách chờ. Hệ thống sẽ gửi thư mời khi bạn được xác nhận suất tham dự."
}
```

**Response sự kiện chưa mở đăng ký (400):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Sự kiện chưa mở đăng ký"
}
```

### 8.6. `DELETE /api/student/registrations/{id}`

Hủy một đăng ký. Chỉ chủ sở hữu mới được hủy (ngược lại trả 403). Không thể hủy nếu sự kiện
đã diễn ra. Khi hủy, hệ thống tự động nâng người có điểm ưu tiên cao nhất trong `WAITLIST`
lên `REGISTERED` (nếu còn slot).

**Response (200):**

```json
{
  "registrationId": 301,
  "status": "CANCELLED"
}
```

**Response hủy đăng ký của người khác (403):**

```json
{
  "status": 403,
  "error": "Forbidden",
  "message": "Không thể huỷ đăng ký của người khác"
}
```

### 8.7. `GET /api/student/my-registrations`

Danh sách đăng ký của sinh viên hiện tại, kèm thông tin sự kiện, vé, điểm danh và trạng thái
đã gửi feedback hay chưa.

**Response (200) (rút gọn):**

```json
{
  "items": [
    {
      "registrationId": 301,
      "status": "REGISTERED",
      "priorityScore": 82.5,
      "registeredAt": "2026-06-20T09:00:00",
      "note": null,
      "event": {
        "id": 45,
        "title": "Ngày hội Công nghệ 2026",
        "imageUrl": "/uploads/tech-day.jpg",
        "location": "Hội trường A",
        "startTime": "2026-07-10T08:00:00",
        "endTime": "2026-07-10T11:00:00",
        "status": "PUBLISHED",
        "department": "Công nghệ thông tin"
      },
      "ticket": {
        "code": "AEMS-1A2B3C4D",
        "sentDate": "2026-06-20T09:00:01"
      },
      "attendance": {
        "status": "ATTENDED",
        "checkinTime": "2026-07-10T07:55:00"
      },
      "feedbackSubmitted": false
    }
  ],
  "total": 1
}
```

### 8.8. `POST /api/student/events/{id}/feedback`

Gửi feedback cho sự kiện. Điều kiện: sinh viên phải đã đăng ký **và** đã điểm danh
(`ATTENDED`). Nếu đã từng gửi thì lần này sẽ cập nhật (không cộng điểm lần hai). `rating`
phải nằm trong khoảng 1–5.

**Request body:**

```json
{
  "rating": 5,
  "comment": "Nội dung rất hữu ích, diễn giả nhiệt tình."
}
```

**Response (200):**

```json
{
  "feedbackId": 88,
  "rating": 5,
  "comment": "Nội dung rất hữu ích, diễn giả nhiệt tình.",
  "createdAt": "2026-07-10T12:00:00",
  "pointsAwarded": 8
}
```

**Response chưa tham gia sự kiện (400):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Chỉ được gửi feedback khi đã tham gia sự kiện"
}
```

### 8.9. `GET /api/student/leaderboard`

Top 10 sinh viên theo điểm hoạt động (`totalPoints`).

**Response (200):**

```json
{
  "items": [
    {
      "rank": 1,
      "fullName": "Lê Văn D",
      "major": "An toàn thông tin",
      "faculty": "Công nghệ thông tin",
      "semester": 7,
      "totalPoints": 320,
      "isMe": false
    }
  ]
}
```

### 8.10. Cơ chế điểm ưu tiên (Priority Ranking)

Điểm ưu tiên (`priorityScore`) do `PriorityRankingService` tính, gồm bốn thành phần:

| Thành phần       | Trường JSON          | Ý nghĩa                                                |
|------------------|----------------------|--------------------------------------------------------|
| Điểm ngành        | `priorityMajor`      | Mức phù hợp giữa ngành sinh viên và khoa tổ chức.      |
| Điểm học kỳ       | `prioritySemester`   | Ưu tiên theo học kỳ.                                   |
| Điểm tích lũy     | `priorityPoints`     | Dựa trên điểm hoạt động đã có.                         |
| Điểm thời gian    | `priorityTime`       | Đăng ký sớm được ưu tiên hơn.                          |

Điểm cộng khi hành động: đăng ký `+5`, gửi feedback `+8` (chỉ lần đầu).

---

## 9. Nhóm API: Điểm danh sinh viên (`/student/attendance`)

Controller: `StudentAttendanceController`. Base: `/api/student/attendance`. Quyền: `STUDENT`,
`ADMIN`. Nhóm này trả về **trang HTML** (Thymeleaf) trừ endpoint check-out (JSON).

### 9.1. Bảng tổng hợp

| Method | Endpoint                                                  | Mô tả                          | Kiểu     |
|--------|-----------------------------------------------------------|--------------------------------|----------|
| GET    | `/api/student/attendance/events/{eventId}/checkin`        | Quét QR check-in.              | HTML     |
| GET    | `/api/student/attendance/events/{eventId}/mid`            | Quét QR giữa giờ.              | HTML     |
| GET    | `/api/student/attendance/events/{eventId}/checkout`       | Trang check-out.              | HTML     |
| GET    | `/api/student/attendance/events/{eventId}/checkout-feedback`| Trang check-out + feedback.  | HTML     |
| POST   | `/api/student/attendance/events/{eventId}/checkout`       | Gửi check-out.               | JSON     |
| GET    | `/api/student/attendance/events/{eventId}/status`         | Trang trạng thái điểm danh.   | HTML     |

### 9.2. `GET .../events/{eventId}/checkin?token=...`

Sinh viên quét mã QR check-in. Tham số `token` là mã phiên điểm danh (sinh ra từ
`AttendanceSession`). Trả về trang HTML xác nhận check-in.

### 9.3. `GET .../events/{eventId}/mid?token=...`

Xác minh giữa giờ (mid-session) để chống điểm danh hộ — sinh viên phải có mặt giữa sự kiện.

### 9.4. `POST .../events/{eventId}/checkout`

Gửi check-out (kết thúc tham gia), có thể kèm dữ liệu quiz/feedback tùy luồng.

**Response (200) (ví dụ):**

```json
{
  "success": true,
  "status": "ATTENDED",
  "participationScore": 95.0
}
```

### 9.5. Luồng điểm danh ba bước

1. **Check-in:** quét QR khi tới sự kiện → tạo bản ghi `Attendance` (status `CHECKED_IN`).
2. **Mid-session:** quét QR giữa giờ → cập nhật `mid_verify_time`.
3. **Check-out:** kết thúc → cập nhật `checkout_time`, tính `participation_score`, chuyển
   trạng thái sang `ATTENDED`.

---

## 10. Nhóm API: Quiz sinh viên (`/student/quiz`)

Controller: `StudentQuizController`. Base: `/api/student/quiz`. Quyền: `STUDENT`, `ADMIN`.
Trả về JSON.

### 10.1. Bảng tổng hợp

| Method | Endpoint                                            | Mô tả                         |
|--------|-----------------------------------------------------|-------------------------------|
| GET    | `/api/student/quiz/events/{eventId}/questions`      | Lấy câu hỏi quiz check-out.   |
| POST   | `/api/student/quiz/events/{eventId}/submit`         | Nộp bài quiz.                 |

### 10.2. `GET .../events/{eventId}/questions?limit=5`

Lấy danh sách câu hỏi quiz cho sự kiện (mặc định tối đa 5 câu).

**Response (200):**

```json
[
  {
    "id": 10,
    "questionText": "Sự kiện diễn ra ở đâu?",
    "questionType": "MULTIPLE_CHOICE",
    "optionA": "Hội trường A",
    "optionB": "Hội trường B",
    "optionC": "Sân vận động",
    "optionD": "Thư viện",
    "points": 1
  }
]
```

> Lưu ý: response trả entity `QuizQuestion`; trường `correctAnswer` có thể xuất hiện trong dữ
> liệu — phía client không nên hiển thị đáp án đúng cho sinh viên trước khi nộp.

### 10.3. `POST .../events/{eventId}/submit`

Nộp bài quiz. Body chứa danh sách câu trả lời.

**Request body (ví dụ):**

```json
{
  "answers": [
    { "questionId": 10, "selectedAnswer": "A" },
    { "questionId": 11, "selectedAnswer": "C" }
  ]
}
```

**Response (200):**

```json
{
  "submissionId": 55,
  "totalScore": 2.0,
  "correctCount": 2,
  "totalQuestions": 2
}
```

---

## 11. Nhóm API: Check-in QR (`/checkin`)

Controller: `CheckinController`. Base: `/api/checkin`. **Công khai** (không bị interceptor
chặn) — phục vụ quét QR và tích hợp Google Form. Bảo mật dựa trên token phiên.

### 11.1. Bảng tổng hợp

| Method | Endpoint                                         | Mô tả                                   | Kiểu  |
|--------|--------------------------------------------------|-----------------------------------------|-------|
| GET    | `/api/checkin/events/{eventId}/info`             | Thông tin sự kiện cho trang check-in.   | JSON  |
| GET    | `/api/checkin/events/{eventId}/qr-token`         | Lấy token QR hiện hành.                  | JSON  |
| GET    | `/api/checkin/events/{eventId}/form-redirect`    | Chuyển hướng tới Google Form.           | HTML  |
| GET    | `/api/checkin/events/{eventId}/status`           | Trạng thái điểm danh theo mã SV.        | JSON  |
| POST   | `/api/checkin/events/{eventId}/submit`           | Ghi nhận check-in.                       | JSON  |

### 11.2. `GET .../events/{eventId}/info`

Thông tin sự kiện hiển thị trên màn hình check-in.

**Response (200):**

```json
{
  "eventId": 45,
  "title": "Ngày hội Công nghệ 2026",
  "startTime": "2026-07-10T08:00:00",
  "location": "Hội trường A",
  "status": "PUBLISHED"
}
```

### 11.3. `GET .../events/{eventId}/qr-token?force=...`

Lấy token QR hiện hành. Token xoay vòng theo thời gian (chống chụp màn hình QR để điểm danh
hộ). Tham số `force` (tùy chọn) buộc sinh token mới.

**Response (200):**

```json
{
  "token": "a1b2c3d4e5",
  "expiresAt": "2026-07-10T08:00:30",
  "ttlSeconds": 30
}
```

### 11.4. `POST .../events/{eventId}/submit`

Ghi nhận check-in từ thiết bị sinh viên.

**Request body (ví dụ):**

```json
{
  "studentCode": "SV00012",
  "token": "a1b2c3d4e5"
}
```

**Response thành công (200):**

```json
{
  "success": true,
  "status": "CHECKED_IN",
  "checkinTime": "2026-07-10T07:55:00"
}
```

**Response token hết hạn (400):**

```json
{
  "success": false,
  "message": "Mã QR đã hết hạn, vui lòng quét lại."
}
```

---

## 12. Nhóm API: Hội đồng duyệt đề xuất (`/committee`)

Controller: `CommitteeController`. Base: `/api/committee`. Quyền: `COMMITTEE`, `ADMIN`. Trả
về JSON.

### 12.1. Vòng đời đề xuất

```
PENDING ──approve──▶ APPROVED  (đồng thời tạo Event PUBLISHED)
PENDING ──reject───▶ REJECTED  (ghi lý do)
PENDING ──revise───▶ REVISION  (yêu cầu chỉnh sửa)
REVISION ─approve──▶ APPROVED
```

Chỉ đề xuất ở trạng thái `PENDING` hoặc `REVISION` mới có thể được xử lý (approve/reject/
revise). Đề xuất đã `APPROVED`/`REJECTED` sẽ bị từ chối thao tác lại với lỗi 400.

### 12.2. Bảng tổng hợp

| Method | Endpoint                                  | Mô tả                                  |
|--------|-------------------------------------------|----------------------------------------|
| GET    | `/api/committee/overview`                 | Tổng quan: đếm theo status + 5 mới nhất.|
| GET    | `/api/committee/proposals`                | Danh sách đề xuất (lọc theo status).   |
| GET    | `/api/committee/proposals/{id}`           | Chi tiết một đề xuất.                  |
| POST   | `/api/committee/proposals/{id}/approve`   | Duyệt → tạo Event.                     |
| POST   | `/api/committee/proposals/{id}/reject`    | Từ chối (cần lý do).                   |
| POST   | `/api/committee/proposals/{id}/revise`    | Yêu cầu chỉnh sửa (cần nội dung).      |

### 12.3. `GET /api/committee/overview`

**Response (200):**

```json
{
  "counts": {
    "PENDING": 4,
    "REVISION": 1,
    "APPROVED": 20,
    "REJECTED": 3
  },
  "recent": [
    {
      "id": 70,
      "title": "Workshop AI cơ bản",
      "status": "PENDING",
      "proposedDate": "2026-08-01T09:00:00",
      "organizer": "CLB AI",
      "department": { "id": 1, "name": "Công nghệ thông tin" }
    }
  ]
}
```

### 12.4. `GET /api/committee/proposals?status=PENDING,REVISION&q=...`

Danh sách đề xuất, lọc theo `status` (mặc định `PENDING,REVISION`) và từ khóa `q`.

**Response (200) (rút gọn):**

```json
{
  "items": [
    {
      "id": 70,
      "title": "Workshop AI cơ bản",
      "status": "PENDING",
      "proposedDate": "2026-08-01T09:00:00",
      "proposedEndDate": "2026-08-01T12:00:00",
      "organizer": "CLB AI",
      "speakers": "ThS. Phạm Văn E",
      "supportStaffNeeded": 5,
      "capacity": 80,
      "budget": 3000000,
      "location": "Phòng Lab 4",
      "department": { "id": 1, "name": "Công nghệ thông tin" }
    }
  ],
  "total": 1
}
```

### 12.5. `POST /api/committee/proposals/{id}/approve`

Duyệt đề xuất và tạo `Event` (status `PUBLISHED`). Có thể ghi đè một số thông tin qua body
(không bắt buộc). Nếu đã có Event trùng tiêu đề + khoa + giờ thì tái sử dụng.

**Request body (tùy chọn):**

```json
{
  "startTime": "2026-08-01T09:00",
  "endTime": "2026-08-01T12:00",
  "capacity": 100,
  "location": "Hội trường B",
  "organizer": "CLB AI",
  "speakers": "ThS. Phạm Văn E",
  "supportStaffNeeded": 6,
  "note": "Đã duyệt, lưu ý chuẩn bị thiết bị."
}
```

**Response (200) (rút gọn):**

```json
{
  "id": 70,
  "title": "Workshop AI cơ bản",
  "status": "APPROVED",
  "note": "Đã duyệt, lưu ý chuẩn bị thiết bị.",
  "event": {
    "id": 130,
    "title": "Workshop AI cơ bản",
    "startTime": "2026-08-01T09:00:00",
    "endTime": "2026-08-01T12:00:00",
    "location": "Hội trường B",
    "capacity": 100,
    "status": "PUBLISHED"
  },
  "removedFromWorkflow": true
}
```

**Response đề xuất thiếu khoa (400):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Proposal thiếu khoa, không thể tạo event"
}
```

### 12.6. `POST /api/committee/proposals/{id}/reject`

Từ chối đề xuất. **Bắt buộc** có lý do (`reason` hoặc `note`).

**Request body:**

```json
{
  "reason": "Trùng lịch với sự kiện khác, đề nghị dời ngày."
}
```

**Response (200):**

```json
{
  "id": 70,
  "status": "REJECTED",
  "note": "Từ chối: Trùng lịch với sự kiện khác, đề nghị dời ngày."
}
```

**Response thiếu lý do (400):**

```json
{
  "status": 400,
  "error": "Bad Request",
  "message": "Cần ghi lý do từ chối"
}
```

### 12.7. `POST /api/committee/proposals/{id}/revise`

Yêu cầu chỉnh sửa. **Bắt buộc** có nội dung (`request` hoặc `note`).

**Request body:**

```json
{
  "request": "Bổ sung kế hoạch ngân sách chi tiết và danh sách diễn giả."
}
```

**Response (200):**

```json
{
  "id": 70,
  "status": "REVISION",
  "note": "Yêu cầu chỉnh sửa: Bổ sung kế hoạch ngân sách chi tiết và danh sách diễn giả."
}
```

---

## 13. Nhóm API: Khoa - Điểm danh (`/department/attendance`)

Controller: `DepartmentAttendanceController`. Base: `/api/department/attendance`. Quyền:
`DEPARTMENT`, `MANAGER`, `ADMIN`. Một số endpoint trả HTML, một số trả JSON.

### 13.1. Bảng tổng hợp

| Method | Endpoint                                                  | Mô tả                              | Kiểu  |
|--------|-----------------------------------------------------------|------------------------------------|-------|
| GET    | `/api/department/attendance/events/{eventId}/qr`          | Trang hiển thị QR động.            | HTML  |
| GET    | `/api/department/attendance/events/{eventId}/qr-token`    | Lấy token QR mới.                  | JSON  |
| POST   | `/api/department/attendance/events/{eventId}/mid-session/open`| Mở phiên xác minh giữa giờ.     | JSON  |
| GET    | `/api/department/attendance/events/{eventId}/mid-session` | Trang quản lý mid-session.         | HTML  |
| POST   | `/api/department/attendance/events/{eventId}/mark-absent` | Đánh vắng hàng loạt.               | JSON  |
| GET    | `/api/department/attendance/events/{eventId}/dashboard`   | Trang dashboard điểm danh.         | HTML  |
| GET    | `/api/department/attendance/events/{eventId}/dashboard-data`| Dữ liệu dashboard điểm danh.     | JSON  |

### 13.2. `GET .../events/{eventId}/qr-token`

Lấy token QR mới để hiển thị trên màn hình điểm danh.

**Response (200):**

```json
{
  "token": "x9y8z7",
  "expiresAt": "2026-07-10T08:01:00"
}
```

### 13.3. `POST .../events/{eventId}/mid-session/open`

Mở cửa sổ xác minh giữa giờ. Sinh viên phải quét QR mid trong khoảng thời gian này.

**Response (200):**

```json
{
  "success": true,
  "midSessionOpen": true,
  "expiresAt": "2026-07-10T09:35:00"
}
```

### 13.4. `POST .../events/{eventId}/mark-absent`

Đánh dấu vắng cho các đăng ký không check-in (thường chạy sau khi sự kiện kết thúc).

**Response (200):**

```json
{
  "success": true,
  "markedAbsent": 12
}
```

### 13.5. `GET .../events/{eventId}/dashboard-data`

Dữ liệu thống kê điểm danh thời gian thực.

**Response (200) (ví dụ):**

```json
{
  "eventId": 45,
  "total": 200,
  "checkedIn": 150,
  "attended": 140,
  "absent": 10,
  "rows": [
    {
      "studentCode": "SV00012",
      "fullName": "Nguyễn Văn A",
      "checkinTime": "2026-07-10T07:55:00",
      "status": "ATTENDED"
    }
  ]
}
```

---

## 14. Nhóm API: Khoa - Phản hồi (`/department/feedback`)

Controller: `DepartmentFeedbackController`. Base: `/api/department/feedback`. Quyền:
`DEPARTMENT`, `MANAGER`, `ADMIN`.

### 14.1. Bảng tổng hợp

| Method | Endpoint                                              | Mô tả                          | Kiểu  |
|--------|-------------------------------------------------------|--------------------------------|-------|
| GET    | `/api/department/feedback/events/{eventId}`           | Trang thống kê phản hồi.       | HTML  |
| GET    | `/api/department/feedback/events/{eventId}/data`      | Dữ liệu phản hồi.              | JSON  |
| GET    | `/api/department/feedback/events/{eventId}/ai-analysis`| Phân tích phản hồi bằng AI.   | JSON  |

### 14.2. `GET .../events/{eventId}/data`

Tổng hợp phản hồi của một sự kiện.

**Response (200) (ví dụ):**

```json
{
  "eventId": 45,
  "averageRating": 4.5,
  "totalFeedback": 80,
  "ratingDistribution": { "5": 50, "4": 20, "3": 7, "2": 2, "1": 1 },
  "comments": [
    { "rating": 5, "comment": "Tuyệt vời!", "createdAt": "2026-07-10T12:00:00" }
  ]
}
```

### 14.3. `GET .../events/{eventId}/ai-analysis`

Gọi `FeedbackAiAnalysisService` để phân tích các bình luận: cảm xúc chung, chủ đề nổi bật,
gợi ý cải thiện.

**Response (200) (ví dụ):**

```json
{
  "eventId": 45,
  "sentiment": "POSITIVE",
  "summary": "Đa số sinh viên đánh giá cao nội dung và diễn giả.",
  "highlights": ["Nội dung thực tế", "Diễn giả nhiệt tình"],
  "improvements": ["Cải thiện âm thanh hội trường", "Kéo dài phần hỏi đáp"]
}
```

---

## 15. Nhóm API: Khoa - Quiz (`/department/quiz`)

Controller: `DepartmentQuizController`. Base: `/api/department/quiz`. Quyền: `DEPARTMENT`,
`MANAGER`, `ADMIN`.

### 15.1. Bảng tổng hợp

| Method | Endpoint                                              | Mô tả                          | Kiểu  |
|--------|-------------------------------------------------------|--------------------------------|-------|
| GET    | `/api/department/quiz/events/{eventId}`               | Trang quản lý câu hỏi.         | HTML  |
| POST   | `/api/department/quiz/events/{eventId}/questions`     | Tạo câu hỏi mới.              | JSON  |
| PUT    | `/api/department/quiz/questions/{questionId}`         | Cập nhật câu hỏi.            | JSON  |
| DELETE | `/api/department/quiz/questions/{questionId}`         | Xóa câu hỏi.                | JSON  |
| GET    | `/api/department/quiz/events/{eventId}/questions-data`| Lấy danh sách câu hỏi.      | JSON  |
| GET    | `/api/department/quiz/events/{eventId}/results`       | Kết quả quiz tổng hợp.      | JSON  |

### 15.2. `POST .../events/{eventId}/questions`

Tạo câu hỏi quiz cho sự kiện.

**Request body:**

```json
{
  "questionText": "Diễn giả chính của sự kiện là ai?",
  "questionType": "MULTIPLE_CHOICE",
  "optionA": "TS. Nguyễn Văn C",
  "optionB": "ThS. Phạm Văn E",
  "optionC": "PGS. Lê Văn F",
  "optionD": "CN. Trần Văn G",
  "correctAnswer": "A",
  "points": 1
}
```

**Response (200):**

```json
{
  "id": 12,
  "questionText": "Diễn giả chính của sự kiện là ai?",
  "questionType": "MULTIPLE_CHOICE",
  "optionA": "TS. Nguyễn Văn C",
  "optionB": "ThS. Phạm Văn E",
  "optionC": "PGS. Lê Văn F",
  "optionD": "CN. Trần Văn G",
  "correctAnswer": "A",
  "points": 1
}
```

### 15.3. `PUT .../questions/{questionId}`

Cập nhật một câu hỏi (cùng cấu trúc body như tạo mới).

### 15.4. `DELETE .../questions/{questionId}`

Xóa câu hỏi.

**Response (200):**

```json
{
  "message": "Đã xóa câu hỏi."
}
```

### 15.5. `GET .../events/{eventId}/results`

Kết quả quiz của sự kiện: số bài nộp, điểm trung bình, phân bố.

**Response (200) (ví dụ):**

```json
{
  "eventId": 45,
  "totalSubmissions": 120,
  "averageScore": 3.8,
  "maxScore": 5,
  "scoreDistribution": { "5": 40, "4": 35, "3": 25, "2": 15, "1": 5 }
}
```

---

## 16. Nhóm API: Quản trị (`/admin`)

Controller: `AdminDashboardController`. Base: `/api/admin`. Quyền: phần lớn `ADMIN`; một số
endpoint cho `ADMIN`/`MANAGER`/`DEPARTMENT` (events, proposals, dashboard, đọc departments).

### 16.1. Bảng tổng hợp

| Method | Endpoint                                          | Mô tả                                   | Quyền        |
|--------|---------------------------------------------------|-----------------------------------------|--------------|
| GET    | `/api/admin/dashboard`                            | Số liệu tổng quan dashboard.            | ADMIN/MGR/DEP|
| GET    | `/api/admin/overview`                             | Tổng quan chi tiết (cache).             | ADMIN        |
| GET    | `/api/admin/users`                                | Danh sách người dùng.                   | ADMIN        |
| POST   | `/api/admin/users`                                | Tạo người dùng.                         | ADMIN        |
| PUT    | `/api/admin/users/{id}`                           | Cập nhật người dùng.                    | ADMIN        |
| DELETE | `/api/admin/users/{id}`                           | Xóa người dùng.                         | ADMIN        |
| GET    | `/api/admin/roles`                                | Danh sách vai trò.                      | ADMIN        |
| POST   | `/api/admin/roles`                                | Tạo vai trò.                            | ADMIN        |
| PUT    | `/api/admin/roles/{id}`                           | Cập nhật vai trò.                       | ADMIN        |
| DELETE | `/api/admin/roles/{id}`                           | Xóa vai trò.                            | ADMIN        |
| GET    | `/api/admin/departments`                          | Danh sách khoa.                         | ADMIN/MGR/DEP|
| POST   | `/api/admin/departments`                          | Tạo khoa.                               | ADMIN        |
| GET    | `/api/admin/events`                               | Danh sách sự kiện (quản trị).           | ADMIN/MGR/DEP|
| POST   | `/api/admin/events`                               | Tạo sự kiện.                            | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}`                          | Cập nhật sự kiện.                       | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}/status`                   | Đổi trạng thái sự kiện.                 | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}/capacity`                 | Đổi sức chứa.                           | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}/speakers`                 | Cập nhật diễn giả.                      | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}/google-form-url`          | Gắn URL Google Form check-in.           | ADMIN/MGR/DEP|
| PUT    | `/api/admin/events/{id}/google-form-url-checkout` | Gắn URL Google Form check-out.          | ADMIN/MGR/DEP|
| POST   | `/api/admin/events/{id}/google-form/auto-create`  | Tự tạo Google Form check-in.            | ADMIN/MGR/DEP|
| POST   | `/api/admin/events/{id}/google-form/auto-create-checkout`| Tự tạo Google Form check-out.    | ADMIN/MGR/DEP|
| POST   | `/api/admin/events/{id}/google-form/sync`         | Đồng bộ phản hồi từ Google Form.        | ADMIN/MGR/DEP|
| DELETE | `/api/admin/events/{id}`                          | Xóa sự kiện.                            | ADMIN/MGR/DEP|
| GET    | `/api/admin/proposals`                            | Danh sách đề xuất (quản trị).           | ADMIN/MGR/DEP|
| POST   | `/api/admin/proposals`                            | Tạo đề xuất.                            | ADMIN/MGR/DEP|
| PUT    | `/api/admin/proposals/{id}`                       | Cập nhật đề xuất.                       | ADMIN/MGR/DEP|
| PUT    | `/api/admin/proposals/{id}/status`                | Đổi trạng thái đề xuất.                 | ADMIN/MGR/DEP|
| POST   | `/api/admin/proposals/{id}/publish`               | Công bố đề xuất thành sự kiện.          | ADMIN/MGR/DEP|
| DELETE | `/api/admin/proposals/{id}`                       | Xóa đề xuất.                            | ADMIN/MGR/DEP|
| GET    | `/api/admin/registrations`                        | Danh sách đăng ký.                      | ADMIN        |
| POST   | `/api/admin/registrations`                        | Tạo đăng ký (thủ công).                 | ADMIN        |
| PUT    | `/api/admin/registrations/{id}/status`            | Đổi trạng thái đăng ký.                 | ADMIN        |
| GET    | `/api/admin/feedback`                             | Danh sách phản hồi.                     | ADMIN        |
| DELETE | `/api/admin/feedback/{id}`                        | Xóa phản hồi.                           | ADMIN        |
| GET    | `/api/admin/activity-logs`                        | Nhật ký hoạt động (phân trang).         | ADMIN        |

### 16.2. `GET /api/admin/dashboard`

Số liệu tổng quan cho trang dashboard quản trị.

**Response (200) (ví dụ):**

```json
{
  "stats": {
    "totalUsers": 3200,
    "activeUsers": 3100,
    "lockedUsers": 100,
    "totalRoles": 5,
    "totalDepartments": 8,
    "totalEvents": 124,
    "todayEvents": 2,
    "upcomingEvents": 15,
    "pendingProposals": 4,
    "totalRegistrations": 9800,
    "waitlistRegistrations": 120,
    "totalTickets": 8800,
    "attendanceCount": 7600,
    "totalFeedback": 5400,
    "averageRating": 4.3,
    "sentEmails": 9000,
    "failedEmails": 12
  }
}
```

### 16.3. `GET /api/admin/users`

Danh sách người dùng kèm thông tin sinh viên (nếu có).

**Response (200) (một phần tử):**

```json
[
  {
    "id": 12,
    "fullName": "Nguyễn Văn A",
    "email": "sv001@fpt.edu.vn",
    "phone": "0900000001",
    "status": "ACTIVE",
    "active": true,
    "createdAt": "2025-09-01T08:00:00",
    "roleId": 4,
    "role": "STUDENT",
    "roleDescription": "Sinh viên",
    "departmentPosition": "STAFF",
    "departmentPositionLabel": "Nhân viên",
    "major": "Kỹ thuật phần mềm",
    "facultyName": "Công nghệ thông tin",
    "studentId": 12,
    "studentCode": "SV00012",
    "semester": 5,
    "totalPoints": 145
  }
]
```

### 16.4. `POST /api/admin/users`

Tạo người dùng mới.

**Request body (ví dụ):**

```json
{
  "fullName": "Phạm Thị H",
  "email": "committee01@fpt.edu.vn",
  "password": "matkhau123",
  "phone": "0911111111",
  "roleId": 2,
  "major": "Marketing"
}
```

**Response (200/201):**

```json
{
  "success": true,
  "id": 401,
  "message": "Tạo người dùng thành công."
}
```

### 16.5. `PUT /api/admin/users/{id}`

Cập nhật thông tin người dùng (họ tên, vai trò, trạng thái khóa/mở, v.v.).

### 16.6. `DELETE /api/admin/users/{id}`

Xóa người dùng.

**Response (200):**

```json
{
  "success": true,
  "message": "Đã xóa người dùng."
}
```

### 16.7. `GET /api/admin/events`

Danh sách sự kiện ở góc nhìn quản trị (kèm số liệu đăng ký, điểm danh tổng hợp).

### 16.8. `PUT /api/admin/events/{id}/status`

Đổi trạng thái sự kiện (ví dụ `PUBLISHED` → `COMPLETED`).

**Request body:**

```json
{
  "status": "COMPLETED"
}
```

### 16.9. `PUT /api/admin/events/{id}/capacity`

Đổi sức chứa sự kiện.

**Request body:**

```json
{
  "capacity": 250
}
```

### 16.10. `POST /api/admin/events/{id}/google-form/auto-create`

Tự động tạo Google Form check-in cho sự kiện (tích hợp `GoogleFormsApiService`).

**Response (200) (ví dụ):**

```json
{
  "success": true,
  "formUrl": "https://docs.google.com/forms/d/.../viewform",
  "formId": "1FAIpQLSc...",
  "sheetId": "1AbCdEf..."
}
```

### 16.11. `POST /api/admin/events/{id}/google-form/sync`

Đồng bộ phản hồi (câu trả lời) từ Google Form về hệ thống.

**Response (200) (ví dụ):**

```json
{
  "success": true,
  "synced": 35,
  "lastSyncAt": "2026-07-10T13:00:00"
}
```

### 16.12. `GET /api/admin/activity-logs?page=0&size=80`

Nhật ký hoạt động, phân trang. `size` tối đa 200.

**Response (200):**

```json
{
  "items": [
    {
      "id": 9001,
      "user": "Nguyễn Văn A",
      "activityType": "REGISTER_EVENT",
      "description": "Đăng ký 'Ngày hội Công nghệ 2026' (điểm ưu tiên: 82.5)",
      "pointsEarned": 5,
      "createdAt": "2026-06-20T09:00:00"
    }
  ],
  "page": 0,
  "size": 80,
  "totalItems": 9001,
  "totalPages": 113
}
```

### 16.13. `GET /api/admin/feedback` & `DELETE /api/admin/feedback/{id}`

Liệt kê và xóa phản hồi (kiểm duyệt nội dung không phù hợp).

---

## 17. Nhóm API: Tải ảnh (`/admin/uploads`)

Controller: `UploadController`. Base: `/api/admin/uploads`. Thuộc namespace `/admin` nên áp
dụng quy tắc phân quyền `/admin`.

### 17.1. `POST /api/admin/uploads/image`

Tải lên một ảnh (ví dụ ảnh bìa sự kiện). Dùng `multipart/form-data`, trường `file`.

**Request (multipart):**

```
POST /api/admin/uploads/image
Content-Type: multipart/form-data; boundary=...

file: <binary image data>
```

**Response thành công (200):**

```json
{
  "success": true,
  "url": "/uploads/2026/06/abc123.jpg"
}
```

**Response file rỗng (400):**

```json
{
  "success": false,
  "message": "Tệp ảnh trống."
}
```

---

## 18. Mã lỗi thường gặp

### 18.1. Bảng mã lỗi theo HTTP status

| HTTP | Thông điệp ví dụ                                            | Nguyên nhân & cách xử lý                              |
|------|------------------------------------------------------------|------------------------------------------------------|
| 400  | `Rating phải từ 1 đến 5`                                    | Dữ liệu đầu vào sai. Kiểm tra body request.          |
| 400  | `Sự kiện chưa mở đăng ký`                                   | Trạng thái sự kiện không cho đăng ký.                |
| 400  | `Sự kiện đã diễn ra`                                        | Đăng ký/hủy sau khi sự kiện bắt đầu.                 |
| 400  | `Chỉ được gửi feedback khi đã tham gia sự kiện`             | Chưa điểm danh `ATTENDED`.                            |
| 400  | `Cần ghi lý do từ chối`                                     | Reject thiếu lý do.                                  |
| 400  | `Mật khẩu mới phải có ít nhất 8 ký tự`                      | Reset password mật khẩu quá ngắn.                   |
| 401  | `Bạn cần đăng nhập để tiếp tục.`                            | Chưa có phiên đăng nhập.                             |
| 401  | `Yêu cầu đăng nhập (X-User-Email)`                          | Thiếu/không hợp lệ header `X-User-Email`.            |
| 403  | `Bạn không có quyền truy cập chức năng này.`                | Sai vai trò.                                         |
| 403  | `Bạn không thể truy cập dữ liệu của người dùng khác.`        | `X-User-Email` khác phiên.                           |
| 403  | `ACCOUNT_LOCKED` (login)                                    | Đăng nhập sai quá nhiều lần.                          |
| 404  | `Không tìm thấy sự kiện`                                    | ID không tồn tại.                                   |
| 404  | `Không tìm thấy đề xuất`                                    | Proposal ID không tồn tại.                           |
| 404  | `Không tìm thấy đăng ký`                                    | Registration ID không tồn tại.                       |
| 500  | (lỗi máy chủ)                                               | Lỗi ngoài dự kiến — kiểm tra log server.            |

### 18.2. Cách đọc lỗi `ResponseStatusException`

Lỗi do controller chủ động ném ra (qua `ResponseStatusException`) sẽ có trường `message` rõ
ràng bằng tiếng Việt. Frontend nên hiển thị trực tiếp `message` cho người dùng.

### 18.3. Cách đọc lỗi interceptor

Lỗi do phân quyền có cấu trúc `{ success: false, error, message }`. Khi gặp 401 với điều
hướng HTML, server tự redirect về trang đăng nhập.

---

## 19. Quy ước trạng thái (status enum)

### 19.1. Trạng thái sự kiện (`event.status`)

| Trạng thái  | Ý nghĩa                                                     |
|-------------|------------------------------------------------------------|
| `PENDING`   | Chờ xử lý (hiếm dùng cho event, chủ yếu cho proposal).      |
| `APPROVED`  | Đã duyệt (có thể mở đăng ký).                               |
| `PUBLISHED` | Đã công bố, sinh viên đăng ký được.                         |
| `REJECTED`  | Bị từ chối.                                                 |
| `COMPLETED` | Đã kết thúc.                                                |

> Đăng ký chỉ mở khi status thuộc `PUBLISHED` hoặc `APPROVED` (xem `UPCOMING_STATUSES`).

### 19.2. Trạng thái đề xuất (`event_proposal.status`)

| Trạng thái  | Ý nghĩa                                  |
|-------------|------------------------------------------|
| `PENDING`   | Chờ duyệt.                                |
| `REVISION`  | Yêu cầu chỉnh sửa.                        |
| `APPROVED`  | Đã duyệt (đã tạo event).                  |
| `REJECTED`  | Bị từ chối.                              |

### 19.3. Trạng thái đăng ký (`registration.status`)

| Trạng thái   | Ý nghĩa                                          |
|--------------|--------------------------------------------------|
| `REGISTERED` | Đã giữ chỗ thành công (có vé).                    |
| `WAITLIST`   | Danh sách chờ.                                    |
| `CANCELLED`  | Đã hủy.                                           |

### 19.4. Trạng thái điểm danh (`attendance.status`)

| Trạng thái   | Ý nghĩa                                          |
|--------------|--------------------------------------------------|
| `CHECKED_IN` | Đã check-in.                                      |
| `ATTENDED`   | Đã hoàn tất (check-in + mid + check-out).         |
| `ABSENT`     | Vắng mặt.                                         |

---

## 20. Ghi chú bảo mật

### 20.1. Quản lý phiên

- Phiên đăng nhập là nguồn danh tính tin cậy. Bảo vệ cookie phiên (`HttpOnly`, `Secure` ở
  production).
- Không truyền thông tin nhạy cảm (mật khẩu, OTP) trong URL/query string.

### 20.2. Chống mạo danh

- Interceptor kiểm tra `X-User-Email` so với email phiên cho nhóm `/student` và `/committee`.
- ADMIN được phép thao tác thay người dùng khác (bỏ qua kiểm tra này).

### 20.3. Chống điểm danh hộ

- Token QR có thời hạn ngắn (xoay vòng), chống chụp ảnh QR để dùng lại.
- Cơ chế xác minh giữa giờ (mid-session) buộc sinh viên có mặt trong suốt sự kiện.

### 20.4. Mật khẩu và OTP

- Mật khẩu được băm trước khi lưu (không lưu plaintext).
- Mật khẩu mới tối thiểu 8 ký tự.
- OTP có thời hạn (`otp_expiry`), dùng cho đăng ký và đặt lại mật khẩu.
- `AttemptLimiter` chống brute-force đăng nhập.

### 20.5. Quyền tối thiểu

- Mỗi nhóm endpoint chỉ mở cho các vai trò cần thiết.
- Endpoint quản trị nhạy cảm (users, roles, registrations, feedback...) chỉ dành cho ADMIN.

### 20.6. Dữ liệu cá nhân

- Tên người dùng trong hàng đợi/feedback công khai được **che một phần** (mask) để bảo vệ
  quyền riêng tư (ví dụ "Nguyễn Văn A.").
- Kết quả phân tích AI tổng hợp ở mức sự kiện, không công khai từng cá nhân.

### 20.7. Cấu hình bí mật

- Không commit secret (mật khẩu DB, client secret OAuth, API key AI) vào git.
- Dùng biến môi trường để cấu hình (xem `PROJECT_DOCUMENTATION.md`).

### 20.8. CORS và HTTPS

- Dùng HTTPS ở môi trường production.
- Cấu hình CORS phù hợp nếu frontend tách miền.

---

## 21. Phụ lục: Bảng tổng hợp endpoint

### 21.1. Nhóm công khai

| Method | Endpoint                          |
|--------|-----------------------------------|
| GET    | `/api/auth/test`                  |
| GET    | `/api/auth/department-structure`  |
| POST   | `/api/auth/login`                 |
| POST   | `/api/auth/register/send-otp`     |
| POST   | `/api/auth/register`              |
| POST   | `/api/auth/forgot-password`       |
| POST   | `/api/auth/verify-otp`            |
| POST   | `/api/auth/reset-password`        |
| GET    | `/api/auth/oauth-status`          |
| GET    | `/api/public/landing`             |
| GET    | `/api/checkin/events/{id}/info`   |
| GET    | `/api/checkin/events/{id}/qr-token`|
| GET    | `/api/checkin/events/{id}/form-redirect`|
| GET    | `/api/checkin/events/{id}/status` |
| POST   | `/api/checkin/events/{id}/submit` |

### 21.2. Nhóm sinh viên

| Method | Endpoint                                  |
|--------|-------------------------------------------|
| GET    | `/api/student/me`                         |
| GET    | `/api/student/events`                     |
| GET    | `/api/student/events/{id}`                |
| POST   | `/api/student/events/{id}/register`       |
| DELETE | `/api/student/registrations/{id}`         |
| GET    | `/api/student/my-registrations`           |
| POST   | `/api/student/events/{id}/feedback`       |
| GET    | `/api/student/leaderboard`                |
| GET    | `/api/student/attendance/events/{id}/checkin` |
| GET    | `/api/student/attendance/events/{id}/mid` |
| GET    | `/api/student/attendance/events/{id}/checkout` |
| GET    | `/api/student/attendance/events/{id}/checkout-feedback` |
| POST   | `/api/student/attendance/events/{id}/checkout` |
| GET    | `/api/student/attendance/events/{id}/status` |
| GET    | `/api/student/quiz/events/{id}/questions` |
| POST   | `/api/student/quiz/events/{id}/submit`    |

### 21.3. Nhóm hội đồng

| Method | Endpoint                                  |
|--------|-------------------------------------------|
| GET    | `/api/committee/overview`                 |
| GET    | `/api/committee/proposals`                |
| GET    | `/api/committee/proposals/{id}`           |
| POST   | `/api/committee/proposals/{id}/approve`   |
| POST   | `/api/committee/proposals/{id}/reject`    |
| POST   | `/api/committee/proposals/{id}/revise`    |

### 21.4. Nhóm khoa

| Method | Endpoint                                          |
|--------|---------------------------------------------------|
| GET    | `/api/department/attendance/events/{id}/qr`       |
| GET    | `/api/department/attendance/events/{id}/qr-token` |
| POST   | `/api/department/attendance/events/{id}/mid-session/open` |
| GET    | `/api/department/attendance/events/{id}/mid-session` |
| POST   | `/api/department/attendance/events/{id}/mark-absent` |
| GET    | `/api/department/attendance/events/{id}/dashboard`|
| GET    | `/api/department/attendance/events/{id}/dashboard-data` |
| GET    | `/api/department/feedback/events/{id}`            |
| GET    | `/api/department/feedback/events/{id}/data`       |
| GET    | `/api/department/feedback/events/{id}/ai-analysis`|
| GET    | `/api/department/quiz/events/{id}`                |
| POST   | `/api/department/quiz/events/{id}/questions`      |
| PUT    | `/api/department/quiz/questions/{id}`             |
| DELETE | `/api/department/quiz/questions/{id}`             |
| GET    | `/api/department/quiz/events/{id}/questions-data` |
| GET    | `/api/department/quiz/events/{id}/results`        |

### 21.5. Nhóm quản trị

Xem bảng đầy đủ ở mục 16.1.

---

## 22. Hướng dẫn thử nghiệm nhanh với cURL

### 22.1. Đăng nhập và lưu cookie

```bash
curl -i -c cookie.txt -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sv001@fpt.edu.vn","password":"matkhau123"}'
```

### 22.2. Gọi API sinh viên dùng cookie + header

```bash
curl -b cookie.txt http://localhost:8081/api/student/me \
  -H "X-User-Email: sv001@fpt.edu.vn"
```

### 22.3. Đăng ký sự kiện

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/student/events/45/register \
  -H "X-User-Email: sv001@fpt.edu.vn"
```

### 22.4. Duyệt đề xuất (committee)

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/committee/proposals/70/approve \
  -H "Content-Type: application/json" \
  -d '{"capacity":100,"note":"Đã duyệt"}'
```

---

## 23. Câu hỏi thường gặp (API)

**Hỏi: Vì sao gọi `/api/student/me` bị 401 dù đã đăng nhập?**
Đáp: Có thể thiếu header `X-User-Email` hoặc cookie phiên không được gửi kèm. Đảm bảo gửi cả
hai.

**Hỏi: Vì sao đăng ký trả về `WAITLIST` thay vì `REGISTERED`?**
Đáp: Sự kiện đã đầy và điểm ưu tiên của bạn thấp hơn các suất đang giữ chỗ. Bạn sẽ tự động
được nâng suất khi có người hủy.

**Hỏi: Tại sao không gửi feedback được?**
Đáp: Bạn phải đã đăng ký và đã điểm danh (`ATTENDED`) sự kiện đó.

**Hỏi: API có dùng JWT không?**
Đáp: Luồng nội bộ dùng phiên server, không dùng JWT bearer token. Có hỗ trợ đăng nhập Google
OAuth2.

**Hỏi: Múi giờ của các trường thời gian là gì?**
Đáp: Hệ thống dùng `LocalDateTime` (giờ cục bộ máy chủ), không kèm offset múi giờ.

---

*Tài liệu API được biên soạn dựa trên mã nguồn controller hiện tại. Khi thêm/sửa endpoint,
vui lòng cập nhật tài liệu tương ứng để giữ tính chính xác.*
