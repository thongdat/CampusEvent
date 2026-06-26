# RBL — Cơ chế xếp chỗ theo điểm ưu tiên (Ranking-Based aLlocation)

> Tài liệu dùng để **thuyết minh thay cho demo trực tiếp**. Giải thích RBL hoạt động
> thế nào, khác gì so với cơ chế đăng ký thông thường (FIFO), và minh hoạ bằng **một
> sự kiện cụ thể với dữ liệu thật trong SQL Server**: tên sự kiện, người tham gia, **vì
> sao họ được nhận chỗ**, thời gian đăng ký và điểm ưu tiên.

---

## 1. RBL là gì?

**RBL (Ranking-Based aLlocation)** = cơ chế cấp phát chỗ ngồi của sự kiện **dựa trên điểm
ưu tiên** của từng cặp *(sinh viên, sự kiện)*, thay vì cấp theo thứ tự bấm nút.

Mỗi lần sinh viên đăng ký, hệ thống tính một **điểm ưu tiên 0–100** và lưu vào cột
`registration.priority_score`. Khi sự kiện đã đầy:

- Người mới có điểm **cao hơn** người đang giữ chỗ **thấp nhất** sẽ **giành slot**;
  người thấp nhất bị đẩy xuống **hàng chờ (WAITLIST)**.
- Khi có người **huỷ**, hệ thống **tự kéo người điểm cao nhất** trong hàng chờ lên
  (auto-promote).

Mã nguồn liên quan:

```36:49:src/main/java/com/example/service/PriorityRankingService.java
    private static final double WEIGHT_MAJOR = 0.40;
    private static final double WEIGHT_SEMESTER = 0.30;
    private static final double WEIGHT_POINTS = 0.20;
    private static final double WEIGHT_TIME = 0.10;

    public static final double SCORE_MAJOR_EXACT = 100.0;
    public static final double SCORE_MAJOR_RELATED = 60.0;
    public static final double SCORE_MAJOR_OTHER = 30.0;
```

---

## 2. Khác biệt cốt lõi: hệ thống khác (FIFO) vs hệ thống của mình (RBL)

| Tiêu chí | Hệ thống khác — **FIFO** | Hệ thống của mình — **RBL** |
|---|---|---|
| Nguyên tắc nhận chỗ | Ai **đăng ký trước** thì được trước (chỉ lấy người đăng ký gần/sớm nhất cho tới khi đầy) | Ai có **điểm ưu tiên cao** thì được, bất kể bấm sớm hay muộn |
| Căn cứ | Duy nhất: **thời gian đăng ký** | 4 tiêu chí: **chuyên ngành (40%) + học kỳ (30%) + điểm hoạt động (20%) + thời gian (10%)** |
| Khi đầy chỗ | Người đến sau luôn vào hàng chờ | Người đến sau **điểm cao vẫn giành được slot**, đẩy người điểm thấp xuống hàng chờ |
| Khi có người huỷ | Lấy người **đầu hàng chờ** (đăng ký sớm nhất) | Lấy người **điểm cao nhất** trong hàng chờ |
| Công bằng | Ưu ái người rảnh/bấm nhanh | Ưu ái người **đúng ngành, sắp ra trường, tích cực** — đúng đối tượng sự kiện hướng tới |
| Nhược điểm khắc phục | Sinh viên đúng chuyên ngành có thể bị mất chỗ vì vào trễ vài giây | Vẫn còn 10% trọng số cho thời gian để khuyến khích đăng ký sớm |

> **Tóm tắt một câu:** FIFO trả lời câu hỏi *"ai bấm trước?"*, còn RBL trả lời câu hỏi
> *"ai xứng đáng có chỗ nhất?"*.

---

## 3. Công thức điểm ưu tiên

```
Priority = 0.40 · M + 0.30 · S + 0.20 · P + 0.10 · T      (mọi thành phần ∈ [0, 100])
```

| Ký hiệu | Tiêu chí | Trọng số | Cách tính (thang 0–100) |
|---|---|---|---|
| **M** | Mức phù hợp **chuyên ngành** với khoa tổ chức | 40% | Đúng chuyên ngành = **100**; cùng khoa lớn (liên quan) = **60**; khác = **30** |
| **S** | **Học kỳ** (càng cuối càng ưu tiên) | 30% | `round(min(semester, 9) / 9 × 100)` → kỳ 1 ≈ 11, kỳ 9 = 100 |
| **P** | **Điểm hoạt động** tích luỹ | 20% | `points` nếu ≤ 100; nếu > 100 dùng `min(100, √points × 10)` |
| **T** | **Thời điểm đăng ký** trong khung mở đăng ký | 10% | 20% đầu khung = **100**; 20–70% = **70**; sau 70% = **40** |

