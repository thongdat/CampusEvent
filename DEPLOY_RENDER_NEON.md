# Deploy CampusEvent lên Render + Neon

## 1. Tạo PostgreSQL miễn phí trên Neon

1. Đăng nhập https://neon.tech và tạo project mới.
2. Trong **Connection Details**, ghi lại `host`, `database`, `user` và `password`.
3. Chọn connection có SSL (nên dùng pooled connection nếu Neon cung cấp).

Giá trị dùng trên Render:

```text
DB_URL=jdbc:postgresql://<NEON_HOST>/<DATABASE>?sslmode=require
DB_USERNAME=<NEON_USER>
DB_PASSWORD=<NEON_PASSWORD>
```

Không commit các giá trị thật vào GitHub.

## 2. Đẩy code lên GitHub

Commit các file chuyển đổi Render + Neon rồi push lên repository GitHub. Render phải đọc được `render.yaml` và `Dockerfile` ở thư mục gốc.

## 3. Tạo dịch vụ trên Render

1. Đăng nhập https://render.com bằng GitHub.
2. Chọn **New > Blueprint**.
3. Kết nối repository `CampusEvent` và chọn branch cần deploy.
4. Render đọc `render.yaml` và yêu cầu nhập các biến có `sync: false`.

`render.yaml` đặt dịch vụ tại Singapore để gần Neon AWS Asia Pacific 1, giảm độ trễ giữa backend và database.

Các biến bắt buộc:

```text
DB_URL
DB_USERNAME
DB_PASSWORD
DEMO_ADMIN_PASSWORD
DEMO_DEPARTMENT_PASSWORD
DEMO_COMMITTEE_PASSWORD
DEMO_STUDENT_PASSWORD
```

Bốn mật khẩu demo phải dài ít nhất 8 ký tự. Có thể dùng cùng một mật khẩu mạnh cho buổi demo, nhưng không dùng mật khẩu cá nhân.

Các biến tùy chọn:

```text
SMTP_USERNAME
SMTP_PASSWORD
GOOGLE_CLIENT_ID
GOOGLE_CLIENT_SECRET
GOOGLE_REDIRECT_URI
APP_PUBLIC_BASE_URL
```

Nếu chưa cấu hình Gmail/Google OAuth, có thể bỏ qua các biến tùy chọn. Ứng dụng dùng placeholder an toàn để tự ẩn nút Google; đăng nhập bằng tài khoản demo vẫn hoạt động.

## 4. Cập nhật URL công khai

Sau deploy đầu tiên, Render cung cấp URL dạng:

```text
https://campus-event-xxxx.onrender.com
```

Vào **Environment** của Render và đặt:

```text
APP_PUBLIC_BASE_URL=https://campus-event-xxxx.onrender.com
GOOGLE_REDIRECT_URI=https://campus-event-xxxx.onrender.com/api/login/oauth2/code/google
```

Nếu dùng Google Login, thêm chính xác `GOOGLE_REDIRECT_URI` vào **Authorized redirect URIs** trong Google Cloud Console rồi redeploy.

## 5. Kiểm tra

```text
Health check: https://campus-event-xxxx.onrender.com/api/auth/test
Trang chính:  https://campus-event-xxxx.onrender.com/api/landing.html
Đăng nhập:    https://campus-event-xxxx.onrender.com/api/login.html
```

Tài khoản được seed:

```text
Admin:     aems.admin01@uni.edu.vn
Manager:   dept01@uni.edu.vn
Committee: committee01@uni.edu.vn
Student:   student001@uni.edu.vn
```

Mật khẩu là giá trị tương ứng đã nhập ở Render Environment.

## 6. Ghi chú gói miễn phí

Render free web service có thể ngủ khi không có truy cập. Lần mở đầu tiên sau thời gian ngủ sẽ chậm hơn. Neon có thể tạm dừng compute khi không hoạt động và tự bật lại khi ứng dụng kết nối.
