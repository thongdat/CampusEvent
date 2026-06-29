-- =============================================================================
-- RBL Demo Snapshot — SQL Server (SSMS)
-- Clone sự kiện THẬT (Event 34) → sự kiện ĐÃ DIỄN RA, dữ liệu RBL nhất quán.
--
-- Cách dùng:
--   1) Chạy toàn bộ script (F5).
--   2) Sự kiện gốc [AEMS] Event 34... KHÔNG bị sửa.
--   3) Demo mới: [RBL-DEMO] Event 34 - English Presentation Day
--   4) Chạy các query kiểm tra ở cuối file.
--
-- Chạy lại script = xóa demo cũ và tạo lại (idempotent).
-- =============================================================================

SET NOCOUNT ON;

DECLARE @SourceLike NVARCHAR(200) = N'%Event 34 - English Presentation Day%';
DECLARE @DemoTitle  NVARCHAR(200) = N'[RBL-DEMO] Event 34 - English Presentation Day';

DECLARE @SourceEventId BIGINT;
DECLARE @DemoEventId   BIGINT;
DECLARE @Capacity      INT;

SELECT TOP 1 @SourceEventId = e.id, @Capacity = e.capacity
FROM event e
WHERE e.title LIKE @SourceLike
  AND e.title NOT LIKE N'[RBL-DEMO]%'
ORDER BY e.id;

IF @SourceEventId IS NULL
BEGIN
    RAISERROR(N'Không tìm thấy sự kiện nguồn (Event 34). Kiểm tra title trong DB.', 16, 1);
    RETURN;
END;

