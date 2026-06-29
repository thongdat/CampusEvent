-- RBL Demo: [AEMS] Event 34 - English Presentation Day
-- PostgreSQL (campus_event). Dùng SSMS? Xem file rbl-event34-queries-sqlserver.sql

-- 1) Thông tin sự kiện
SELECT e.id, e.title, d.name AS khoa_to_chuc, e.capacity, e.status,
       e.created_at AS mo_dang_ky, e.start_time AS bat_dau, e.end_time AS ket_thuc,
       COUNT(r.id) AS tong_dang_ky,
       COUNT(*) FILTER (WHERE r.status = 'REGISTERED') AS so_registered,
       COUNT(*) FILTER (WHERE r.status = 'WAITLIST') AS so_waitlist
FROM event e
JOIN department d ON d.id = e.department_id
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title LIKE '%[AEMS] Event 32 - Business Case Challenge%'
GROUP BY e.id, d.name;

-- 2) Backfill priority_score (nếu NULL) — công thức xấp xỉ PriorityRankingService
UPDATE registration r
SET priority_score = CAST(
    0.40 * CASE
        WHEN u.major IS NULL OR u.major = '' THEN 30
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
        ELSE LEAST(100, SQRT(u.total_points) * 10)
    END
    + 0.10 * CASE
        WHEN e.created_at IS NULL OR e.start_time IS NULL OR r.registration_date IS NULL THEN 70
        WHEN r.registration_date <= e.created_at + (e.start_time - e.created_at) * 0.2 THEN 100
        WHEN r.registration_date <= e.created_at + (e.start_time - e.created_at) * 0.7 THEN 70
        ELSE 40
    END AS NUMERIC(5,2))
FROM student s
JOIN users u ON u.id = s.user_id
JOIN event e ON e.id = r.event_id
JOIN department d ON d.id = e.department_id
WHERE s.id = r.student_id
  AND e.title LIKE '%Event 34 - English Presentation Day%'
  AND r.priority_score IS NULL;

-- 3) TOÀN BỘ đăng ký — thời gian đăng ký + điểm RBL cạnh nhau
SELECT
    RANK() OVER (ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC) AS hang,
    u.full_name,
    s.student_code,
    s.major AS chuyen_nganh,
    u.semester AS hoc_ky,
    u.total_points AS diem_hoat_dong,
    to_char(r.registration_date, 'DD/MM/YYYY HH24:MI') AS thoi_gian_dang_ky,
    r.priority_score AS diem_uu_tien,
    to_char(r.registration_date, 'DD/MM/YYYY HH24:MI')
        || ' · ' || COALESCE(r.priority_score::text, 'NULL') || ' điểm' AS dang_ky_va_diem,
    r.status AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE '%Event 34 - English Presentation Day%'
ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC;

-- 4) TOP người REGISTERED (được nhận chỗ / có vé)
SELECT
    ROW_NUMBER() OVER (ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC) AS thu_hang,
    u.full_name,
    s.student_code,
    s.major,
    to_char(r.registration_date, 'DD/MM/YYYY HH24:MI')
        || ' · ' || COALESCE(r.priority_score::text, 'NULL') || ' điểm' AS dang_ky_va_diem,
    r.priority_score,
    EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE '%Event 34 - English Presentation Day%'
  AND r.status = 'REGISTERED'
ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC;

-- 5) WAITLIST (hàng chờ)
SELECT
    ROW_NUMBER() OVER (ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC) AS thu_hang_cho,
    u.full_name,
    s.student_code,
    s.major,
    to_char(r.registration_date, 'DD/MM/YYYY HH24:MI')
        || ' · ' || COALESCE(r.priority_score::text, 'NULL') || ' điểm' AS dang_ky_va_diem,
    r.priority_score,
    r.note
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE '%Event 34 - English Presentation Day%'
  AND r.status = 'WAITLIST'
ORDER BY r.priority_score DESC NULLS LAST, r.registration_date ASC;

-- 6) Đối chứng FIFO
SELECT
    ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam,
    u.full_name,
    s.major,
    to_char(r.registration_date, 'DD/MM/YYYY HH24:MI') AS thoi_gian_dang_ky,
    r.priority_score,
    r.status AS trang_thai_rbl,
    CASE WHEN ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) <= e.capacity
         THEN 'Nhận chỗ (FIFO)' ELSE 'Hàng chờ (FIFO)' END AS ket_qua_fifo
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE e.title LIKE '%Event 34 - English Presentation Day%'
ORDER BY r.registration_date ASC;
