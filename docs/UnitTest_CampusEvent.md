# TÀI LIỆU KIỂM THỬ ĐƠN VỊ (UNIT TEST) — CampusEvent / AEMS

> **Phiên bản đã hiệu chỉnh cho khớp mã nguồn thực tế** (JUnit 5 + Mockito).
> Bản này thay thế nội dung cũ trong `UnitTest_CampusEvent.docx` ở những chỗ mô tả API không đúng code.

| Mục | Nội dung |
|-----|----------|
| Dự án | Academic Event Management System (AEMS) — CampusEvent |
| Công nghệ | Spring Boot MVC, Spring Data JPA, SQL Server/PostgreSQL, Thymeleaf, Spring Security |
| Môn học / Lớp | SWR301 — SE20A05 |
| Nhóm | SWR301_SE20A05_Group1 (Group 6) — DatHVT, AnhNVT, TuHNC, SangTM |
| Loại tài liệu | Unit Test Documentation |
| Phiên bản | v3.0 (khớp code) |
| Ngày cập nhật | 2026-07-20 |

---

## 1. Mục tiêu

- Kiểm thử đơn vị xác minh tính đúng đắn của từng phương thức nghiệp vụ độc lập ở tầng Service/Security/Config.
- Bảo đảm công thức ưu tiên `Priority = 0.4·M + 0.3·S + 0.2·P + 0.1·T` luôn nằm trong `[0, 100]`.
- Kiểm soát cơ chế phát/thu hồi vé theo trạng thái đăng ký (REGISTERED ↔ WAITLIST).
- Xác minh công thức điểm tham gia và các quy tắc validate đầu vào khi đăng ký.
- Tạo lưới an toàn (safety net) cho kiểm thử hồi quy tự động khi sửa code (`mvn test`).

## 2. Phạm vi & Công cụ

| Hạng mục | Nội dung |
|----------|----------|
| Kỹ thuật | Hộp trắng (statement/branch), phân tích giá trị biên (BVA), phân vùng tương đương (EP), dùng Mockito mock repository |
| Framework | JUnit 5 (Jupiter) + Mockito |
| Ngôn ngữ / JDK | Java 17 |
| Build | Maven — lệnh: `mvn test` |
| Đối tượng test (thực tế) | `PriorityRankingService`, `TicketService`, `AuthService`, `AttendanceService`, `AttemptLimiter`, `AcademicStructure` + các lớp hỗ trợ (Controller/Repository/Converter/OAuth) |
| Số lớp test / phương thức | **12 lớp test — 68 phương thức `@Test`** (42 ca nghiệp vụ cốt lõi + 26 ca hỗ trợ) |
| Nguyên tắc | Mỗi test độc lập; mock repository; theo mẫu Arrange–Act–Assert |

> **Ghi chú quan trọng (đã sửa cho khớp code):** Bản tài liệu cũ mô tả các lớp/phương thức không tồn tại trong mã nguồn (`EventRegistrationService`, `RegisterValidator`, `PriorityRankingService.majorScore/semesterScore/...`, `AttendanceService.participationScore(int)`, `normalizeGender`). Bản này ánh xạ lại đúng các lớp/phương thức **đang có thật** trong dự án. Xem chi tiết ở **Mục 6 — Nhật ký hiệu chỉnh**.

---

## 3. Danh sách ca kiểm thử đơn vị (42 ca) theo thành viên

### 3.1 Đạt (DatHVT) — Sinh viên / RBL / Vé

**`PriorityRankingServiceTest` — công thức ưu tiên RBL (13 ca)**
Phương thức thật được test: `computeBreakdown(Student, Event, LocalDateTime)` (trả `Breakdown` có `majorScore/semesterScore/pointsScore/timeScore/total`) và `resolvePriorityTier(...)`.

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| STU-U01 | Biên | M = 100 khi đúng chuyên ngành | Pass |
| STU-U02 | Biên | M = 60 khi cùng khoa lớn | Pass |
| STU-U03 | Biên | M = 30 khi khác khoa | Pass |
| STU-U04 | Biên | S(kỳ 9) = 100 | Pass |
| STU-U05 | Biên | S(kỳ 1) ≈ 11.11 | Pass |
| STU-U06 | Biên | S(kỳ > 9) chặn tại 9 → 100 | Pass |
| STU-U07 | Biên | P = 0 khi không có điểm | Pass |
| STU-U08 | Biên | P = 50 khi điểm ≤ 100 | Pass |
| STU-U09 | Biên | P = 100 khi điểm > 100 (chuẩn hóa sqrt, cap 100) | Pass |
| STU-U10 | Biên | T = 100 khi đăng ký sớm (≤ 20% cửa sổ) | Pass |
| STU-U11 | Biên | T = 40 khi đăng ký muộn (> 70%) | Pass |
| STU-U12 | Bình thường | Tổng = 100 và tier = HIGH (SV lý tưởng) | Pass |
| STU-U13 | Bình thường | tier = LOW (SV khác ngành, kỳ thấp, muộn) | Pass |

