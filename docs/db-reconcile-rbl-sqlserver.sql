-- =============================================================================
-- CampusEvent — Chuẩn hóa DB (SQL Server / SSMS)
--
-- Sửa các lệch thường gặp sau DataSeeder / TestRegistrationBackfill:
--   1) Tính lại priority_score (RBL)
--   2) Gộp đăng ký trùng (cùng event + student)
--   3) REGISTERED/WAITLIST theo capacity (top N điểm cao nhất)
--   4) Vé: REGISTERED có vé, WAITLIST/CANCELLED không có vé
--
-- CÁCH CHẠY:
--   - Backup DB trước (khuyến nghị).
--   - Mở SSMS → chọn database → F5 chạy toàn file.
--   - Bỏ qua [QA-TEST]% và [RBL-DEMO]% (giữ data test/demo riêng).
--
-- An toàn chạy lại nhiều lần (idempotent).
-- =============================================================================

SET NOCOUNT ON;
SET XACT_ABORT ON;

DECLARE @SkipQaDemo BIT = 0;  -- 1 = bỏ qua [QA-TEST]%/[RBL-DEMO]%; 0 = áp dụng tất cả

-- Chế độ chạy:
--   'SAFE' = chỉ tính lại priority_score + đồng bộ vé, GIỮ NGUYÊN REGISTERED/WAITLIST.
--   'FULL' = thêm: huỷ đăng ký trùng + xếp lại REGISTERED/WAITLIST theo capacity.
DECLARE @Mode NVARCHAR(10) = N'FULL';

-------------------------------------------------------------------------
-- Bảng map Ngành -> Khoa lớn (khớp com.example.config.AcademicStructure)
-- Dùng để tính điểm M: đúng ngành = 100, cùng khoa lớn = 60, khác = 30.
-------------------------------------------------------------------------
IF OBJECT_ID('tempdb..#major_faculty') IS NOT NULL DROP TABLE #major_faculty;
CREATE TABLE #major_faculty (major NVARCHAR(200) NOT NULL, faculty NVARCHAR(200) NOT NULL);

INSERT INTO #major_faculty (major, faculty) VALUES
    (N'Công nghệ Thông tin',          N'Công nghệ Thông tin'),
    (N'Kỹ thuật phần mềm',            N'Công nghệ Thông tin'),
    (N'An toàn thông tin',            N'Công nghệ Thông tin'),
    (N'Trí tuệ nhân tạo',             N'Công nghệ Thông tin'),
    (N'Data Science',                 N'Công nghệ Thông tin'),
    (N'Kinh tế',                      N'Kinh tế'),
    (N'Marketing',                    N'Kinh tế'),
    (N'Quản trị kinh doanh',          N'Kinh tế'),
    (N'Tài chính Ngân hàng',          N'Kinh tế'),
    (N'Thiết kế Mỹ thuật số',         N'Thiết kế & Truyền thông'),
    (N'Thiết kế Đồ họa',              N'Thiết kế & Truyền thông'),
    (N'Truyền thông đa phương tiện',  N'Thiết kế & Truyền thông'),
    (N'Ngôn ngữ Anh',                 N'Ngôn ngữ'),
    (N'Ngôn ngữ Nhật',                N'Ngôn ngữ'),
    (N'Du lịch - Khách sạn',          N'Du lịch - Khách sạn'),
    (N'Hospitality Management',       N'Du lịch - Khách sạn');

