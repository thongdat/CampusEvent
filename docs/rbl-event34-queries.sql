-- RBL Demo: [AEMS] Event 34 - English Presentation Day
-- SQL Server / SSMS version.
-- All queries are locked to exactly 1 event: latest event matching 'Event 34 - English Presentation Day'.

-- Event picker used in every query:
-- SELECT TOP (1) id FROM [event]
-- WHERE title LIKE '%Event 34 - English Presentation Day%'
-- ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC

-- 1) Event summary
SELECT
    e.id,
    e.title,
    d.name AS khoa_to_chuc,
    e.capacity,
    e.status,
    e.created_at AS mo_dang_ky,
    e.start_time AS bat_dau,
    e.end_time AS ket_thuc,
    COUNT(r.id) AS tong_dang_ky,
    SUM(CASE WHEN r.status = 'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
    SUM(CASE WHEN r.status = 'WAITLIST' THEN 1 ELSE 0 END) AS so_waitlist
FROM [event] e
JOIN department d ON d.id = e.department_id
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
GROUP BY
    e.id, e.title, d.name, e.capacity, e.status,
    e.created_at, e.start_time, e.end_time;

-- 2) Backfill priority_score when NULL
UPDATE r
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
        ELSE CASE
            WHEN SQRT(CAST(u.total_points AS FLOAT)) * 10 > 100 THEN 100
            ELSE SQRT(CAST(u.total_points AS FLOAT)) * 10
        END
    END
    + 0.10 * CASE
        WHEN e.created_at IS NULL OR e.start_time IS NULL OR r.registration_date IS NULL THEN 70
        WHEN r.registration_date <= DATEADD(SECOND, CAST(DATEDIFF(SECOND, e.created_at, e.start_time) * 0.2 AS INT), e.created_at) THEN 100
        WHEN r.registration_date <= DATEADD(SECOND, CAST(DATEDIFF(SECOND, e.created_at, e.start_time) * 0.7 AS INT), e.created_at) THEN 70
        ELSE 40
    END AS DECIMAL(5,2))
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN [event] e ON e.id = r.event_id
JOIN department d ON d.id = e.department_id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
AND r.priority_score IS NULL;

-- 3) All registrations: registration time + RBL score
SELECT
    RANK() OVER (
        ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                 r.priority_score DESC,
                 r.registration_date ASC
    ) AS hang,
    u.full_name,
    s.student_code,
    s.major AS chuyen_nganh,
    u.semester AS hoc_ky,
    u.total_points AS diem_hoat_dong,
    CONVERT(VARCHAR(10), r.registration_date, 103) + ' ' + LEFT(CONVERT(VARCHAR(8), r.registration_date, 108), 5) AS thoi_gian_dang_ky,
    r.priority_score AS diem_uu_tien,
    CONCAT(
        CONVERT(VARCHAR(10), r.registration_date, 103),
        ' ',
        LEFT(CONVERT(VARCHAR(8), r.registration_date, 108), 5),
        ' - ',
        COALESCE(CAST(r.priority_score AS VARCHAR(20)), 'NULL'),
        ' diem'
    ) AS dang_ky_va_diem,
    r.status AS trang_thai
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN [event] e ON e.id = r.event_id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC,
         r.registration_date ASC;

-- 4) REGISTERED students with ticket status
SELECT
    ROW_NUMBER() OVER (
        ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                 r.priority_score DESC,
                 r.registration_date ASC
    ) AS thu_hang,
    u.full_name,
    s.student_code,
    s.major,
    CONCAT(
        CONVERT(VARCHAR(10), r.registration_date, 103),
        ' ',
        LEFT(CONVERT(VARCHAR(8), r.registration_date, 108), 5),
        ' - ',
        COALESCE(CAST(r.priority_score AS VARCHAR(20)), 'NULL'),
        ' diem'
    ) AS dang_ky_va_diem,
    r.priority_score,
    CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN 1 ELSE 0 END AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN [event] e ON e.id = r.event_id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
AND r.status = 'REGISTERED'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC,
         r.registration_date ASC;

-- 5) WAITLIST students
SELECT
    ROW_NUMBER() OVER (
        ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
                 r.priority_score DESC,
                 r.registration_date ASC
    ) AS thu_hang_cho,
    u.full_name,
    s.student_code,
    s.major,
    CONCAT(
        CONVERT(VARCHAR(10), r.registration_date, 103),
        ' ',
        LEFT(CONVERT(VARCHAR(8), r.registration_date, 108), 5),
        ' - ',
        COALESCE(CAST(r.priority_score AS VARCHAR(20)), 'NULL'),
        ' diem'
    ) AS dang_ky_va_diem,
    r.priority_score,
    r.note
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN [event] e ON e.id = r.event_id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
AND r.status = 'WAITLIST'
ORDER BY CASE WHEN r.priority_score IS NULL THEN 1 ELSE 0 END,
         r.priority_score DESC,
         r.registration_date ASC;

-- 6) FIFO comparison
SELECT
    ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) AS thu_tu_bam,
    u.full_name,
    s.major,
    CONVERT(VARCHAR(10), r.registration_date, 103) + ' ' + LEFT(CONVERT(VARCHAR(8), r.registration_date, 108), 5) AS thoi_gian_dang_ky,
    r.priority_score,
    r.status AS trang_thai_rbl,
    CASE
        WHEN ROW_NUMBER() OVER (ORDER BY r.registration_date ASC) <= e.capacity THEN 'Nhan cho (FIFO)'
        ELSE 'Hang cho (FIFO)'
    END AS ket_qua_fifo
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN [event] e ON e.id = r.event_id
WHERE e.id = (
    SELECT TOP (1) id
    FROM [event]
    WHERE title LIKE '%Event 34 - English Presentation Day%'
    ORDER BY CASE WHEN start_time IS NULL THEN 1 ELSE 0 END, start_time DESC, id DESC
)
ORDER BY r.registration_date ASC;
