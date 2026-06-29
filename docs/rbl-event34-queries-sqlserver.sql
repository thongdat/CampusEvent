-- RBL Demo: [AEMS] Event 34 - English Presentation Day
-- SQL Server (SSMS). Database: event_management_db (hoặc DB bạn đang dùng)
--
-- Chuẩn hóa TOÀN BỘ DB [AEMS] (capacity, RBL, vé):
--   → docs/db-reconcile-rbl-sqlserver.sql
--
-- Demo snapshot sự kiện đã diễn ra (clone Event 34):
--   → docs/rbl-event34-snapshot-demo-sqlserver.sql

-- 1) Thông tin sự kiện
SELECT e.id, e.title, d.name AS khoa_to_chuc, e.capacity, e.status,
       e.created_at AS mo_dang_ky, e.start_time AS bat_dau, e.end_time AS ket_thuc,
       COUNT(r.id) AS tong_dang_ky,
       SUM(CASE WHEN r.status = N'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
       SUM(CASE WHEN r.status = N'WAITLIST' THEN 1 ELSE 0 END) AS so_waitlist
FROM event e
JOIN department d ON d.id = e.department_id
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
GROUP BY e.id, e.title, d.name, e.capacity, e.status, e.created_at, e.start_time, e.end_time;

-- 2) Backfill priority_score (nếu NULL)
UPDATE r
SET priority_score = CAST(
    0.40 * CASE
        WHEN u.major IS NULL OR u.major = N'' THEN 30
        WHEN u.major = d.name THEN 100
        ELSE 60
    END
    + 0.30 * CASE
        WHEN u.semester IS NULL OR u.semester < 1 THEN 10
        ELSE (CASE WHEN u.semester > 9 THEN 9 ELSE u.semester END * 100.0 / 9.0)
    END
    + 0.20 * CASE
        WHEN u.total_points IS NULL OR u.total_points <= 0 THEN 0
        WHEN u.total_points <= 100 THEN u.total_points
        ELSE CASE WHEN 100 < SQRT(u.total_points * 1.0) * 10 THEN 100 ELSE SQRT(u.total_points * 1.0) * 10 END
    END
    + 0.10 * CASE
        WHEN e.created_at IS NULL OR e.start_time IS NULL OR r.registration_date IS NULL THEN 70
        WHEN e.start_time <= e.created_at THEN 70
        WHEN DATEDIFF(MINUTE, e.created_at, e.start_time) <= 0 THEN 100
        WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date) <= 0 THEN 100
        WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date) >= DATEDIFF(MINUTE, e.created_at, e.start_time) THEN 40
        WHEN CAST(DATEDIFF(MINUTE, e.created_at, r.registration_date) AS FLOAT)
             / NULLIF(DATEDIFF(MINUTE, e.created_at, e.start_time), 0) <= 0.20 THEN 100
        WHEN CAST(DATEDIFF(MINUTE, e.created_at, r.registration_date) AS FLOAT)
             / NULLIF(DATEDIFF(MINUTE, e.created_at, e.start_time), 0) <= 0.70 THEN 70
        ELSE 40
    END AS DECIMAL(5,2))
FROM registration r
INNER JOIN student s ON s.id = r.student_id
INNER JOIN users u ON u.id = s.user_id
INNER JOIN event e ON e.id = r.event_id
INNER JOIN department d ON d.id = e.department_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
  AND r.priority_score IS NULL;

-- 3) TOÀN BỘ đăng ký — thời gian + điểm RBL cạnh nhau
--    (REGISTERED + WAITLIST; thường ~29 dòng nếu capacity 27 + 2 hàng chờ)
--    Cột trang_thai = trạng thái LƯU TRONG DB lúc app xử lý, KHÔNG tự cập nhật khi backfill điểm.
--    Xếp hạng theo priority_score hiện tại có thể LỆCH với trang_thai → xem query #8.
SELECT
    RANK() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                          r.priority_score DESC, r.registration_date ASC) AS hang,
    u.full_name,
    s.student_code,
    s.major AS chuyen_nganh,
    u.semester AS hoc_ky,
    u.total_points AS diem_hoat_dong,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
    r.priority_score AS diem_uu_tien,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.status AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;

