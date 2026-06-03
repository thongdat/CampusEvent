USE event_management_db;
GO

SET NOCOUNT ON;

IF COL_LENGTH('users', 'department_position') IS NULL
    ALTER TABLE users ADD department_position VARCHAR(30) NULL;

IF COL_LENGTH('student', 'no_show_count') IS NULL
    ALTER TABLE student ADD no_show_count INT NOT NULL DEFAULT 0;

IF COL_LENGTH('student', 'attendance_reputation') IS NULL
    ALTER TABLE student ADD attendance_reputation FLOAT NOT NULL DEFAULT 100;

IF OBJECT_ID('attendance_session', 'U') IS NULL
BEGIN
    CREATE TABLE attendance_session (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        event_id BIGINT NOT NULL,
        token VARCHAR(120) NOT NULL,
        session_type VARCHAR(30) NOT NULL,
        created_at DATETIME2 NOT NULL,
        expired_at DATETIME2 NOT NULL,
        status VARCHAR(30) NOT NULL
    );
END

IF COL_LENGTH('attendance', 'mid_verify_time') IS NULL
    ALTER TABLE attendance ADD mid_verify_time DATETIME2 NULL;

IF COL_LENGTH('attendance', 'checkout_time') IS NULL
    ALTER TABLE attendance ADD checkout_time DATETIME2 NULL;

IF COL_LENGTH('attendance', 'participation_score') IS NULL
    ALTER TABLE attendance ADD participation_score FLOAT NULL;

IF COL_LENGTH('attendance', 'note') IS NULL
    ALTER TABLE attendance ADD note NVARCHAR(MAX) NULL;

IF COL_LENGTH('attendance', 'event_id') IS NULL
    ALTER TABLE attendance ADD event_id BIGINT NULL;

IF COL_LENGTH('attendance', 'student_id') IS NULL
    ALTER TABLE attendance ADD student_id BIGINT NULL;

IF OBJECT_ID('quiz_question', 'U') IS NULL
BEGIN
    CREATE TABLE quiz_question (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        event_id BIGINT NOT NULL,
        question_text NVARCHAR(MAX) NOT NULL,
        question_type VARCHAR(30) NOT NULL,
        option_a NVARCHAR(500) NULL,
        option_b NVARCHAR(500) NULL,
        option_c NVARCHAR(500) NULL,
        option_d NVARCHAR(500) NULL,
        correct_answer VARCHAR(20) NULL,
        points INT NOT NULL DEFAULT 1
    );
END

IF OBJECT_ID('quiz_submission', 'U') IS NULL
BEGIN
    CREATE TABLE quiz_submission (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        event_id BIGINT NOT NULL,
        student_id BIGINT NOT NULL,
        total_score FLOAT NOT NULL DEFAULT 0,
        submitted_at DATETIME2 NOT NULL
    );
END

IF OBJECT_ID('quiz_answer', 'U') IS NULL
BEGIN
    CREATE TABLE quiz_answer (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        submission_id BIGINT NOT NULL,
        question_id BIGINT NOT NULL,
        selected_answer VARCHAR(20) NULL,
        answer_text NVARCHAR(MAX) NULL,
        is_correct BIT NULL,
        score FLOAT NOT NULL DEFAULT 0,
        submitted_at DATETIME2 NOT NULL
    );
END

IF OBJECT_ID('event_feedback', 'U') IS NULL
BEGIN
    CREATE TABLE event_feedback (
        id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
        event_id BIGINT NOT NULL,
        student_id BIGINT NOT NULL,
        content_rating INT NOT NULL,
        speaker_rating INT NOT NULL,
        organization_rating INT NOT NULL,
        overall_rating INT NOT NULL,
        comment NVARCHAR(MAX) NULL,
        submitted_at DATETIME2 NOT NULL
    );
END
GO

DECLARE @managerRole BIGINT = (SELECT id FROM role WHERE name = 'MANAGER');
DECLARE @departmentRole BIGINT = (SELECT id FROM role WHERE name = 'DEPARTMENT');

UPDATE users
SET department_position = 'HEAD'
WHERE role_id = @managerRole AND (department_position IS NULL OR department_position <> 'HEAD');

UPDATE users
SET department_position = 'STAFF'
WHERE role_id = @departmentRole AND (department_position IS NULL OR department_position = '');

UPDATE users
SET password = 'plain:dept1234'
WHERE email LIKE 'dept%@fpt.edu.vn'
   OR email LIKE 'dept%@uni.edu.vn'
   OR email IN ('staff.se@fpt.edu.vn','staff.ai@fpt.edu.vn','staff.marketing@fpt.edu.vn','head.hr@fpt.edu.vn');

UPDATE users
SET password = 'plain:admin1234'
WHERE email LIKE 'admin%@fpt.edu.vn'
   OR email LIKE 'admin%@uni.edu.vn';