Khung thời gian của T = từ `event.created_at` (mở đăng ký) đến `event.start_time` (bắt đầu).

Nhãn hiển thị theo tổng điểm: **≥ 75 = Ưu tiên cao**, **45–74 = Trung bình**, **< 45 = Thấp**.

---

## 4. Vòng đời một chỗ ngồi trong RBL

1. **Đăng ký** → tính `priority_score`, lưu vào bảng `registration`.
2. **Còn chỗ** → trạng thái `REGISTERED`, phát vé (ticket).
3. **Đã đầy + người mới điểm cao hơn người thấp nhất** → người mới `REGISTERED`, người thấp nhất bị hạ `WAITLIST` (giành slot):

```393:404:src/main/java/com/example/controller/StudentController.java
            if (lowest != null && score.compareTo(lowestScore) > 0) {
                // Demote người thấp nhất sang WAITLIST, đăng ký mới được REGISTERED
                lowest.setStatus("WAITLIST");
                lowest.setNote("Tự động chuyển sang hàng chờ vì có sinh viên điểm ưu tiên cao hơn (" + score + " > " + lowestScore + ")");
                registrationRepository.save(lowest);
                // Xoá ticket cũ (nếu có)
                ticketRepository.findByRegistrationId(lowest.getId()).ifPresent(ticketRepository::delete);
            } else {
                registration.setStatus("WAITLIST");
                registration.setNote("Hàng chờ ưu tiên: điểm của bạn thấp hơn các slot đang giữ chỗ.");
            }
```

4. **Có người huỷ** → kéo người **điểm cao nhất** trong hàng chờ lên `REGISTERED` (auto-promote):

```510:519:src/main/java/com/example/controller/StudentController.java
                Registration promoted = eventRegistrations.stream()
                        .filter(r -> "WAITLIST".equalsIgnoreCase(r.getStatus()))
                        .max(byScoreAsc2.thenComparing(byDateAsc.reversed()))
                        .orElse(null);
                if (promoted != null) {
                    promoted.setStatus("REGISTERED");
                    promoted.setNote("Tự động lên REGISTERED do có người huỷ");
                    registrationRepository.save(promoted);
                    issueTicket(promoted);
                }
```

---

## 5. Ví dụ thực tế có DATA — Sự kiện "Workshop Spring Boot MVC"

### 5.1. Thông tin sự kiện

| Thuộc tính | Giá trị |
|---|---|
| **Tên sự kiện** | `[DEMO] Workshop Spring Boot MVC` |
| **Khoa tổ chức** | Kỹ thuật phần mềm |
| **Sức chứa (capacity)** | **3** (cố tình nhỏ để thấy rõ cơ chế giành slot) |
| **Mở đăng ký** (`created_at`) | 2026-03-01 08:00 |
| **Bắt đầu** (`start_time`) | 2026-03-21 08:00 |
| Khung đăng ký | 20 ngày → 20% đầu = trước 05/03, 70% = trước 15/03 |

### 5.2. 5 sinh viên cùng đăng ký

| # | Sinh viên | Chuyên ngành | Học kỳ | Điểm hoạt động | Thời gian đăng ký |
|---|---|---|---|---|---|
| SV1 | Nguyễn Văn An | Kỹ thuật phần mềm (đúng ngành) | 8 | 95 | 18/03 20:00 (**muộn**) |
| SV2 | Trần Thị Bình | Công nghệ Thông tin (cùng khoa) | 7 | 60 | 02/03 09:00 (sớm) |
| SV3 | Lê Hoàng Cường | Kỹ thuật phần mềm (đúng ngành) | 5 | 120 | 10/03 14:00 (giữa) |
| SV4 | Phạm Minh Dũng | An toàn thông tin (cùng khoa) | 9 | 40 | 03/03 10:00 (sớm) |
| SV5 | Võ Thị Em | Marketing (khác khoa) | 3 | 25 | 01/03 08:30 (**sớm nhất**) |

### 5.3. Tính điểm ưu tiên từng người

