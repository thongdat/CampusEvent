# Báo cáo kết quả Unit Test - CampusEvent (AEMS)

> Tổng hợp **phần nào được test, test cái gì, kết quả ra sao**, phân theo nhiệm vụ 4 thành viên.
> Lệnh chạy: `mvn test` — Kết quả gần nhất: **42/42 PASS**, 0 lỗi.

## Tổng quan theo thành viên

| Thành viên | Module | File test | Số ca | Kết quả |
|-----------|--------|-----------|:----:|:------:|
| **Anh (AnhNVT)** | Xác thực & Bảo mật | `AuthServiceTest` | 7 | ✅ PASS |
| | | `AttemptLimiterTest` | 5 | ✅ PASS |
| **Đạt (DatHVT)** | Sinh viên / RBL / Đăng ký | `PriorityRankingServiceTest` | 13 | ✅ PASS |
| | | `TicketServiceTest` | 5 | ✅ PASS |
| **Tú (TuHNC)** | Điểm danh & Quiz | `AttendanceServiceTest` | 7 | ✅ PASS |
| **Sang (SangTM)** | Quản trị / Sự kiện / Đề xuất | `AcademicStructureTest` | 5 | ✅ PASS |
| | (đã có sẵn) | `GoogleFormsApiServiceTest` | 1 | ✅ PASS |
| **TỔNG** | | | **43** | ✅ **PASS** |

> Ghi chú: bảng chi tiết dưới đây liệt kê 42 ca test mới/chính. Ngoài ra dự án còn các test có sẵn khác (`AuthControllerTest`, `CommitteeControllerTest`, `RegistrationRepositoryTest`, `GenderConverterTest`, `GoogleOAuthAccessTokenServiceTest`).

---

## 1. Anh - Xác thực & Bảo mật

### 1.1 `AuthServiceTest` - Đăng ký & Quên mật khẩu (7 ca)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Mật khẩu < 8 ký tự | Chặn mật khẩu quá ngắn | ✅ |
| 2 | Xác nhận mật khẩu không khớp | Chặn khi 2 ô mật khẩu khác nhau | ✅ |
| 3 | Vai trò không hợp lệ (ADMIN) | Chỉ cho STUDENT/DEPARTMENT | ✅ |
| 4 | Email đã tồn tại | Chặn trùng email | ✅ |
| 5 | Số điện thoại sai định dạng | Bắt buộc đúng 10 chữ số | ✅ |
| 6 | Quên MK: email không tồn tại | Báo lỗi rõ ràng | ✅ |
| 7 | Xác minh OTP: email không tồn tại | Báo lỗi rõ ràng | ✅ |

### 1.2 `AttemptLimiterTest` - Chống brute-force (5 ca)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Tài khoản mới | Chưa sai lần nào → không bị khóa | ✅ |
| 2 | Sai 4 lần | Dưới ngưỡng 5 → vẫn cho thử | ✅ |
| 3 | Sai đủ 5 lần | Đạt ngưỡng → khóa tạm | ✅ |
| 4 | Đăng nhập thành công | Reset bộ đếm → gỡ khóa | ✅ |
| 5 | Chuẩn hóa key | Không phân biệt hoa/thường, khoảng trắng | ✅ |

---

## 2. Đạt - Sinh viên / RBL / Đăng ký

### 2.1 `PriorityRankingServiceTest` - Công thức RBL (13 ca)