BEGIN TRY
    BEGIN TRANSACTION;

    -- Cleanup demo cũ (thứ tự FK)
    DELETE a
    FROM attendance a
    INNER JOIN event e ON e.id = a.event_id
    WHERE e.title = @DemoTitle;

    DELETE t
    FROM ticket t
    INNER JOIN registration r ON r.id = t.registration_id
    INNER JOIN event e ON e.id = r.event_id
    WHERE e.title = @DemoTitle;

    DELETE r
    FROM registration r
    INNER JOIN event e ON e.id = r.event_id
    WHERE e.title = @DemoTitle;

    DELETE FROM event WHERE title = @DemoTitle;

    -- Clone event → COMPLETED, đưa về quá khứ nếu sự kiện gốc còn ở tương lai
    DECLARE @SrcCreated DATETIME2, @SrcStart DATETIME2, @SrcEnd DATETIME2;
    SELECT @SrcCreated = created_at, @SrcStart = start_time, @SrcEnd = end_time
    FROM event WHERE id = @SourceEventId;

    DECLARE @ShiftDays INT = 0;
    IF @SrcEnd > SYSDATETIME()
        SET @ShiftDays = DATEDIFF(DAY, @SrcEnd, SYSDATETIME()) + 7; -- kết thúc cách đây ~7 ngày

    INSERT INTO event (
        title, description, location, start_time, end_time, capacity, status,
        created_at, budget, organizer, speakers, department_id
    )
    SELECT
        @DemoTitle,
        N'Snapshot demo RBL từ dữ liệu thật Event 34. Trạng thái REGISTERED/WAITLIST '
            + N'được tính lại theo top ' + CAST(capacity AS NVARCHAR(10)) + N' điểm cao nhất.',
        location,
        DATEADD(DAY, -@ShiftDays, start_time),
        DATEADD(DAY, -@ShiftDays, end_time),
        capacity,
        N'COMPLETED',
        DATEADD(DAY, -@ShiftDays, created_at),
        budget,
        organizer,
        speakers,
        department_id
    FROM event
    WHERE id = @SourceEventId;

    SET @DemoEventId = SCOPE_IDENTITY();

    -- Copy đăng ký + tính lại priority_score + gán status RBL chuẩn
    ;WITH source_regs AS (
        SELECT r.student_id, r.registration_date
        FROM registration r
        WHERE r.event_id = @SourceEventId
          AND UPPER(r.status) IN (N'REGISTERED', N'WAITLIST')
    ),
    scored AS (
        SELECT
            sr.student_id,
            DATEADD(DAY, -@ShiftDays, sr.registration_date) AS registration_date,
            CAST(
                0.40 * CASE
                    WHEN s.major IS NULL OR s.major = N'' THEN 30
                    WHEN s.major = d.name THEN 100
                    ELSE 60
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
                    WHEN ev.created_at IS NULL OR ev.start_time IS NULL OR sr.registration_date IS NULL THEN 70
                    WHEN ev.start_time <= ev.created_at THEN 70
                    WHEN DATEDIFF(MINUTE, ev.created_at, ev.start_time) <= 0 THEN 100
                    WHEN DATEDIFF(MINUTE, ev.created_at, sr.registration_date) <= 0 THEN 100
                    WHEN DATEDIFF(MINUTE, ev.created_at, sr.registration_date)
                         >= DATEDIFF(MINUTE, ev.created_at, ev.start_time) THEN 40
                    WHEN CAST(DATEDIFF(MINUTE, ev.created_at, sr.registration_date) AS FLOAT)
                         / NULLIF(DATEDIFF(MINUTE, ev.created_at, ev.start_time), 0) <= 0.20 THEN 100
                    WHEN CAST(DATEDIFF(MINUTE, ev.created_at, sr.registration_date) AS FLOAT)
                         / NULLIF(DATEDIFF(MINUTE, ev.created_at, ev.start_time), 0) <= 0.70 THEN 70
                    ELSE 40
                END AS DECIMAL(5,2)
            ) AS priority_score
        FROM source_regs sr
        INNER JOIN student s ON s.id = sr.student_id
        INNER JOIN users u ON u.id = s.user_id
        INNER JOIN event ev ON ev.id = @SourceEventId
        INNER JOIN department d ON d.id = ev.department_id
    ),
    ranked AS (
        SELECT
            student_id,
            registration_date,
            priority_score,
            ROW_NUMBER() OVER (
                ORDER BY priority_score DESC, registration_date ASC
            ) AS thu_hang
        FROM scored
    )
    INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
    SELECT
        registration_date,
        CASE WHEN thu_hang <= @Capacity THEN N'REGISTERED' ELSE N'WAITLIST' END,
        CASE WHEN thu_hang <= @Capacity
             THEN N'Nhận chỗ — top ' + CAST(@Capacity AS NVARCHAR(10)) + N' điểm RBL (demo snapshot)'
             ELSE N'Hàng chờ — dưới top ' + CAST(@Capacity AS NVARCHAR(10)) + N' (demo snapshot)'
        END,
        priority_score,
        @DemoEventId,
        student_id
    FROM ranked;

    -- Vé cho mọi REGISTERED
    INSERT INTO ticket (code, sent_date, registration_id)
    SELECT
        N'AEMS-' + UPPER(SUBSTRING(REPLACE(CONVERT(VARCHAR(36), NEWID()), N'-', N''), 1, 8)),
        DATEADD(HOUR, 1, r.registration_date),
        r.id
    FROM registration r
    WHERE r.event_id = @DemoEventId
      AND r.status = N'REGISTERED';

    -- Điểm danh mẫu (~80% REGISTERED có mặt) — chứng minh sự kiện đã diễn ra
    ;WITH reg_rank AS (
        SELECT
            r.id AS registration_id,
            r.student_id,
            r.priority_score,
            ROW_NUMBER() OVER (ORDER BY r.priority_score DESC, r.registration_date ASC) AS rn,
            COUNT(*) OVER () AS total_reg
        FROM registration r
        WHERE r.event_id = @DemoEventId
          AND r.status = N'REGISTERED'
    )
    INSERT INTO attendance (checkin_time, checkout_time, status, participation_score, note, registration_id, event_id, student_id)
    SELECT
        DATEADD(MINUTE, 5 + (rn % 20), ev.start_time),
        CASE WHEN rn % 5 <> 0 THEN DATEADD(MINUTE, -5, ev.end_time) ELSE NULL END,
        CASE
            WHEN rn % 7 = 0 THEN N'ABSENT'
            WHEN rn % 5 = 0 THEN N'CHECKED_IN'
            ELSE N'COMPLETED'
        END,
        CASE
            WHEN rn % 7 = 0 THEN 0.0
            WHEN rn % 5 = 0 THEN 40.0
            ELSE 90.0
        END,
        CASE
            WHEN rn % 7 = 0 THEN N'Đăng ký nhưng vắng (demo)'
            WHEN rn % 5 = 0 THEN N'Check-in, chưa check-out (demo)'
            ELSE N'Tham gia đầy đủ (demo)'
        END,
        rr.registration_id,
        @DemoEventId,
        rr.student_id
    FROM reg_rank rr
    CROSS JOIN event ev
    WHERE ev.id = @DemoEventId;

    COMMIT TRANSACTION;

    PRINT N'✓ Demo snapshot OK: ' + @DemoTitle;
    PRINT N'  Nguồn event_id = ' + CAST(@SourceEventId AS NVARCHAR(20));
    PRINT N'  Demo event_id  = ' + CAST(@DemoEventId AS NVARCHAR(20));
    PRINT N'  Capacity       = ' + CAST(@Capacity AS NVARCHAR(10));

