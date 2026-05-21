USE [event_management_db];
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRANSACTION;

ALTER TABLE dbo.role ALTER COLUMN description NVARCHAR(MAX) NULL;
ALTER TABLE dbo.department ALTER COLUMN name NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.department ALTER COLUMN description NVARCHAR(MAX) NULL;
ALTER TABLE dbo.users ALTER COLUMN full_name NVARCHAR(100) NOT NULL;
ALTER TABLE dbo.users ALTER COLUMN major NVARCHAR(100) NULL;
ALTER TABLE dbo.student ALTER COLUMN major NVARCHAR(100) NULL;
ALTER TABLE dbo.event ALTER COLUMN title NVARCHAR(200) NOT NULL;
ALTER TABLE dbo.event ALTER COLUMN description NVARCHAR(MAX) NULL;
ALTER TABLE dbo.event ALTER COLUMN location NVARCHAR(200) NULL;
ALTER TABLE dbo.event_proposal ALTER COLUMN title NVARCHAR(200) NOT NULL;
ALTER TABLE dbo.event_proposal ALTER COLUMN description NVARCHAR(MAX) NULL;
ALTER TABLE dbo.event_proposal ALTER COLUMN note NVARCHAR(MAX) NULL;
ALTER TABLE dbo.registration ALTER COLUMN note NVARCHAR(MAX) NULL;
ALTER TABLE dbo.feedback ALTER COLUMN comment NVARCHAR(MAX) NULL;
ALTER TABLE dbo.email_log ALTER COLUMN subject NVARCHAR(200) NOT NULL;
ALTER TABLE dbo.email_log ALTER COLUMN content NVARCHAR(MAX) NULL;
ALTER TABLE dbo.activity_log ALTER COLUMN description NVARCHAR(500) NULL;

UPDATE dbo.role
SET description = CASE name
    WHEN 'ADMIN' THEN N'Quản trị hệ thống: quản lý user, role, department và báo cáo.'
    WHEN 'DEPARTMENT' THEN N'Khoa/Bộ môn: tạo proposal, cập nhật proposal và quản lý event đã duyệt.'
    WHEN 'COMMITTEE' THEN N'Hội đồng duyệt: xem, phê duyệt, từ chối hoặc yêu cầu chỉnh sửa proposal.'
    WHEN 'STUDENT' THEN N'Sinh viên: xem event, đăng ký, check-in và gửi feedback.'
    ELSE description
END;

DECLARE @Department TABLE (
    id BIGINT PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX) NULL
);