| SV | M (40%) | S (30%) | P (20%) | T (10%) | **Tổng** | Nhãn |
|---|---|---|---|---|---|---|
| SV1 An | 100 → 40.00 | 88.89 → 26.67 | 95 → 19.00 | 40 → 4.00 | **89.67** | Cao |
| SV3 Cường | 100 → 40.00 | 55.56 → 16.67 | 100 → 20.00 | 70 → 7.00 | **83.67** | Cao |
| SV4 Dũng | 60 → 24.00 | 100 → 30.00 | 40 → 8.00 | 100 → 10.00 | **72.00** | Trung bình |
| SV2 Bình | 60 → 24.00 | 77.78 → 23.33 | 60 → 12.00 | 100 → 10.00 | **69.33** | Trung bình |
| SV5 Em | 30 → 12.00 | 33.33 → 10.00 | 25 → 5.00 | 100 → 10.00 | **37.00** | Thấp |

*(Giải thích vài số: SV3 có 120 điểm > 100 nên chuẩn hoá `√120 × 10 = 109.5` → cắt còn 100.
SV1 đăng ký muộn nên T chỉ 40, nhưng vẫn cao nhất nhờ đúng ngành + sắp ra trường.)*

### 5.4. Kết quả theo **RBL** (capacity = 3)

Xếp theo điểm giảm dần, **3 người đầu được nhận chỗ**:

| Hạng | Sinh viên | Điểm | Trạng thái | **Vì sao?** |
|---|---|---|---|---|
| 1 | SV1 An | 89.67 | ✅ REGISTERED | Đúng chuyên ngành + kỳ 8 + điểm cao → ưu tiên cao nhất dù **đăng ký muộn nhất** |
| 2 | SV3 Cường | 83.67 | ✅ REGISTERED | Đúng chuyên ngành + điểm hoạt động kịch trần |
| 3 | SV4 Dũng | 72.00 | ✅ REGISTERED | Cùng khoa + sắp tốt nghiệp (kỳ 9) |
| 4 | SV2 Bình | 69.33 | ⏳ WAITLIST | Điểm sát nút, bị 3 người trên vượt qua |
| 5 | SV5 Em | 37.00 | ⏳ WAITLIST | Khác khoa, kỳ thấp → ưu tiên thấp nhất **dù đăng ký sớm nhất** |

### 5.5. Diễn tiến tuần tự (cách "giành slot" diễn ra theo thời gian thực)

| Bước | Ai đăng ký | Trạng thái chỗ trước đó | Xử lý | Đang giữ chỗ sau bước |
|---|---|---|---|---|
| 1 | SV5 Em (01/03) | 0/3 | Còn chỗ → nhận | Em |
| 2 | SV2 Bình (02/03) | 1/3 | Còn chỗ → nhận | Em, Bình |
| 3 | SV4 Dũng (03/03) | 2/3 | Còn chỗ → nhận (**đầy**) | Em, Bình, Dũng |
| 4 | SV3 Cường (10/03) | 3/3 đầy | 83.67 > thấp nhất **Em 37** → Cường giành slot, **Em rớt hàng chờ** | Bình, Dũng, Cường |
| 5 | SV1 An (18/03) | 3/3 đầy | 89.67 > thấp nhất **Bình 69.33** → An giành slot, **Bình rớt hàng chờ** | Dũng, Cường, An |

➡️ Kết quả cuối **trùng khớp** bảng 5.4: nhận chỗ = An, Cường, Dũng; hàng chờ = Bình, Em.

### 5.6. Nếu là **FIFO** (hệ thống khác) thì sao?

FIFO chỉ nhìn thời gian đăng ký, lấy 3 người **đăng ký sớm nhất**:

| Thứ tự bấm | Sinh viên | Trạng thái FIFO | Ghi chú |
|---|---|---|---|
| 1 | SV5 Em (01/03) | ✅ Nhận | Khác khoa, kỳ 3 vẫn được vì bấm sớm nhất |
| 2 | SV2 Bình (02/03) | ✅ Nhận | |
| 3 | SV4 Dũng (03/03) | ✅ Nhận | |
| 4 | SV3 Cường (10/03) | ❌ Hàng chờ | Đúng ngành, điểm max **vẫn trượt** vì vào trễ |
| 5 | SV1 An (18/03) | ❌ Hàng chờ | Đúng ngành, kỳ 8 **vẫn trượt** vì vào trễ nhất |

### 5.7. So sánh trực diện — ai được nhận chỗ?