**`TicketServiceTest` — phát/thu hồi vé (5 ca)** — mock `TicketRepository`.

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| STU-U14 | Ngoại lệ | Đăng ký null/chưa có id → không phát vé | Pass |
| STU-U15 | Bình thường | Đã có vé → trả vé cũ, không tạo mới | Pass |
| STU-U16 | Bình thường | Chưa có vé → tạo & lưu vé mới có mã | Pass |
| STU-U17 | Bình thường | Trạng thái REGISTERED → phát vé | Pass |
| STU-U18 | Bình thường | Trạng thái WAITLIST → thu hồi vé | Pass |

### 3.2 Anh (AnhNVT) — Xác thực & Bảo mật

**`AuthServiceTest` — đăng ký & quên mật khẩu (7 ca)** — mock `UserRepository`, dùng `AttemptLimiter` thật.
Phương thức thật: `AuthService.register(RegisterRequest)`, `forgotPassword(String)`, `verifyOtp(String, String)`.

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| AUTH-U01 | Ngoại lệ | Mật khẩu < 8 ký tự → lỗi | Pass |
| AUTH-U02 | Ngoại lệ | Xác nhận mật khẩu không khớp → lỗi | Pass |
| AUTH-U03 | Ngoại lệ | Vai trò không hợp lệ (ADMIN) → lỗi | Pass |
| AUTH-U04 | Ngoại lệ | Email đã tồn tại → lỗi | Pass |
| AUTH-U05 | Ngoại lệ | Số điện thoại ≠ 10 số → lỗi | Pass |
| AUTH-U06 | Ngoại lệ | Quên MK: email không tồn tại → lỗi | Pass |
| AUTH-U07 | Ngoại lệ | Xác minh OTP: email không tồn tại → lỗi | Pass |

**`AttemptLimiterTest` — chống brute-force (5 ca)**

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| AUTH-U08 | Bình thường | Tài khoản mới → không bị khóa | Pass |
| AUTH-U09 | Biên | Sai 4 lần (dưới ngưỡng 5) → chưa khóa | Pass |
| AUTH-U10 | Biên | Sai đủ 5 lần → khóa tạm | Pass |
| AUTH-U11 | Bình thường | Đăng nhập thành công (reset) → gỡ khóa | Pass |
| AUTH-U12 | Bình thường | Chuẩn hóa key (hoa/thường, khoảng trắng) | Pass |

### 3.3 Tú (TuHNC) — Điểm danh & Quiz

**`AttendanceServiceTest` — điểm tham gia & phân loại (7 ca)** — mock các repository/service phụ.
Phương thức thật: `AttendanceService.classify(double)` và `calculateParticipationScore(studentId, eventId)`.

Công thức thật: `điểm = check-in(40) + mid(20) + min(20, quiz%·0.2) + feedback(10) + check-out(10)`.

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| ATT-U01 | Biên | ≥ 90 → "Excellent Participation" | Pass |
| ATT-U02 | Biên | 70..89 → "Active Participation" | Pass |
| ATT-U03 | Biên | 50..69 → "Partial Participation" | Pass |
| ATT-U04 | Biên | < 50 → "Low Participation" | Pass |
| ATT-U05 | Biên | Chưa làm gì → điểm = 0 | Pass |
| ATT-U06 | Biên | Hoàn thành đầy đủ → điểm = 100 | Pass |
| ATT-U07 | Biên | Chỉ check-in + quiz 50% → điểm = 50 | Pass |

### 3.4 Sang (SangTM) — Quản trị / Cấu trúc học thuật

**`AcademicStructureTest` — ánh xạ ngành/khoa (5 ca)** — kiểm thử hàm tĩnh.

| Mã | Kỹ thuật | Kiểm tra | KQ |
|----|----------|----------|:--:|
| ADM-U01 | Bình thường | Ngành thuộc khoa → tìm đúng khoa cha | Pass |
| ADM-U02 | Phân vùng TĐ | Tên viết tắt/tiếng Anh (CNTT, IT, Economics) → đúng khoa | Pass |
| ADM-U03 | Phân vùng TĐ | Bỏ dấu & hoa/thường → chuẩn hóa đúng | Pass |
| ADM-U04 | Ngoại lệ | Tên không tồn tại → "Khác" | Pass |
| ADM-U05 | Bình thường | Kiểm tra ngành thuộc/không thuộc khoa | Pass |

### 3.5 Ca kiểm thử hỗ trợ (26 ca) — bao phủ Controller / Repository / Converter / OAuth