INSERT INTO @Department (id, name, description) VALUES
(1, N'IT Department', N'Bộ phận Công nghệ Thông tin'),
(2, N'HR Department', N'Bộ phận Nhân sự'),
(10002, N'Công nghệ Thông tin', N'Quản lý seminar, workshop lập trình, cloud, database và software engineering.'),
(10003, N'Kỹ thuật phần mềm', N'Phụ trách các hoạt động về quy trình phát triển phần mềm và SWP.'),
(10004, N'Trí tuệ nhân tạo', N'Tổ chức workshop AI, machine learning, data mining và ứng dụng AI.'),
(10005, N'An toàn thông tin', N'Tổ chức chuyên đề bảo mật, CTF, network security và secure coding.'),
(10006, N'Data Science', N'Quản lý sự kiện phân tích dữ liệu, BI, thống kê và trực quan hóa.'),
(10007, N'Thiết kế Mỹ thuật số', N'Phụ trách seminar UX/UI, product design và multimedia design.'),
(10008, N'Kinh tế', N'Tổ chức talkshow kinh tế, tài chính và phân tích thị trường.'),
(10009, N'Marketing', N'Quản lý sự kiện truyền thông, branding và digital marketing.'),
(10010, N'Quản trị kinh doanh', N'Tổ chức hội thảo quản trị, khởi nghiệp và kỹ năng lãnh đạo.'),
(10011, N'Ngôn ngữ Anh', N'Quản lý English club, workshop học thuật và giao lưu quốc tế.'),
(10012, N'Du lịch - Khách sạn', N'Tổ chức event hướng nghiệp, dịch vụ và hospitality management.'),
(10013, N'Truyền thông đa phương tiện', N'Quản lý workshop video, content creation và truyền thông số.'),
(10014, N'Công nghệ Thông tin', N'Quản lý seminar, workshop lập trình, cloud, database và software engineering.'),
(10015, N'Kỹ thuật phần mềm', N'Phụ trách các hoạt động về quy trình phát triển phần mềm và SWP.'),
(10016, N'Trí tuệ nhân tạo', N'Tổ chức workshop AI, machine learning, data mining và ứng dụng AI.'),
(10017, N'Thiết kế Mỹ thuật số', N'Phụ trách seminar UX/UI, product design và multimedia design.'),
(10018, N'Kinh tế', N'Tổ chức talkshow kinh tế, tài chính và phân tích thị trường.'),
(10019, N'Quản trị kinh doanh', N'Tổ chức hội thảo quản trị, khởi nghiệp và kỹ năng lãnh đạo.'),
(10020, N'Ngôn ngữ Anh', N'Quản lý English club, workshop học thuật và giao lưu quốc tế.'),
(10021, N'Du lịch - Khách sạn', N'Tổ chức event hướng nghiệp, dịch vụ và hospitality management.'),
(10022, N'Truyền thông đa phương tiện', N'Quản lý workshop video, content creation và truyền thông số.'),
(10023, N'Công nghệ Thông tin', N'Quản lý seminar, workshop lập trình, cloud, database và software engineering.'),
(10024, N'Kỹ thuật phần mềm', N'Phụ trách các hoạt động về quy trình phát triển phần mềm và SWP.'),
(10025, N'Trí tuệ nhân tạo', N'Tổ chức workshop AI, machine learning, data mining và ứng dụng AI.'),
(10026, N'Thiết kế Mỹ thuật số', N'Phụ trách seminar UX/UI, product design và multimedia design.'),
(10027, N'Kinh tế', N'Tổ chức talkshow kinh tế, tài chính và phân tích thị trường.'),
(10028, N'Quản trị kinh doanh', N'Tổ chức hội thảo quản trị, khởi nghiệp và kỹ năng lãnh đạo.'),
(10029, N'Ngôn ngữ Anh', N'Quản lý English club, workshop học thuật và giao lưu quốc tế.'),
(10030, N'Du lịch - Khách sạn', N'Tổ chức event hướng nghiệp, dịch vụ và hospitality management.'),
(10031, N'Truyền thông đa phương tiện', N'Quản lý workshop video, content creation và truyền thông số.');

UPDATE d
SET d.name = m.name,
    d.description = m.description
FROM dbo.department d
JOIN @Department m ON m.id = d.id;

;WITH department_refs AS (
    SELECT d.id,
           d.name,
           (
               SELECT COUNT_BIG(*) FROM dbo.event e WHERE e.department_id = d.id
           ) + (
               SELECT COUNT_BIG(*) FROM dbo.event_proposal p WHERE p.department_id = d.id
           ) AS ref_count
    FROM dbo.department d
),
ranked AS (
    SELECT id,
           ref_count,
           ROW_NUMBER() OVER (
               PARTITION BY name
               ORDER BY CASE WHEN ref_count > 0 THEN 0 ELSE 1 END, id
           ) AS row_num
    FROM department_refs
)
DELETE d
FROM dbo.department d
JOIN ranked r ON r.id = d.id
WHERE r.row_num > 1
  AND r.ref_count = 0;

DECLARE @DeptIndex TABLE (idx INT PRIMARY KEY, name NVARCHAR(100) NOT NULL);
INSERT INTO @DeptIndex (idx, name) VALUES
(1, N'Công nghệ Thông tin'),
(2, N'Kỹ thuật phần mềm'),
(3, N'Trí tuệ nhân tạo'),
(4, N'An toàn thông tin'),
(5, N'Data Science'),
(6, N'Thiết kế Mỹ thuật số'),
(7, N'Kinh tế'),
(8, N'Marketing'),
(9, N'Quản trị kinh doanh'),
(10, N'Ngôn ngữ Anh'),
(11, N'Du lịch - Khách sạn'),
(12, N'Truyền thông đa phương tiện');