Công thức: `Priority = 0.40·M + 0.30·S + 0.20·P + 0.10·T`

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | M: Đúng chuyên ngành | → 100 điểm | ✅ |
| 2 | M: Cùng khoa (liên quan) | → 60 điểm | ✅ |
| 3 | M: Khác khoa | → 30 điểm | ✅ |
| 4 | S: Kỳ 9 (biên trên) | → 100 điểm | ✅ |
| 5 | S: Kỳ 1 (biên dưới) | → ~11.11 điểm | ✅ |
| 6 | S: Kỳ > 9 | Chặn tại kỳ 9 → 100 điểm | ✅ |
| 7 | P: 0 điểm | → 0 | ✅ |
| 8 | P: 50 điểm (≤100) | Giữ nguyên 50 | ✅ |
| 9 | P: 400 điểm (>100) | Chuẩn hóa, chặn tại 100 | ✅ |
| 10 | T: Đăng ký sớm | Trong 20% đầu → 100 điểm | ✅ |
| 11 | T: Đăng ký muộn | Sau 70% → 40 điểm | ✅ |
| 12 | Tổng: SV lý tưởng | Tổng 100, tier HIGH | ✅ |
| 13 | Tổng: SV yếu | Khác ngành/kỳ thấp/muộn → tier LOW | ✅ |

### 2.2 `TicketServiceTest` - Đồng bộ vé theo đăng ký (5 ca)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Đăng ký null/chưa có id | Không phát vé | ✅ |
| 2 | Đã có vé | Trả lại vé cũ, không tạo mới | ✅ |
| 3 | Chưa có vé | Tạo & lưu vé mới có mã | ✅ |
| 4 | Trạng thái REGISTERED | Tự động phát vé | ✅ |
| 5 | Trạng thái WAITLIST | Thu hồi vé (nếu có) | ✅ |

---

## 3. Tú - Điểm danh & Quiz

### 3.1 `AttendanceServiceTest` - Tính điểm tham gia (7 ca)

Điểm = check-in(40) + mid(20) + quiz(tối đa 20) + feedback(10) + check-out(10)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Phân loại ≥90 | → Excellent Participation | ✅ |
| 2 | Phân loại 70..89 | → Active Participation | ✅ |
| 3 | Phân loại 50..69 | → Partial Participation | ✅ |
| 4 | Phân loại <50 | → Low Participation | ✅ |
| 5 | Chưa làm gì | Điểm = 0 | ✅ |
| 6 | Hoàn thành đầy đủ | Điểm = 100 | ✅ |
| 7 | Chỉ check-in + quiz 50% | Điểm = 50 | ✅ |

---

## 4. Sang - Quản trị / Sự kiện / Đề xuất

### 4.1 `AcademicStructureTest` - Ánh xạ ngành/khoa (5 ca)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Ngành thuộc khoa | Tìm đúng khoa cha | ✅ |
| 2 | Tên viết tắt/tiếng Anh | CNTT, IT, Economics → đúng khoa | ✅ |
| 3 | Bỏ dấu & hoa/thường | Chuẩn hóa chuỗi khi so sánh | ✅ |
| 4 | Tên không tồn tại | Trả về "Khác" | ✅ |
| 5 | Kiểm tra thuộc khoa | Ngành có/không thuộc khoa | ✅ |

### 4.2 `GoogleFormsApiServiceTest` - Retry Google API (đã có sẵn)

| # | Ca kiểm thử | Kiểm tra điều gì | Kết quả |
|---|-------------|------------------|:------:|
| 1 | Phân biệt lỗi tạm thời | 429/5xx retry, 4xx không retry | ✅ |

---

## Kỹ thuật kiểm thử đã áp dụng

| Kỹ thuật | Áp dụng ở |
|----------|-----------|
| White-box (theo nhánh code) | RBL, AttemptLimiter, AuthService, AttendanceService |
| Boundary Value Analysis (giá trị biên) | Kỳ 1/9/>9; điểm 0/50/400; ngưỡng khóa 4/5; điểm phân loại 90/70/50 |
| Equivalence Partitioning (phân vùng tương đương) | Ánh xạ ngành/khoa; vai trò hợp lệ/không |
| Mock (Mockito - giả lập DB/service) | TicketService, AuthService, AttendanceService |

## Cách chạy lại

```bash
mvn test
```

Xem báo cáo HTML tự sinh: `target/reports/surefire.html`