| Lớp test | Số ca | Nội dung chính |
|----------|:----:|----------------|
| `CommitteeControllerTest` | 9 | Duyệt/từ chối/yêu cầu chỉnh sửa đề xuất; validate lý do; 404 khi không tìm thấy; 400 khi trạng thái không hợp lệ/thiếu khoa; tự sửa giờ kết thúc |
| `AuthControllerTest` | 2 | Quên mật khẩu chỉ chấp nhận đúng email đang đăng nhập (bỏ qua hoa/thường) |
| `RegistrationRepositoryTest` | 6 | `preferred()` chọn bản REGISTERED > WAITLIST > khác; cùng trạng thái chọn id nhỏ; rỗng/1 phần tử; `statusRank()` |
| `GenderConverterTest` | 3 | Đọc giá trị giới tính cũ (tiếng Việt) & chuẩn (MALE/FEMALE/OTHER); ghi giá trị chuẩn; null-safe |
| `GoogleOAuthAccessTokenServiceTest` | 3 | Dùng token còn hạn; refresh token hết hạn; báo lỗi khi không thể refresh |
| `GoogleFormsApiServiceTest` | 3 | `isRetryable()`: retry 429/5xx; không retry 4xx; không retry 2xx & 5xx ngoài danh sách |

---

## 4. Mã nguồn kiểm thử (trích, khớp code thật)

### 4.1 `PriorityRankingServiceTest` (Đạt)

```java
@DisplayName("RBL - Công thức xếp hạng ưu tiên đăng ký")
class PriorityRankingServiceTest {
    private final PriorityRankingService service = new PriorityRankingService();
    private static final double DELTA = 0.01;

    @Test
    @DisplayName("M: Đúng chuyên ngành -> 100 điểm")
    void majorExact() {
        Student s = student("Công nghệ Thông tin", 5, 50);
        Event e = eventOfDept("Công nghệ Thông tin");
        assertEquals(100.0, service.computeBreakdown(s, e, LocalDateTime.now()).majorScore, DELTA);
    }
    // ... 12 ca còn lại (S, P, T, tổng & tier)
}
```

### 4.2 `TicketServiceTest` (Đạt) — dùng Mockito

```java
@Test
@DisplayName("Trạng thái WAITLIST -> thu hồi vé (nếu có)")
void syncWaitlistRevokesTicket() {
    TicketRepository repo = mock(TicketRepository.class);
    TicketService service = new TicketService(repo);
    Registration reg = registration(4L, "WAITLIST");
    Ticket existing = new Ticket("AEMS-REVOKE", LocalDateTime.now(), reg);
    when(repo.findByRegistrationId(4L)).thenReturn(Optional.of(existing));

    service.syncTicketForRegistration(reg);

    verify(repo).delete(existing);
    verify(repo, never()).save(any());
}
```

### 4.3 `AuthServiceTest` (Anh) — mock repository

```java
@Test
@DisplayName("Đăng ký: email đã tồn tại -> báo lỗi")
void registerEmailAlreadyUsed() {
    when(userRepository.findByEmail("newuser@uni.edu.vn")).thenReturn(Optional.of(new User()));
    RegisterResponse res = authService.register(baseRequest());
    assertFalse(res.isSuccess());
    assertEquals("Email đã được sử dụng. Vui lòng chọn email khác.", res.getMessage());
}
```

### 4.4 `AttendanceServiceTest` (Tú) — công thức điểm thật

```java
@Test
@DisplayName("Điểm: hoàn thành đầy đủ -> 100")
void scoreFullParticipation() {
    Attendance a = new Attendance();
    a.setCheckinTime(LocalDateTime.now());
    a.setMidVerifyTime(LocalDateTime.now());
    a.setCheckoutTime(LocalDateTime.now());
    when(attendanceRepository.findByEventIdAndStudentId(anyLong(), anyLong())).thenReturn(Optional.of(a));
    when(quizService.quizPercentage(anyLong(), anyLong())).thenReturn(100.0);
    when(feedbackService.hasSubmitted(anyLong(), anyLong())).thenReturn(true);

    // 40 + 20 + min(20, 100*0.2) + 10 + 10 = 100
    assertEquals(100.0, newService().calculateParticipationScore(1L, 1L), 0.001);
}
```

### 4.5 `AttemptLimiterTest` (Anh) & 4.6 `AcademicStructureTest` (Sang)

```java
@Test @DisplayName("Sai đủ 5 lần -> bị khóa tạm")
void reachThresholdLocks() {
    AttemptLimiter limiter = new AttemptLimiter();
    for (int i = 0; i < AttemptLimiter.MAX_ATTEMPTS; i++) limiter.recordFailure("login:user@uni.edu.vn");
    assertTrue(limiter.isLocked("login:user@uni.edu.vn"));
}

@Test @DisplayName("Tên viết tắt/tiếng Anh -> đúng khoa")
void aliasMapsToFaculty() {
    assertEquals("Công nghệ Thông tin", AcademicStructure.facultyOf("CNTT"));
}
```

