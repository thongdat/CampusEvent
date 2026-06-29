# CampusEvent (AEMS) — Test Data cho toàn bộ dự án

Tài liệu này gồm 2 phần:
1. **Tài khoản đăng nhập** (do `DataSeeder` tạo sẵn khi chạy app).
2. **Bộ dữ liệu nghiệp vụ tất định** trong `docs/test-data.sql` để kiểm thử mọi tính năng.

---

## 1. Tài khoản đăng nhập (mật khẩu chung: `Campus@2026`)

> `DataSeeder` tự tạo khi `app.seed.enabled=true` (mặc định). Mật khẩu đổi qua biến môi trường `DEMO_*_PASSWORD`.

| Vai trò | Email đăng nhập | Mật khẩu | Ghi chú |
|--------|------------------|----------|---------|
| ADMIN | `aems.admin01@uni.edu.vn` | `Campus@2026` | Toàn quyền |
| ADMIN | `aems.admin02@uni.edu.vn` | `Campus@2026` | |
| ADMIN (khoá) | `locked@example.com` | `locked123` | `status=false` → test đăng nhập bị khoá |
| MANAGER (Trưởng khoa/HEAD) | `dept01@uni.edu.vn` … `dept12@uni.edu.vn` | `Campus@2026` | Mỗi khoa 1 tài khoản (12 khoa) |
| COMMITTEE | `committee01@uni.edu.vn` … `committee08@uni.edu.vn` | `Campus@2026` | Hội đồng duyệt |
| STUDENT | `student001@uni.edu.vn` … `student096@uni.edu.vn` | `Campus@2026` | 96 sinh viên; vài tài khoản bị khoá (vd `student023`) |

> **Lưu ý:** seeder không tạo sẵn tài khoản role **DEPARTMENT (STAFF)**. Muốn test STAFF, dùng chức năng tự đăng ký (role DEPARTMENT) trên `login.html`, hoặc Admin tạo user với role DEPARTMENT.

**Tài khoản dùng nhanh để demo:**
- Admin: `aems.admin01@uni.edu.vn`
- Trưởng khoa CNTT: `dept01@uni.edu.vn`
- Hội đồng: `committee01@uni.edu.vn`
- Sinh viên: `student001@uni.edu.vn`

---

## 2. Bộ dữ liệu nghiệp vụ (`docs/test-data.sql`)

### Cách nạp

Cần một DB đã chạy app ít nhất 1 lần (đã có schema + seeder). Sau đó:

```powershell
psql "postgresql://postgres:postgres@localhost:5432/campus_event" -f docs/test-data.sql
```

- Tất cả bản ghi mang tiền tố **`[QA-TEST]`**.
- Script tự **xoá dữ liệu `[QA-TEST]` cũ** rồi chèn lại → chạy lại nhiều lần vẫn sạch, **không đụng** dữ liệu seeder.
- File **không tạo user mới** (tái dùng `student001..student005` đã seed) nên không cần băm mật khẩu.

### Dữ liệu được tạo & dùng để test cái gì

| Nhóm | Bản ghi | Phục vụ test |
|------|---------|--------------|
| **Proposal** | 4 cái: PENDING, REVISION, APPROVED, REJECTED (APPROVED có sẵn `quiz_payload`) | Luồng Committee duyệt/từ chối/yêu cầu sửa; publish proposal |
| **Event** | `Sự kiện ĐĂNG KÝ` (PUBLISHED, sức chứa **3**), `Sự kiện HÔM NAY` (PUBLISHED, đang diễn ra), `Sự kiện ĐÃ KẾT THÚC` (COMPLETED), `Sự kiện QUIZ 1 CÂU` (COMPLETED), `Sự kiện ĐÃ HUỶ` (CANCELLED) | Đăng ký, check-in, report, trạng thái event |
| **Registration** | ĐĂNG KÝ: 3 REGISTERED + 1 WAITLIST + 1 CANCELLED | **Test waitlist** (đầy 3 chỗ → người thứ 4 vào chờ); huỷ đăng ký |
| **Ticket** | Vé cho các đăng ký REGISTERED | Hiển thị vé trong "Đăng ký của tôi" |
| **Attendance** | Sự kiện ĐÃ KẾT THÚC: COMPLETED / CHECKED_IN / ABSENT | Dashboard tham gia, tỉ lệ check-in, đánh vắng |
| **Quiz (đã kết thúc)** | 2 trắc nghiệm + 1 tự luận; 2 lượt nộp (đúng 100% và đúng 50%) | Kết quả quiz, câu khó nhất |
| **Quiz (1 câu)** | 1 trắc nghiệm / 1 điểm; 1 lượt nộp đúng → **100%** | Kiểm chứng phân tích AI quiz "1 câu/1 điểm" → "Hiểu bài tốt" |
| **Feedback (1-5)** | 2 phản hồi (5★, 4★) | Feedback legacy của sinh viên |
| **Event_feedback (checkout)** | 2 phản hồi 4 thang điểm + bình luận (1 tích cực, 1 nêu vấn đề "âm thanh nhỏ", "thời gian dài") | **Phân tích AI feedback**: sentiment, khía cạnh, khuyến nghị |
| **Attendance_session** | Token `QATESTTOKEN0001` ACTIVE cho `Sự kiện HÔM NAY` | Test QR check-in còn hiệu lực |
| **Email log** | 1 SENT + 1 FAILED | Màn hình Email Logs của Admin |
| **Activity log** | REGISTER_EVENT (+5), FEEDBACK (+8) | Điểm tích lũy, nhật ký hoạt động |

### Kịch bản test gợi ý theo vai trò

- **Committee** (`committee01`): mở `[QA-TEST] Proposal PENDING` → duyệt; `Proposal REVISION` → yêu cầu sửa; `Proposal REJECTED` đã ở trạng thái cuối.
- **Trưởng khoa** (`dept01`): vào **Reports** xem `Sự kiện ĐÃ KẾT THÚC` (tỉ lệ check-in, điểm hài lòng); vào **AEMS Toolkit** chiếu QR cho `Sự kiện HÔM NAY`; mở **Feedback + AI** để xem phân tích.
- **Sinh viên** (`student006` — chưa đăng ký sự kiện ĐĂNG KÝ): thử đăng ký `[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)` → vì đã đủ 3 chỗ nên sẽ vào **WAITLIST**.
- **Admin**: kiểm tra Users/Email Logs/Reports toàn hệ thống.

### Kiểm tra nhanh sau khi nạp

```sql
SELECT title, status FROM event WHERE title LIKE '[QA-TEST]%' ORDER BY id;
SELECT status, count(*) FROM registration r JOIN event e ON e.id=r.event_id
  WHERE e.title LIKE '[QA-TEST]%' GROUP BY status;
```