-- Cảnh báo nếu có ngành sinh viên chưa map được (sẽ tính như "khác khoa")
DECLARE @UnmappedMajors INT = (
    SELECT COUNT(DISTINCT s.major)
    FROM student s
    WHERE s.major IS NOT NULL AND s.major <> N''
      AND NOT EXISTS (SELECT 1 FROM #major_faculty mf WHERE mf.major = s.major)
);
IF @UnmappedMajors > 0
    PRINT N'⚠ Có ' + CAST(@UnmappedMajors AS NVARCHAR(10)) + N' ngành SV chưa map khoa (sẽ tính điểm M như khác khoa).';

DECLARE @ScoresUpdated     INT = 0;
DECLARE @DupCancelled      INT = 0;
DECLARE @StatusChanged     INT = 0;
DECLARE @TicketsIssued     INT = 0;
DECLARE @TicketsRevoked    INT = 0;

BEGIN TRY
    BEGIN TRANSACTION;

    -------------------------------------------------------------------------
    -- Bước 1: Tính lại priority_score (REGISTERED + WAITLIST)
    -------------------------------------------------------------------------
    UPDATE r
    SET priority_score = CAST(
        0.40 * CASE
            WHEN s.major IS NULL OR s.major = N'' THEN 30          -- SV không có ngành
            WHEN d.name IS NULL OR d.name = N'' THEN 60            -- khoa tổ chức trống
            WHEN s.major = d.name THEN 100                        -- đúng ngành
            WHEN mfs.faculty IS NOT NULL AND mfs.faculty = mfd.faculty THEN 60  -- cùng khoa lớn
            ELSE 30                                               -- khác khoa
        END
        + 0.30 * CASE
            WHEN u.semester IS NULL OR u.semester < 1 THEN 10
            ELSE (CASE WHEN u.semester > 9 THEN 9 ELSE u.semester END * 100.0 / 9.0)
        END
        + 0.20 * CASE
            WHEN u.total_points IS NULL OR u.total_points <= 0 THEN 0
            WHEN u.total_points <= 100 THEN u.total_points
            ELSE CASE WHEN 100 < SQRT(u.total_points * 1.0) * 10
                      THEN 100 ELSE SQRT(u.total_points * 1.0) * 10 END
        END
        + 0.10 * CASE
            WHEN e.created_at IS NULL OR e.start_time IS NULL OR r.registration_date IS NULL THEN 70
            WHEN e.start_time <= e.created_at THEN 70
            WHEN DATEDIFF(MINUTE, e.created_at, e.start_time) <= 0 THEN 100
            WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date) <= 0 THEN 100
            WHEN DATEDIFF(MINUTE, e.created_at, r.registration_date)
                 >= DATEDIFF(MINUTE, e.created_at, e.start_time) THEN 40
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
    LEFT JOIN #major_faculty mfs ON mfs.major = s.major
    LEFT JOIN #major_faculty mfd ON mfd.major = d.name
    WHERE UPPER(r.status) IN (N'REGISTERED', N'WAITLIST')
      AND (@SkipQaDemo = 0 OR (e.title NOT LIKE N'[[]QA-TEST]%' AND e.title NOT LIKE N'[[]RBL-DEMO]%'));

    SET @ScoresUpdated = @@ROWCOUNT;

    -------------------------------------------------------------------------
    -- Bước 2: Huỷ bản ghi trùng (giữ 1 đăng ký / event / student) — chỉ FULL
    -------------------------------------------------------------------------
    IF @Mode = N'FULL'
    BEGIN
    ;WITH dup_rank AS (
        SELECT
            r.id,
            ROW_NUMBER() OVER (
                PARTITION BY r.event_id, r.student_id
                ORDER BY
                    CASE UPPER(r.status)
                        WHEN N'REGISTERED' THEN 3
                        WHEN N'WAITLIST'   THEN 2
                        ELSE 1
                    END DESC,
                    r.id DESC
            ) AS rn
        FROM registration r
        INNER JOIN event e ON e.id = r.event_id
        WHERE UPPER(r.status) IN (N'REGISTERED', N'WAITLIST', N'CANCELLED')
          AND (@SkipQaDemo = 0 OR (e.title NOT LIKE N'[[]QA-TEST]%' AND e.title NOT LIKE N'[[]RBL-DEMO]%'))
    )
    UPDATE r
    SET status = N'CANCELLED',
        note   = N'Trùng đăng ký — DB reconcile (giữ bản ghi mới nhất/ưu tiên cao hơn)'
    FROM registration r
    INNER JOIN dup_rank d ON d.id = r.id
    WHERE d.rn > 1
      AND UPPER(r.status) <> N'CANCELLED';

    SET @DupCancelled = @@ROWCOUNT;
    END

    -------------------------------------------------------------------------
    -- Bước 3: REGISTERED / WAITLIST theo capacity + RBL — chỉ FULL
    -------------------------------------------------------------------------
    IF @Mode = N'FULL'
    BEGIN
    ;WITH eligible AS (
        SELECT
            r.id,
            e.capacity,
            ROW_NUMBER() OVER (
                PARTITION BY r.event_id
                ORDER BY
                    r.priority_score DESC,
                    r.registration_date ASC,
                    r.id ASC
            ) AS slot_rank
        FROM registration r
        INNER JOIN event e ON e.id = r.event_id
        WHERE UPPER(r.status) IN (N'REGISTERED', N'WAITLIST')
          AND e.capacity IS NOT NULL
          AND e.capacity > 0
          AND (@SkipQaDemo = 0 OR (e.title NOT LIKE N'[[]QA-TEST]%' AND e.title NOT LIKE N'[[]RBL-DEMO]%'))
    )
    UPDATE r
    SET status = CASE WHEN el.slot_rank <= el.capacity THEN N'REGISTERED' ELSE N'WAITLIST' END,
        note   = CASE
            WHEN el.slot_rank <= el.capacity THEN
                CASE WHEN UPPER(r.status) = N'WAITLIST'
                     THEN N'Lên REGISTERED — top ' + CAST(el.capacity AS NVARCHAR(10)) + N' RBL (DB reconcile)'
                     ELSE COALESCE(r.note, N'')
                END
            ELSE
                CASE WHEN UPPER(r.status) = N'REGISTERED'
                     THEN N'Hàng chờ — vượt capacity ' + CAST(el.capacity AS NVARCHAR(10)) + N' (DB reconcile)'
                     ELSE COALESCE(r.note, N'')
                END
        END
    FROM registration r
    INNER JOIN eligible el ON el.id = r.id
    WHERE (UPPER(r.status) = N'REGISTERED' AND el.slot_rank > el.capacity)
       OR (UPPER(r.status) = N'WAITLIST'   AND el.slot_rank <= el.capacity);

    SET @StatusChanged = @@ROWCOUNT;
    END

    -------------------------------------------------------------------------
    -- Bước 4: Thu hồi vé (không còn REGISTERED)
    -------------------------------------------------------------------------
    DELETE t
    FROM ticket t
    INNER JOIN registration r ON r.id = t.registration_id
    INNER JOIN event e ON e.id = r.event_id
    WHERE UPPER(r.status) <> N'REGISTERED'
      AND (@SkipQaDemo = 0 OR (e.title NOT LIKE N'[[]QA-TEST]%' AND e.title NOT LIKE N'[[]RBL-DEMO]%'));

    SET @TicketsRevoked = @@ROWCOUNT;

    -------------------------------------------------------------------------
    -- Bước 5: Phát vé cho REGISTERED thiếu vé
    -------------------------------------------------------------------------
    INSERT INTO ticket (code, sent_date, registration_id)
    SELECT
        N'AEMS-' + UPPER(SUBSTRING(REPLACE(CONVERT(VARCHAR(36), NEWID()), N'-', N''), 1, 8)),
        COALESCE(r.registration_date, SYSDATETIME()),
        r.id
    FROM registration r
    INNER JOIN event e ON e.id = r.event_id
    WHERE UPPER(r.status) = N'REGISTERED'
      AND NOT EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id)
      AND (@SkipQaDemo = 0 OR (e.title NOT LIKE N'[[]QA-TEST]%' AND e.title NOT LIKE N'[[]RBL-DEMO]%'));

    SET @TicketsIssued = @@ROWCOUNT;

    COMMIT TRANSACTION;

    PRINT N'========================================';
    PRINT N' DB RECONCILE — HOÀN TẤT (mode=' + @Mode + N')';
    PRINT N'========================================';
    PRINT N'  priority_score cập nhật : ' + CAST(@ScoresUpdated AS NVARCHAR(20));
    PRINT N'  trùng → CANCELLED         : ' + CAST(@DupCancelled AS NVARCHAR(20));
    PRINT N'  đổi REGISTERED/WAITLIST   : ' + CAST(@StatusChanged AS NVARCHAR(20));
    PRINT N'  vé thu hồi                : ' + CAST(@TicketsRevoked AS NVARCHAR(20));
    PRINT N'  vé phát mới               : ' + CAST(@TicketsIssued AS NVARCHAR(20));
    PRINT N'========================================';

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    DECLARE @Msg NVARCHAR(4000) = ERROR_MESSAGE();
    RAISERROR(N'DB reconcile thất bại: %s', 16, 1, @Msg);
