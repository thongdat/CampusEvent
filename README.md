# CampusEvent

CampusEvent là hệ thống quản lý sự kiện sinh viên cho campus FPT. Dự án dùng Spring Boot, Spring Security, Spring Data JPA và giao diện HTML/CSS/JavaScript tĩnh trong `src/main/resources/static`.

## Tính năng chính

- Đăng ký, đăng nhập, quên mật khẩu và OAuth2 Google.
- Landing page giới thiệu sự kiện và ảnh campus FPT.
- Màn hình theo vai trò: Student, Department, Committee, Admin.
- Quản lý event, proposal, registration, attendance, feedback, email log.
- Dashboard admin với thống kê, phân quyền và dữ liệu vận hành.
- **Cơ chế xếp slot ưu tiên thông minh**: thay vì FIFO, hệ thống tính điểm ưu tiên
  cho từng cặp sinh viên - event dựa trên 4 tiêu chí có trọng số
  `0.4·M + 0.3·S + 0.2·P + 0.1·T` (chuyên ngành / học kỳ / điểm hoạt động /
  thời điểm đăng ký). Khi event đầy, người có điểm cao hơn sẽ "giành" slot
  khỏi người có điểm thấp hơn (đẩy về hàng chờ). Khi có người huỷ, hàng chờ
  được tự động lên slot theo điểm cao nhất.

## API sinh viên (`/api/student/*`)

Tất cả endpoint yêu cầu header `X-User-Email` (frontend đọc từ sessionStorage).

| Endpoint | Mục đích |
|---|---|
| `GET /me` | Profile + tổng quan (stats, rank) |
| `GET /events?q=&scope=&faculty=&sort=` | Danh sách event + điểm ưu tiên ước tính cho từng event |
| `GET /events/{id}` | Chi tiết event + breakdown 4 tiêu chí + hàng đợi (top 8) |
| `POST /events/{id}/register` | Đăng ký với cơ chế xếp slot ưu tiên |
| `DELETE /registrations/{id}` | Huỷ và auto-promote hàng chờ |
| `GET /my-registrations` | Danh sách đăng ký + vé + check-in + trạng thái feedback |
| `POST /events/{id}/feedback` | Gửi feedback (chỉ khi đã ATTENDED) |
| `GET /leaderboard` | Top 20 sinh viên theo điểm tích lũy |

## Công nghệ

- Java 17
- Spring Boot 2.7.14
- Spring Web, Spring Security, OAuth2 Client
- Spring Data JPA, Hibernate
- SQL Server
- Maven

## Cấu trúc thư mục

```text
CampusEvent/
|-- pom.xml
|-- mssql-jdbc_auth-10.2.3.x64.dll
|-- apache-maven-3.9.9/
|-- src/
|   |-- main/
|       |-- java/com/example/
|       |   |-- config/
|       |   |-- controller/
|       |   |-- dto/
|       |   |-- model/
|       |   |-- repository/
|       |   |-- security/
|       |   |-- service/
|       |   `-- EventManagementApplication.java
|       `-- resources/
|           |-- application.properties
|           |-- schema.sql
|           `-- static/
|               |-- landing.html
|               |-- login.html
|               |-- register.html
|               |-- screen-student.html
|               |-- screen-department.html
|               |-- screen-committee.html
|               `-- admin-screen/
`-- README.md
```

## Yêu cầu

- JDK 17 trở lên.
- SQL Server đang chạy ở `localhost:1433`.
- Database tên `event_management_db`.
- Windows Authentication cho SQL Server, hoặc chỉnh lại username/password trong `application.properties`.

## Cấu hình database

### Tạo schema + nạp dữ liệu mẫu phong phú (khuyến nghị)

File `database_full.sql` ở thư mục gốc đã chứa toàn bộ schema + bộ dữ liệu mẫu
chuẩn FPT: 15 chuyên ngành, 96 user (3 admin, 15 điều phối khoa, 8 hội đồng, 70
sinh viên), 32 event kèm ảnh chất lượng cao, 35 proposal, ~500 đăng ký, ~280
check-in, ~140 feedback, ~700 email log và ~900 activity log.

1. Mở SSMS, kết nối SQL Server.
2. Mở file `database_full.sql`, bấm **Execute** một lần. Script tự tạo
   `event_management_db` nếu chưa có và reset toàn bộ bảng.
3. Khởi động backend Spring Boot.
4. Hash lại mật khẩu seed:
   ```http
   GET http://localhost:8081/api/auth/init-passwords
   ```
   Sau bước này có thể đăng nhập với các tài khoản mẫu:
   - `admin01@fpt.edu.vn` / `admin123`
   - `dept01@fpt.edu.vn` / `dept123`
   - `com01@fpt.edu.vn` / `com123`
   - `sv001@fpt.edu.vn` / `stu123`

Nếu chỉ muốn tạo database trống:

```sql
CREATE DATABASE event_management_db;
```

Cấu hình hiện tại nằm trong `src/main/resources/application.properties`:

```properties
spring.datasource.url=jdbc:sqlserver://localhost:1433;databaseName=event_management_db;integratedSecurity=true;encrypt=false;trustServerCertificate=true
spring.datasource.driver-class-name=com.microsoft.sqlserver.jdbc.SQLServerDriver
server.port=8081
server.servlet.context-path=/api
```

Nếu dùng tài khoản SQL Server thay vì Windows Authentication, bỏ `integratedSecurity=true` trong URL và bật lại:

```properties
spring.datasource.username=sa
spring.datasource.password=your_password
```

## Chạy project

Trong thư mục repo:

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
```

Hoặc nếu máy đã cài Maven:

```powershell
mvn spring-boot:run
```

Sau khi chạy, mở:

- Landing page: `http://localhost:8081/api/landing.html`
- Login: `http://localhost:8081/api/login.html`
- Admin: `http://localhost:8081/api/admin-screen/overview.html`

## Build

```powershell
.\apache-maven-3.9.9\bin\mvn.cmd -DskipTests package
```

File build nằm trong `target/`. Thư mục này đã được ignore bởi Git.

## Cấu hình Google OAuth (tùy chọn)

Mặc định nút **Đăng nhập với Google** trên `login.html` sẽ tự động ẩn nếu chưa
cấu hình OAuth (endpoint `/api/auth/oauth-status` trả về `googleEnabled=false`).

Để bật:

1. Vào https://console.cloud.google.com/apis/credentials
2. **Create Credentials → OAuth client ID → Web application**.
3. **Authorized redirect URIs** thêm:
   `http://localhost:8081/api/login/oauth2/code/google`
4. Copy `Client ID` và `Client Secret`.
5. Đặt biến môi trường rồi chạy lại Spring Boot:

   ```powershell
   $env:GOOGLE_CLIENT_ID  = "xxxxx.apps.googleusercontent.com"
   $env:GOOGLE_CLIENT_SECRET = "GOCSPX-xxxxxxxx"
   .\apache-maven-3.9.9\bin\mvn.cmd spring-boot:run
   ```

Hoặc sửa trực tiếp `application.properties` (lưu ý không commit secret).

## Ghi chú bảo mật

Không commit secret thật lên repo nhóm. Các giá trị như OAuth client secret, SMTP password hoặc database password nên được thay bằng biến môi trường hoặc file cấu hình riêng khi triển khai.

## License

Dự án SWP - All Rights Reserved.