| Sinh viên | FIFO | RBL | Khác biệt |
|---|---|---|---|
| SV1 An (đúng ngành, kỳ 8) | ❌ Trượt | ✅ Nhận | **RBL cứu đúng đối tượng** sự kiện hướng tới |
| SV3 Cường (đúng ngành, điểm max) | ❌ Trượt | ✅ Nhận | RBL coi trọng năng lực hơn tốc độ bấm |
| SV4 Dũng (cùng khoa, kỳ 9) | ✅ Nhận | ✅ Nhận | Cả hai đều nhận |
| SV2 Bình (cùng khoa) | ✅ Nhận | ⏳ Chờ | |
| SV5 Em (khác khoa, kỳ 3) | ✅ Nhận | ⏳ Chờ | **FIFO ưu ái nhầm** người bấm nhanh |

> **Kết luận demo:** Cùng một tập đăng ký, FIFO nhận *Em, Bình, Dũng*; RBL nhận
> *An, Cường, Dũng*. RBL loại bớt người ít liên quan và giữ lại đúng sinh viên chuyên
> ngành, sắp ra trường, tích cực — điều mà FIFO không làm được.

---

## 6. Tái tạo dữ liệu trong SQL Server

> Chạy trong database `event_management_db`. Cột `priority_score` ở đây được điền sẵn
> đúng bằng giá trị mà ứng dụng tính ra, để thấy rõ dữ liệu. Trạng thái là kết quả
> **cuối cùng** sau khi cả 5 người đã đăng ký.

```sql
-- 1) Khoa tổ chức
INSERT INTO department (name, description, created_at)
VALUES (N'Kỹ thuật phần mềm', N'Khoa tổ chức demo RBL', SYSUTCDATETIME());

-- 2) 5 user sinh viên (role_id của STUDENT giả sử = 5; chỉnh theo bảng role thực tế)
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
 (N'Nguyễn Văn An',  'an.demo@uni.edu.vn',    '$noop', '0900000001', SYSUTCDATETIME(), 1, 5, N'Kỹ thuật phần mềm',     8,  95),
 (N'Trần Thị Bình',  'binh.demo@uni.edu.vn',  '$noop', '0900000002', SYSUTCDATETIME(), 1, 5, N'Công nghệ Thông tin',   7,  60),
 (N'Lê Hoàng Cường', 'cuong.demo@uni.edu.vn', '$noop', '0900000003', SYSUTCDATETIME(), 1, 5, N'Kỹ thuật phần mềm',     5, 120),
 (N'Phạm Minh Dũng', 'dung.demo@uni.edu.vn',  '$noop', '0900000004', SYSUTCDATETIME(), 1, 5, N'An toàn thông tin',     9,  40),
 (N'Võ Thị Em',      'em.demo@uni.edu.vn',    '$noop', '0900000005', SYSUTCDATETIME(), 1, 5, N'Marketing',             3,  25);

-- 3) Hồ sơ student (major dùng để tính M)
INSERT INTO student (student_code, major, year, user_id)
SELECT 'RBL001', N'Kỹ thuật phần mềm',   4, id FROM users WHERE email='an.demo@uni.edu.vn'   UNION ALL
SELECT 'RBL002', N'Công nghệ Thông tin', 4, id FROM users WHERE email='binh.demo@uni.edu.vn' UNION ALL
SELECT 'RBL003', N'Kỹ thuật phần mềm',   3, id FROM users WHERE email='cuong.demo@uni.edu.vn' UNION ALL
SELECT 'RBL004', N'An toàn thông tin',   5, id FROM users WHERE email='dung.demo@uni.edu.vn' UNION ALL
SELECT 'RBL005', N'Marketing',           2, id FROM users WHERE email='em.demo@uni.edu.vn';

-- 4) Sự kiện demo (capacity = 3)
INSERT INTO event (title, description, location, start_time, end_time, capacity, status, created_at, department_id)
SELECT N'[DEMO] Workshop Spring Boot MVC', N'Demo cơ chế RBL', N'Lab 3 - Tòa B',
       '2026-03-21 08:00:00', '2026-03-21 11:00:00', 3, 'PUBLISHED', '2026-03-01 08:00:00', d.id
FROM department d WHERE d.name = N'Kỹ thuật phần mềm';

-- 5) Đăng ký + điểm ưu tiên + trạng thái cuối cùng theo RBL
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT '2026-03-18 20:00:00', 'REGISTERED', N'Điểm cao nhất dù đăng ký muộn',     89.67, e.id, s.id
  FROM event e JOIN student s ON s.student_code='RBL001' WHERE e.title=N'[DEMO] Workshop Spring Boot MVC' UNION ALL
SELECT '2026-03-02 09:00:00', 'WAITLIST',   N'Bị 3 người điểm cao hơn vượt qua',  69.33, e.id, s.id
  FROM event e JOIN student s ON s.student_code='RBL002' WHERE e.title=N'[DEMO] Workshop Spring Boot MVC' UNION ALL
SELECT '2026-03-10 14:00:00', 'REGISTERED', N'Đúng ngành, điểm hoạt động kịch trần', 83.67, e.id, s.id
  FROM event e JOIN student s ON s.student_code='RBL003' WHERE e.title=N'[DEMO] Workshop Spring Boot MVC' UNION ALL
SELECT '2026-03-03 10:00:00', 'REGISTERED', N'Cùng khoa, sắp tốt nghiệp',         72.00, e.id, s.id
  FROM event e JOIN student s ON s.student_code='RBL004' WHERE e.title=N'[DEMO] Workshop Spring Boot MVC' UNION ALL
SELECT '2026-03-01 08:30:00', 'WAITLIST',   N'Khác khoa, kỳ thấp dù đăng ký sớm nhất', 37.00, e.id, s.id
  FROM event e JOIN student s ON s.student_code='RBL005' WHERE e.title=N'[DEMO] Workshop Spring Boot MVC';
```