---

## 5. Kết quả thực thi

Chạy: `mvn test`

| Lớp test | Thành viên | Số `@Test` | Pass | Fail |
|----------|-----------|:----:|:----:|:----:|
| `PriorityRankingServiceTest` | Đạt | 13 | 13 | 0 |
| `TicketServiceTest` | Đạt | 5 | 5 | 0 |
| `AuthServiceTest` | Anh | 7 | 7 | 0 |
| `AttemptLimiterTest` | Anh | 5 | 5 | 0 |
| `AttendanceServiceTest` | Tú | 7 | 7 | 0 |
| `AcademicStructureTest` | Sang | 5 | 5 | 0 |
| `CommitteeControllerTest` | Hỗ trợ | 9 | 9 | 0 |
| `AuthControllerTest` | Hỗ trợ | 2 | 2 | 0 |
| `RegistrationRepositoryTest` | Hỗ trợ | 6 | 6 | 0 |
| `GenderConverterTest` | Hỗ trợ | 3 | 3 | 0 |
| `GoogleOAuthAccessTokenServiceTest` | Hỗ trợ | 3 | 3 | 0 |
| `GoogleFormsApiServiceTest` | Hỗ trợ | 3 | 3 | 0 |
| **TỔNG** | | **68** | **68** | **0** |

> Kết quả Maven: `Tests run: 68, Failures: 0, Errors: 0, Skipped: 0` → **BUILD SUCCESS**.

---

## 6. Nhật ký hiệu chỉnh (tài liệu ⟶ khớp code)

Các điểm bản tài liệu cũ **không khớp mã nguồn** đã được sửa:

| # | Bản cũ (docx) | Thực tế trong code | Cách xử lý |
|---|---------------|--------------------|-----------|
| 1 | `PriorityRankingService.majorScore(sv, ev)`, `semesterScore(int)`, `pointsScore(int)`, `timeScore(double)`, `calculate(...)` | Không có các hàm public này; chỉ có `computeBreakdown(...)` trả `Breakdown{majorScore, semesterScore, pointsScore, timeScore, total}` và `resolvePriorityTier(...)` | Viết lại test dùng `computeBreakdown(...)`; giữ nguyên các ca biên M/S/P/T |
| 2 | Lớp `EventRegistrationService` (register/waitlist/swap/cancel/promote) | Logic đăng ký/hàng chờ nằm trong `StudentController.register()` & `cancelRegistration()`, không phải service riêng | Bỏ khỏi Unit; các kịch bản này chuyển sang **Integration/System test** (STU-022..034 trong TestCases) |
| 3 | Lớp `RegisterValidator` (validatePassword/phone/...) | Validate nằm trực tiếp trong `AuthService.register(...)` | Test qua `AuthService.register(...)` (AUTH-U01..U05) |
| 4 | `AttendanceService.participationScore(int quizPercent) = 40 + min(40, quizPercent*0.4)` | Hàm thật `calculateParticipationScore(studentId, eventId) = check-in(40)+mid(20)+min(20, quiz%*0.2)+feedback(10)+check-out(10)` | Sửa công thức & test theo code thật (ATT-U05..U07) |
| 5 | `AttendanceService.normalizeGender(...)` | Chuẩn hóa giới tính nằm ở enum `Gender.fromValue(...)` (MALE/FEMALE/OTHER) | Không thuộc AttendanceService; nếu cần test giới tính dùng `GenderConverterTest`/`Gender.fromValue` |
| 6 | `BusinessException`, `ValidationException`, `Status` enum, `Registration.getTicketCode()` | Không tồn tại; dự án dùng `ResponseStatusException`, `RegistrationStatus`, và entity `Ticket` riêng | Cập nhật mô tả cho đúng kiểu dữ liệu thật |
| 7 | "24 phương thức / 4 lớp test" | Thực tế **68 phương thức / 12 lớp test** đang chạy pass (42 nghiệp vụ cốt lõi + 26 hỗ trợ) | Cập nhật số liệu |

## 7. Kết luận

- Các logic nghiệp vụ trọng yếu (RBL, vé, validate đăng ký, điểm tham gia, chống brute-force, ánh xạ khoa) đã được kiểm thử đơn vị và đạt **Pass 42/42**.
- Tài liệu đã được hiệu chỉnh để **mô tả đúng lớp/phương thức/công thức có thật** trong mã nguồn, đảm bảo mọi ca test đều **chạy được bằng `mvn test`**.
- Các kịch bản đăng ký/hàng chờ/duyệt đề xuất (vốn nằm ở Controller) được đưa về đúng cấp **Integration/System** trong `TestCases_CampusEvent.xlsx`.