END TRY
BEGIN CATCH
    IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
    THROW;
END CATCH;

GO

-- =============================================================================
-- QUERY KIỂM TRA (chạy sau khi build)
-- =============================================================================

DECLARE @DemoTitle NVARCHAR(200) = N'[RBL-DEMO] Event 34 - English Presentation Day';

-- A) Thông tin sự kiện demo
SELECT e.id, e.title, d.name AS khoa, e.capacity, e.status,
       e.created_at AS mo_dang_ky, e.start_time AS bat_dau, e.end_time AS ket_thuc,
       SUM(CASE WHEN r.status = N'REGISTERED' THEN 1 ELSE 0 END) AS so_registered,
       SUM(CASE WHEN r.status = N'WAITLIST' THEN 1 ELSE 0 END) AS so_waitlist
FROM event e
JOIN department d ON d.id = e.department_id
LEFT JOIN registration r ON r.event_id = e.id
WHERE e.title = @DemoTitle
GROUP BY e.id, e.title, d.name, e.capacity, e.status, e.created_at, e.start_time, e.end_time;

-- B) Bảng xếp hạng RBL (toàn bộ) — trạng thái phải khớp thứ hạng
SELECT
    ROW_NUMBER() OVER (ORDER BY r.priority_score DESC, r.registration_date ASC) AS hang,
    u.full_name, s.student_code, s.major,
    FORMAT(r.registration_date, N'dd/MM/yyyy HH:mm') AS thoi_gian_dang_ky,
    r.priority_score,
    r.status,
    CASE WHEN EXISTS (SELECT 1 FROM ticket t WHERE t.registration_id = r.id) THEN N'Có vé' ELSE N'Chưa có vé' END AS co_ve
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN event e ON e.id = r.event_id
WHERE e.title = @DemoTitle
ORDER BY r.priority_score DESC, r.registration_date ASC;

-- C) WAITLIST
SELECT u.full_name, s.student_code, r.priority_score, r.note
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN event e ON e.id = r.event_id
WHERE e.title = @DemoTitle AND r.status = N'WAITLIST'
ORDER BY r.priority_score DESC, r.registration_date ASC;

-- D) Điểm danh (chứng minh đã diễn ra)
SELECT u.full_name, a.status, a.checkin_time, a.checkout_time, a.participation_score
FROM attendance a
JOIN registration r ON r.id = a.registration_id
JOIN student s ON s.id = r.student_id
JOIN users u ON u.id = s.user_id
JOIN event e ON e.id = a.event_id
WHERE e.title = @DemoTitle
ORDER BY a.checkin_time;