DECLARE @LastName TABLE (idx INT PRIMARY KEY, name NVARCHAR(40) NOT NULL);
INSERT INTO @LastName (idx, name) VALUES
(1, N'Nguyễn'), (2, N'Trần'), (3, N'Lê'), (4, N'Phạm'),
(5, N'Hoàng'), (6, N'Đỗ'), (7, N'Vũ'), (8, N'Ngô'),
(9, N'Bùi'), (10, N'Đặng'), (11, N'Võ'), (12, N'Mai');

DECLARE @MiddleName TABLE (idx INT PRIMARY KEY, name NVARCHAR(40) NOT NULL);
INSERT INTO @MiddleName (idx, name) VALUES
(1, N'Minh'), (2, N'Gia'), (3, N'Thu'), (4, N'Hoàng'),
(5, N'Quốc'), (6, N'Bảo'), (7, N'Khánh'), (8, N'Thanh'),
(9, N'Anh'), (10, N'Hữu'), (11, N'Ngọc'), (12, N'Phương');

DECLARE @FirstName TABLE (idx INT PRIMARY KEY, name NVARCHAR(40) NOT NULL);
INSERT INTO @FirstName (idx, name) VALUES
(1, N'An'), (2, N'Bình'), (3, N'Chi'), (4, N'Dũng'),
(5, N'Hà'), (6, N'Khang'), (7, N'Linh'), (8, N'Nam'),
(9, N'Phúc'), (10, N'Quân'), (11, N'Thảo'), (12, N'Vy');

DECLARE @CommitteeName TABLE (idx INT PRIMARY KEY, name NVARCHAR(100) NOT NULL);
INSERT INTO @CommitteeName (idx, name) VALUES
(1, N'Lê Thu Hà'),
(2, N'Phạm Quốc Minh'),
(3, N'Nguyễn Bảo Anh'),
(4, N'Trần Khánh Linh'),
(5, N'Đỗ Minh Khang'),
(6, N'Võ Hoàng Nam'),
(7, N'Mai Phương Thảo'),
(8, N'Bùi Thanh Sơn');

UPDATE dbo.users
SET full_name = N'Admin Primary',
    major = N'Hệ thống'
WHERE email = 'aems.admin01@uni.edu.vn';

UPDATE dbo.users
SET full_name = N'Admin Operations',
    major = N'Hệ thống'
WHERE email = 'aems.admin02@uni.edu.vn';

UPDATE dbo.users
SET full_name = N'Hồ Văn Thông Đạt'
WHERE email = 'hovanthongdat90@gmail.com';

UPDATE dbo.users
SET full_name = N'Đạt Hồ Văn Thông'
WHERE email = 'hovanthongdat96.2019tcv@gmail.com';

UPDATE dbo.users
SET major = N'Kỹ thuật Phần mềm'
WHERE email = 'Camtu.shops@gmail.com';

UPDATE u
SET u.full_name = N'Điều phối ' + d.name,
    u.major = d.name
FROM dbo.users u
CROSS APPLY (SELECT TRY_CONVERT(INT, SUBSTRING(u.email, 5, 2)) AS dept_idx) parsed
JOIN @DeptIndex d ON d.idx = parsed.dept_idx
WHERE u.email LIKE 'dept[0-9][0-9]@uni.edu.vn';

UPDATE u
SET u.full_name = c.name,
    u.major = N'Hội đồng duyệt'
FROM dbo.users u
CROSS APPLY (SELECT TRY_CONVERT(INT, SUBSTRING(u.email, 10, 2)) AS committee_idx) parsed
JOIN @CommitteeName c ON c.idx = parsed.committee_idx
WHERE u.email LIKE 'committee[0-9][0-9]@uni.edu.vn';

UPDATE u
SET u.full_name = l.name + N' ' + m.name + N' ' + f.name,
    u.major = d.name
