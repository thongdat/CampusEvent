# CampusEvent - Tài liệu kiểm thử (Testing Guide)

> Tài liệu hướng dẫn kiểm thử toàn diện cho hệ thống **CampusEvent** (Academic Event Management
> System - AEMS): mục tiêu, phạm vi, kỹ thuật black box / white box, test case theo từng chức
> năng, checklist kiểm thử API / UI / phân quyền, và danh sách lỗi thường gặp cùng cách xác
> nhận.
>
> Tài liệu bám sát mã nguồn hiện tại (controller, service, security, model) và các test có sẵn
> trong `src/test/java/com/example`.

---

## Mục lục

1. [Mục tiêu kiểm thử](#1-mục-tiêu-kiểm-thử)
2. [Phạm vi kiểm thử](#2-phạm-vi-kiểm-thử)
3. [Chiến lược và cấp độ kiểm thử](#3-chiến-lược-và-cấp-độ-kiểm-thử)
4. [Môi trường và dữ liệu kiểm thử](#4-môi-trường-và-dữ-liệu-kiểm-thử)
5. [Quy ước viết test case](#5-quy-ước-viết-test-case)
6. [Black box testing](#6-black-box-testing)
7. [White box testing](#7-white-box-testing)
8. [Test case: Xác thực & tài khoản](#8-test-case-xác-thực--tài-khoản)
9. [Test case: Sự kiện & đăng ký](#9-test-case-sự-kiện--đăng-ký)
10. [Test case: Hàng đợi ưu tiên](#10-test-case-hàng-đợi-ưu-tiên)
11. [Test case: Điểm danh QR](#11-test-case-điểm-danh-qr)
12. [Test case: Quiz](#12-test-case-quiz)
13. [Test case: Phản hồi & phân tích AI](#13-test-case-phản-hồi--phân-tích-ai)
14. [Test case: Đề xuất & duyệt (Committee)](#14-test-case-đề-xuất--duyệt-committee)
15. [Test case: Quản trị (Admin)](#15-test-case-quản-trị-admin)
16. [Checklist test API](#16-checklist-test-api)
17. [Checklist test UI](#17-checklist-test-ui)
18. [Checklist test phân quyền](#18-checklist-test-phân-quyền)
19. [Kiểm thử phi chức năng](#19-kiểm-thử-phi-chức-năng)
20. [Lỗi thường gặp và cách xác nhận](#20-lỗi-thường-gặp-và-cách-xác-nhận)
21. [Quy trình báo lỗi (bug report)](#21-quy-trình-báo-lỗi-bug-report)
22. [Phụ lục: dữ liệu mẫu & lệnh hữu ích](#22-phụ-lục-dữ-liệu-mẫu--lệnh-hữu-ích)
23. [Kết luận](#23-kết-luận)
24. [Quy trình kiểm thử chi tiết theo luồng](#24-quy-trình-kiểm-thử-chi-tiết-theo-luồng-test-procedures)
25. [Ma trận truy vết (Traceability Matrix)](#25-ma-trận-truy-vết-traceability-matrix)
26. [Chiến lược tự động hóa kiểm thử](#26-chiến-lược-tự-động-hóa-kiểm-thử)
27. [Bộ test hồi quy (Regression Suite)](#27-bộ-test-hồi-quy-regression-suite)
28. [Các trường hợp biên cần chú ý đặc biệt](#28-các-trường-hợp-biên-cần-chú-ý-đặc-biệt)
29. [Báo cáo kết quả kiểm thử](#29-báo-cáo-kết-quả-kiểm-thử-test-summary-report)
30. [Phụ lục: tổng hợp test case theo mức ưu tiên](#30-phụ-lục-bảng-tổng-hợp-test-case-theo-mức-ưu-tiên)
31. [Kiểm thử thăm dò (Exploratory Charters)](#31-kiểm-thử-thăm-dò-exploratory-testing-charters)
32. [Bộ dữ liệu kiểm thử (Test Data Setup)](#32-bộ-dữ-liệu-kiểm-thử-test-data-setup)
33. [Bộ kiểm thử API chi tiết (theo endpoint)](#33-bộ-kiểm-thử-api-chi-tiết-theo-endpoint)
34. [Ma trận test âm tính (Negative Testing Matrix)](#34-ma-trận-test-âm-tính-negative-testing-matrix)
35. [Kế hoạch kiểm thử hiệu năng chi tiết](#35-kế-hoạch-kiểm-thử-hiệu-năng-chi-tiết)
36. [Kiểm thử bảo mật chuyên sâu](#36-kiểm-thử-bảo-mật-chuyên-sâu)
37. [Kiểm thử tích hợp dịch vụ ngoài](#37-kiểm-thử-tích-hợp-dịch-vụ-ngoài)
38. [Kiểm thử khả năng tương thích](#38-kiểm-thử-khả-năng-tương-thích-compatibility)
39. [Tiêu chí vào/ra kiểm thử](#39-tiêu-chí-vàora-kiểm-thử-entryexit-criteria)
40. [Phụ lục: endpoint cần kiểm thử ưu tiên](#40-phụ-lục-bảng-tổng-hợp-endpoint-cần-kiểm-thử-ưu-tiên)
41. [Test case chi tiết: chấm điểm quiz và điểm tham gia](#41-test-case-chi-tiết-chấm-điểm-quiz-và-điểm-tham-gia)
42. [Cấu hình môi trường kiểm thử](#42-cấu-hình-môi-trường-kiểm-thử)
43. [Quản lý phiên kiểm thử và quy trình chạy](#43-quản-lý-phiên-kiểm-thử-và-quy-trình-chạy)
44. [Phụ lục: thuật ngữ kiểm thử](#44-phụ-lục-thuật-ngữ-kiểm-thử)

---

## 1. Mục tiêu kiểm thử

### 1.1. Mục tiêu chung

Đảm bảo hệ thống CampusEvent hoạt động đúng đặc tả, ổn định, an toàn và mang lại trải nghiệm
tốt cho các vai trò người dùng (sinh viên, hội đồng, khoa, quản trị viên).

### 1.2. Mục tiêu cụ thể

1. **Tính đúng đắn:** mọi chức năng cho kết quả đúng với đặc tả nghiệp vụ.
2. **Tính toàn vẹn dữ liệu:** dữ liệu trong database nhất quán sau mỗi thao tác (đăng ký, hủy,
   điểm danh, duyệt đề xuất).
3. **Phân quyền chính xác:** mỗi vai trò chỉ truy cập đúng phạm vi cho phép.
4. **Bảo mật:** chống mạo danh, chống điểm danh hộ, chống brute-force đăng nhập.
5. **Khả dụng (UX):** giao diện rõ ràng, thông báo lỗi dễ hiểu (tiếng Việt).
6. **Hiệu năng:** danh sách lớn (sự kiện, người dùng, nhật ký) tải nhanh, tránh N+1 query.
7. **Tương thích:** hoạt động trên các trình duyệt phổ biến và cả hai DB (PostgreSQL/SQL Server).

### 1.3. Tiêu chí hoàn thành (Exit criteria)

- 100% test case mức độ ưu tiên cao (Critical/High) đã thực thi.
- Không còn lỗi mức Critical/High chưa được xử lý.
- Các luồng nghiệp vụ chính (đăng ký → điểm danh → feedback; đề xuất → duyệt → công bố) chạy
  thông suốt end-to-end.

---

## 2. Phạm vi kiểm thử

### 2.1. Trong phạm vi (In scope)

| Nhóm chức năng              | Mô tả                                                      |
|----------------------------|------------------------------------------------------------|
| Xác thực & tài khoản        | Đăng nhập, đăng ký (OTP), quên mật khẩu, OAuth2 Google.    |
| Quản lý sự kiện             | Liệt kê, chi tiết, lọc, tìm kiếm, sắp xếp.                |
| Đăng ký tham gia            | Đăng ký, hủy, hàng đợi ưu tiên, cấp vé.                   |
| Điểm danh                   | Check-in / mid-session / check-out qua QR.                |
| Quiz                        | Lấy câu hỏi, nộp bài, chấm điểm.                          |
| Phản hồi                    | Gửi feedback, thống kê, phân tích AI.                     |
| Đề xuất & duyệt             | Tạo, duyệt, từ chối, yêu cầu chỉnh sửa.                   |
| Quản trị                    | Quản lý người dùng, vai trò, khoa, sự kiện, đề xuất.       |
| Phân quyền (RBAC)           | Kiểm soát truy cập theo vai trò qua interceptor.          |
| Tích hợp Google Form        | Tạo form, đồng bộ phản hồi.                               |

### 2.2. Ngoài phạm vi (Out of scope)

- Kiểm thử hạ tầng Render/Neon (thuộc trách nhiệm nhà cung cấp).
- Kiểm thử nội bộ thư viện bên thứ ba (Spring, Hibernate).
- Tải trọng cực lớn (stress test quy mô hàng triệu bản ghi) — chỉ thực hiện nếu có yêu cầu
  riêng.

### 2.3. Rủi ro trọng tâm cần test kỹ

- Logic hàng đợi ưu tiên (demote/promote) khi đầy chỗ.
- Điều kiện gửi feedback (phải đã `ATTENDED`).
- Phân quyền namespace `/admin` (phần nhạy cảm chỉ ADMIN).
- Chống mạo danh qua `X-User-Email`.
- Token QR hết hạn.

---

## 3. Chiến lược và cấp độ kiểm thử

### 3.1. Kim tự tháp kiểm thử

```
            ▲   E2E / UI (ít, đắt)
           ───
          ─────  Integration (vừa)
         ────────
        ──────────  Unit test (nhiều, rẻ)
```

### 3.2. Các cấp độ

| Cấp độ        | Mục tiêu                                              | Công cụ gợi ý               |
|---------------|------------------------------------------------------|-----------------------------|
| Unit test     | Kiểm thử từng lớp service/security/util độc lập.     | JUnit 5, Mockito.           |
| Integration   | Kiểm thử controller + repository + DB.               | Spring Boot Test, H2/Testcontainers. |
| API test      | Kiểm thử endpoint qua HTTP.                          | cURL, Postman, REST Assured. |
| UI / E2E      | Kiểm thử luồng người dùng trên trình duyệt.         | Thủ công, Selenium/Playwright.|
| Phi chức năng | Hiệu năng, bảo mật, tương thích.                    | JMeter, kiểm tra thủ công.  |

### 3.3. Test hiện có trong dự án

Trong `src/test/java/com/example`:

| File test                              | Phạm vi kiểm thử                              |
|----------------------------------------|----------------------------------------------|
| `controller/AuthControllerTest`        | Luồng đăng nhập / đăng ký.                    |
| `controller/CommitteeControllerTest`   | Duyệt/từ chối/chỉnh sửa đề xuất.             |
| `repository/RegistrationRepositoryTest`| Truy vấn đăng ký.                            |
| `security/GoogleOAuthAccessTokenServiceTest`| Lấy/đổi token Google OAuth.            |
| `service/GoogleFormsApiServiceTest`    | Tích hợp Google Forms.                        |

### 3.4. Chạy test tự động

```bash
# Chạy toàn bộ test
./mvnw test

# Chạy một lớp test cụ thể
./mvnw -Dtest=AuthControllerTest test

# Chạy một phương thức test
./mvnw -Dtest=AuthControllerTest#login_success test
```

> Trên Windows PowerShell, dùng `.\mvnw.cmd test` nếu cần.

---

## 4. Môi trường và dữ liệu kiểm thử

### 4.1. Môi trường

| Thành phần   | Giá trị                                          |
|--------------|--------------------------------------------------|
| Base URL     | `http://localhost:8081/api`                      |
| Cổng         | 8081 (mặc định, có thể đổi qua `PORT`).          |
| DB dev       | SQL Server.                                       |
| DB prod      | PostgreSQL (Neon).                                |
| DB test      | H2 in-memory hoặc Testcontainers (khuyến nghị).  |

### 4.2. Tài khoản kiểm thử theo vai trò

Chuẩn bị (qua seeder hoặc tạo thủ công) tối thiểu mỗi vai trò một tài khoản:

| Vai trò      | Email mẫu                  | Dùng để test                         |
|--------------|----------------------------|--------------------------------------|
| STUDENT      | `sv001@fpt.edu.vn`         | Đăng ký, điểm danh, feedback, quiz.  |
| COMMITTEE    | `committee01@fpt.edu.vn`   | Duyệt đề xuất.                        |
| DEPARTMENT   | `khoa01@fpt.edu.vn`        | Quản lý điểm danh, quiz, feedback.   |
| MANAGER      | `manager01@fpt.edu.vn`     | Truy cập một phần `/admin`.           |
| ADMIN        | `admin@fpt.edu.vn`         | Toàn quyền.                          |

### 4.3. Dữ liệu nền tối thiểu

- Ít nhất 1 khoa.
- 1 sự kiện `PUBLISHED` sắp diễn ra (có `capacity` nhỏ, ví dụ 2, để test hàng đợi).
- 1 sự kiện `COMPLETED` (để test feedback/quiz).
- 1 đề xuất `PENDING` (để test duyệt).
- Vài sinh viên có điểm ưu tiên khác nhau.

### 4.4. Nguyên tắc dữ liệu test

- Mỗi test nên tự dọn dẹp hoặc chạy trên transaction rollback (`@Transactional` trong test).
- Không phụ thuộc thứ tự chạy giữa các test.
- Không dùng dữ liệu thật (PII) trong môi trường test.

---

## 5. Quy ước viết test case

### 5.1. Mẫu test case

Mỗi test case gồm các trường:

| Trường           | Ý nghĩa                                        |
|------------------|------------------------------------------------|
| ID               | Mã định danh (vd `TC-AUTH-001`).               |
| Tiêu đề          | Mô tả ngắn mục tiêu.                            |
| Tiền điều kiện   | Trạng thái cần có trước khi chạy.              |
| Bước thực hiện   | Các bước cụ thể.                              |
| Dữ liệu đầu vào  | Giá trị nhập.                                  |
| Kết quả mong đợi | Kết quả đúng.                                  |
| Mức ưu tiên      | Critical / High / Medium / Low.                |
| Loại             | Black box / White box; Positive / Negative.    |

### 5.2. Quy ước mã ID

```
TC-<NHÓM>-<SỐ>
```

Ví dụ: `TC-AUTH-001`, `TC-REG-005`, `TC-PERM-003`.

### 5.3. Phân loại ưu tiên

| Mức       | Tiêu chí                                                       |
|-----------|---------------------------------------------------------------|
| Critical  | Ảnh hưởng bảo mật/toàn vẹn dữ liệu hoặc chặn luồng chính.     |
| High      | Chức năng quan trọng, dùng thường xuyên.                      |
| Medium    | Chức năng phụ trợ.                                            |
| Low       | Giao diện nhỏ, văn bản, trường hợp hiếm.                      |

---

## 6. Black box testing

### 6.1. Định nghĩa

Black box testing (kiểm thử hộp đen) kiểm tra hệ thống **từ góc nhìn bên ngoài**, không quan
tâm tới mã nguồn bên trong. Người kiểm thử chỉ dựa trên đầu vào và đầu ra mong đợi theo đặc tả.

### 6.2. Khi nào áp dụng

- Kiểm thử API qua HTTP.
- Kiểm thử UI theo luồng người dùng.
- Kiểm thử chấp nhận (acceptance) với khách hàng/giảng viên.

### 6.3. Kỹ thuật black box dùng trong dự án

#### 6.3.1. Phân vùng tương đương (Equivalence Partitioning)

Chia đầu vào thành các nhóm cho cùng hành vi. Ví dụ trường `rating` của feedback:

| Phân vùng          | Giá trị đại diện | Kết quả mong đợi |
|--------------------|------------------|------------------|
| Hợp lệ (1–5)       | 3                | Chấp nhận.       |
| Dưới ngưỡng (< 1)  | 0                | 400 Bad Request. |
| Trên ngưỡng (> 5)  | 6                | 400 Bad Request. |
| Không phải số      | "abc"            | 400 Bad Request. |
| Rỗng/null          | null             | 400 Bad Request. |

#### 6.3.2. Phân tích giá trị biên (Boundary Value Analysis)

Tập trung vào giá trị ở ranh giới. Với `rating`: kiểm tra 0, 1, 5, 6. Với mật khẩu mới (≥ 8
ký tự): kiểm tra 7, 8, 9 ký tự.

| Trường         | Biên dưới | Hợp lệ thấp nhất | Hợp lệ cao nhất | Biên trên |
|----------------|-----------|------------------|-----------------|-----------|
| `rating`       | 0 (lỗi)   | 1                | 5               | 6 (lỗi)   |
| `newPassword`  | 7 (lỗi)   | 8                | —               | —         |
| `capacity`     | 0/âm      | 1                | —               | —         |

#### 6.3.3. Bảng quyết định (Decision Table)

Dùng cho logic nhiều điều kiện, ví dụ đăng ký sự kiện:

| Sự kiện PUBLISHED? | Còn slot? | Điểm > người thấp nhất? | Đã đăng ký? | Kết quả             |
|:------------------:|:---------:|:-----------------------:|:-----------:|---------------------|
| Không              | -         | -                       | -           | 400 chưa mở đăng ký |
| Có                 | -         | -                       | Có          | alreadyRegistered   |
| Có                 | Có        | -                       | Không       | REGISTERED + vé     |
| Có                 | Không     | Có                      | Không       | REGISTERED (đẩy người khác xuống WAITLIST) |
| Có                 | Không     | Không                   | Không       | WAITLIST            |

#### 6.3.4. Đoán lỗi (Error Guessing)

Dựa trên kinh nghiệm để đoán điểm dễ lỗi: ký tự đặc biệt trong tên, email viết hoa/thường,
khoảng trắng đầu/cuối, gửi request thiếu trường, double-click nút đăng ký.

### 6.4. Ví dụ kịch bản black box (đăng ký sự kiện)

1. Đăng nhập sinh viên.
2. Mở danh sách sự kiện, chọn một sự kiện `PUBLISHED` còn slot.
3. Nhấn "Đăng ký".
4. Quan sát: trạng thái trả về `REGISTERED`, có `ticketCode`, điểm hoạt động tăng +5.
5. Nhấn "Đăng ký" lần nữa: hệ thống báo `alreadyRegistered`, không tạo bản ghi mới.

---

## 7. White box testing

### 7.1. Định nghĩa

White box testing (kiểm thử hộp trắng) kiểm tra **logic bên trong** mã nguồn: các nhánh
điều kiện, vòng lặp, xử lý ngoại lệ. Yêu cầu hiểu code.

### 7.2. Độ phủ (coverage) mục tiêu

| Loại coverage         | Mô tả                                       | Mục tiêu     |
|-----------------------|---------------------------------------------|--------------|
| Statement coverage    | Mỗi dòng lệnh được chạy ít nhất một lần.    | ≥ 70%        |
| Branch coverage       | Mỗi nhánh (if/else) được chạy cả hai phía.  | ≥ 60%        |
| Path coverage         | Các đường đi quan trọng được kiểm thử.      | Luồng chính  |

> Có thể đo bằng JaCoCo (`./mvnw test jacoco:report`).

### 7.3. Các điểm logic cần white box test

#### 7.3.1. `PriorityRankingService` (tính điểm ưu tiên)

Kiểm thử từng thành phần điểm: ngành, học kỳ, điểm tích lũy, thời gian. Test các nhánh:

- Sinh viên cùng ngành với khoa tổ chức → điểm ngành cao.
- Sinh viên khác ngành → điểm ngành thấp/0.
- Đăng ký sớm vs muộn → điểm thời gian khác nhau.

#### 7.3.2. Logic hàng đợi trong `StudentController.register`

Các nhánh cần phủ:

- `event.status` null hoặc không thuộc `UPCOMING_STATUSES` → ném 400.
- `event.startTime` đã qua → ném 400.
- Đã đăng ký (chưa CANCELLED) → trả `alreadyRegistered`.
- Còn slot → `REGISTERED`.
- Hết slot, điểm cao hơn người thấp nhất → demote người đó, mình `REGISTERED`.
- Hết slot, điểm thấp hơn → mình `WAITLIST`.

#### 7.3.3. Logic hủy trong `cancelRegistration`

- Hủy đăng ký của người khác → 403.
- Sự kiện đã diễn ra → 400.
- Sau khi hủy, còn slot → promote người cao điểm nhất trong WAITLIST.

#### 7.3.4. Điều kiện feedback trong `submitFeedback`

- Chưa đăng ký → 400 "Bạn chưa đăng ký sự kiện này".
- Đăng ký nhưng chưa `ATTENDED` → 400 "Chỉ được gửi feedback khi đã tham gia".
- `rating` ngoài 1–5 → 400.
- Đã feedback → cập nhật, `pointsAwarded = 0`.

#### 7.3.5. `AuthorizationInterceptor` (phân quyền)

- role null → 401.
- Sai vai trò → 403.
- `/admin` nhạy cảm với vai trò không phải ADMIN → 403.
- `X-User-Email` khác phiên (không phải ADMIN) → 403.

#### 7.3.6. `CommitteeController` (duyệt đề xuất)

- Đề xuất không `PENDING`/`REVISION` → 400.
- Đề xuất thiếu khoa → 400.
- Reject thiếu lý do → 400.
- Revise thiếu nội dung → 400.

### 7.4. Ví dụ unit test (mô phỏng)

```java
@Test
void register_whenEventFull_andHigherPriority_demotesLowest() {
    // given: sự kiện capacity = 1, đã có 1 người REGISTERED điểm 50
    // when: sinh viên điểm 80 đăng ký
    // then: người điểm 50 bị WAITLIST, sinh viên 80 REGISTERED + có vé
}

@Test
void submitFeedback_whenNotAttended_returnsBadRequest() {
    // given: sinh viên đã đăng ký nhưng attendance != ATTENDED
    // when: gửi feedback
    // then: ResponseStatusException 400
}
```

---

## 8. Test case: Xác thực & tài khoản

### 8.1. Đăng nhập

| ID         | Tiêu đề                                   | Đầu vào                                  | Kết quả mong đợi                             | Ưu tiên |
|------------|-------------------------------------------|------------------------------------------|----------------------------------------------|---------|
| TC-AUTH-001| Đăng nhập đúng                            | email + mật khẩu đúng                     | 200, `success=true`, mở phiên, trả `user`.   | Critical|
| TC-AUTH-002| Sai mật khẩu                              | email đúng, mật khẩu sai                  | 401, `success=false`.                         | High    |
| TC-AUTH-003| Email không tồn tại                       | email lạ                                  | 401, `success=false`.                         | High    |
| TC-AUTH-004| Thiếu trường                              | bỏ trống mật khẩu                         | 400 (validation).                             | Medium  |
| TC-AUTH-005| Tài khoản bị khóa (nhiều lần sai)         | sai mật khẩu vượt ngưỡng                  | 403, `errorType=ACCOUNT_LOCKED`.              | Critical|
| TC-AUTH-006| Email có khoảng trắng / hoa thường        | ` SV001@FPT.EDU.VN `                      | Chuẩn hóa và đăng nhập đúng (nếu hỗ trợ).     | Medium  |

### 8.2. Đăng ký (OTP)

| ID         | Tiêu đề                          | Đầu vào                       | Kết quả mong đợi                          | Ưu tiên |
|------------|----------------------------------|-------------------------------|-------------------------------------------|---------|
| TC-AUTH-010| Gửi OTP đăng ký                  | email hợp lệ                  | 200, `success=true`.                       | High    |
| TC-AUTH-011| Gửi OTP thiếu email             | email rỗng                    | 400, "Vui lòng nhập email."               | Medium  |
| TC-AUTH-012| Đăng ký với OTP đúng            | thông tin + OTP đúng         | 201, tạo tài khoản.                        | Critical|
| TC-AUTH-013| Đăng ký email đã dùng           | email trùng                   | 400, "Email đã được sử dụng."             | High    |
| TC-AUTH-014| Đăng ký OTP sai/hết hạn         | OTP sai                       | 400.                                       | High    |

### 8.3. Quên mật khẩu

| ID         | Tiêu đề                          | Đầu vào                       | Kết quả mong đợi                          | Ưu tiên |
|------------|----------------------------------|-------------------------------|-------------------------------------------|---------|
| TC-AUTH-020| Gửi OTP quên mật khẩu           | email tồn tại                 | 200, gửi OTP.                              | High    |
| TC-AUTH-021| Xác minh OTP đúng              | email + OTP đúng             | 200, `success=true`.                       | High    |
| TC-AUTH-022| Xác minh OTP thiếu dữ liệu     | thiếu otp                     | 400, "Vui lòng nhập email và mã OTP."     | Medium  |
| TC-AUTH-023| Đặt lại mật khẩu hợp lệ        | mật khẩu ≥ 8 ký tự           | 200, đổi mật khẩu.                         | Critical|
| TC-AUTH-024| Mật khẩu mới quá ngắn          | 7 ký tự                       | 400, "...ít nhất 8 ký tự."                | High    |
| TC-AUTH-025| Email không khớp phiên          | đang login, gửi email khác    | 400, "Email không khớp..."                | High    |

### 8.4. OAuth2 Google

| ID         | Tiêu đề                          | Kết quả mong đợi                                      | Ưu tiên |
|------------|----------------------------------|------------------------------------------------------|---------|
| TC-AUTH-030| Kiểm tra cấu hình OAuth          | `GET /auth/oauth-status` trả `enabled`.              | Medium  |
| TC-AUTH-031| Đăng nhập Google thành công      | Ánh xạ email Google sang user, mở phiên.            | High    |
| TC-AUTH-032| Email Google ngoài tên miền     | Bị từ chối (nếu có ràng buộc tên miền).             | High    |

---

## 9. Test case: Sự kiện & đăng ký

### 9.1. Danh sách & chi tiết sự kiện

| ID        | Tiêu đề                            | Đầu vào                          | Kết quả mong đợi                                  | Ưu tiên |
|-----------|------------------------------------|----------------------------------|--------------------------------------------------|---------|
| TC-EVT-001| Liệt kê sự kiện                    | `GET /student/events`            | 200, danh sách `items`, `total`.                 | High    |
| TC-EVT-002| Lọc theo scope=upcoming           | `scope=upcoming`                 | Chỉ sự kiện sắp diễn ra.                          | Medium  |
| TC-EVT-003| Lọc theo scope=past               | `scope=past`                     | Sự kiện trong 8 tháng gần đây.                    | Medium  |
| TC-EVT-004| Lọc theo scope=today              | `scope=today`                    | Sự kiện trong hôm nay.                            | Medium  |
| TC-EVT-005| Tìm kiếm theo từ khóa             | `q=công nghệ`                    | Lọc theo tiêu đề/mô tả/địa điểm/khoa.            | Medium  |
| TC-EVT-006| Sắp xếp theo priority             | `sort=priority`                  | Sắp giảm dần theo điểm ưu tiên.                  | Medium  |
| TC-EVT-007| Chi tiết sự kiện tồn tại          | `GET /student/events/{id}`       | 200, kèm `priorityBreakdown`, `queue`, feedback. | High    |
| TC-EVT-008| Chi tiết sự kiện không tồn tại    | id không tồn tại                 | 404, "Không tìm thấy sự kiện".                   | High    |

### 9.2. Đăng ký

| ID        | Tiêu đề                                | Tiền điều kiện                       | Kết quả mong đợi                                | Ưu tiên |
|-----------|----------------------------------------|-------------------------------------|-------------------------------------------------|---------|
| TC-REG-001| Đăng ký thành công còn slot            | sự kiện PUBLISHED, còn slot          | 200, `REGISTERED`, có `ticketCode`, +5 điểm.    | Critical|
| TC-REG-002| Đăng ký khi chưa mở                    | sự kiện không PUBLISHED/APPROVED     | 400, "Sự kiện chưa mở đăng ký".                 | High    |
| TC-REG-003| Đăng ký sự kiện đã diễn ra             | startTime < now                      | 400, "Sự kiện đã diễn ra".                       | High    |
| TC-REG-004| Đăng ký lại khi đã đăng ký             | đã REGISTERED                        | 200, `alreadyRegistered=true`.                  | High    |
| TC-REG-005| Đăng ký khi hết slot, điểm thấp        | đầy, điểm thấp                       | 200, `WAITLIST`, không có vé.                     | Critical|
| TC-REG-006| Đăng ký không gửi X-User-Email         | thiếu header                         | 401, "Yêu cầu đăng nhập (X-User-Email)".         | High    |

### 9.3. Hủy đăng ký

| ID        | Tiêu đề                            | Tiền điều kiện                  | Kết quả mong đợi                              | Ưu tiên |
|-----------|------------------------------------|--------------------------------|-----------------------------------------------|---------|
| TC-REG-010| Hủy đăng ký của mình              | đang REGISTERED                | 200, `CANCELLED`, xóa vé.                      | High    |
| TC-REG-011| Hủy đăng ký của người khác        | id của người khác              | 403, "Không thể huỷ đăng ký của người khác".  | Critical|
| TC-REG-012| Hủy sau khi sự kiện diễn ra       | startTime < now                | 400, "Sự kiện đã diễn ra, không thể huỷ".     | High    |
| TC-REG-013| Hủy kích hoạt promote waitlist    | có người trong WAITLIST        | Người cao điểm nhất lên REGISTERED + có vé.   | Critical|

### 9.4. Danh sách đăng ký của tôi

| ID        | Tiêu đề                     | Kết quả mong đợi                                     | Ưu tiên |
|-----------|-----------------------------|-----------------------------------------------------|---------|
| TC-REG-020| Xem đăng ký của tôi        | 200, kèm event, ticket, attendance, feedbackSubmitted.| High    |

---

## 10. Test case: Hàng đợi ưu tiên

### 10.1. Mục tiêu

Đảm bảo cơ chế xếp hàng (demote/promote) hoạt động đúng — đây là logic trọng yếu và dễ sai.

### 10.2. Kịch bản

| ID        | Kịch bản                                                                 | Kết quả mong đợi                                                        | Ưu tiên |
|-----------|--------------------------------------------------------------------------|------------------------------------------------------------------------|---------|
| TC-PRI-001| capacity=1, A (điểm 50) đăng ký trước                                     | A `REGISTERED`.                                                          | Critical|
| TC-PRI-002| Sau đó B (điểm 80) đăng ký                                                | B `REGISTERED`, A bị đẩy `WAITLIST`, vé của A bị xóa.                    | Critical|
| TC-PRI-003| Sau đó C (điểm 30) đăng ký                                                | C `WAITLIST` (điểm thấp hơn B).                                          | High    |
| TC-PRI-004| B hủy đăng ký                                                            | A (điểm cao nhất trong WAITLIST) lên `REGISTERED` + có vé.              | Critical|
| TC-PRI-005| Hai sinh viên điểm bằng nhau, đăng ký trước được ưu tiên giữ slot         | Người đăng ký sớm hơn giữ `REGISTERED`.                                  | Medium  |

### 10.3. Cách xác nhận

- Kiểm tra trường `status` và `priorityScore` trong response.
- Truy vấn DB bảng `registration` để xác nhận trạng thái thực tế.
- Kiểm tra bảng `ticket`: chỉ `REGISTERED` mới có vé.
- Kiểm tra `note` ghi rõ lý do demote/promote.

---

## 11. Test case: Điểm danh QR

### 11.1. Luồng ba bước

| ID        | Tiêu đề                                | Kết quả mong đợi                                        | Ưu tiên |
|-----------|----------------------------------------|--------------------------------------------------------|---------|
| TC-ATT-001| Lấy token QR check-in                  | `GET /checkin/events/{id}/qr-token` trả token + hạn.   | High    |
| TC-ATT-002| Check-in với token hợp lệ              | 200, tạo attendance `CHECKED_IN`.                       | Critical|
| TC-ATT-003| Check-in với token hết hạn             | 400, "Mã QR đã hết hạn...".                             | Critical|
| TC-ATT-004| Xác minh giữa giờ (mid)               | Cập nhật `mid_verify_time`.                             | High    |
| TC-ATT-005| Check-out                             | Cập nhật `checkout_time`, tính `participation_score`, `ATTENDED`. | High |
| TC-ATT-006| Token xoay vòng                       | Token mới khác token cũ sau khi hết TTL.               | High    |
| TC-ATT-007| Đánh vắng hàng loạt                   | Người chưa check-in → `ABSENT`.                         | High    |

### 11.2. Chống điểm danh hộ

| ID        | Kịch bản                                          | Kết quả mong đợi                          | Ưu tiên |
|-----------|---------------------------------------------------|-------------------------------------------|---------|
| TC-ATT-010| Dùng lại ảnh chụp QR cũ                            | Token hết hạn → từ chối.                  | Critical|
| TC-ATT-011| Không tham gia giữa giờ                            | Không đạt `ATTENDED` (thiếu mid-verify).  | High    |

### 11.3. Cách xác nhận

- Kiểm tra bảng `attendance`: `status`, `checkin_time`, `mid_verify_time`, `checkout_time`.
- Kiểm tra bảng `attendance_session`: `expired_at`, `status`.

---

## 12. Test case: Quiz

| ID        | Tiêu đề                              | Kết quả mong đợi                                       | Ưu tiên |
|-----------|--------------------------------------|-------------------------------------------------------|---------|
| TC-QUIZ-001| Lấy câu hỏi quiz                    | `GET /student/quiz/events/{id}/questions` trả danh sách.| High    |
| TC-QUIZ-002| Giới hạn số câu hỏi                 | `limit=5` trả tối đa 5 câu.                            | Medium  |
| TC-QUIZ-003| Nộp bài tất cả đúng                 | `totalScore` = tổng điểm các câu.                      | High    |
| TC-QUIZ-004| Nộp bài có câu sai                  | Câu sai `score=0`, `is_correct=false`.                 | High    |
| TC-QUIZ-005| Khoa tạo câu hỏi                    | `POST /department/quiz/events/{id}/questions` tạo OK.  | High    |
| TC-QUIZ-006| Khoa sửa/xóa câu hỏi               | PUT/DELETE cập nhật đúng.                              | Medium  |
| TC-QUIZ-007| Xem kết quả quiz                   | `results` trả số bài nộp, điểm trung bình.            | Medium  |
| TC-QUIZ-008| Quiz từ đề xuất được duyệt         | `quiz_payload` chuyển thành `quiz_question` khi duyệt. | High    |

---

## 13. Test case: Phản hồi & phân tích AI

### 13.1. Gửi feedback

| ID        | Tiêu đề                              | Tiền điều kiện              | Kết quả mong đợi                                      | Ưu tiên |
|-----------|--------------------------------------|----------------------------|------------------------------------------------------|---------|
| TC-FB-001 | Gửi feedback hợp lệ                  | đã `ATTENDED`              | 200, lưu feedback, +8 điểm (lần đầu).                | Critical|
| TC-FB-002 | Gửi feedback khi chưa đăng ký        | chưa đăng ký              | 400, "Bạn chưa đăng ký sự kiện này".                 | High    |
| TC-FB-003 | Gửi feedback khi chưa tham gia       | đăng ký, chưa ATTENDED    | 400, "Chỉ được gửi feedback khi đã tham gia".        | Critical|
| TC-FB-004 | Rating ngoài 1–5                     | rating=6                  | 400, "Rating phải từ 1 đến 5".                       | High    |
| TC-FB-005 | Gửi lại feedback                     | đã feedback               | 200, cập nhật, `pointsAwarded=0`.                    | Medium  |

### 13.2. Thống kê & phân tích AI

| ID        | Tiêu đề                              | Kết quả mong đợi                                      | Ưu tiên |
|-----------|--------------------------------------|------------------------------------------------------|---------|
| TC-FB-010 | Khoa xem thống kê feedback           | `data` trả `averageRating`, phân bố, bình luận.      | High    |
| TC-FB-011 | Phân tích AI                        | `ai-analysis` trả sentiment, summary, gợi ý.         | Medium  |
| TC-FB-012 | Tên người che một phần              | Bình luận công khai hiển thị tên dạng "Nguyễn Văn A.".| High    |

---

## 14. Test case: Đề xuất & duyệt (Committee)

| ID        | Tiêu đề                              | Tiền điều kiện              | Kết quả mong đợi                                       | Ưu tiên |
|-----------|--------------------------------------|----------------------------|-------------------------------------------------------|---------|
| TC-CMT-001| Xem tổng quan committee              | có đề xuất                 | `counts` theo status + 5 mới nhất.                    | Medium  |
| TC-CMT-002| Liệt kê đề xuất lọc theo status     | `status=PENDING`           | Chỉ đề xuất PENDING.                                   | High    |
| TC-CMT-003| Duyệt đề xuất PENDING               | đề xuất PENDING có khoa    | 200, status `APPROVED`, tạo event PUBLISHED.          | Critical|
| TC-CMT-004| Duyệt đề xuất thiếu khoa            | đề xuất không có khoa      | 400, "Proposal thiếu khoa...".                        | High    |
| TC-CMT-005| Duyệt đề xuất đã APPROVED           | đề xuất đã APPROVED        | 400, "...không thể xử lý lại".                         | High    |
| TC-CMT-006| Từ chối có lý do                    | đề xuất PENDING            | 200, status `REJECTED`, note "Từ chối: ...".          | High    |
| TC-CMT-007| Từ chối thiếu lý do                 | không gửi reason           | 400, "Cần ghi lý do từ chối".                         | High    |
| TC-CMT-008| Yêu cầu chỉnh sửa có nội dung       | đề xuất PENDING            | 200, status `REVISION`.                               | Medium  |
| TC-CMT-009| Yêu cầu chỉnh sửa thiếu nội dung    | không gửi request          | 400, "Cần ghi rõ yêu cầu chỉnh sửa".                  | Medium  |
| TC-CMT-010| Duyệt tạo kèm quiz                  | đề xuất có quiz_payload    | Tạo `quiz_question` cho event.                        | High    |
| TC-CMT-011| Duyệt re-use event trùng           | đã có event trùng         | Tái dùng event, không tạo trùng.                      | Medium  |

---

## 15. Test case: Quản trị (Admin)

### 15.1. Người dùng & vai trò

| ID         | Tiêu đề                       | Kết quả mong đợi                            | Ưu tiên |
|------------|-------------------------------|--------------------------------------------|---------|
| TC-ADM-001 | Liệt kê người dùng           | 200, danh sách kèm thông tin sinh viên.    | High    |
| TC-ADM-002 | Tạo người dùng               | Tạo thành công, gán vai trò.               | High    |
| TC-ADM-003 | Cập nhật người dùng          | Cập nhật họ tên/vai trò/trạng thái.        | High    |
| TC-ADM-004 | Khóa/mở tài khoản            | `status` đổi đúng.                          | High    |
| TC-ADM-005 | Xóa người dùng               | Xóa thành công.                            | Medium  |
| TC-ADM-006 | CRUD vai trò                 | Tạo/sửa/xóa role hoạt động.                | Medium  |

### 15.2. Sự kiện & đề xuất (admin)

| ID         | Tiêu đề                       | Kết quả mong đợi                            | Ưu tiên |
|------------|-------------------------------|--------------------------------------------|---------|
| TC-ADM-010 | Liệt kê sự kiện (admin)      | 200, kèm số liệu tổng hợp.                  | High    |
| TC-ADM-011 | Đổi trạng thái sự kiện       | PUBLISHED → COMPLETED.                       | High    |
| TC-ADM-012 | Đổi sức chứa                 | capacity cập nhật.                           | Medium  |
| TC-ADM-013 | Tạo Google Form check-in     | Trả `formUrl`, `formId`.                     | Medium  |
| TC-ADM-014 | Đồng bộ phản hồi Google Form| `synced` > 0, `last_sheet_sync_at` cập nhật.| Medium  |
| TC-ADM-015 | Công bố đề xuất             | Đề xuất → event PUBLISHED.                   | High    |

### 15.3. Dashboard & nhật ký

| ID         | Tiêu đề                       | Kết quả mong đợi                            | Ưu tiên |
|------------|-------------------------------|--------------------------------------------|---------|
| TC-ADM-020 | Xem dashboard                | `stats` đầy đủ chỉ số.                      | Medium  |
| TC-ADM-021 | Nhật ký hoạt động phân trang | `page`, `size`, `totalItems`, `totalPages`.| Medium  |
| TC-ADM-022 | size vượt 200                | Bị giới hạn về 200.                         | Low     |

### 15.4. Tải ảnh

| ID         | Tiêu đề                       | Kết quả mong đợi                            | Ưu tiên |
|------------|-------------------------------|--------------------------------------------|---------|
| TC-ADM-030 | Tải ảnh hợp lệ               | 200, trả `url`.                             | Medium  |
| TC-ADM-031 | Tải file rỗng                | 400, "Tệp ảnh trống."                       | Medium  |

---

## 16. Checklist test API

### 16.1. Cho mỗi endpoint

- [ ] Phương thức HTTP đúng (GET/POST/PUT/DELETE).
- [ ] Đường dẫn đúng (kèm tiền tố `/api`).
- [ ] Mã trạng thái HTTP đúng cho trường hợp thành công.
- [ ] Mã trạng thái HTTP đúng cho từng trường hợp lỗi (400/401/403/404).
- [ ] Cấu trúc JSON response đúng (các trường mong đợi).
- [ ] Kiểu dữ liệu các trường đúng (số/chuỗi/boolean/null).
- [ ] Ký tự tiếng Việt hiển thị đúng (UTF-8).
- [ ] Trường thời gian đúng định dạng ISO.

### 16.2. Đầu vào (request)

- [ ] Body hợp lệ → thành công.
- [ ] Thiếu trường bắt buộc → 400.
- [ ] Trường sai kiểu → 400.
- [ ] Giá trị biên (min/max) xử lý đúng.
- [ ] Chuỗi quá dài bị từ chối/cắt phù hợp.
- [ ] Ký tự đặc biệt / khoảng trắng đầu cuối xử lý đúng.
- [ ] Body rỗng (nếu cho phép) xử lý đúng.

### 16.3. Xác thực & phiên

- [ ] Không đăng nhập → 401 (với nhóm bảo vệ).
- [ ] Phiên hợp lệ → truy cập được.
- [ ] Thiếu `X-User-Email` cho nhóm student → 401.
- [ ] `X-User-Email` khác phiên → 403.

### 16.4. Tính bền vững (idempotency / trùng lặp)

- [ ] Gửi request hai lần (double submit) không tạo dữ liệu trùng (vd đăng ký).
- [ ] Thao tác trên id không tồn tại → 404.
- [ ] Thao tác trên trạng thái không hợp lệ → 400.

### 16.5. Phân trang & lọc

- [ ] `page`/`size` hoạt động đúng.
- [ ] `size` vượt giới hạn bị chặn (vd 200).
- [ ] Tham số lọc (`status`, `scope`, `q`, `faculty`, `sort`) cho kết quả đúng.

---

## 17. Checklist test UI

### 17.1. Tổng quát

- [ ] Trang tải không lỗi console.
- [ ] Bố cục hiển thị đúng trên desktop và mobile (responsive).
- [ ] Tiếng Việt hiển thị đúng dấu.
- [ ] Trạng thái tải (loading/skeleton) hiển thị khi chờ dữ liệu.
- [ ] Thông báo lỗi rõ ràng, bằng tiếng Việt.

### 17.2. Form (đăng nhập, đăng ký, đề xuất...)

- [ ] Validate phía client trước khi gửi (bắt buộc, định dạng email).
- [ ] Nút submit bị vô hiệu khi đang gửi (tránh double submit).
- [ ] Hiển thị lỗi từ server đúng vị trí.
- [ ] Sau khi thành công, điều hướng/thông báo phù hợp.
- [ ] OTP: đếm ngược thời gian, nút gửi lại sau khi hết hạn.

### 17.3. Màn hình sinh viên

- [ ] Danh sách sự kiện hiển thị: tiêu đề, thời gian, địa điểm, slot còn lại, điểm ưu tiên.
- [ ] Bộ lọc scope/faculty/sort hoạt động.
- [ ] Nút đăng ký đổi trạng thái sau khi đăng ký (REGISTERED/WAITLIST).
- [ ] Mục "Đăng ký của tôi" hiển thị vé, trạng thái điểm danh, đã feedback chưa.
- [ ] Leaderboard hiển thị đúng hạng, đánh dấu "isMe".

### 17.4. Màn hình điểm danh

- [ ] Mã QR hiển thị và tự làm mới.
- [ ] Quét QR check-in/mid/check-out cho phản hồi đúng.
- [ ] Dashboard điểm danh cập nhật số liệu.

### 17.5. Màn hình khoa / hội đồng / admin

- [ ] Danh sách đề xuất hiển thị đúng trạng thái.
- [ ] Nút duyệt/từ chối/yêu cầu sửa hoạt động, yêu cầu nhập lý do khi cần.
- [ ] Bảng quản lý người dùng cho phép tạo/sửa/khóa/xóa.
- [ ] Dashboard hiển thị số liệu tổng quan.

### 17.6. Khả năng tiếp cận (Accessibility) cơ bản

- [ ] Có thể thao tác bằng bàn phím (tab/enter).
- [ ] Tương phản màu đủ đọc.
- [ ] Ảnh có thuộc tính alt.

---

## 18. Checklist test phân quyền

### 18.1. Ma trận phân quyền cần kiểm

| Endpoint nhóm        | STUDENT | COMMITTEE | DEPARTMENT | MANAGER | ADMIN |
|----------------------|:-------:|:---------:|:----------:|:-------:|:-----:|
| `/student/**`        |   ✔    |    ✘     |     ✘      |   ✘    |  ✔   |
| `/committee/**`      |   ✘    |    ✔     |     ✘      |   ✘    |  ✔   |
| `/department/**`     |   ✘    |    ✘     |     ✔      |   ✔    |  ✔   |
| `/admin/**` nhạy cảm |   ✘    |    ✘     |     ✘      |   ✘    |  ✔   |
| `/admin/**` còn lại  |   ✘    |    ✘     |     ✔      |   ✔    |  ✔   |

> Nhóm `/admin` nhạy cảm gồm: users, roles, email-logs, activity-logs, registrations,
> feedback, overview, reports, và ghi (POST/PUT/DELETE) departments.

### 18.2. Test case phân quyền

| ID         | Kịch bản                                                | Kết quả mong đợi | Ưu tiên |
|------------|---------------------------------------------------------|------------------|---------|
| TC-PERM-001| Chưa đăng nhập gọi `/student/me`                        | 401.             | Critical|
| TC-PERM-002| STUDENT gọi `/committee/proposals`                      | 403.             | Critical|
| TC-PERM-003| STUDENT gọi `/admin/users`                              | 403.             | Critical|
| TC-PERM-004| COMMITTEE gọi `/admin/users`                            | 403.             | Critical|
| TC-PERM-005| DEPARTMENT gọi `/admin/events` (GET)                    | 200 (cho phép).  | High    |
| TC-PERM-006| DEPARTMENT gọi `/admin/users` (GET)                     | 403.             | Critical|
| TC-PERM-007| MANAGER gọi `/admin/departments` (GET)                  | 200.             | High    |
| TC-PERM-008| MANAGER gọi `/admin/departments` (POST)                 | 403.             | High    |
| TC-PERM-009| ADMIN gọi mọi endpoint                                  | 200/đúng nghiệp vụ.| High  |
| TC-PERM-010| STUDENT gửi `X-User-Email` của người khác               | 403.             | Critical|
| TC-PERM-011| ADMIN gửi `X-User-Email` của người khác (thay mặt)      | Cho phép.        | Medium  |
| TC-PERM-012| Điều hướng HTML chưa đăng nhập (GET, Accept text/html)  | Redirect `/api/login.html`.| Medium|

### 18.3. Cách xác nhận

- Dùng các tài khoản theo vai trò ở mục 4.2.
- Với mỗi vai trò, gọi lần lượt các endpoint và kiểm tra mã trạng thái.
- Lưu ý kiểm tra cả phương thức (GET vs POST) đối với `/admin/departments`.

---

## 19. Kiểm thử phi chức năng

### 19.1. Hiệu năng

| ID         | Kịch bản                                              | Tiêu chí                                  |
|------------|-------------------------------------------------------|-------------------------------------------|
| TC-PERF-001| Tải danh sách sự kiện với nhiều bản ghi               | Không bị N+1 query; phản hồi < 1s.        |
| TC-PERF-002| Dashboard admin với nhiều dữ liệu                     | Dùng cache; phản hồi nhanh.               |
| TC-PERF-003| Nhiều sinh viên đăng ký đồng thời một sự kiện         | Không vượt capacity; không lỗi đồng thời. |

> Ghi chú: code có chú thích tối ưu N+1 (fetch-join, nạp 1 lần rồi gom theo event), cần xác
> nhận hành vi này khi dữ liệu lớn.

### 19.2. Bảo mật

| ID         | Kịch bản                                       | Tiêu chí                                 |
|------------|------------------------------------------------|------------------------------------------|
| TC-SEC-001 | Brute-force đăng nhập                          | Bị khóa sau số lần sai (AttemptLimiter). |
| TC-SEC-002 | Truy cập trực tiếp endpoint nhạy cảm           | Bị chặn theo vai trò.                    |
| TC-SEC-003 | Mạo danh qua `X-User-Email`                    | Bị chặn (trừ ADMIN).                     |
| TC-SEC-004 | Dùng lại QR cũ                                 | Token hết hạn → từ chối.                 |
| TC-SEC-005 | Mật khẩu lưu dạng băm                          | DB không chứa mật khẩu plaintext.        |
| TC-SEC-006 | Inject (SQL/HTML) qua input                     | Dữ liệu được xử lý an toàn.              |

### 19.3. Tương thích

- [ ] Hoạt động trên Chrome, Edge, Firefox phiên bản mới.
- [ ] Hoạt động với cả PostgreSQL và SQL Server.
- [ ] Hiển thị tốt trên màn hình nhỏ (mobile).

### 19.4. Độ tin cậy

- [ ] Scheduler tự đóng sự kiện sau khi kết thúc hoạt động đúng.
- [ ] Thư mời được lên lịch/gửi đúng thời điểm.
- [ ] Phục hồi tốt khi dịch vụ ngoài (Google, AI) lỗi (không sập toàn hệ thống).

---

## 20. Lỗi thường gặp và cách xác nhận

### 20.1. Bảng lỗi nghiệp vụ

| Lỗi / Thông điệp                                  | Nguyên nhân                                  | Cách xác nhận / kiểm tra                                  |
|---------------------------------------------------|----------------------------------------------|----------------------------------------------------------|
| "Sự kiện chưa mở đăng ký"                          | status không PUBLISHED/APPROVED              | Kiểm tra `event.status`.                                  |
| "Sự kiện đã diễn ra"                               | startTime < now                              | Kiểm tra `event.start_time` so với hiện tại.             |
| "Chỉ được gửi feedback khi đã tham gia sự kiện"    | attendance != ATTENDED                       | Kiểm tra bảng `attendance.status`.                       |
| "Bạn chưa đăng ký sự kiện này"                     | không có registration                        | Kiểm tra bảng `registration`.                            |
| "Rating phải từ 1 đến 5"                           | rating ngoài khoảng                          | Kiểm tra body request.                                   |
| "Không thể huỷ đăng ký của người khác"             | registration.student != current             | Kiểm tra `registration.student_id`.                     |
| "Proposal thiếu khoa, không thể tạo event"         | proposal.department null                     | Kiểm tra `event_proposal.department_id`.                 |
| "Cần ghi lý do từ chối"                            | reject thiếu reason                          | Kiểm tra body có `reason`/`note`.                        |
| "...không thể xử lý lại"                           | proposal không PENDING/REVISION              | Kiểm tra `event_proposal.status`.                       |
| "Mã QR đã hết hạn..."                              | token quá `expired_at`                       | Kiểm tra `attendance_session.expired_at`.                |

### 20.2. Bảng lỗi xác thực/phân quyền

| Lỗi / Thông điệp                                       | Nguyên nhân                          | Cách xác nhận                                 |
|--------------------------------------------------------|--------------------------------------|----------------------------------------------|
| "Bạn cần đăng nhập để tiếp tục."                       | không có phiên                       | Kiểm tra cookie phiên / đăng nhập lại.       |
| "Yêu cầu đăng nhập (X-User-Email)"                     | thiếu header                         | Thêm header `X-User-Email`.                  |
| "Bạn không có quyền truy cập chức năng này."           | sai vai trò                          | Kiểm tra vai trò tài khoản.                  |
| "Bạn không thể truy cập dữ liệu của người dùng khác."  | `X-User-Email` khác phiên            | Đảm bảo header khớp email phiên.             |
| `ACCOUNT_LOCKED`                                       | đăng nhập sai nhiều lần              | Chờ mở khóa / kiểm tra AttemptLimiter.       |

### 20.3. Lỗi kỹ thuật thường gặp khi test

| Triệu chứng                                  | Nguyên nhân khả dĩ                                  | Cách xác nhận / xử lý                               |
|----------------------------------------------|----------------------------------------------------|----------------------------------------------------|
| 401 dù đã đăng nhập                          | Cookie phiên không gửi kèm / thiếu `X-User-Email`. | Bật gửi cookie; thêm header.                        |
| Ký tự tiếng Việt bị lỗi font                 | Sai charset.                                       | Đảm bảo `Content-Type` UTF-8.                       |
| Sai giờ sự kiện                             | Nhầm định dạng ngày giờ.                           | Dùng ISO `yyyy-MM-ddTHH:mm:ss`.                     |
| Đăng ký tạo bản ghi trùng                    | Double submit.                                     | Kiểm tra `registration` theo (event, student).     |
| Vé vẫn còn sau khi hủy                        | Ticket chưa bị xóa.                                | Kiểm tra bảng `ticket` theo `registration_id`.     |
| Danh sách tải chậm                          | N+1 query.                                         | Kiểm tra log SQL, dùng fetch-join.                 |
| Phân tích AI trống                          | Dịch vụ AI lỗi / thiếu API key.                    | Kiểm tra cấu hình `AI_API_KEY`, log service.       |
| Google Form không tạo                       | Thiếu cấu hình OAuth/Forms API.                    | Kiểm tra client-id/secret, quyền API.              |

### 20.4. Quy trình xác nhận lỗi chung

1. Tái hiện lỗi với các bước rõ ràng.
2. Ghi lại request (method, URL, headers, body) và response (status, body).
3. Đối chiếu với kết quả mong đợi trong test case.
4. Kiểm tra trạng thái dữ liệu trong DB (bảng liên quan).
5. Xem log server (stack trace nếu 500).
6. Xác định lớp gây lỗi (controller/service/repository/security).
7. Báo lỗi theo mẫu ở mục 21.

---

## 21. Quy trình báo lỗi (bug report)

### 21.1. Mẫu báo lỗi

```
[ID]        BUG-<số>
[Tiêu đề]   Mô tả ngắn gọn lỗi
[Mức độ]    Critical / High / Medium / Low
[Môi trường] Local / Dev / Prod, trình duyệt, DB
[Tài khoản] Vai trò + email test
[Tiền điều kiện] Trạng thái trước khi lỗi xảy ra
[Các bước tái hiện]
   1. ...
   2. ...
[Kết quả thực tế]   ...
[Kết quả mong đợi]  ...
[Bằng chứng]        Ảnh chụp màn hình / log / request-response
[Ghi chú]           Thông tin thêm
```

### 21.2. Mức độ nghiêm trọng

| Mức      | Định nghĩa                                                       |
|----------|-----------------------------------------------------------------|
| Critical | Mất dữ liệu, lỗ hổng bảo mật, chặn luồng chính.                  |
| High     | Chức năng quan trọng sai, có cách lách tạm.                      |
| Medium   | Lỗi chức năng phụ, ảnh hưởng vừa.                                |
| Low      | Lỗi giao diện nhỏ, chính tả.                                     |

### 21.3. Vòng đời báo lỗi

```
NEW → ASSIGNED → IN PROGRESS → FIXED → RETEST → (CLOSED | REOPENED)
```

### 21.4. Nguyên tắc

- Một báo lỗi cho một vấn đề.
- Tiêu đề rõ ràng, có thể tìm kiếm.
- Luôn kèm bước tái hiện và bằng chứng.
- Ghi rõ commit/branch khi phát hiện (ví dụ nhánh `TuanAnh`).

---

## 22. Phụ lục: dữ liệu mẫu & lệnh hữu ích

### 22.1. Lệnh chạy ứng dụng

```bash
# Build
./mvnw clean install

# Chạy ứng dụng
./mvnw spring-boot:run

# Truy cập
http://localhost:8081/api
```

### 22.2. Lệnh chạy test

```bash
# Toàn bộ test
./mvnw test

# Một lớp
./mvnw -Dtest=CommitteeControllerTest test

# Báo cáo coverage (nếu cấu hình JaCoCo)
./mvnw test jacoco:report
```

### 22.3. cURL mẫu cho kiểm thử API

**Đăng nhập (lưu cookie):**

```bash
curl -i -c cookie.txt -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"sv001@fpt.edu.vn","password":"matkhau123"}'
```

**Gọi API sinh viên:**

```bash
curl -b cookie.txt http://localhost:8081/api/student/me \
  -H "X-User-Email: sv001@fpt.edu.vn"
```

**Đăng ký sự kiện:**

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/student/events/45/register \
  -H "X-User-Email: sv001@fpt.edu.vn"
```

**Gửi feedback:**

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/student/events/45/feedback \
  -H "Content-Type: application/json" \
  -H "X-User-Email: sv001@fpt.edu.vn" \
  -d '{"rating":5,"comment":"Rất bổ ích"}'
```

**Duyệt đề xuất (committee):**

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/committee/proposals/70/approve \
  -H "Content-Type: application/json" \
  -d '{"capacity":100,"note":"Đã duyệt"}'
```

**Từ chối đề xuất (kiểm tra lỗi thiếu lý do):**

```bash
curl -b cookie.txt -X POST http://localhost:8081/api/committee/proposals/70/reject \
  -H "Content-Type: application/json" \
  -d '{}'
# Mong đợi: 400 "Cần ghi lý do từ chối"
```

### 22.4. Truy vấn DB kiểm chứng

```sql
-- Kiểm tra trạng thái đăng ký một sự kiện
SELECT id, student_id, status, priority_score
FROM registration
WHERE event_id = 45
ORDER BY priority_score DESC;

-- Kiểm tra vé chỉ cấp cho REGISTERED
SELECT t.id, t.code, r.status
FROM ticket t JOIN registration r ON t.registration_id = r.id
WHERE r.event_id = 45;

-- Kiểm tra điểm danh
SELECT registration_id, status, checkin_time, mid_verify_time, checkout_time
FROM attendance
WHERE event_id = 45;

-- Kiểm tra điểm hoạt động và nhật ký
SELECT u.email, u.total_points
FROM users u WHERE u.email = 'sv001@fpt.edu.vn';

SELECT activity_type, points_earned, created_at
FROM activity_log
WHERE user_id = (SELECT id FROM users WHERE email = 'sv001@fpt.edu.vn')
ORDER BY created_at DESC;
```

### 22.5. Bộ dữ liệu test gợi ý cho hàng đợi ưu tiên

| Sinh viên | Điểm ưu tiên (dự kiến) | Thời điểm đăng ký |
|-----------|------------------------|-------------------|
| A         | 50                     | sớm nhất          |
| B         | 80                     | sau A             |
| C         | 30                     | sau B             |

Sự kiện `capacity = 1`. Kỳ vọng: cuối cùng B `REGISTERED`, A và C `WAITLIST`; khi B hủy, A
lên `REGISTERED`.

### 22.6. Checklist hồi quy nhanh trước khi phát hành

- [ ] Đăng nhập tất cả vai trò OK.
- [ ] Luồng đăng ký → điểm danh → feedback OK.
- [ ] Luồng đề xuất → duyệt → công bố → sinh viên thấy sự kiện OK.
- [ ] Hàng đợi ưu tiên demote/promote OK.
- [ ] Phân quyền các nhóm OK (xem mục 18).
- [ ] Không có lỗi 500 trong log khi chạy luồng chính.
- [ ] Tiếng Việt hiển thị đúng.

---

## 23. Kết luận

Tài liệu này cung cấp khung kiểm thử toàn diện cho CampusEvent, kết hợp black box và white
box, kèm test case cụ thể theo từng chức năng và checklist thực thi. Đội kiểm thử nên:

1. Ưu tiên các test case Critical/High (đăng nhập, đăng ký, hàng đợi, phân quyền, điểm danh).
2. Tự động hóa các unit/integration test cho logic phức tạp (priority, queue, feedback).
3. Thực hiện kiểm thử thủ công cho UI và các luồng end-to-end.
4. Ghi nhận và theo dõi lỗi theo quy trình ở mục 21.

*Tài liệu kiểm thử được biên soạn dựa trên mã nguồn hiện tại. Khi chức năng thay đổi, vui lòng
cập nhật test case và checklist tương ứng.*

---

## 24. Quy trình kiểm thử chi tiết theo luồng (test procedures)

Phần này mô tả các thủ tục kiểm thử end-to-end dạng từng bước, dùng cho kiểm thử thủ công và
kiểm thử chấp nhận.

### 24.1. Thủ tục TP-01: Vòng đời sinh viên đầy đủ

**Mục tiêu:** xác nhận sinh viên có thể đi hết hành trình từ đăng nhập đến gửi feedback.

**Tiền điều kiện:**

- Có tài khoản sinh viên `sv001@fpt.edu.vn`.
- Có một sự kiện `PUBLISHED` sắp diễn ra (`E1`) còn slot.
- Có một sự kiện `COMPLETED` (`E2`) mà sinh viên đã `ATTENDED`.

**Các bước:**

1. Mở trang đăng nhập, nhập email + mật khẩu, nhấn Đăng nhập.
   - *Kỳ vọng:* đăng nhập thành công, chuyển vào màn hình sinh viên.
2. Gọi/xem hồ sơ (`/student/me`).
   - *Kỳ vọng:* hiển thị đúng họ tên, mã sinh viên, điểm, hạng.
3. Mở danh sách sự kiện, lọc `scope=upcoming`.
   - *Kỳ vọng:* thấy `E1` trong danh sách.
4. Mở chi tiết `E1`.
   - *Kỳ vọng:* hiển thị `priorityBreakdown`, hàng đợi, feedback nổi bật.
5. Nhấn Đăng ký `E1`.
   - *Kỳ vọng:* trạng thái `REGISTERED`, có `ticketCode`, điểm tăng +5.
6. Mở "Đăng ký của tôi".
   - *Kỳ vọng:* thấy `E1` với vé, trạng thái đăng ký.
7. Với `E2` (đã ATTENDED), gửi feedback rating 5.
   - *Kỳ vọng:* lưu feedback, điểm tăng +8 (nếu là lần đầu).
8. Mở leaderboard.
   - *Kỳ vọng:* hạng phản ánh điểm mới.

**Tiêu chí đạt:** tất cả 8 bước cho kết quả như kỳ vọng, không lỗi 500.

### 24.2. Thủ tục TP-02: Vòng đời đề xuất → sự kiện

**Mục tiêu:** xác nhận luồng từ đề xuất tới công bố sự kiện cho sinh viên.

**Tiền điều kiện:**

- Có tài khoản COMMITTEE.
- Có một đề xuất `PENDING` (`P1`) gắn với một khoa, có `quiz_payload`.

**Các bước:**

1. Đăng nhập COMMITTEE.
2. Mở tổng quan committee.
   - *Kỳ vọng:* `counts.PENDING` ≥ 1, `P1` trong danh sách mới nhất.
3. Mở danh sách đề xuất `status=PENDING`.
   - *Kỳ vọng:* thấy `P1`.
4. Mở chi tiết `P1`, xem mô tả, ngân sách, diễn giả.
5. Nhấn Duyệt với `capacity=100`, ghi `note`.
   - *Kỳ vọng:* `P1` → `APPROVED`, tạo `event` `PUBLISHED`, trả `event` trong response.
6. Kiểm tra quiz: event có các `quiz_question` từ `quiz_payload`.
7. Đăng nhập sinh viên, mở danh sách sự kiện.
   - *Kỳ vọng:* thấy sự kiện vừa tạo, có thể đăng ký.

**Tiêu chí đạt:** đề xuất chuyển trạng thái, sự kiện xuất hiện cho sinh viên, quiz được mang
sang.

### 24.3. Thủ tục TP-03: Điểm danh ba bước

**Mục tiêu:** xác nhận luồng check-in → mid → check-out và tính `ATTENDED`.

**Các bước:**

1. (Khoa) Mở trang QR điểm danh của sự kiện, lấy token check-in.
2. (Sinh viên) Quét QR check-in trong thời hạn token.
   - *Kỳ vọng:* tạo `attendance` `CHECKED_IN`.
3. (Khoa) Mở phiên xác minh giữa giờ (`mid-session/open`).
4. (Sinh viên) Quét QR mid.
   - *Kỳ vọng:* cập nhật `mid_verify_time`.
5. (Sinh viên) Thực hiện check-out (có thể kèm quiz).
   - *Kỳ vọng:* cập nhật `checkout_time`, tính `participation_score`, trạng thái `ATTENDED`.
6. (Khoa) Mở dashboard điểm danh.
   - *Kỳ vọng:* số liệu phản ánh đúng (checkedIn, attended).

**Biến thể âm:** dùng token hết hạn ở bước 2 → từ chối; không quét mid → không đạt `ATTENDED`.

### 24.4. Thủ tục TP-04: Hàng đợi ưu tiên cạnh tranh slot

**Mục tiêu:** xác nhận demote/promote chính xác khi nhiều sinh viên cạnh tranh.

**Tiền điều kiện:** sự kiện `capacity=1`; ba sinh viên A(50), B(80), C(30).

**Các bước:**

1. A đăng ký → `REGISTERED`.
2. B đăng ký → B `REGISTERED`, A bị `WAITLIST` (vé A bị xóa).
3. C đăng ký → C `WAITLIST`.
4. B hủy → A (cao điểm nhất trong WAITLIST) lên `REGISTERED` + có vé.
5. Kiểm tra DB xác nhận trạng thái cuối: A `REGISTERED`, C `WAITLIST`, B `CANCELLED`.

**Tiêu chí đạt:** trạng thái và vé khớp kỳ vọng tại mỗi bước.

### 24.5. Thủ tục TP-05: Kiểm thử phân quyền chéo

**Mục tiêu:** xác nhận mỗi vai trò chỉ truy cập đúng phạm vi.

**Các bước:** với từng tài khoản (STUDENT, COMMITTEE, DEPARTMENT, MANAGER, ADMIN), gọi lần
lượt một endpoint đại diện của mỗi nhóm (`/student/me`, `/committee/overview`,
`/department/feedback/events/1/data`, `/admin/users`, `/admin/events`) và ghi lại mã trạng
thái. Đối chiếu với ma trận ở mục 18.1.

---

## 25. Ma trận truy vết (Traceability Matrix)

Ma trận liên kết chức năng ↔ controller ↔ test case, đảm bảo mọi chức năng đều có test.

| Chức năng                  | Controller / lớp                  | Test case liên quan                  |
|----------------------------|-----------------------------------|--------------------------------------|
| Đăng nhập                  | `AuthController.login`            | TC-AUTH-001..006, TC-SEC-001         |
| Đăng ký tài khoản (OTP)    | `AuthController.register*`        | TC-AUTH-010..014                     |
| Quên / đặt lại mật khẩu    | `AuthController` (forgot/reset)   | TC-AUTH-020..025                     |
| OAuth2 Google              | `OAuth2LoginSuccessHandler`       | TC-AUTH-030..032                     |
| Danh sách / chi tiết sự kiện| `StudentController` (events)     | TC-EVT-001..008                      |
| Đăng ký / hủy              | `StudentController` (register)    | TC-REG-001..013                      |
| Hàng đợi ưu tiên           | `PriorityRankingService`         | TC-PRI-001..005, TP-04               |
| Điểm danh                  | `CheckinController`, `*Attendance`| TC-ATT-001..011, TP-03               |
| Quiz                       | `StudentQuizController`, `DepartmentQuizController` | TC-QUIZ-001..008  |
| Feedback                   | `StudentController.submitFeedback`| TC-FB-001..005                       |
| Phân tích AI               | `FeedbackAiAnalysisService`      | TC-FB-010..012                       |
| Duyệt đề xuất              | `CommitteeController`            | TC-CMT-001..011, TP-02               |
| Quản trị người dùng        | `AdminDashboardController`       | TC-ADM-001..006                      |
| Quản trị sự kiện/đề xuất   | `AdminDashboardController`       | TC-ADM-010..015                      |
| Tải ảnh                    | `UploadController`              | TC-ADM-030..031                      |
| Phân quyền (RBAC)          | `AuthorizationInterceptor`      | TC-PERM-001..012, TP-05              |

---

## 26. Chiến lược tự động hóa kiểm thử

### 26.1. Phạm vi nên tự động hóa

| Loại                 | Tự động hóa? | Lý do                                       |
|----------------------|:------------:|---------------------------------------------|
| Logic priority/queue | Có (cao)     | Phức tạp, dễ hồi quy.                        |
| Điều kiện feedback   | Có           | Nhiều nhánh điều kiện.                       |
| Phân quyền           | Có           | Bảo mật quan trọng, lặp lại nhiều.          |
| Auth/OTP             | Có (một phần)| Phần không phụ thuộc email thật.            |
| Tích hợp Google/AI   | Một phần     | Cần mock dịch vụ ngoài.                      |
| UI/E2E               | Tùy nguồn lực| Đắt; ưu tiên luồng chính.                    |

### 26.2. Mock dịch vụ ngoài

- Google Forms API: mock `GoogleFormsApiService` để không gọi mạng thật.
- Email: mock `EmailService` để xác nhận nội dung/đối tượng nhận mà không gửi thật.
- AI: mock `FeedbackAiAnalysisService` trả kết quả cố định.

### 26.3. Mẫu cấu trúc test JUnit

```java
@SpringBootTest
@AutoConfigureMockMvc
class StudentRegistrationTest {

    @Autowired MockMvc mockMvc;

    @Test
    void register_success_returnsRegistered() throws Exception {
        // arrange: chuẩn bị student + event PUBLISHED còn slot
        // act + assert
        mockMvc.perform(post("/student/events/45/register")
                .header("X-User-Email", "sv001@fpt.edu.vn"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REGISTERED"))
            .andExpect(jsonPath("$.ticketCode").isNotEmpty());
    }

    @Test
    void register_eventNotOpen_returns400() throws Exception {
        mockMvc.perform(post("/student/events/{id}/register", closedEventId)
                .header("X-User-Email", "sv001@fpt.edu.vn"))
            .andExpect(status().isBadRequest());
    }
}
```

### 26.4. Dữ liệu test cô lập

- Dùng `@Transactional` ở lớp test để rollback sau mỗi phương thức.
- Hoặc dùng database in-memory (H2) tái tạo schema từ `schema-postgresql.sql` (cẩn thận khác
  biệt phương ngữ) — khuyến nghị Testcontainers PostgreSQL để giống production.

### 26.5. Tích hợp CI

- Cấu hình GitHub Actions chạy `./mvnw test` trên mỗi pull request.
- Chặn merge nếu test thất bại.
- Tùy chọn: xuất báo cáo coverage (JaCoCo) như một artifact.

---

## 27. Bộ test hồi quy (Regression Suite)

### 27.1. Smoke test (mỗi lần deploy)

| # | Kiểm tra                                     | Kỳ vọng                  |
|---|----------------------------------------------|--------------------------|
| 1 | `GET /api/auth/test`                         | Trả "Hello World".       |
| 2 | Đăng nhập admin                              | 200.                     |
| 3 | `GET /api/public/landing`                    | 200, có dữ liệu.         |
| 4 | Đăng nhập sinh viên + `/student/me`          | 200.                     |
| 5 | Liệt kê sự kiện                              | 200, có `items`.         |

### 27.2. Bộ hồi quy đầy đủ (trước mỗi release lớn)

Chạy lại toàn bộ test case Critical/High ở các mục 8–15, cộng các thủ tục TP-01..TP-05 ở mục
24, và toàn bộ checklist phân quyền ở mục 18.

### 27.3. Tần suất

| Hoạt động           | Tần suất                          |
|---------------------|-----------------------------------|
| Smoke test          | Mỗi lần deploy.                    |
| Unit/integration    | Mỗi commit/PR (qua CI).           |
| Hồi quy đầy đủ      | Trước mỗi release lớn.            |
| Kiểm thử bảo mật    | Định kỳ + khi đổi cơ chế auth.    |

---

## 28. Các trường hợp biên cần chú ý đặc biệt

### 28.1. Liên quan thời gian

- Đăng ký đúng thời điểm sự kiện bắt đầu (ranh giới `startTime`).
- Token QR ngay tại thời điểm hết hạn (`expired_at`).
- Sự kiện `scope=past` ở ranh giới 8 tháng.
- Chuyển ngày (sự kiện `today` lúc gần nửa đêm).

### 28.2. Liên quan đồng thời (concurrency)

- Hai sinh viên đăng ký slot cuối cùng cùng lúc → chỉ một người `REGISTERED`.
- Hủy và promote xảy ra đồng thời.
- Double-click nút đăng ký.

### 28.3. Liên quan dữ liệu rỗng/null

- Sự kiện không có `capacity` (null) → không giới hạn.
- Đề xuất không có `quiz_payload` → không tạo quiz.
- Feedback `comment` rỗng nhưng có `rating`.
- Sinh viên thiếu bản ghi `student` (tự khởi tạo trong `resolveStudentOptional`).

### 28.4. Liên quan ký tự & độ dài

- Tiêu đề sự kiện dài tối đa (200 ký tự).
- Tên có ký tự đặc biệt khi mask (ví dụ tên một từ).
- Email viết hoa/thường, có khoảng trắng.

### 28.5. Liên quan quyền hỗn hợp

- ADMIN thao tác thay sinh viên (gửi `X-User-Email` khác) — được phép.
- DEPARTMENT đọc được `/admin/events` nhưng không ghi `/admin/departments`.

---

## 29. Báo cáo kết quả kiểm thử (Test Summary Report)

### 29.1. Cấu trúc báo cáo

```
[Phiên bản/Branch]  TuanAnh @ <commit>
[Thời gian]         <ngày bắt đầu> - <ngày kết thúc>
[Phạm vi]           <các nhóm chức năng đã test>
[Thống kê]
   - Tổng test case:        N
   - Đạt (Pass):            N1
   - Không đạt (Fail):      N2
   - Bị chặn (Blocked):     N3
   - Tỷ lệ đạt:             N1/N %
[Lỗi theo mức độ]
   - Critical:  ...
   - High:      ...
   - Medium:    ...
   - Low:       ...
[Đánh giá]          Sẵn sàng phát hành? Có/Không + lý do
[Khuyến nghị]       ...
```

### 29.2. Tiêu chí "Sẵn sàng phát hành"

- Không còn lỗi Critical/High mở.
- Tỷ lệ đạt test case Critical/High = 100%.
- Smoke test xanh trên môi trường đích.

### 29.3. Lưu trữ

- Lưu báo cáo kèm phiên bản/commit để truy vết.
- Đính kèm bằng chứng (ảnh, log) cho các lỗi quan trọng.

---

## 30. Phụ lục: bảng tổng hợp test case theo mức ưu tiên

### 30.1. Critical (bắt buộc xanh trước phát hành)

- TC-AUTH-001, TC-AUTH-005, TC-AUTH-012, TC-AUTH-023
- TC-REG-001, TC-REG-005, TC-REG-011, TC-REG-013
- TC-PRI-001, TC-PRI-002, TC-PRI-004
- TC-ATT-002, TC-ATT-003, TC-ATT-010
- TC-FB-001, TC-FB-003
- TC-CMT-003
- TC-PERM-001..004, TC-PERM-006, TC-PERM-010

### 30.2. High

- TC-AUTH-002,003,010,013,014,021,024,025,031,032
- TC-EVT-001,007,008
- TC-REG-002,003,004,006,010,012,020
- TC-PRI-003
- TC-ATT-001,004,005,006,007,011
- TC-QUIZ-001,003,004,005,008
- TC-FB-002,004,010,012
- TC-CMT-002,004,005,006,007,010
- TC-ADM-001..004,010,011,015
- TC-PERM-005,007,008,012
- TC-SEC-001..005

### 30.3. Medium / Low

- Các test case còn lại (lọc, sắp xếp, giao diện phụ, phân trang giới hạn, chính tả...).

---

## 31. Kiểm thử thăm dò (Exploratory Testing Charters)

Kiểm thử thăm dò bổ sung cho test case kịch bản: tester chủ động khám phá hệ thống theo một
"charter" (mục tiêu) trong khoảng thời gian cố định (timebox), ghi lại quan sát và lỗi.

### 31.1. Mẫu charter

```
[Charter] Khám phá <khu vực> nhằm tìm <loại rủi ro>
[Timebox] 60 phút
[Tài khoản] <vai trò>
[Khu vực] <màn hình / endpoint>
[Ghi chú] Quan sát, câu hỏi, lỗi phát hiện
```

### 31.2. Danh sách charter gợi ý

| ID       | Charter                                                                 | Timebox |
|----------|------------------------------------------------------------------------|---------|
| EXP-01   | Khám phá luồng đăng ký để tìm lỗi cạnh tranh slot và double-submit.     | 60'     |
| EXP-02   | Khám phá bộ lọc/tìm kiếm sự kiện với từ khóa tiếng Việt có dấu/đặc biệt.| 45'     |
| EXP-03   | Khám phá điểm danh QR để tìm cách lách (token cũ, đổi thiết bị).        | 60'     |
| EXP-04   | Khám phá phân quyền bằng cách đổi vai trò và gọi endpoint chéo.         | 60'     |
| EXP-05   | Khám phá luồng OTP (gửi lại nhiều lần, OTP hết hạn, sai nhiều lần).     | 45'     |
| EXP-06   | Khám phá nhập liệu biên (chuỗi rất dài, ký tự emoji, HTML trong comment).| 45'    |
| EXP-07   | Khám phá hành vi khi dịch vụ ngoài lỗi (Google Form, AI, email).        | 45'     |

### 31.3. Heuristic gợi ý khi thăm dò

- **CRUD**: Tạo–Đọc–Sửa–Xóa từng thực thể, kiểm tra trạng thái sau mỗi thao tác.
- **Goldilocks**: thử giá trị quá nhỏ, quá lớn, vừa đủ.
- **Interruption**: tải lại trang giữa thao tác, bấm Back, mở hai tab.
- **CRUD ngược**: xóa rồi thao tác trên id vừa xóa.

---

## 32. Bộ dữ liệu kiểm thử (Test Data Setup)

Phần này cung cấp script gợi ý để dựng dữ liệu kiểm thử **trên database test riêng** (không
chạy trên dữ liệu thật, không sửa file SQL gốc).

### 32.1. Dựng nhanh tài khoản và vai trò

> Lưu ý: mật khẩu seed dùng tiền tố `plain:` để `AuthService` tự băm BCrypt khi đăng nhập lần
> đầu. Khi tự tạo dữ liệu test, có thể dùng quy ước này để dễ đăng nhập.

```sql
-- Đảm bảo có đủ 5 vai trò
INSERT INTO role (name, description) VALUES
 ('ADMIN','Quản trị'), ('MANAGER','Quản lý'), ('DEPARTMENT','Khoa'),
 ('COMMITTEE','Hội đồng'), ('STUDENT','Sinh viên');
```

### 32.2. Dựng sự kiện để test hàng đợi

```sql
-- Sự kiện capacity nhỏ để test demote/promote
INSERT INTO event (title, description, start_time, end_time, capacity, status, created_at, department_id, budget)
VALUES ('Test Queue Event', 'Sự kiện test hàng đợi',
        NOW() + INTERVAL '3 days', NOW() + INTERVAL '3 days' + INTERVAL '2 hours',
        1, 'PUBLISHED', NOW(), 1, 0);
```

### 32.3. Truy vấn dọn dẹp dữ liệu test

```sql
-- Xóa dữ liệu test theo tiền tố tiêu đề (cẩn thận thứ tự FK)
DELETE FROM attendance WHERE event_id IN (SELECT id FROM event WHERE title LIKE 'Test %');
DELETE FROM ticket WHERE registration_id IN (
  SELECT r.id FROM registration r JOIN event e ON e.id = r.event_id WHERE e.title LIKE 'Test %');
DELETE FROM registration WHERE event_id IN (SELECT id FROM event WHERE title LIKE 'Test %');
DELETE FROM event WHERE title LIKE 'Test %';
```

> Khuyến nghị: thay vì xóa thủ công, ưu tiên `@Transactional` rollback trong test tự động hoặc
> dùng database riêng tái tạo từ đầu cho mỗi lần chạy.

---

## 33. Bộ kiểm thử API chi tiết (theo endpoint)

Phần này liệt kê kiểm thử cho từng endpoint quan trọng dưới dạng cặp "tình huống → kỳ vọng",
bám sát phân quyền và quy ước response ở `docs/API_DOCUMENTATION.md`.

### 33.1. `/auth` (công khai)

| Tình huống                                       | Kỳ vọng                                  |
|--------------------------------------------------|------------------------------------------|
| `GET /api/auth/test`                             | 200, "Hello World".                      |
| `POST /api/auth/login` đúng                      | 200, `success=true`, mở phiên.           |
| `POST /api/auth/login` sai mật khẩu              | 401, `success=false`.                    |
| `POST /api/auth/login` quá số lần sai            | 403, `errorType=ACCOUNT_LOCKED`.         |
| `POST /api/auth/register/send-otp` thiếu email   | 400.                                     |
| `POST /api/auth/register` email trùng            | 400, "Email đã được sử dụng".            |
| `POST /api/auth/reset-password` mật khẩu < 8     | 400.                                     |
| `GET /api/auth/oauth-status`                     | 200, cờ `googleEnabled`.                 |

### 33.2. `/student` (STUDENT/ADMIN + `X-User-Email`)

| Tình huống                                            | Kỳ vọng                              |
|-------------------------------------------------------|--------------------------------------|
| `GET /api/student/me` thiếu header                    | 401.                                 |
| `GET /api/student/me` hợp lệ                          | 200, hồ sơ + stats + rank.           |
| `GET /api/student/events?scope=upcoming`              | 200, chỉ sự kiện sắp diễn ra.        |
| `POST /api/student/events/{id}/register` còn slot     | 200, `REGISTERED` + `ticketCode`.    |
| `POST /api/student/events/{id}/register` hết slot     | 200, `WAITLIST`.                      |
| `DELETE /api/student/registrations/{id}` của người khác| 403.                                |
| `POST /api/student/events/{id}/feedback` chưa ATTENDED| 400.                                 |
| `GET /api/student/leaderboard`                        | 200, top 10.                         |

### 33.3. `/committee` (COMMITTEE/ADMIN)

| Tình huống                                              | Kỳ vọng                            |
|---------------------------------------------------------|------------------------------------|
| `GET /api/committee/overview`                           | 200, counts + recent.              |
| `POST /api/committee/proposals/{id}/approve` đủ điều kiện| 200, `APPROVED` + tạo event.      |
| `POST /api/committee/proposals/{id}/approve` thiếu khoa | 400.                               |
| `POST /api/committee/proposals/{id}/reject` thiếu lý do | 400.                               |
| `POST /api/committee/proposals/{id}/revise` thiếu nội dung| 400.                             |
| STUDENT gọi bất kỳ endpoint `/committee/**`            | 403.                               |

### 33.4. `/department` (DEPARTMENT/MANAGER/ADMIN)

| Tình huống                                                 | Kỳ vọng                          |
|------------------------------------------------------------|----------------------------------|
| `GET /api/department/attendance/events/{id}/qr-token`      | 200, token + QR.                 |
| `POST /api/department/attendance/events/{id}/mid-session/open` | 200.                         |
| `POST /api/department/attendance/events/{id}/mark-absent`  | 200, số người bị đánh vắng.      |
| `GET /api/department/feedback/events/{id}/data`            | 200, thống kê feedback.          |
| `GET /api/department/feedback/events/{id}/ai-analysis`     | 200, kết quả phân tích.          |
| `POST /api/department/quiz/events/{id}/questions`          | 200, câu hỏi mới.                |
| STUDENT/COMMITTEE gọi `/department/**`                     | 403.                             |

### 33.5. `/admin` (ADMIN; một số cho MANAGER/DEPARTMENT)

| Tình huống                                          | Kỳ vọng                              |
|-----------------------------------------------------|--------------------------------------|
| `GET /api/admin/users` (ADMIN)                      | 200.                                 |
| `GET /api/admin/users` (DEPARTMENT)                 | 403.                                 |
| `GET /api/admin/events` (DEPARTMENT)                | 200 (được phép).                     |
| `POST /api/admin/departments` (MANAGER)             | 403 (chỉ ADMIN ghi).                 |
| `GET /api/admin/departments` (MANAGER)              | 200 (đọc được).                      |
| `PUT /api/admin/events/{id}/status`                 | 200, đổi trạng thái.                 |
| `GET /api/admin/activity-logs?page=0&size=80`       | 200, có phân trang.                  |
| `POST /api/admin/uploads/image` file rỗng           | 400.                                 |

### 33.6. `/checkin` (công khai)

| Tình huống                                       | Kỳ vọng                              |
|--------------------------------------------------|--------------------------------------|
| `GET /api/checkin/events/{id}/info`              | 200, thông tin sự kiện + quiz.       |
| `GET /api/checkin/events/{id}/qr-token`          | 200, token hiện hành.                |
| `POST /api/checkin/events/{id}/submit` token hợp lệ | 200, ghi nhận check-in.           |
| `POST /api/checkin/events/{id}/submit` token hết hạn| 400.                              |

---

## 34. Ma trận test âm tính (Negative Testing Matrix)

Tập trung vào đầu vào bất hợp lệ và trạng thái sai để kiểm tra hệ thống xử lý lỗi gọn gàng.

| Khu vực    | Đầu vào bất hợp lệ                              | Kỳ vọng                                  |
|------------|-------------------------------------------------|------------------------------------------|
| Login      | Email sai định dạng                              | 400 (validation).                        |
| Login      | Body rỗng                                        | 400.                                     |
| Đăng ký SK | id sự kiện không tồn tại                         | 404.                                     |
| Đăng ký SK | Sự kiện đã `COMPLETED`                           | 400, "chưa mở đăng ký".                  |
| Hủy ĐK     | id đăng ký không tồn tại                         | 404.                                     |
| Feedback   | rating = 0 / 6 / "abc"                           | 400.                                     |
| Quiz       | submit thiếu `answers`                           | 400/xử lý an toàn.                       |
| Proposal   | approve khi đã `REJECTED`                        | 400.                                     |
| Upload     | file > 5MB hoặc sai định dạng                    | 400.                                     |
| Phân quyền | Gọi endpoint khác vai trò                        | 403.                                     |
| Phiên      | Gọi endpoint bảo vệ không có cookie phiên        | 401.                                     |

---

## 35. Kế hoạch kiểm thử hiệu năng chi tiết

### 35.1. Mục tiêu hiệu năng (gợi ý)

| Chỉ số                                  | Mục tiêu             |
|-----------------------------------------|----------------------|
| Thời gian phản hồi p95 (API danh sách)  | < 1s                 |
| Thời gian phản hồi p95 (API chi tiết)   | < 500ms              |
| Tỷ lệ lỗi dưới tải mục tiêu             | < 1%                 |
| Số người dùng đồng thời (mục tiêu)      | tùy quy mô triển khai|

### 35.2. Kịch bản tải trọng

| ID         | Kịch bản                                                   | Cách đo                          |
|------------|------------------------------------------------------------|----------------------------------|
| PERF-LST-01| Nhiều người xem danh sách sự kiện cùng lúc                  | JMeter ramp-up, đo p95.          |
| PERF-REG-01| Nhiều sinh viên đăng ký một sự kiện gần đầy                 | Kiểm tra không vượt capacity.    |
| PERF-DSH-01| Admin mở dashboard liên tục                                | Xác nhận cache hoạt động.        |
| PERF-LOG-01| Truy vấn nhật ký hoạt động phân trang sâu                  | Đo thời gian theo `page` lớn.    |

### 35.3. Lưu ý chống N+1

Mã nguồn có chú thích tối ưu N+1 (nạp một lần rồi gom theo event). Khi test hiệu năng cần:

- Bật log SQL (`spring.jpa.show-sql=true` ở môi trường test) để đếm số truy vấn.
- Xác nhận số truy vấn không tăng tuyến tính theo số bản ghi.

### 35.4. Kiểm thử đồng thời (concurrency)

- Mô phỏng nhiều request `register` đồng thời cho slot cuối → chỉ một `REGISTERED`.
- Quan sát khóa/giao dịch để tránh vượt `capacity`.
- Kiểm tra trạng thái cuối trong DB nhất quán.

---

## 36. Kiểm thử bảo mật chuyên sâu

### 36.1. Danh mục kiểm tra theo OWASP (rút gọn)

| Hạng mục                       | Kiểm tra trong CampusEvent                                  |
|--------------------------------|------------------------------------------------------------|
| Kiểm soát truy cập (Broken Access Control) | Gọi endpoint chéo vai trò; mạo danh `X-User-Email`.|
| Xác thực yếu                   | Brute-force login (AttemptLimiter); độ dài mật khẩu.       |
| Lộ dữ liệu nhạy cảm            | Mật khẩu băm BCrypt; không trả mật khẩu/OTP trong response.|
| Injection                      | Nhập SQL/HTML vào comment, tiêu đề; dùng JPA tham số hóa.  |
| Cấu hình sai                   | Không commit secret; biến môi trường cho client-secret.   |
| Quản lý phiên                  | Cookie phiên `HttpOnly`/`Secure` ở production.             |

### 36.2. Test case bảo mật mở rộng

| ID         | Kịch bản                                                       | Kỳ vọng                          |
|------------|---------------------------------------------------------------|----------------------------------|
| TC-SEC-010 | Gửi `<script>` trong `comment` feedback                        | Lưu/escape an toàn, không XSS.    |
| TC-SEC-011 | Gửi `' OR '1'='1` trong tham số `q`                            | Không lộ dữ liệu (JPA an toàn).   |
| TC-SEC-012 | Truy cập `/admin/users` bằng tài khoản COMMITTEE              | 403.                             |
| TC-SEC-013 | Dùng lại token QR sau khi hết hạn                              | Từ chối.                          |
| TC-SEC-014 | Đổi `X-User-Email` để xem dữ liệu sinh viên khác (non-admin)  | 403.                             |
| TC-SEC-015 | Đăng nhập sai 6 lần liên tiếp                                  | Khóa tạm thời (15 phút).          |

### 36.3. Xác nhận mật khẩu được băm

```sql
-- Sau khi người dùng đăng nhập lần đầu, password không còn tiền tố plain:
SELECT email, LEFT(password, 7) AS prefix
FROM users
WHERE email = 'sv001@fpt.edu.vn';
-- Kỳ vọng: prefix bắt đầu bằng "$2a$" hoặc "$2b$" (BCrypt), không phải "plain:".
```

---

## 37. Kiểm thử tích hợp dịch vụ ngoài

### 37.1. Google OAuth2 / Google Forms

| ID         | Kịch bản                                                     | Kỳ vọng                            |
|------------|-------------------------------------------------------------|------------------------------------|
| TC-EXT-001 | Đăng nhập Google với tài khoản đã tồn tại                    | Mở phiên, cộng điểm hoạt động.     |
| TC-EXT-002 | Đăng nhập Google với email chưa có tài khoản                 | Chuyển sang trang đăng ký.         |
| TC-EXT-003 | Đăng nhập Google với tài khoản bị khóa                       | Chuyển về login với `oauth=locked`.|
| TC-EXT-004 | Tạo Google Form check-in khi thiếu phiên Google              | Báo lỗi rõ ràng, không sập.        |
| TC-EXT-005 | Đồng bộ phản hồi form khi token Google hết hạn               | Tự refresh token hoặc báo lỗi.     |

### 37.2. Email (SMTP / Brevo)

| ID         | Kịch bản                                                     | Kỳ vọng                            |
|------------|-------------------------------------------------------------|------------------------------------|
| TC-EXT-010 | Gửi OTP đăng ký                                              | `email_log.status = SENT`.         |
| TC-EXT-011 | Gửi thư mời khi xác nhận suất                                | Ghi `invitation_sent_at`.          |
| TC-EXT-012 | SMTP lỗi                                                     | `email_log.status = FAILED`, không sập luồng. |

### 37.3. Phân tích AI phản hồi

| ID         | Kịch bản                                                     | Kỳ vọng                            |
|------------|-------------------------------------------------------------|------------------------------------|
| TC-EXT-020 | Phân tích khi có nhiều bình luận tích cực                    | sentiment phản ánh đúng xu hướng.  |
| TC-EXT-021 | Phân tích khi không có bình luận                             | Trả kết quả rỗng/an toàn.          |

> Với test tự động, **mock** các dịch vụ ngoài (Google, email, AI) để không phụ thuộc mạng và
> không gửi dữ liệu thật.

---

## 38. Kiểm thử khả năng tương thích (Compatibility)

### 38.1. Trình duyệt

| Trình duyệt | Phiên bản       | Kiểm tra                                  |
|-------------|-----------------|-------------------------------------------|
| Chrome      | mới nhất        | Toàn bộ luồng chính.                       |
| Edge        | mới nhất        | Toàn bộ luồng chính.                       |
| Firefox     | mới nhất        | Luồng chính + hiển thị QR.                 |
| Safari      | mới nhất (nếu có)| Đăng nhập, danh sách, đăng ký.           |

### 38.2. Thiết bị

- Desktop (≥ 1280px): bố cục đầy đủ.
- Tablet (~768px): bố cục co lại hợp lý.
- Mobile (~375px): menu, nút, QR vẫn dùng được.

### 38.3. Cơ sở dữ liệu

- Chạy bộ kiểm thử chính trên cả PostgreSQL và SQL Server.
- Đặc biệt chú ý cột boolean (`BIT` vs `BOOLEAN`) ở `users.status` và `quiz_answer.is_correct`.

---

## 39. Tiêu chí vào/ra kiểm thử (Entry/Exit Criteria)

### 39.1. Tiêu chí vào (Entry)

- Build thành công, ứng dụng khởi động được.
- Smoke test cơ bản (mục 27.1) xanh.
- Có dữ liệu nền tối thiểu (mục 4.3).

### 39.2. Tiêu chí ra (Exit)

- 100% test case Critical/High đã chạy.
- Không còn lỗi Critical/High mở.
- Các thủ tục TP-01..TP-05 đạt.
- Báo cáo kết quả kiểm thử (mục 29) đã lập.

### 39.3. Tiêu chí tạm dừng (Suspension)

- Lỗi chặn (blocker) khiến không thể tiếp tục phần lớn test → tạm dừng, báo dev.
- Môi trường test không ổn định (DB/đăng nhập lỗi liên tục).

---

## 40. Phụ lục: bảng tổng hợp endpoint cần kiểm thử ưu tiên

| Ưu tiên  | Endpoint tiêu biểu                                  | Lý do                                |
|----------|-----------------------------------------------------|--------------------------------------|
| Critical | `POST /api/auth/login`                              | Cổng vào hệ thống + bảo mật.         |
| Critical | `POST /api/student/events/{id}/register`           | Logic hàng đợi phức tạp.             |
| Critical | `DELETE /api/student/registrations/{id}`           | Promote waitlist + quyền sở hữu.     |
| Critical | `POST /api/checkin/events/{id}/submit`             | Chống điểm danh hộ.                  |
| Critical | `POST /api/committee/proposals/{id}/approve`       | Tạo event + chuyển trạng thái.       |
| Critical | Phân quyền `/admin/**` nhạy cảm                     | Bảo mật dữ liệu quản trị.            |
| High     | `POST /api/student/events/{id}/feedback`           | Điều kiện ATTENDED + cộng điểm.      |
| High     | `POST /api/department/attendance/.../mark-absent`  | Ảnh hưởng dữ liệu điểm danh.         |
| High     | `GET /api/student/events`                           | Lọc/sắp xếp/priority preview.        |
| Medium   | `GET /api/department/feedback/.../ai-analysis`     | Phụ thuộc dịch vụ ngoài.             |

---

## 41. Test case chi tiết: chấm điểm quiz và điểm tham gia

### 41.1. Chấm điểm quiz (`quiz_answer.score`, `quiz_submission.total_score`)

| ID         | Tình huống                                                  | Kỳ vọng                                          |
|------------|------------------------------------------------------------|--------------------------------------------------|
| TC-QS-001  | Trả lời đúng câu `points = 1`                               | `is_correct = true`, `score = 1`.                |
| TC-QS-002  | Trả lời sai                                                 | `is_correct = false`, `score = 0`.               |
| TC-QS-003  | Bài có 5 câu, đúng 3                                        | `total_score` = tổng points của 3 câu đúng.      |
| TC-QS-004  | Câu tự luận (`SHORT_ANSWER`)                                | Lưu `answer_text`; chấm theo quy tắc cấu hình.   |
| TC-QS-005  | Đáp án so khớp không phân biệt hoa/thường (nếu hỗ trợ)      | Vẫn tính đúng.                                    |
| TC-QS-006  | Nộp lại bài quiz                                            | Theo quy tắc nghiệp vụ (ghi đè/không cộng lại).  |

**Cách xác nhận:**

```sql
SELECT qs.id AS submission_id, qs.total_score,
       SUM(qa.score) AS sum_answer_score
FROM quiz_submission qs
JOIN quiz_answer qa ON qa.submission_id = qs.id
WHERE qs.event_id = 45
GROUP BY qs.id, qs.total_score;
-- Kỳ vọng: total_score = sum_answer_score cho mỗi bài nộp.
```

### 41.2. Điểm tham gia (`participation_score`)

| ID         | Tình huống                                                  | Kỳ vọng                                          |
|------------|------------------------------------------------------------|--------------------------------------------------|
| TC-PS-001  | Đủ ba bước (check-in + mid + check-out)                     | `participation_score` cao nhất, `ATTENDED`.      |
| TC-PS-002  | Thiếu mid-verify                                            | Điểm thấp hơn / không đạt `ATTENDED`.            |
| TC-PS-003  | Chỉ check-in, không check-out                               | Trạng thái chưa hoàn tất (`CHECKED_IN`).         |
| TC-PS-004  | Không tham gia                                              | `ABSENT` sau khi đánh vắng.                       |

---

## 42. Cấu hình môi trường kiểm thử

### 42.1. Thuộc tính gợi ý cho profile test

> Đây là gợi ý cho file cấu hình test riêng (ví dụ `application-test.properties`), **không**
> sửa cấu hình production.

```properties
# Cổng riêng cho test (tránh đụng cổng dev 8081)
server.port=0

# Log SQL để đếm truy vấn (phát hiện N+1)
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

# Tắt gửi email thật trong test (mock thay thế)
# spring.mail.* trỏ tới máy chủ giả lập hoặc dùng @MockBean EmailService
```

### 42.2. Biến môi trường cần cho test tích hợp

| Biến                   | Dùng cho                          | Ghi chú test                       |
|------------------------|-----------------------------------|------------------------------------|
| `DATABASE_URL`         | Kết nối DB test                   | Trỏ tới DB test, không phải prod.  |
| `GOOGLE_CLIENT_ID`     | OAuth2                            | Có thể bỏ trống nếu mock.          |
| `GOOGLE_CLIENT_SECRET` | OAuth2                            | Có thể bỏ trống nếu mock.          |
| `AI_API_KEY`           | Phân tích AI                      | Mock khi test tự động.             |

### 42.3. Chạy test với profile riêng

```bash
# Kích hoạt profile test khi chạy
./mvnw test -Dspring.profiles.active=test
```

---

## 43. Quản lý phiên kiểm thử và quy trình chạy

### 43.1. Quy trình một phiên kiểm thử

1. Chuẩn bị môi trường (build, dữ liệu nền, tài khoản các vai trò).
2. Chạy smoke test (mục 27.1). Nếu fail → tạm dừng, báo dev.
3. Chạy test theo nhóm ưu tiên (Critical → High → Medium → Low).
4. Ghi nhận kết quả từng test case (Pass/Fail/Blocked).
5. Báo lỗi theo mẫu (mục 21) cho các Fail.
6. Lập báo cáo tổng kết (mục 29).

### 43.2. Bảng theo dõi thực thi (mẫu)

| Test case   | Lần chạy | Người chạy | Kết quả | Ghi chú       |
|-------------|----------|------------|---------|---------------|
| TC-AUTH-001 |          |            |         |               |
| TC-REG-001  |          |            |         |               |
| TC-PRI-002  |          |            |         |               |
| ...         |          |            |         |               |

### 43.3. Nguyên tắc ghi nhận

- Mỗi test case ghi rõ kết quả thực tế, không chỉ Pass/Fail.
- Lỗi kèm bằng chứng (ảnh, log, request/response).
- Test bị chặn (Blocked) ghi rõ lý do và phụ thuộc.

---

## 44. Phụ lục: thuật ngữ kiểm thử

| Thuật ngữ              | Giải thích                                                       |
|------------------------|-----------------------------------------------------------------|
| Black box              | Kiểm thử dựa trên đầu vào/đầu ra, không xem mã nguồn.            |
| White box              | Kiểm thử dựa trên logic bên trong mã nguồn.                     |
| Smoke test             | Bộ kiểm tra nhanh xác nhận hệ thống chạy được ở mức cơ bản.     |
| Regression             | Kiểm thử lại để đảm bảo thay đổi không phá vỡ chức năng cũ.     |
| Boundary value         | Giá trị ở ranh giới hợp lệ/bất hợp lệ.                          |
| Equivalence partition  | Nhóm đầu vào cho cùng hành vi.                                  |
| Decision table         | Bảng tổ hợp điều kiện → kết quả.                                |
| Exploratory testing    | Kiểm thử thăm dò theo charter và timebox.                      |
| Positive / Negative    | Test với dữ liệu hợp lệ / bất hợp lệ.                          |
| Critical / High / Medium / Low | Mức ưu tiên / nghiêm trọng của test case / lỗi.        |
| Coverage               | Tỷ lệ mã nguồn được test (statement/branch/path).              |
| Mock                   | Đối tượng giả thay cho dịch vụ thật trong test.               |
| N+1 query              | Lỗi hiệu năng do truy vấn lặp theo số bản ghi.                 |
| RBAC                   | Kiểm soát truy cập dựa trên vai trò.                            |

---

*Hết tài liệu kiểm thử CampusEvent.*