UPDATE users
SET password = 'plain:com12345'
WHERE email LIKE 'com%@fpt.edu.vn'
   OR email LIKE 'committee%@uni.edu.vn';

UPDATE users
SET password = 'plain:stu12345'
WHERE email LIKE 'sv%@fpt.edu.vn'
   OR email LIKE 'student%@uni.edu.vn';

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff.se@fpt.edu.vn')
    INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points, department_position)
    VALUES (N'Nhân sự SE Proposal', 'staff.se@fpt.edu.vn', 'plain:dept1234', '0912000101', DATEADD(DAY, -18, GETDATE()), 1, @departmentRole, N'Kỹ thuật phần mềm', NULL, 0, 'STAFF');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff.ai@fpt.edu.vn')
    INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points, department_position)
    VALUES (N'Nhân sự AI Proposal', 'staff.ai@fpt.edu.vn', 'plain:dept1234', '0912000102', DATEADD(DAY, -17, GETDATE()), 1, @departmentRole, N'Trí tuệ nhân tạo', NULL, 0, 'STAFF');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'staff.marketing@fpt.edu.vn')
    INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points, department_position)
    VALUES (N'Nhân sự Marketing Proposal', 'staff.marketing@fpt.edu.vn', 'plain:dept1234', '0912000103', DATEADD(DAY, -16, GETDATE()), 1, @departmentRole, N'Marketing', NULL, 0, 'STAFF');

IF NOT EXISTS (SELECT 1 FROM users WHERE email = 'head.hr@fpt.edu.vn')
    INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points, department_position)
    VALUES (N'Trưởng khoa HR Demo', 'head.hr@fpt.edu.vn', 'plain:dept1234', '0912000104', DATEADD(DAY, -15, GETDATE()), 1, @managerRole, N'Kinh tế', NULL, 0, 'HEAD');

DECLARE @seDept BIGINT = (SELECT TOP 1 id FROM department WHERE name = N'Kỹ thuật phần mềm');
DECLARE @aiDept BIGINT = (SELECT TOP 1 id FROM department WHERE name = N'Trí tuệ nhân tạo');
DECLARE @mktDept BIGINT = (SELECT TOP 1 id FROM department WHERE name = N'Marketing');

IF @seDept IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_proposal WHERE title = N'Demo Proposal: Secure Academic Participation Workshop')
    INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id)
    VALUES (N'Demo Proposal: Secure Academic Participation Workshop',
            N'Workshop hướng dẫn sinh viên hiểu dynamic QR, mid-session verification, quiz checkout và feedback học thuật.',
            DATEADD(DAY, 21, GETDATE()), 'PENDING', N'Dữ liệu demo cho nhân sự bộ môn tạo proposal.',
            DATEADD(DAY, -2, GETDATE()), @seDept);

IF @aiDept IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_proposal WHERE title = N'Demo Proposal: AI Learning Analytics Seminar')
    INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id)
    VALUES (N'Demo Proposal: AI Learning Analytics Seminar',
            N'Seminar về phân tích dữ liệu tham gia học thuật và gợi ý sự kiện phù hợp cho sinh viên.',
            DATEADD(DAY, 28, GETDATE()), 'REVISION', N'Bổ sung diễn giả và ngân sách trước khi duyệt.',
            DATEADD(DAY, -4, GETDATE()), @aiDept);

IF @mktDept IS NOT NULL AND NOT EXISTS (SELECT 1 FROM event_proposal WHERE title = N'Demo Proposal: Event Branding Clinic')
    INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id)
    VALUES (N'Demo Proposal: Event Branding Clinic',
            N'Clinic về truyền thông sự kiện học thuật, landing page, email và feedback sau chương trình.',
            DATEADD(DAY, 35, GETDATE()), 'APPROVED', N'Đã duyệt demo để trưởng khoa xem báo cáo.',
            DATEADD(DAY, -6, GETDATE()), @mktDept);

DECLARE @eventId BIGINT = (SELECT TOP 1 id FROM event WHERE status IN ('PUBLISHED', 'APPROVED', 'COMPLETED') ORDER BY start_time DESC);

UPDATE a
SET a.event_id = r.event_id,
    a.student_id = r.student_id
FROM attendance a
JOIN registration r ON r.id = a.registration_id
WHERE a.event_id IS NULL OR a.student_id IS NULL;

UPDATE TOP (16) a
SET a.status = 'COMPLETED',
    a.mid_verify_time = DATEADD(MINUTE, 55, a.checkin_time),
    a.checkout_time = DATEADD(MINUTE, 115, a.checkin_time),
    a.participation_score = 96,
    a.note = N'Excellent Participation'
FROM attendance a
WHERE @eventId IS NOT NULL
  AND a.event_id = @eventId
  AND a.status IN ('ATTENDED', 'CHECKED_IN', 'MID_VERIFIED');

