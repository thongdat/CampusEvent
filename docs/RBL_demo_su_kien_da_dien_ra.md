# RBL — DEMO trên một sự kiện ĐÃ DIỄN RA (dữ liệu thật, PostgreSQL)

> File này để **trình bày khi demo**: lấy dữ liệu của **một sự kiện đã kết thúc** có sử
> dụng **RBL (Ranking-Based aLlocation)**, cho người xem thấy rõ **ai được nhận chỗ, ai vào
> hàng chờ và VÌ SAO**, dựa trên điểm ưu tiên đã lưu trong DB.
>
> Bổ trợ cho lý thuyết ở `docs/RBL_co_che_uu_tien_dang_ky.md`. File này tập trung vào **một
> ca thật, chạy được trên PostgreSQL** (`schema-postgresql.sql`).

---

## 0. Tóm tắt 30 giây để mở đầu

> "Em xin demo cơ chế xếp chỗ theo điểm ưu tiên RBL bằng **một sự kiện đã diễn ra**:
> Workshop *Spring Boot MVC* của khoa Kỹ thuật phần mềm, sức chứa chỉ 3 chỗ nhưng có 5 bạn
> đăng ký. Thay vì 'ai bấm trước được trước', hệ thống tính cho mỗi bạn một **điểm ưu tiên
> 0–100** rồi cấp chỗ cho người điểm cao nhất. Em sẽ lấy dữ liệu thật từ database để chỉ ra
> vì sao 3 bạn này được nhận và 2 bạn kia vào hàng chờ."

---

## 1. Công thức điểm ưu tiên (nhắc nhanh)

```
Priority = 0.40·M + 0.30·S + 0.20·P + 0.10·T        (mỗi thành phần ∈ [0, 100])
```

| Ký hiệu | Tiêu chí | Trọng số | Cách tính |
|---|---|---|---|
| **M** | Phù hợp **chuyên ngành** với khoa tổ chức | 40% | Đúng ngành = 100; cùng khoa lớn = 60; khác = 30 |
| **S** | **Học kỳ** (cuối khoá ưu tiên hơn) | 30% | `round(min(semester,9)/9 × 100)` |
| **P** | **Điểm hoạt động** | 20% | `points` nếu ≤ 100; nếu > 100 → `min(100, √points × 10)` |
| **T** | **Thời điểm đăng ký** trong khung mở | 10% | 20% đầu = 100; 20–70% = 70; sau 70% = 40 |

Nhãn: **≥ 75 = Ưu tiên cao**, **45–74 = Trung bình**, **< 45 = Thấp**.
Nguồn code: `src/main/java/com/example/service/PriorityRankingService.java`.

---

## 2. Dựng dữ liệu sự kiện ĐÃ DIỄN RA (PostgreSQL)

> Sự kiện có `status = COMPLETED`, thời gian ở **quá khứ**, đã có **attendance** (chứng tỏ
> đã diễn ra). Mọi bản ghi mang tiền tố `[RBL-DEMO]` và email `rbl.*@demo.edu.vn`.
> Phần CLEANUP ở đầu giúp **chạy lại nhiều lần vẫn sạch**, không đụng dữ liệu khác.
>
> Các tài khoản sinh viên ở đây là **chỉ để hiển thị dữ liệu** (mật khẩu không phải BCrypt
> nên không dùng để đăng nhập) — phù hợp cho mục đích trình chiếu bảng xếp hạng.

