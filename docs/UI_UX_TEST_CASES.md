# Kịch bản kiểm thử UI/UX - CampusEvent (AEMS)

> Tài liệu này dùng để **demo kiểm thử giao diện (UI) và trải nghiệm người dùng (UX)** theo hướng **Black-box / Manual**.
> Người kiểm thử làm theo từng bước và đánh dấu Pass/Fail. Trình duyệt khuyến nghị: **Chrome / Edge (bản mới nhất)**.

## 1. Quy ước

| Ký hiệu | Ý nghĩa |
|---|---|
| **TC** | Test Case (ca kiểm thử) |
| **Steps** | Các bước thực hiện |
| **Expected** | Kết quả mong đợi |
| **Priority** | Mức ưu tiên (Critical / High / Medium) |
| Kết quả | Ghi **Pass** / **Fail** khi chạy |

Tài khoản demo: dùng các tài khoản seed sẵn (student / department / committee / admin).

---

## 2. Kiểm thử chức năng UI (Functional UI)

| TC | Màn hình | Steps | Expected | Priority | Kết quả |
|----|----------|-------|----------|----------|---------|
| UI-01 | Đăng nhập | Nhập email + mật khẩu đúng → Đăng nhập | Chuyển đúng trang theo vai trò (SV/Khoa/Hội đồng/Admin) | Critical | |
| UI-02 | Đăng nhập | Nhập sai mật khẩu 5 lần | Hiện thông báo bị khóa tạm, không cho đăng nhập | High | |
| UI-03 | Đăng ký | Bỏ trống trường bắt buộc → Submit | Hiển thị lỗi validate ngay dưới ô nhập, không gửi form | High | |
| UI-04 | Đăng ký | Nhập email sai định dạng | Báo lỗi "email không hợp lệ" | Medium | |
| UI-05 | Landing | Mở trang chủ khi chưa đăng nhập | Hiện thống kê + sự kiện nổi bật, nút đăng nhập/đăng ký | Medium | |
| UI-06 | Duyệt sự kiện (SV) | Lọc theo khoa và từ khóa | Danh sách cập nhật đúng bộ lọc | High | |
| UI-07 | Chi tiết sự kiện | Mở 1 sự kiện | Hiện điểm ưu tiên tách M/S/P/T (tooltip/progress bar) | High | |
| UI-08 | Đăng ký sự kiện | Bấm "Đăng ký" khi còn slot | Hiện trạng thái REGISTERED + mã vé | Critical | |
| UI-09 | Đăng ký sự kiện | Đăng ký khi đã đầy chỗ | Hiện trạng thái WAITLIST (hàng đợi) | High | |
| UI-10 | Quản lý phòng (Admin) | Thêm/sửa/xóa phòng | Bảng cập nhật, có phân trang & tìm kiếm | High | |
| UI-11 | Tạo đề xuất (Khoa) | Chọn địa điểm | Là **dropdown chọn phòng** (không nhập tay) | Medium | |
| UI-12 | Đóng đăng ký (Admin) | Bấm "Đóng sự kiện {tên}" | Hiện hộp xác nhận ngắn gọn, sau khi xác nhận thì SV không đăng ký được nữa | High | |
| UI-13 | Phân quyền | Đăng nhập SV rồi mở URL trang admin | Bị chặn / chuyển hướng, không xem được | Critical | |

---

## 3. Kiểm thử trải nghiệm (UX)

| TC | Tiêu chí | Cách kiểm tra | Expected | Kết quả |
|----|----------|---------------|----------|---------|
| UX-01 | Rõ ràng | Xem nhãn nút, tiêu đề | Dùng từ ngữ dễ hiểu, nhất quán tiếng Việt | |
| UX-02 | Phản hồi thao tác | Sau khi bấm đăng ký/lưu | Có thông báo thành công/thất bại rõ ràng (toast/alert) | |
| UX-03 | Trạng thái chờ | Thao tác gọi server (đăng ký, gửi email) | Có loading/disable nút, tránh double-click | |
| UX-04 | Chống nhầm lẫn | Hành động nguy hiểm (xóa, đóng sự kiện) | Có hộp xác nhận trước khi thực hiện | |
| UX-05 | Điều hướng | Di chuyển giữa các trang | Có menu/breadcrumb, quay lại dễ dàng | |
| UX-06 | Thông báo lỗi | Nhập sai dữ liệu | Lỗi hiển thị gần ô nhập, màu dễ thấy | |
| UX-07 | Nhất quán | So sánh các trang | Màu sắc, font, khoảng cách đồng bộ | |

---

## 4. Kiểm thử giao diện đáp ứng (Responsive) & tương thích

| TC | Thiết bị/kích thước | Expected | Kết quả |
|----|---------------------|----------|---------|
| RS-01 | Desktop 1920×1080 | Bố cục đầy đủ, không tràn ngang | |
| RS-02 | Laptop 1366×768 | Nội dung co giãn hợp lý | |
| RS-03 | Tablet ~768px | Menu/bảng chuyển bố cục phù hợp | |
| RS-04 | Mobile ~375px | Chữ đọc được, nút bấm đủ lớn | |
| CP-01 | Chrome mới nhất | Hiển thị & chức năng đúng | |
| CP-02 | Edge mới nhất | Hiển thị & chức năng đúng | |

> Mẹo demo: dùng **DevTools (F12) → Toggle device toolbar (Ctrl+Shift+M)** để đổi kích thước nhanh.

---

## 5. Cách thực hiện khi demo cho thầy

1. Khởi động ứng dụng, mở trình duyệt tới trang đăng nhập.
2. Chạy tuần tự các TC ở mục 2 → 3 → 4, đánh dấu Pass/Fail vào cột **Kết quả**.
3. Chụp màn hình các bước quan trọng (đăng ký thành công, WAITLIST, bị chặn phân quyền…).
4. Tổng kết: số TC Pass / tổng số, liệt kê lỗi (nếu có).

> (Nâng cao – tùy chọn) Có thể tự động hóa UI bằng **Selenium WebDriver**. Với đồ án demo, kiểm thử tay theo bảng này là đủ và trực quan.