FROM dbo.users u
CROSS APPLY (SELECT TRY_CONVERT(INT, SUBSTRING(u.email, 8, 3)) AS student_idx) parsed
JOIN @LastName l ON l.idx = ((parsed.student_idx - 1) % 12) + 1
JOIN @MiddleName m ON m.idx = (parsed.student_idx * 3 % 12) + 1
JOIN @FirstName f ON f.idx = (parsed.student_idx * 5 % 12) + 1
JOIN @DeptIndex d ON d.idx = ((parsed.student_idx - 1) % 12) + 1
WHERE u.email LIKE 'student[0-9][0-9][0-9]@uni.edu.vn'
  AND parsed.student_idx BETWEEN 1 AND 999;

UPDATE u
SET u.full_name = v.full_name
FROM dbo.users u
JOIN (VALUES
    ('student1@example.com', N'Nguyễn Văn A'),
    ('student2@example.com', N'Trần Thị B'),
    ('student3@example.com', N'Phạm Văn C'),
    ('student4@example.com', N'Hoàng Thị D'),
    ('student5@example.com', N'Lê Văn E'),
    ('student6@example.com', N'Đỗ Thị F'),
    ('student7@example.com', N'Vũ Văn G'),
    ('student8@example.com', N'Ngô Thị H')
) v(email, full_name) ON v.email = u.email;

UPDATE s
SET s.major = d.name
FROM dbo.student s
JOIN dbo.users u ON u.id = s.user_id
JOIN @DeptIndex d ON d.name = u.major
WHERE u.email LIKE 'student[0-9][0-9][0-9]@uni.edu.vn';

UPDATE s
SET s.major = v.major
FROM dbo.student s
JOIN (VALUES
    ('SV001', N'Công nghệ Thông tin'),
    ('SV002', N'Công nghệ Thông tin'),
    ('SV003', N'Công nghệ Thông tin'),
    ('SV004', N'Quản trị kinh doanh'),
    ('SV005', N'Công nghệ Thông tin'),
    ('SV006', N'Quản trị kinh doanh'),
    ('SV007', N'Công nghệ Thông tin'),
    ('SV008', N'Thiết kế Đồ họa')
) v(student_code, major) ON v.student_code = s.student_code;

DECLARE @EventTopic TABLE (idx INT PRIMARY KEY, title NVARCHAR(200) NOT NULL);
INSERT INTO @EventTopic (idx, title) VALUES
(1, N'Academic Tech Day'),
(2, N'Workshop Spring Boot MVC'),
(3, N'SQL Server Clinic'),
(4, N'Cloud Computing Lab'),
(5, N'AI Product Demo'),
(6, N'Cyber Security CTF'),
(7, N'UX Portfolio Review'),
(8, N'Business Case Challenge'),
(9, N'Marketing Analytics Talk'),
(10, N'English Presentation Day'),
(11, N'Tourism Career Fair'),
(12, N'Media Production Workshop');

DECLARE @EventLocation TABLE (idx INT PRIMARY KEY, location NVARCHAR(200) NOT NULL);
INSERT INTO @EventLocation (idx, location) VALUES
(1, N'Hội trường Alpha'),
(2, N'Phòng 101 - Tòa A'),
(3, N'Lab 3 - Tòa B'),
(4, N'Innovation Hub'),
(5, N'Auditorium Beta'),
(6, N'Phòng 205 - Tòa C'),
(7, N'Studio Media'),
(8, N'Sảnh sự kiện');

UPDATE dbo.event
SET title = CASE id
    WHEN 1 THEN N'Workshop: Web Development'
    WHEN 2 THEN N'Hội thảo: Career Path'
    WHEN 3 THEN N'Networking Event 2026'
    WHEN 4 THEN N'Hackathon 2026'
    WHEN 5 THEN N'Seminar: AI & Machine Learning'
    ELSE title
END,
description = CASE id
    WHEN 1 THEN N'Hướng dẫn phát triển web hiện đại với React và Node.js'
    WHEN 2 THEN N'Tìm hiểu lộ trình sự nghiệp trong ngành IT'
    WHEN 3 THEN N'Gặp gỡ và kết nối với các lập trình viên'
    WHEN 4 THEN N'Cuộc thi lập trình 24 tiếng'
    WHEN 5 THEN N'Giới thiệu AI và ML trong thực tế'
    ELSE description