### 6.1. Truy vấn xem bảng xếp hạng RBL

```sql
SELECT
    RANK() OVER (ORDER BY r.priority_score DESC, r.registration_date ASC) AS hang,
    u.full_name              AS sinh_vien,
    s.major                  AS chuyen_nganh,
    u.semester               AS hoc_ky,
    u.total_points           AS diem_hoat_dong,
    r.registration_date      AS thoi_gian_dang_ky,
    r.priority_score         AS diem_uu_tien,
    r.status                 AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = N'[DEMO] Workshop Spring Boot MVC'
ORDER BY r.priority_score DESC, r.registration_date ASC;
```

### 6.2. So sánh: nếu xếp theo **FIFO**

```sql
SELECT
    ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam,
    u.full_name, s.major, r.registration_date,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) <= e.capacity
         THEN N'Nhận chỗ' ELSE N'Hàng chờ' END AS ket_qua_fifo
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = N'[DEMO] Workshop Spring Boot MVC'
ORDER BY r.registration_date ASC;
```

---

## 7. Kịch bản auto-promote khi có người huỷ

Giả sử **SV4 Dũng huỷ**. Còn 2/3 chỗ → hệ thống kéo người **điểm cao nhất** trong hàng
chờ lên. Hàng chờ gồm Bình (69.33) và Em (37.00) → **Bình được lên REGISTERED**.

```sql
-- Huỷ Dũng
UPDATE r SET r.status = 'CANCELLED'
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = N'[DEMO] Workshop Spring Boot MVC' AND s.student_code = 'RBL004';

-- Auto-promote người điểm cao nhất trong WAITLIST (ở đây là Bình)
UPDATE r SET r.status = 'REGISTERED', r.note = N'Tự động lên REGISTERED do có người huỷ'
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = N'[DEMO] Workshop Spring Boot MVC'
  AND r.id = (
      SELECT TOP 1 r2.id FROM registration r2
      JOIN event e2 ON e2.id = r2.event_id
      WHERE e2.title = N'[DEMO] Workshop Spring Boot MVC' AND r2.status = 'WAITLIST'
      ORDER BY r2.priority_score DESC, r2.registration_date ASC);
```

➡️ Sau cùng nhận chỗ: **An, Cường, Bình**; hàng chờ: **Em**. (FIFO khi Dũng huỷ sẽ kéo
*Cường* lên vì Cường ở đầu hàng chờ theo thời gian — lại khác kết quả của RBL.)

---

## 8. Ghi chú khi chạy trên PostgreSQL (DB thật của dự án)

DB hiện tại là **PostgreSQL** (`schema-postgresql.sql`). Cú pháp gần như giống hệt, chỉ
khác vài chỗ:

- Bỏ tiền tố `N''` cho chuỗi Unicode (PostgreSQL mặc định UTF-8): dùng `'Kỹ thuật phần mềm'`.
- `SYSUTCDATETIME()` → `NOW()`.
- `TOP 1 ... ORDER BY` → `... ORDER BY ... LIMIT 1`.
- `UPDATE r SET ... FROM registration r JOIN ...` → dùng cú pháp `UPDATE registration r SET ... FROM student s WHERE ...` của PostgreSQL.

Trên thực tế **không cần điền tay `priority_score`**: chỉ cần đăng ký qua giao diện sinh
viên (`/api/screen-student.html`), ứng dụng sẽ tự tính và lưu. Các câu INSERT ở mục 6
chỉ nhằm dựng sẵn dữ liệu minh hoạ cho buổi thuyết trình.