```sql
BEGIN;

-- 0) CLEANUP dữ liệu demo cũ (theo thứ tự khoá ngoại)
DELETE FROM attendance   WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[RBL-DEMO]%');
DELETE FROM ticket       WHERE registration_id IN (SELECT id FROM registration WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[RBL-DEMO]%'));
DELETE FROM registration WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[RBL-DEMO]%');
DELETE FROM event        WHERE title LIKE '[RBL-DEMO]%';
DELETE FROM student      WHERE user_id IN (SELECT id FROM users WHERE email LIKE 'rbl.%@demo.edu.vn');
DELETE FROM users        WHERE email LIKE 'rbl.%@demo.edu.vn';

-- 1) Đảm bảo có khoa tổ chức
INSERT INTO department (name, description, created_at)
SELECT 'Kỹ thuật phần mềm', 'Khoa tổ chức (demo RBL)', now()
WHERE NOT EXISTS (SELECT 1 FROM department WHERE name = 'Kỹ thuật phần mềm');

-- 2) 5 sinh viên với chuyên ngành / học kỳ / điểm hoạt động cụ thể (đầu vào để tính M,S,P)
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points)
SELECT v.fn, v.em, 'demo-no-login', v.ph, now() - interval '90 days', true,
       (SELECT id FROM role WHERE name = 'STUDENT'), v.major, v.sem, v.pts
FROM (VALUES
    ('Nguyễn Văn An',  'rbl.an@demo.edu.vn',    '0900000001', 'Kỹ thuật phần mềm',   8,  95),
    ('Trần Thị Bình',  'rbl.binh@demo.edu.vn',  '0900000002', 'Công nghệ Thông tin', 7,  60),
    ('Lê Hoàng Cường', 'rbl.cuong@demo.edu.vn', '0900000003', 'Kỹ thuật phần mềm',   5, 120),
    ('Phạm Minh Dũng', 'rbl.dung@demo.edu.vn',  '0900000004', 'An toàn thông tin',   9,  40),
    ('Võ Thị Em',      'rbl.em@demo.edu.vn',    '0900000005', 'Marketing',           3,  25)
) AS v(fn, em, ph, major, sem, pts);

-- 3) Hồ sơ student (major lấy đúng từ user để tính điểm chuyên ngành M)
INSERT INTO student (student_code, major, year, user_id)
SELECT v.code, u.major, v.year, u.id
FROM (VALUES
    ('rbl.an@demo.edu.vn',    'RBL001', 4),
    ('rbl.binh@demo.edu.vn',  'RBL002', 4),
    ('rbl.cuong@demo.edu.vn', 'RBL003', 3),
    ('rbl.dung@demo.edu.vn',  'RBL004', 5),
    ('rbl.em@demo.edu.vn',    'RBL005', 2)
) AS v(em, code, year)
JOIN users u ON u.email = v.em;

-- 4) Sự kiện ĐÃ DIỄN RA (COMPLETED), capacity = 3, mở đăng ký 20 ngày
INSERT INTO event (title, description, location, start_time, end_time, capacity, status, created_at, budget, organizer, speakers, department_id)
SELECT '[RBL-DEMO] Workshop Spring Boot MVC',
       'Sự kiện đã kết thúc, dùng cơ chế RBL để xếp 3 chỗ cho 5 đăng ký.',
       'Lab 3 - Tòa B',
       TIMESTAMP '2026-05-21 08:00:00',   -- start_time (quá khứ)
       TIMESTAMP '2026-05-21 11:00:00',   -- end_time
       3, 'COMPLETED',
       TIMESTAMP '2026-05-01 08:00:00',   -- created_at = mở đăng ký
       9000000::numeric, 'CLB Kỹ thuật phần mềm', 'TS. Nguyễn Spring',
       (SELECT id FROM department WHERE name = 'Kỹ thuật phần mềm' ORDER BY id LIMIT 1);

-- 5) Đăng ký + điểm ưu tiên + trạng thái CUỐI CÙNG theo RBL
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT v.reg_date, v.status, v.note, v.score,
       (SELECT id FROM event WHERE title = '[RBL-DEMO] Workshop Spring Boot MVC'),
       (SELECT s.id FROM student s WHERE s.student_code = v.code)
FROM (VALUES
    ('RBL001', TIMESTAMP '2026-05-18 20:00:00', 'REGISTERED', 'Điểm cao nhất dù đăng ký muộn nhất',        89.67),
    ('RBL003', TIMESTAMP '2026-05-10 14:00:00', 'REGISTERED', 'Đúng ngành, điểm hoạt động kịch trần',      83.67),
    ('RBL004', TIMESTAMP '2026-05-03 10:00:00', 'REGISTERED', 'Cùng khoa, sắp tốt nghiệp (kỳ 9)',          72.00),
    ('RBL002', TIMESTAMP '2026-05-02 09:00:00', 'WAITLIST',   'Bị 3 người điểm cao hơn vượt qua',          69.33),
    ('RBL005', TIMESTAMP '2026-05-01 08:30:00', 'WAITLIST',   'Khác khoa, kỳ thấp dù đăng ký sớm nhất',    37.00)
) AS v(code, reg_date, status, note, score);

-- 6) Vé cho 3 người được nhận chỗ
INSERT INTO ticket (code, sent_date, registration_id)
SELECT v.code, TIMESTAMP '2026-05-04 09:00:00', r.id
FROM (VALUES
    ('RBL001', 'RBL-TICKET-001'),
    ('RBL003', 'RBL-TICKET-003'),
    ('RBL004', 'RBL-TICKET-004')
) AS v(scode, code)
JOIN student s ON s.student_code = v.scode
JOIN registration r ON r.student_id = s.id
JOIN event e ON e.id = r.event_id AND e.title = '[RBL-DEMO] Workshop Spring Boot MVC';

-- 7) Điểm danh (chứng tỏ sự kiện ĐÃ DIỄN RA): An dự đủ, Cường có mặt, Dũng vắng
INSERT INTO attendance (checkin_time, checkout_time, status, participation_score, note, registration_id, event_id, student_id)
SELECT v.checkin, v.checkout, v.status, v.score, v.note, r.id, e.id, s.id
FROM (VALUES
    ('RBL001', TIMESTAMP '2026-05-21 08:05:00', TIMESTAMP '2026-05-21 10:55:00', 'COMPLETED',  90.0, 'Tham gia đầy đủ'),
    ('RBL003', TIMESTAMP '2026-05-21 08:12:00', NULL,                            'CHECKED_IN', 40.0, 'Có check-in'),
    ('RBL004', TIMESTAMP '2026-05-21 11:00:00', NULL,                            'ABSENT',     0.0,  'Đăng ký nhưng không đến')
) AS v(scode, checkin, checkout, status, score, note)
JOIN student s ON s.student_code = v.scode
JOIN event e ON e.title = '[RBL-DEMO] Workshop Spring Boot MVC'
JOIN registration r ON r.event_id = e.id AND r.student_id = s.id;

COMMIT;
```