END,
location = CASE id
    WHEN 1 THEN N'Phòng 101 - Tòa A'
    WHEN 2 THEN N'Hội trường lớn'
    WHEN 3 THEN N'Cafe Sinh viên'
    WHEN 4 THEN N'Lab 3 - Tòa B'
    WHEN 5 THEN N'Phòng 201 - Tòa A'
    ELSE location
END
WHERE id BETWEEN 1 AND 5;

UPDATE e
SET e.title = N'[AEMS] Event ' + RIGHT('00' + CONVERT(VARCHAR(10), parsed.event_idx), 2) + N' - ' + t.title,
    e.description = N'Sự kiện học thuật do ' + d.name + N' tổ chức, gồm chia sẻ chuyên môn, thảo luận và hoạt động thực hành.',
    e.location = l.location
FROM dbo.event e
CROSS APPLY (SELECT TRY_CONVERT(INT, SUBSTRING(e.title, 14, 2)) AS event_idx) parsed
JOIN @EventTopic t ON t.idx = ((parsed.event_idx - 1) % 12) + 1
JOIN @EventLocation l ON l.idx = ((parsed.event_idx - 1) % 8) + 1
JOIN dbo.department d ON d.id = e.department_id
WHERE e.title LIKE '[[]AEMS] Event __ - %'
  AND parsed.event_idx IS NOT NULL;

DECLARE @ProposalTopic TABLE (idx INT PRIMARY KEY, title NVARCHAR(200) NOT NULL);
INSERT INTO @ProposalTopic (idx, title) VALUES
(1, N'AI ứng dụng trong học thuật'),
(2, N'Career Talk doanh nghiệp'),
(3, N'Workshop Cloud Native'),
(4, N'Seminar UX Research'),
(5, N'Data Analytics Bootcamp'),
(6, N'Ngày hội học thuật'),
(7, N'Secure Coding Lab'),
(8, N'English Academic Forum'),
(9, N'Digital Marketing Day'),
(10, N'Startup Pitching'),
(11, N'Research Method Workshop'),
(12, N'Multimedia Production Camp');

UPDATE p
SET p.title = N'[AEMS] Proposal ' + RIGHT('00' + CONVERT(VARCHAR(10), parsed.proposal_idx), 2) + N' - ' + t.title,
    p.description = N'Đề xuất tổ chức ' + LOWER(t.title) + N' cho sinh viên ' + d.name + N'.',
    p.note = CASE UPPER(p.status)
        WHEN 'APPROVED' THEN N'Đã đủ thông tin, chuyển sang tạo event chính thức.'
        WHEN 'REVISION' THEN N'Cần bổ sung ngân sách, diễn giả và kế hoạch truyền thông.'
        WHEN 'REJECTED' THEN N'Không phù hợp lịch học kỳ hoặc trùng lịch hội trường.'
        ELSE N'Đang chờ hội đồng duyệt.'
    END
FROM dbo.event_proposal p
CROSS APPLY (SELECT TRY_CONVERT(INT, SUBSTRING(p.title, 17, 2)) AS proposal_idx) parsed
JOIN @ProposalTopic t ON t.idx = ((parsed.proposal_idx - 1) % 12) + 1
JOIN dbo.department d ON d.id = p.department_id
WHERE p.title LIKE '[[]AEMS] Proposal __ - %'
  AND parsed.proposal_idx IS NOT NULL;

UPDATE dbo.registration
SET note = CASE
    WHEN UPPER(status) = 'WAITLIST' THEN N'Chờ mở thêm slot'
    WHEN note IS NULL THEN NULL
    ELSE N'Không có ghi chú'
END;

