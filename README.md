# CampusEvent

CampusEvent là hệ thống quản lý sự kiện sinh viên cho campus FPT. Dự án dùng Spring Boot, Spring Security, Spring Data JPA và giao diện HTML/CSS/JavaScript tĩnh trong `src/main/resources/static`.

## Tính năng chính

- Đăng ký, đăng nhập, quên mật khẩu và OAuth2 Google.
- Landing page giới thiệu sự kiện và ảnh campus FPT.
- Màn hình theo vai trò: Student, Department, Committee, Admin.
- Quản lý event, proposal, registration, attendance, feedback, email log.
- Dashboard admin với thống kê, phân quyền và dữ liệu vận hành.

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

Tạo database trong SQL Server:

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

## Ghi chú bảo mật

Không commit secret thật lên repo nhóm. Các giá trị như OAuth client secret, SMTP password hoặc database password nên được thay bằng biến môi trường hoặc file cấu hình riêng khi triển khai.

## License

Dự án SWP - All Rights Reserved.