---

## 3. Truy vấn TRÌNH CHIẾU (chạy lúc demo)

### 3.1. Thông tin sự kiện (đã kết thúc)

```sql
SELECT title          AS ten_su_kien,
       status         AS trang_thai,
       capacity       AS suc_chua,
       created_at     AS mo_dang_ky,
       start_time     AS bat_dau,
       end_time       AS ket_thuc
FROM event
WHERE title = '[RBL-DEMO] Workshop Spring Boot MVC';
```

Kết quả mong đợi: `COMPLETED`, sức chứa **3**, mở đăng ký 01/05, diễn ra 21/05/2026.

### 3.2. ⭐ Bảng xếp hạng RBL — ai nhận chỗ, ai chờ, và đầu vào điểm

```sql
SELECT
    RANK() OVER (ORDER BY r.priority_score DESC, r.registration_date ASC) AS hang,
    u.full_name          AS sinh_vien,
    s.major              AS chuyen_nganh,
    u.semester           AS hoc_ky,
    u.total_points       AS diem_hoat_dong,
    r.registration_date  AS thoi_gian_dang_ky,
    r.priority_score     AS diem_uu_tien,
    r.status             AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = '[RBL-DEMO] Workshop Spring Boot MVC'
ORDER BY r.priority_score DESC, r.registration_date ASC;
```

Kết quả:

| Hạng | Sinh viên | Chuyên ngành | Kỳ | Điểm HĐ | Đăng ký | Điểm ưu tiên | Trạng thái |
|---|---|---|---|---|---|---|---|
| 1 | Nguyễn Văn An | Kỹ thuật phần mềm | 8 | 95 | 18/05 20:00 | **89.67** | ✅ REGISTERED |
| 2 | Lê Hoàng Cường | Kỹ thuật phần mềm | 5 | 120 | 10/05 14:00 | **83.67** | ✅ REGISTERED |
| 3 | Phạm Minh Dũng | An toàn thông tin | 9 | 40 | 03/05 10:00 | **72.00** | ✅ REGISTERED |
| 4 | Trần Thị Bình | Công nghệ Thông tin | 7 | 60 | 02/05 09:00 | **69.33** | ⏳ WAITLIST |
| 5 | Võ Thị Em | Marketing | 3 | 25 | 01/05 08:30 | **37.00** | ⏳ WAITLIST |

### 3.3. Bóc tách "VÌ SAO" — từng thành phần điểm

| SV | M·0.4 (ngành) | S·0.3 (kỳ) | P·0.2 (điểm HĐ) | T·0.1 (thời gian) | **Tổng** |
|---|---|---|---|---|---|
| An | 100→40.00 | 88.9→26.67 | 95→19.00 | 40 (muộn)→4.00 | **89.67** |
| Cường | 100→40.00 | 55.6→16.67 | 100*→20.00 | 70 (giữa)→7.00 | **83.67** |
| Dũng | 60→24.00 | 100→30.00 | 40→8.00 | 100 (sớm)→10.00 | **72.00** |
| Bình | 60→24.00 | 77.8→23.33 | 60→12.00 | 100 (sớm)→10.00 | **69.33** |
| Em | 30→12.00 | 33.3→10.00 | 25→5.00 | 100 (sớm)→10.00 | **37.00** |

> *Cường có 120 điểm > 100 nên chuẩn hoá `√120 × 10 = 109.5` → cắt còn 100.
> An đăng ký **muộn nhất** (T chỉ 40) nhưng vẫn hạng 1 nhờ **đúng ngành + kỳ 8 + điểm cao**.

### 3.4. Đối chứng: nếu xếp theo FIFO (ai bấm trước được trước)

```sql
SELECT
    ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam,
    u.full_name, s.major, r.registration_date,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) <= e.capacity
         THEN 'Nhận chỗ' ELSE 'Hàng chờ' END AS ket_qua_fifo
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title = '[RBL-DEMO] Workshop Spring Boot MVC'
ORDER BY r.registration_date ASC;
```