UPDATE TOP (8) a
SET a.status = 'MID_VERIFIED',
    a.mid_verify_time = DATEADD(MINUTE, 50, a.checkin_time),
    a.checkout_time = NULL,
    a.participation_score = 60,
    a.note = N'Partial Participation - chưa checkout'
FROM attendance a
WHERE @eventId IS NOT NULL
  AND a.event_id = @eventId
  AND a.status IN ('ATTENDED', 'CHECKED_IN')
  AND a.checkout_time IS NULL;

UPDATE a
SET a.participation_score = CASE
        WHEN a.status = 'COMPLETED' THEN COALESCE(a.participation_score, 96)
        WHEN a.status = 'MID_VERIFIED' THEN COALESCE(a.participation_score, 60)
        WHEN a.status = 'ABSENT' THEN 0
        ELSE COALESCE(a.participation_score, 40)
    END,
    a.note = COALESCE(a.note, CASE WHEN a.status = 'ABSENT' THEN N'Registered student did not check in' ELSE N'Demo attendance migrated' END)
FROM attendance a
WHERE @eventId IS NOT NULL AND a.event_id = @eventId;

INSERT INTO attendance (checkin_time, status, registration_id, mid_verify_time, checkout_time, participation_score, note, event_id, student_id)
SELECT TOP 6
    ISNULL(e.end_time, GETDATE()),
    'ABSENT',
    r.id,
    NULL,
    NULL,
    0,
    N'Registered student did not check in',
    r.event_id,
    r.student_id
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE @eventId IS NOT NULL
  AND r.event_id = @eventId
  AND r.status = 'REGISTERED'
  AND NOT EXISTS (SELECT 1 FROM attendance a WHERE a.registration_id = r.id);

;WITH no_attendance AS (
    SELECT
        r.id AS registration_id,
        r.event_id,
        r.student_id,
        e.start_time,
        ROW_NUMBER() OVER (ORDER BY r.id) AS rn
    FROM registration r
    JOIN event e ON e.id = r.event_id
    WHERE @eventId IS NOT NULL
      AND r.event_id = @eventId
      AND r.status = 'REGISTERED'
      AND NOT EXISTS (SELECT 1 FROM attendance a WHERE a.registration_id = r.id)
)
INSERT INTO attendance (checkin_time, status, registration_id, mid_verify_time, checkout_time, participation_score, note, event_id, student_id)
SELECT
    DATEADD(MINUTE, -8, start_time),
    CASE rn WHEN 1 THEN 'CHECKED_IN' WHEN 2 THEN 'MID_VERIFIED' ELSE 'CHECKED_OUT' END,
    registration_id,
    CASE WHEN rn >= 2 THEN DATEADD(MINUTE, 55, start_time) ELSE NULL END,
    CASE WHEN rn >= 3 THEN DATEADD(MINUTE, 125, start_time) ELSE NULL END,
    CASE rn WHEN 1 THEN 40 WHEN 2 THEN 60 ELSE 80 END,
    CASE rn WHEN 1 THEN N'Checked in only - incomplete' WHEN 2 THEN N'Mid verified - missing checkout' ELSE N'Checked out - pending completion score' END,
    event_id,
    student_id
FROM no_attendance
WHERE rn <= 3;

UPDATE TOP (1) attendance
SET status = 'CHECKED_OUT',
    mid_verify_time = DATEADD(MINUTE, 55, checkin_time),
    checkout_time = DATEADD(MINUTE, 125, checkin_time),
    participation_score = 80,
    note = N'Checked out - active participation'
WHERE @eventId IS NOT NULL
  AND event_id = @eventId
  AND status = 'ABSENT';

UPDATE TOP (1) attendance
SET status = 'CHECKED_IN',
    mid_verify_time = NULL,
    checkout_time = NULL,
    participation_score = 40,
    note = N'Checked in only - incomplete'
WHERE @eventId IS NOT NULL
  AND event_id = @eventId
  AND status = 'ABSENT';

UPDATE TOP (1) attendance
SET status = 'MID_VERIFIED',
    mid_verify_time = DATEADD(MINUTE, 55, checkin_time),
    checkout_time = NULL,
    participation_score = 60,
    note = N'Mid verified - missing checkout'
WHERE @eventId IS NOT NULL
  AND event_id = @eventId
  AND status = 'ABSENT';