END CATCH;

GO

-- =============================================================================
-- KIỂM TRA SAU KHI CHẠY
-- =============================================================================

-- A) Event nào vẫn REGISTERED > capacity? (kỳ vọng: 0 dòng)
SELECT
    e.id,
    e.title,
    e.capacity,
    SUM(CASE WHEN UPPER(r.status) = N'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
    SUM(CASE WHEN UPPER(r.status) = N'WAITLIST'   THEN 1 ELSE 0 END) AS so_waitlist
FROM event e
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title NOT LIKE N'[[]QA-TEST]%'
  AND e.title NOT LIKE N'[[]RBL-DEMO]%'
GROUP BY e.id, e.title, e.capacity
HAVING e.capacity IS NOT NULL
   AND e.capacity > 0
   AND SUM(CASE WHEN UPPER(r.status) = N'REGISTERED' THEN 1 ELSE 0 END) > e.capacity
ORDER BY e.id;

-- B) REGISTERED thiếu vé? (kỳ vọng: 0 dòng)
SELECT e.title, u.full_name, s.student_code, r.priority_score
FROM registration r
JOIN event e ON e.id = r.event_id
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
WHERE UPPER(r.status) = N'REGISTERED'
  AND e.title NOT LIKE N'[[]QA-TEST]%'
  AND e.title NOT LIKE N'[[]RBL-DEMO]%'
  AND NOT EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id);

-- C) Event 32 mẫu (Business Case Challenge)
SELECT
    e.title, e.capacity, e.status,
    SUM(CASE WHEN UPPER(r.status) = N'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
    SUM(CASE WHEN UPPER(r.status) = N'WAITLIST'   THEN 1 ELSE 0 END) AS so_waitlist
FROM event e
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title LIKE N'%Event 32 - Business Case Challenge%'
  AND e.title NOT LIKE N'[[]RBL-DEMO]%'
GROUP BY e.title, e.capacity, e.status;

-- D) Tổng hợp tất cả sự kiện sau reconcile (REGISTERED đều ≤ capacity)
SELECT
    e.id,
    LEFT(e.title, 55) AS title,
    e.capacity,
    e.status,
    SUM(CASE WHEN UPPER(r.status) = N'REGISTERED' THEN 1 ELSE 0 END) AS registered,
    SUM(CASE WHEN UPPER(r.status) = N'WAITLIST'   THEN 1 ELSE 0 END) AS waitlist
FROM event e
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title NOT LIKE N'[[]QA-TEST]%'
  AND e.title NOT LIKE N'[[]RBL-DEMO]%'
GROUP BY e.id, e.title, e.capacity, e.status
ORDER BY e.id;