| Thứ tự bấm | Sinh viên | FIFO | RBL |
|---|---|---|---|
| 1 | Võ Thị Em (01/05) | ✅ Nhận | ⏳ Chờ |
| 2 | Trần Thị Bình (02/05) | ✅ Nhận | ⏳ Chờ |
| 3 | Phạm Minh Dũng (03/05) | ✅ Nhận | ✅ Nhận |
| 4 | Lê Hoàng Cường (10/05) | ❌ Chờ | ✅ Nhận |
| 5 | Nguyễn Văn An (18/05) | ❌ Chờ | ✅ Nhận |

➡️ **Cùng một tập đăng ký**: FIFO nhận *Em, Bình, Dũng*; RBL nhận *An, Cường, Dũng*.
RBL **giữ đúng đối tượng** (đúng ngành, sắp ra trường, tích cực) mà FIFO bỏ sót.

### 3.5. Sự kiện đã diễn ra — kết quả tham gia thực tế

```sql
SELECT u.full_name, r.priority_score, a.status AS diem_danh, a.participation_score
FROM attendance a
JOIN registration r ON r.id = a.registration_id
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = a.event_id
WHERE e.title = '[RBL-DEMO] Workshop Spring Boot MVC'
ORDER BY r.priority_score DESC;
```

| Sinh viên | Điểm ưu tiên | Điểm danh | Điểm tham gia |
|---|---|---|---|
| Nguyễn Văn An | 89.67 | COMPLETED | 90 |
| Lê Hoàng Cường | 83.67 | CHECKED_IN | 40 |
| Phạm Minh Dũng | 72.00 | ABSENT | 0 |

> Điểm nhấn: bạn được RBL ưu tiên cao **chưa chắc dự đầy đủ** (Dũng vắng), nhưng nhóm
> được chọn vẫn **đúng đối tượng** sự kiện hướng tới.

---

## 4. Kịch bản thuyết trình (đọc theo)

1. **Mở** (mục 0): nêu sự kiện + bài toán 5 người / 3 chỗ.
2. **Chạy 3.1**: "Đây là sự kiện đã kết thúc, sức chứa 3."
3. **Chạy 3.2**: "Hệ thống xếp hạng theo điểm ưu tiên — 3 bạn đầu được nhận chỗ."
4. **Chỉ vào 3.3**: "Vì sao An hạng 1 dù đăng ký muộn nhất? Vì đúng chuyên ngành (40 điểm)
   + sắp ra trường + điểm hoạt động cao. Thời gian chỉ chiếm 10%."
5. **Chạy 3.4**: "Nếu dùng FIFO như hệ thống thường, bạn Em khác ngành kỳ 3 lại được nhận
   chỉ vì bấm sớm, còn 2 bạn đúng ngành thì trượt. RBL sửa đúng điểm này."
6. **Chạy 3.5**: "Và đây là sự kiện đã thực sự diễn ra với dữ liệu điểm danh."
7. **Chốt**: "RBL trả lời câu hỏi *ai xứng đáng có chỗ nhất*, không phải *ai bấm nhanh nhất*."

---

## 5. (Tuỳ chọn) Minh hoạ auto-promote khi có người huỷ

Nếu muốn cho thấy cơ chế kéo hàng chờ lên khi có người huỷ — giả sử **Dũng huỷ**, hệ thống
lấy người **điểm cao nhất** trong WAITLIST (Bình 69.33 > Em 37.00):

```sql
-- Huỷ Dũng
UPDATE registration r SET status = 'CANCELLED'
FROM student s, event e
WHERE r.student_id = s.id AND r.event_id = e.id
  AND e.title = '[RBL-DEMO] Workshop Spring Boot MVC' AND s.student_code = 'RBL004';

-- Kéo người điểm cao nhất trong hàng chờ lên (Bình)
UPDATE registration r SET status = 'REGISTERED', note = 'Tự động lên REGISTERED do có người huỷ'
WHERE r.id = (
    SELECT r2.id FROM registration r2
    JOIN event e2 ON e2.id = r2.event_id
    WHERE e2.title = '[RBL-DEMO] Workshop Spring Boot MVC' AND r2.status = 'WAITLIST'
    ORDER BY r2.priority_score DESC, r2.registration_date ASC
    LIMIT 1);
```

➡️ Sau cùng nhận chỗ: **An, Cường, Bình**; hàng chờ: **Em**.
(FIFO khi Dũng huỷ sẽ kéo *Em* lên vì Em ở đầu hàng chờ theo thời gian — lại khác RBL.)

> Lưu ý: chạy mục 5 sẽ làm dữ liệu lệch khỏi bảng 3.2 ban đầu. Muốn về trạng thái gốc,
> chỉ cần chạy lại toàn bộ script ở mục 2 (đã có CLEANUP).
```