DECLARE @FeedbackComment TABLE (idx INT PRIMARY KEY, comment NVARCHAR(MAX) NOT NULL);
INSERT INTO @FeedbackComment (idx, comment) VALUES
(1, N'Nội dung rõ ràng, có nhiều ví dụ thực tế.'),
(2, N'Diễn giả truyền đạt tốt, nên tăng thời lượng thực hành.'),
(3, N'Quy trình check-in nhanh, email ticket dễ dùng.'),
(4, N'Sự kiện hữu ích cho định hướng học tập và nghề nghiệp.'),
(5, N'Không gian tổ chức tốt, tài liệu cần gửi sớm hơn.');

;WITH feedback_order AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY id) AS row_num
    FROM dbo.feedback
)
UPDATE f
SET f.comment = c.comment
FROM dbo.feedback f
JOIN feedback_order o ON o.id = f.id
JOIN @FeedbackComment c ON c.idx = ((o.row_num - 1) % 5) + 1;

UPDATE el
SET el.subject = N'AEMS - Ticket và mã check-in',
    el.content = N'Mã ticket của bạn là ' + t.code
FROM dbo.email_log el
JOIN dbo.ticket t ON t.registration_id = el.registration_id
WHERE el.registration_id IS NOT NULL
  AND el.subject LIKE '%Ticket%';

UPDATE el
SET el.subject = N'AEMS - Xác nhận đăng ký ' + e.title,
    el.content = N'Hệ thống ghi nhận trạng thái đăng ký: ' + r.status
FROM dbo.email_log el
JOIN dbo.registration r ON r.id = el.registration_id
JOIN dbo.event e ON e.id = r.event_id
WHERE el.registration_id IS NOT NULL
  AND el.subject NOT LIKE '%Ticket%';

UPDATE dbo.email_log
SET subject = N'Xác nhận đăng ký Workshop',
    content = N'Bạn đã đăng ký thành công.'
WHERE id IN (1, 2);

;WITH admin_email_order AS (
    SELECT id, ROW_NUMBER() OVER (ORDER BY sent_at) AS week_num
    FROM dbo.email_log
    WHERE registration_id IS NULL
      AND event_id IS NULL
)
UPDATE el
SET el.subject = N'AEMS - Báo cáo vận hành tuần ' + CONVERT(NVARCHAR(10), o.week_num),
    el.content = N'Tổng hợp user, proposal, registration, attendance và feedback.'
FROM dbo.email_log el
JOIN admin_email_order o ON o.id = el.id;

UPDATE dbo.activity_log
SET description = CASE activity_type
    WHEN 'REGISTER_EVENT' THEN N'Đăng ký sự kiện'
    WHEN 'CHECK_IN' THEN N'Check-in sự kiện'
    WHEN 'FEEDBACK' THEN N'Gửi feedback sau sự kiện'
    WHEN 'GOOGLE_REGISTER' THEN N'Đăng ký tài khoản mới qua Google OAuth'
    WHEN 'GOOGLE_LOGIN' THEN N'Đăng nhập bằng Google'
    WHEN 'EMAIL_REGISTER' THEN N'Đăng ký tài khoản mới qua email'
    WHEN 'EMAIL_LOGIN' THEN N'Đăng nhập bằng email'
    WHEN 'ADMIN_REPORT' THEN N'Xuất báo cáo thống kê dashboard'
    WHEN 'ADMIN_USER' THEN N'Cập nhật tài khoản hoặc phân quyền'
    ELSE description
END;

COMMIT TRANSACTION;

SELECT
    (SELECT COUNT(*) FROM dbo.department) AS department_count,
    (SELECT COUNT(*) FROM dbo.department WHERE name LIKE '%?%' OR description LIKE '%?%') AS broken_department_count,
    (SELECT COUNT(*) FROM dbo.users WHERE full_name LIKE '%?%' OR major LIKE '%?%') AS broken_user_count,
    (SELECT COUNT(*) FROM dbo.event WHERE title LIKE '%?%' OR description LIKE '%?%' OR location LIKE '%?%') AS broken_event_count,
    (SELECT COUNT(*) FROM dbo.event_proposal WHERE title LIKE '%?%' OR description LIKE '%?%' OR note LIKE '%?%') AS broken_proposal_count;
GO
