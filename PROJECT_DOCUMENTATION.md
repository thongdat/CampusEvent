# CampusEvent - Tài liệu dự án

> Hệ thống quản lý sự kiện trong khuôn viên trường (Academic Event Management System - AEMS)
> được xây dựng bằng Spring Boot, hỗ trợ quản lý sự kiện, đăng ký tham gia, điểm danh, khảo
> sát ý kiến và phân tích phản hồi bằng AI.

---

## Mục lục

1. [Giới thiệu tổng quan](#1-giới-thiệu-tổng-quan)
2. [Kiến trúc hệ thống](#2-kiến-trúc-hệ-thống)
3. [Công nghệ sử dụng](#3-công-nghệ-sử-dụng)
4. [Cấu trúc thư mục](#4-cấu-trúc-thư-mục)
5. [Mô hình dữ liệu](#5-mô-hình-dữ-liệu)
6. [Phân quyền và vai trò](#6-phân-quyền-và-vai-trò)
7. [Luồng nghiệp vụ chính](#7-luồng-nghiệp-vụ-chính)
8. [Tài liệu API](#8-tài-liệu-api)
9. [Bảo mật](#9-bảo-mật)
10. [Tích hợp bên thứ ba](#10-tích-hợp-bên-thứ-ba)
11. [Hướng dẫn cài đặt](#11-hướng-dẫn-cài-đặt)
12. [Cấu hình môi trường](#12-cấu-hình-môi-trường)
13. [Triển khai (Deployment)](#13-triển-khai-deployment)
14. [Kiểm thử](#14-kiểm-thử)
15. [Quy ước phát triển](#15-quy-ước-phát-triển)
16. [Lộ trình phát triển](#16-lộ-trình-phát-triển)
17. [Câu hỏi thường gặp](#17-câu-hỏi-thường-gặp)

---

## 1. Giới thiệu tổng quan

CampusEvent là một nền tảng web giúp các trường đại học, cao đẳng tổ chức và quản lý các sự
kiện học thuật cũng như ngoại khóa. Hệ thống hướng đến việc số hóa toàn bộ vòng đời của một
sự kiện, từ khâu đề xuất ý tưởng, phê duyệt, công bố, đăng ký tham gia, điểm danh cho đến
thu thập và phân tích phản hồi.

### 1.1. Mục tiêu

- Giảm thiểu thủ tục giấy tờ trong quá trình tổ chức sự kiện.
- Tăng tính minh bạch trong quy trình phê duyệt đề xuất sự kiện.
- Cung cấp công cụ điểm danh nhanh chóng bằng mã QR.
- Hỗ trợ ban tổ chức ra quyết định dựa trên dữ liệu phản hồi thực tế.
- Tích hợp đăng nhập bằng tài khoản Google để thuận tiện cho sinh viên.

### 1.2. Đối tượng sử dụng

- **Sinh viên**: đăng ký tham gia sự kiện, điểm danh, làm khảo sát, gửi phản hồi.
- **Ban tổ chức / Câu lạc bộ**: đề xuất sự kiện, quản lý đăng ký, tạo phiên điểm danh.
- **Quản trị viên (Admin)**: phê duyệt đề xuất, quản lý người dùng, xem báo cáo tổng quan.
- **Phòng/Khoa (Department)**: theo dõi mức độ tham gia của sinh viên thuộc đơn vị.

### 1.3. Phạm vi tài liệu

Tài liệu này mô tả kiến trúc, các thành phần kỹ thuật, quy trình nghiệp vụ và hướng dẫn vận
hành dự án. Tài liệu hướng đến lập trình viên mới gia nhập dự án và đội ngũ vận hành.

---

## 2. Kiến trúc hệ thống

Hệ thống được xây dựng theo mô hình phân lớp (layered architecture) kết hợp với kiến trúc
client-server truyền thống.

### 2.1. Sơ đồ tổng quát

```
┌─────────────────────────────────────────────────────────┐
│                     Trình duyệt (Client)                 │
│   HTML + CSS + JavaScript thuần (static resources)       │
└───────────────────────────┬─────────────────────────────┘
                            │ HTTP/HTTPS (REST + Thymeleaf)
┌───────────────────────────▼─────────────────────────────┐
│                  Spring Boot Application                  │
│  ┌────────────┐  ┌────────────┐  ┌──────────────────┐    │
│  │ Controller │─▶│  Service   │─▶│   Repository     │    │
│  └────────────┘  └────────────┘  └────────┬─────────┘    │
│  ┌────────────────────────────────────────┼──────────┐   │
│  │ Security (Interceptor, OAuth2, JWT)     │          │   │
│  └─────────────────────────────────────────┘          │   │
└───────────────────────────┬─────────────────────────────┘
                            │ JDBC / JPA
┌───────────────────────────▼─────────────────────────────┐
│        Cơ sở dữ liệu (PostgreSQL / SQL Server)           │
└─────────────────────────────────────────────────────────┘
```

### 2.2. Các lớp chính

- **Controller Layer**: tiếp nhận request HTTP, kiểm tra dữ liệu đầu vào, ủy quyền xử lý
  cho service và trả về response (JSON hoặc trang Thymeleaf).
- **Service Layer**: chứa logic nghiệp vụ, điều phối giao dịch (transaction), gọi các API
  bên ngoài và xử lý dữ liệu.
- **Repository Layer**: truy xuất dữ liệu thông qua Spring Data JPA.
- **Model Layer**: các entity ánh xạ tới bảng trong cơ sở dữ liệu.
- **Security Layer**: xử lý xác thực, phân quyền và bảo vệ endpoint.

### 2.3. Nguyên tắc thiết kế

- Tách biệt rõ ràng giữa các lớp để dễ bảo trì và kiểm thử.
- Ưu tiên sử dụng DTO cho dữ liệu trao đổi qua API thay vì expose trực tiếp entity.
- Xử lý ngoại lệ tập trung để phản hồi lỗi nhất quán.
- Cấu hình theo môi trường (profile) để tách biệt dev / production.

---

## 3. Công nghệ sử dụng

| Thành phần            | Công nghệ                          | Ghi chú                            |
|-----------------------|------------------------------------|------------------------------------|
| Ngôn ngữ              | Java 17+                           | Sử dụng các tính năng hiện đại      |
| Framework             | Spring Boot 3.x                    | Web, Data JPA, Security             |
| Build tool            | Maven 3.9.9                        | Kèm theo Maven Wrapper             |
| Cơ sở dữ liệu         | PostgreSQL (prod), SQL Server (dev)| Hỗ trợ migration giữa hai loại     |
| ORM                   | Hibernate / Spring Data JPA        | Ánh xạ entity                      |
| Template engine       | Thymeleaf                          | Render trang server-side           |
| Frontend              | HTML, CSS, JavaScript thuần        | Không dùng framework nặng          |
| Xác thực              | JWT + OAuth2 (Google)              | Đăng nhập kép                      |
| Triển khai            | Render + Neon                      | PaaS + Postgres serverless         |
| CI/CD                 | GitHub Actions                     | Tự động build và kiểm thử          |

---

## 4. Cấu trúc thư mục

```
CampusEvent/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── controller/      # Các REST controller và MVC controller
│   │   │   ├── service/         # Logic nghiệp vụ
│   │   │   ├── repository/      # Spring Data JPA repository
│   │   │   ├── model/           # Entity ánh xạ database
│   │   │   ├── dto/             # Đối tượng truyền dữ liệu
│   │   │   ├── security/        # Cấu hình bảo mật, interceptor, OAuth
│   │   │   ├── config/          # Cấu hình ứng dụng, seeder dữ liệu
│   │   │   └── tools/           # Công cụ phụ trợ (migration...)
│   │   └── resources/
│   │       ├── static/          # Tài nguyên tĩnh (HTML, CSS, JS, ảnh)
│   │       ├── templates/       # Template Thymeleaf
│   │       ├── application.properties
│   │       └── application-render.properties
│   └── test/
│       └── java/com/example/    # Unit test và integration test
├── apache-maven-3.9.9/          # Maven đóng gói sẵn
├── scripts/                     # Script tiện ích
├── .github/workflows/           # Cấu hình CI/CD
├── database_full.sql            # Script khởi tạo database
├── pom.xml                      # Khai báo dependency
├── DEPLOY_RENDER_NEON.md        # Hướng dẫn triển khai
└── PROJECT_DOCUMENTATION.md     # Tài liệu này
```

### 4.1. Quy ước đặt tên package

- `controller`: kết thúc bằng `Controller` (ví dụ `StudentQuizController`).
- `service`: kết thúc bằng `Service` (ví dụ `FeedbackService`).
- `repository`: kết thúc bằng `Repository` (ví dụ `EventProposalRepository`).
- `dto`: mô tả mục đích (ví dụ `RegisterRequest`, `LoginResponse`).

---

## 5. Mô hình dữ liệu

### 5.1. Các entity chính

| Entity              | Mô tả                                                       |
|---------------------|-------------------------------------------------------------|
| `Student`           | Thông tin sinh viên (mã số, họ tên, email, khoa).           |
| `Role`              | Vai trò người dùng trong hệ thống.                          |
| `Department`        | Phòng/Khoa của trường.                                      |
| `Event`             | Sự kiện đã được công bố.                                    |
| `EventProposal`     | Đề xuất sự kiện chờ phê duyệt.                              |
| `Ticket`            | Vé/đăng ký tham gia sự kiện của sinh viên.                  |
| `Attendance`        | Bản ghi điểm danh.                                          |
| `AttendanceSession` | Phiên điểm danh gắn với một sự kiện.                        |
| `QuizSubmission`    | Bài làm khảo sát/quiz của sinh viên.                        |
| `EventFeedback`     | Phản hồi của sinh viên về sự kiện.                          |

### 5.2. Quan hệ giữa các entity

- Một `Department` có nhiều `Student` (one-to-many).
- Một `Student` có thể có nhiều `Ticket`, mỗi `Ticket` thuộc về một `Event`.
- Một `Event` có nhiều `AttendanceSession`, mỗi session có nhiều `Attendance`.
- Một `Event` có nhiều `EventFeedback` và `QuizSubmission`.
- Một `EventProposal` khi được duyệt sẽ sinh ra một `Event`.

### 5.3. Sơ đồ quan hệ rút gọn

```
Department 1───* Student 1───* Ticket *───1 Event
                    │                          │
                    │                          ├──* AttendanceSession 1──* Attendance
                    │                          ├──* EventFeedback
                    └──* QuizSubmission *──────┘

EventProposal ──(được duyệt)──▶ Event
```

### 5.4. Khởi tạo dữ liệu

- File `database_full.sql` chứa schema và dữ liệu mẫu.
- Lớp `DataSeeder` tự động chèn dữ liệu khởi tạo khi ứng dụng khởi động (nếu chưa có).
- Lớp `ProposalDataBackfill` bổ sung dữ liệu thiếu cho các đề xuất cũ.

---

## 6. Phân quyền và vai trò

### 6.1. Danh sách vai trò

| Vai trò       | Quyền hạn chính                                                       |
|---------------|----------------------------------------------------------------------|
| `STUDENT`     | Đăng ký sự kiện, điểm danh, làm khảo sát, gửi phản hồi.               |
| `COMMITTEE`   | Tạo và quản lý đề xuất sự kiện, quản lý đăng ký, tạo phiên điểm danh. |
| `DEPARTMENT`  | Theo dõi mức độ tham gia của sinh viên thuộc khoa.                    |
| `ADMIN`       | Phê duyệt đề xuất, quản lý người dùng, xem báo cáo tổng hợp.          |

### 6.2. Cơ chế kiểm soát truy cập

- `AuthorizationInterceptor` chặn các request không hợp lệ trước khi tới controller.
- Mỗi endpoint được gắn với một hoặc nhiều vai trò được phép.
- Token JWT chứa thông tin vai trò, được giải mã ở mỗi request.

### 6.3. Ma trận quyền (rút gọn)

| Chức năng                  | STUDENT | COMMITTEE | DEPARTMENT | ADMIN |
|----------------------------|:-------:|:---------:|:----------:|:-----:|
| Xem danh sách sự kiện      |   ✔    |     ✔     |     ✔      |   ✔   |
| Đăng ký tham gia           |   ✔    |     ✔     |     ✔      |   ✔   |
| Tạo đề xuất sự kiện        |        |     ✔     |            |   ✔   |
| Phê duyệt đề xuất          |        |           |            |   ✔   |
| Tạo phiên điểm danh        |        |     ✔     |            |   ✔   |
| Xem báo cáo tham gia       |        |     ✔     |     ✔      |   ✔   |
| Quản lý người dùng         |        |           |            |   ✔   |

---

## 7. Luồng nghiệp vụ chính

### 7.1. Đề xuất và phê duyệt sự kiện

1. Ban tổ chức (COMMITTEE) tạo một `EventProposal` với đầy đủ thông tin.
2. Đề xuất ở trạng thái `PENDING`.
3. Quản trị viên xem xét và quyết định `APPROVED` hoặc `REJECTED`.
4. Khi được duyệt, hệ thống tạo một `Event` tương ứng và công bố.
5. Sinh viên có thể bắt đầu đăng ký tham gia.

### 7.2. Đăng ký tham gia

1. Sinh viên đăng nhập và xem danh sách sự kiện đang mở.
2. Chọn sự kiện và nhấn đăng ký.
3. Hệ thống kiểm tra điều kiện (còn slot, chưa đăng ký, đủ điều kiện).
4. Tạo một `Ticket` và gửi xác nhận.

### 7.3. Điểm danh bằng QR

1. Ban tổ chức tạo `AttendanceSession` cho sự kiện.
2. Hệ thống sinh mã QR cho phiên điểm danh.
3. Sinh viên quét QR khi tham dự.
4. Hệ thống ghi nhận `Attendance` kèm thời gian.
5. Trạng thái điểm danh được cập nhật theo thời gian thực.

### 7.4. Khảo sát và phản hồi

1. Sau sự kiện, sinh viên làm bài khảo sát (quiz) nếu có.
2. Sinh viên gửi `EventFeedback` đánh giá sự kiện.
3. Hệ thống lưu trữ và tổng hợp.
4. AI phân tích phản hồi để đưa ra đánh giá định tính.

### 7.5. Phân tích phản hồi bằng AI

- `FeedbackAiAnalysisService` gửi nội dung phản hồi tới mô hình AI.
- Kết quả gồm: cảm xúc chung (tích cực/tiêu cực), chủ đề nổi bật, gợi ý cải thiện.
- Ban tổ chức dùng kết quả này để rút kinh nghiệm cho sự kiện sau.

---

## 8. Tài liệu API

> Lưu ý: các đường dẫn dưới đây mang tính minh họa, xem mã nguồn controller để biết chi tiết.

### 8.1. Xác thực

| Method | Endpoint            | Mô tả                          | Quyền     |
|--------|---------------------|--------------------------------|-----------|
| POST   | `/api/auth/register`| Đăng ký tài khoản sinh viên    | Công khai |
| POST   | `/api/auth/login`   | Đăng nhập, trả về JWT          | Công khai |
| GET    | `/oauth-success`    | Callback sau đăng nhập Google  | Công khai |

**Ví dụ request đăng nhập:**

```json
POST /api/auth/login
{
  "username": "sv001@fpt.edu.vn",
  "password": "matkhau123"
}
```

**Ví dụ response:**

```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6...",
  "role": "STUDENT",
  "fullName": "Nguyễn Văn A"
}
```

### 8.2. Sự kiện

| Method | Endpoint              | Mô tả                       | Quyền       |
|--------|-----------------------|-----------------------------|-------------|
| GET    | `/api/events`         | Danh sách sự kiện           | Đã đăng nhập|
| GET    | `/api/events/{id}`    | Chi tiết sự kiện            | Đã đăng nhập|
| POST   | `/api/events/{id}/register` | Đăng ký tham gia      | STUDENT     |

### 8.3. Đề xuất sự kiện

| Method | Endpoint                 | Mô tả                 | Quyền     |
|--------|--------------------------|-----------------------|-----------|
| GET    | `/api/proposals`         | Danh sách đề xuất     | COMMITTEE |
| POST   | `/api/proposals`         | Tạo đề xuất mới       | COMMITTEE |
| PUT    | `/api/proposals/{id}/approve` | Phê duyệt        | ADMIN     |
| PUT    | `/api/proposals/{id}/reject`  | Từ chối          | ADMIN     |

### 8.4. Điểm danh

| Method | Endpoint                       | Mô tả                | Quyền     |
|--------|--------------------------------|----------------------|-----------|
| POST   | `/api/attendance/sessions`     | Tạo phiên điểm danh  | COMMITTEE |
| POST   | `/api/attendance/checkin`      | Điểm danh bằng QR    | STUDENT   |
| GET    | `/api/attendance/status/{id}`  | Trạng thái điểm danh | COMMITTEE |

### 8.5. Phản hồi

| Method | Endpoint                  | Mô tả                  | Quyền   |
|--------|---------------------------|------------------------|---------|
| POST   | `/api/feedback`           | Gửi phản hồi           | STUDENT |
| GET    | `/api/feedback/{eventId}` | Xem phản hồi sự kiện   | COMMITTEE|
| GET    | `/api/feedback/{eventId}/analysis` | Phân tích AI  | COMMITTEE|

### 8.6. Quy ước phản hồi lỗi

```json
{
  "timestamp": "2026-06-29T12:00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Email đã được sử dụng",
  "path": "/api/auth/register"
}
```

---

## 9. Bảo mật

### 9.1. Xác thực JWT

- Sau khi đăng nhập thành công, hệ thống cấp một JWT.
- Token được gửi kèm trong header `Authorization: Bearer <token>`.
- Token có thời hạn, hết hạn cần đăng nhập lại.

### 9.2. Đăng nhập OAuth2 (Google)

- `OAuth2LoginSuccessHandler` xử lý sau khi Google xác thực thành công.
- `OAuth2TokenStore` lưu trữ token tạm thời.
- `StudentIdentityService` ánh xạ email Google sang tài khoản sinh viên.
- Chỉ chấp nhận email thuộc tên miền của trường.

### 9.3. Giới hạn số lần thử

- `AttemptLimiter` giới hạn số lần đăng nhập sai để chống brute-force.
- Sau số lần thất bại nhất định, tài khoản bị khóa tạm thời.

### 9.4. Các biện pháp khác

- Mật khẩu được băm trước khi lưu (không lưu plaintext).
- Kiểm tra và làm sạch dữ liệu đầu vào để tránh injection.
- Sử dụng HTTPS ở môi trường production.
- Phân quyền chặt chẽ qua interceptor cho từng endpoint.

---

## 10. Tích hợp bên thứ ba

### 10.1. Google OAuth2

- Dùng để đăng nhập nhanh bằng tài khoản trường.
- Cấu hình `client-id` và `client-secret` trong biến môi trường.

### 10.2. Google Forms API

- `GoogleFormsApiService` tích hợp với Google Forms để tạo và đồng bộ khảo sát.
- Cho phép thu thập câu trả lời từ form và đưa vào hệ thống.

### 10.3. Dịch vụ AI phân tích phản hồi

- Gửi nội dung phản hồi tới một mô hình ngôn ngữ.
- Nhận về tóm tắt, phân loại cảm xúc và gợi ý cải thiện.

---

## 11. Hướng dẫn cài đặt

### 11.1. Yêu cầu hệ thống

- Java Development Kit (JDK) 17 trở lên.
- Maven 3.9+ (hoặc dùng Maven Wrapper kèm theo).
- PostgreSQL hoặc SQL Server.
- Git.

### 11.2. Các bước cài đặt

1. Clone repository:

```bash
git clone <repository-url>
cd CampusEvent
```

2. Tạo cơ sở dữ liệu và chạy script khởi tạo:

```bash
psql -U postgres -d campusevent -f database_full.sql
```

3. Cấu hình kết nối database trong `application.properties`.

4. Build dự án:

```bash
./mvnw clean install
```

5. Chạy ứng dụng:

```bash
./mvnw spring-boot:run
```

6. Truy cập ứng dụng tại `http://localhost:8080`.

### 11.3. Tài khoản mặc định

Sau khi `DataSeeder` chạy, hệ thống tạo sẵn một số tài khoản mẫu để thử nghiệm. Vui lòng đổi
mật khẩu các tài khoản này trước khi đưa vào sử dụng thực tế.

---

## 12. Cấu hình môi trường

### 12.1. Biến môi trường quan trọng

| Biến                    | Mô tả                                | Bắt buộc |
|-------------------------|--------------------------------------|:--------:|
| `DATABASE_URL`          | Chuỗi kết nối database               |    ✔     |
| `DATABASE_USERNAME`     | Tên người dùng database              |    ✔     |
| `DATABASE_PASSWORD`     | Mật khẩu database                    |    ✔     |
| `JWT_SECRET`            | Khóa bí mật để ký JWT                |    ✔     |
| `GOOGLE_CLIENT_ID`      | Client ID cho OAuth2                 |    ✔     |
| `GOOGLE_CLIENT_SECRET`  | Client Secret cho OAuth2            |    ✔     |
| `AI_API_KEY`            | Khóa API cho dịch vụ phân tích AI    | Tùy chọn |

### 12.2. Profile cấu hình

- `application.properties`: cấu hình mặc định cho môi trường phát triển.
- `application-render.properties`: cấu hình riêng cho môi trường Render.
- Kích hoạt profile bằng `spring.profiles.active=render`.

### 12.3. Lưu ý về bảo mật cấu hình

- Không commit các giá trị bí mật vào git.
- Sử dụng biến môi trường hoặc dịch vụ quản lý secret.
- Tệp `.gitignore` đã loại trừ các tệp cấu hình nhạy cảm.

---

## 13. Triển khai (Deployment)

### 13.1. Render + Neon

Dự án được thiết kế để triển khai trên Render (nền tảng PaaS) kết hợp với Neon (PostgreSQL
serverless). Chi tiết xem trong tệp `DEPLOY_RENDER_NEON.md`.

### 13.2. Các bước tóm tắt

1. Tạo database trên Neon và lấy chuỗi kết nối.
2. Tạo web service mới trên Render, trỏ tới repository.
3. Cấu hình các biến môi trường cần thiết.
4. Render tự động build và chạy ứng dụng khi có commit mới.

### 13.3. Migration dữ liệu

- Lớp `SqlServerToPostgresMigration` hỗ trợ chuyển dữ liệu từ SQL Server sang PostgreSQL.
- Sử dụng khi chuyển môi trường phát triển sang production.

---

## 14. Kiểm thử

### 14.1. Cấu trúc test

- Unit test nằm trong `src/test/java/com/example/`.
- Đặt tên test theo lớp được kiểm thử, ví dụ `AuthControllerTest`.

### 14.2. Chạy test

```bash
./mvnw test
```

### 14.3. Phạm vi kiểm thử hiện có

- `AuthControllerTest`: kiểm thử luồng đăng ký và đăng nhập.
- `GoogleFormsApiServiceTest`: kiểm thử tích hợp Google Forms.

### 14.4. Khuyến nghị

- Viết test cho mọi service chứa logic nghiệp vụ phức tạp.
- Sử dụng database trong bộ nhớ (H2) cho test tích hợp khi phù hợp.
- Duy trì độ phủ test ở mức hợp lý cho các luồng quan trọng.

---

## 15. Quy ước phát triển

### 15.1. Quy ước commit

- Viết commit message rõ ràng, ngắn gọn, mô tả "tại sao" thay đổi.
- Một commit nên tập trung vào một thay đổi logic.

### 15.2. Quy ước nhánh

- `main`: nhánh ổn định, dùng để triển khai.
- Nhánh tính năng đặt theo tên thành viên hoặc tính năng (ví dụ `TuanAnh`).
- Tạo pull request để merge vào `main` sau khi review.

### 15.3. Quy ước mã nguồn

- Tuân thủ quy ước đặt tên Java (camelCase, PascalCase).
- Tách logic nghiệp vụ ra khỏi controller.
- Dùng DTO cho dữ liệu trao đổi qua API.
- Tránh comment thừa, chỉ comment khi cần giải thích ý đồ.

### 15.4. Quy trình làm việc

1. Tạo nhánh mới từ `main`.
2. Phát triển và kiểm thử cục bộ.
3. Push và tạo pull request.
4. Review, chỉnh sửa theo góp ý.
5. Merge khi được duyệt và CI thành công.

---

## 16. Lộ trình phát triển

### 16.1. Đã hoàn thành

- Đăng ký, đăng nhập (JWT và OAuth2).
- Quản lý sự kiện và đề xuất sự kiện.
- Đăng ký tham gia sự kiện.
- Điểm danh bằng QR.
- Khảo sát và phản hồi.
- Phân tích phản hồi bằng AI.

### 16.2. Đang phát triển

- Cải thiện giao diện quản trị.
- Bổ sung thống kê và biểu đồ trực quan.
- Tối ưu hiệu năng truy vấn.

### 16.3. Dự kiến tương lai

- Ứng dụng di động.
- Thông báo đẩy (push notification).
- Tích hợp lịch (Google Calendar).
- Hệ thống điểm thưởng cho sinh viên tích cực.

---

## 17. Câu hỏi thường gặp

**Hỏi: Tôi quên mật khẩu thì làm thế nào?**
Đáp: Hiện hệ thống hỗ trợ đăng nhập bằng Google. Tính năng đặt lại mật khẩu qua email đang
được phát triển.

**Hỏi: Vì sao tôi không đăng ký được sự kiện?**
Đáp: Có thể sự kiện đã hết slot, đã đóng đăng ký, hoặc bạn đã đăng ký trước đó.

**Hỏi: Mã QR điểm danh có hết hạn không?**
Đáp: Có, mỗi phiên điểm danh có khung thời gian hiệu lực do ban tổ chức quy định.

**Hỏi: Dữ liệu phản hồi của tôi có ẩn danh không?**
Đáp: Phản hồi được lưu kèm thông tin để phục vụ thống kê, nhưng kết quả phân tích AI được
tổng hợp ở mức sự kiện chứ không công khai từng cá nhân.

**Hỏi: Làm sao để trở thành ban tổ chức?**
Đáp: Liên hệ quản trị viên để được cấp quyền `COMMITTEE`.

---

## Phụ lục: Bảng thuật ngữ

| Thuật ngữ          | Giải thích                                                      |
|--------------------|----------------------------------------------------------------|
| AEMS               | Academic Event Management System - tên gọi khác của hệ thống.  |
| Proposal           | Đề xuất sự kiện chờ phê duyệt.                                  |
| Session            | Phiên điểm danh gắn với một sự kiện.                            |
| Ticket             | Bản ghi đăng ký tham gia của sinh viên.                        |
| Committee          | Ban tổ chức sự kiện.                                            |
| JWT                | JSON Web Token, dùng cho xác thực không trạng thái.            |
| OAuth2             | Giao thức ủy quyền, dùng để đăng nhập bằng Google.            |

---

*Tài liệu được duy trì bởi đội ngũ phát triển CampusEvent. Vui lòng cập nhật khi có thay đổi
về kiến trúc hoặc tính năng.*