IF @eventId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM quiz_question WHERE event_id = @eventId)
BEGIN
    INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points) VALUES
    (@eventId, N'Mục tiêu chính của sự kiện là gì?', 'MULTIPLE_CHOICE', N'Cập nhật kiến thức học thuật', N'Điểm danh hình thức', N'Bán vé', N'Không có mục tiêu', 'A', 2),
    (@eventId, N'Vì sao QR động an toàn hơn QR tĩnh?', 'MULTIPLE_CHOICE', N'Đổi token theo thời gian', N'Dễ chụp màn hình hơn', N'Không cần đăng nhập', N'Không hết hạn', 'A', 2),
    (@eventId, N'Mid-session verification dùng để làm gì?', 'MULTIPLE_CHOICE', N'Xác nhận sinh viên còn ở lại sự kiện', N'Tạo proposal', N'Đổi mật khẩu', N'Gửi email marketing', 'A', 2),
    (@eventId, N'Nêu một nội dung bạn học được từ sự kiện.', 'SHORT_ANSWER', NULL, NULL, NULL, NULL, NULL, 2),
    (@eventId, N'Feedback sau sự kiện giúp khoa cải thiện điều gì?', 'MULTIPLE_CHOICE', N'Nội dung, diễn giả và tổ chức', N'Mật khẩu admin', N'Cấu hình SQL Server', N'Role hệ thống', 'A', 2);
END

IF @eventId IS NOT NULL AND NOT EXISTS (SELECT 1 FROM attendance_session WHERE event_id = @eventId AND status = 'ACTIVE')
BEGIN
    INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
    VALUES
    (@eventId, CONCAT('DEMO-CHECKIN-', @eventId), 'CHECK_IN', GETDATE(), DATEADD(MINUTE, 3, GETDATE()), 'ACTIVE'),
    (@eventId, CONCAT('DEMO-MID-', @eventId), 'MID_SESSION', GETDATE(), DATEADD(MINUTE, 3, GETDATE()), 'ACTIVE');
END

INSERT INTO event_feedback (event_id, student_id, content_rating, speaker_rating, organization_rating, overall_rating, comment, submitted_at)
SELECT TOP 12
    r.event_id,
    r.student_id,
    4 + (ABS(CHECKSUM(NEWID())) % 2),
    4 + (ABS(CHECKSUM(NEWID())) % 2),
    4,
    4 + (ABS(CHECKSUM(NEWID())) % 2),
    N'Demo feedback: nội dung hữu ích, tổ chức rõ ràng, nên thêm phần thực hành.',
    DATEADD(HOUR, 2, ISNULL(e.end_time, GETDATE()))
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE @eventId IS NOT NULL
  AND r.event_id = @eventId
  AND r.status = 'REGISTERED'
  AND NOT EXISTS (
      SELECT 1 FROM event_feedback ef
      WHERE ef.event_id = r.event_id AND ef.student_id = r.student_id
  );

INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT TOP 12
    r.event_id,
    r.student_id,
    8,
    DATEADD(HOUR, 1, ISNULL(e.end_time, GETDATE()))
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE @eventId IS NOT NULL
  AND r.event_id = @eventId
  AND r.status = 'REGISTERED'
  AND NOT EXISTS (
      SELECT 1 FROM quiz_submission qs
      WHERE qs.event_id = r.event_id AND qs.student_id = r.student_id
  );

INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT
    qs.id,
    qq.id,
    CASE WHEN qq.question_type = 'MULTIPLE_CHOICE' THEN 'A' ELSE NULL END,
    CASE WHEN qq.question_type = 'SHORT_ANSWER' THEN N'Sinh viên học được cách xác thực tham gia bằng QR động, quiz và feedback.' ELSE NULL END,
    CASE WHEN qq.question_type = 'MULTIPLE_CHOICE' THEN 1 ELSE NULL END,
    CASE WHEN qq.question_type = 'MULTIPLE_CHOICE' THEN qq.points ELSE 0 END,
    qs.submitted_at
FROM quiz_submission qs
JOIN quiz_question qq ON qq.event_id = qs.event_id
WHERE @eventId IS NOT NULL
  AND qs.event_id = @eventId
  AND NOT EXISTS (
      SELECT 1 FROM quiz_answer qa
      WHERE qa.submission_id = qs.id AND qa.question_id = qq.id
  );

UPDATE s
SET s.no_show_count = x.absent_count,
    s.attendance_reputation = CASE
        WHEN 100 - x.absent_count * 10 < 0 THEN 0
        ELSE 100 - x.absent_count * 10
    END
FROM student s
JOIN (
    SELECT student_id, COUNT(*) AS absent_count
    FROM attendance
    WHERE status = 'ABSENT' AND student_id IS NOT NULL
    GROUP BY student_id
) x ON x.student_id = s.id;

PRINT N'AEMS demo data inserted/updated successfully.';
PRINT N'Login examples:';
PRINT N'  dept01@fpt.edu.vn / dept1234       (HEAD)';
PRINT N'  staff.se@fpt.edu.vn / dept1234     (STAFF)';
PRINT N'  head.hr@fpt.edu.vn / dept1234      (HEAD demo)';
GO