-- 4) CHỈ REGISTERED — được nhận chỗ (thường ~27 dòng = capacity)
--    Khác query #3: lọc status = REGISTERED + thêm cột co_ve.
--    Không có WAITLIST; thứ hạng 1..27 chỉ trong nhóm đã nhận chỗ.
SELECT
    ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                r.priority_score DESC, r.registration_date ASC) AS thu_hang,
    u.full_name,
    s.student_code,
    s.major,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.priority_score,
    CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN N'Có vé' ELSE N'Chưa có vé' END AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
  AND r.status = N'REGISTERED'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;

-- 5) WAITLIST — hàng chờ
SELECT
    ROW_NUMBER() OVER (ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                                r.priority_score DESC, r.registration_date ASC) AS thu_hang_cho,
    u.full_name,
    s.student_code,
    s.major,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm')
        + N' · ' + COALESCE(CAST(r.priority_score AS NVARCHAR(20)), N'NULL') + N' điểm' AS dang_ky_va_diem,
    r.priority_score,
    r.note
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
  AND r.status = N'WAITLIST'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC, r.registration_date ASC;

-- 6) Đối chứng FIFO
WITH fifo AS (
    SELECT
        u.full_name,
        s.major,
        FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
        r.priority_score,
        r.status AS trang_thai_rbl,
        e.capacity,
        ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam
    FROM registration r
    JOIN student s ON s.id = r.student_id
    JOIN users   u ON u.id = s.user_id
    JOIN event   e ON e.id = r.event_id
    WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
)
SELECT
    thu_tu_bam,
    full_name,
    major,
    thoi_gian_dang_ky,
    priority_score,
    trang_thai_rbl,
    CASE WHEN thu_tu_bam <= capacity THEN N'Nhận chỗ (FIFO)' ELSE N'Hàng chờ (FIFO)' END AS ket_qua_fifo
FROM fifo
ORDER BY thu_tu_bam;

-- 8) So sánh: trạng thái THỰC TẾ (DB) vs RBL MÔ PHỎNG (top capacity theo điểm)
--    Giải thích vì sao query #3 thấy 68 điểm WAITLIST nhưng 54.67 REGISTERED.
WITH event_ctx AS (
    SELECT e.id AS event_id, e.capacity
    FROM event e
    WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
),
ranked AS (
    SELECT
        r.id,
        u.full_name,
        s.student_code,
        s.major,
        r.priority_score,
        r.status AS trang_thai_thuc_te,
        FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
        ec.capacity,
        ROW_NUMBER() OVER (
            ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                     r.priority_score DESC, r.registration_date ASC
        ) AS thu_hang_rbl
    FROM registration r
    JOIN student s ON s.id = r.student_id
    JOIN users u ON u.id = s.user_id
    JOIN event_ctx ec ON ec.event_id = r.event_id
)
SELECT
    thu_hang_rbl,
    full_name,
    student_code,
    major,
    thoi_gian_dang_ky,
    priority_score,
    trang_thai_thuc_te,
    CASE WHEN thu_hang_rbl <= capacity THEN N'REGISTERED' ELSE N'WAITLIST' END AS trang_thai_rbl_mo_phong,
    CASE
        WHEN trang_thai_thuc_te = CASE WHEN thu_hang_rbl <= capacity THEN N'REGISTERED' ELSE N'WAITLIST' END
        THEN N'Khớp'
        ELSE N'Lệch (dữ liệu seed/admin/backfill)'
    END AS ghi_chu
FROM ranked
ORDER BY thu_hang_rbl;

-- 7) Backfill vé cho REGISTERED thiếu vé (chạy 1 lần nếu chưa restart app)
INSERT INTO ticket (code, sent_date, registration_id)
SELECT
    N'AEMS-' + UPPER(SUBSTRING(REPLACE(CONVERT(VARCHAR(36), NEWID()), N'-', N''), 1, 8)),
    COALESCE(r.registration_date, SYSDATETIME()),
    r.id
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
  AND r.status = N'REGISTERED'
  AND NOT EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id);

-- Kiểm tra: không còn REGISTERED thiếu vé cho event này
SELECT u.full_name, s.student_code, r.priority_score,
       CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN N'Có vé' ELSE N'Chưa có vé' END AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN event e ON e.id = r.event_id
WHERE e.title LIKE N'%Event 34 - English Presentation Day%'
  AND r.status = N'REGISTERED'
  AND NOT EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id);
