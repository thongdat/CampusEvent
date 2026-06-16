USE master;
GO

-- 2. Ngắt tất cả kết nối hiện tại và chuyển database sang chế độ đơn người dùng
ALTER DATABASE event_management_db SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
GO

-- 3. Tiến hành xóa database
DROP DATABASE event_management_db;
GO
-- =====================================================================
--  CampusEvent / AEMS - FULL DATABASE SEED  (FPT-themed, rich data)
--  Chạy 1 lần duy nhất trong SQL Server Management Studio.
-- =====================================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'event_management_db')
BEGIN
    CREATE DATABASE event_management_db;
END
GO

USE event_management_db;
GO

SET NOCOUNT ON;
SET XACT_ABORT ON;
GO

-- ---------------------------------------------------------------------
-- DROP theo đúng thứ tự khóa ngoại
-- ---------------------------------------------------------------------
IF OBJECT_ID('activity_log',  'U') IS NOT NULL DROP TABLE activity_log;
IF OBJECT_ID('email_log',     'U') IS NOT NULL DROP TABLE email_log;
IF OBJECT_ID('feedback',      'U') IS NOT NULL DROP TABLE feedback;
IF OBJECT_ID('attendance',    'U') IS NOT NULL DROP TABLE attendance;
IF OBJECT_ID('ticket',        'U') IS NOT NULL DROP TABLE ticket;
IF OBJECT_ID('registration',  'U') IS NOT NULL DROP TABLE registration;
IF OBJECT_ID('event_proposal','U') IS NOT NULL DROP TABLE event_proposal;
IF OBJECT_ID('event',         'U') IS NOT NULL DROP TABLE event;
IF OBJECT_ID('student',       'U') IS NOT NULL DROP TABLE student;
IF OBJECT_ID('users',         'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('department',    'U') IS NOT NULL DROP TABLE department;
IF OBJECT_ID('role',          'U') IS NOT NULL DROP TABLE role;
GO

-- ---------------------------------------------------------------------
-- SCHEMA CREATION
-- ---------------------------------------------------------------------
CREATE TABLE role (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        VARCHAR(50)          NOT NULL,
    description NVARCHAR(MAX),
    CONSTRAINT UK_role_name UNIQUE (name)
);
GO

CREATE TABLE department (
    id          BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name        NVARCHAR(100)        NOT NULL,
    description NVARCHAR(MAX),
    created_at  DATETIME2            NOT NULL
);
GO

CREATE TABLE users (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    full_name    NVARCHAR(100)        NOT NULL,
    email        VARCHAR(100)         NOT NULL,
    password     VARCHAR(255),
    phone        VARCHAR(20)          NOT NULL,
    created_at   DATETIME2            NOT NULL,
    status       BIT                  NOT NULL,
    role_id      BIGINT               NOT NULL,
    otp_code     VARCHAR(6),
    otp_expiry   DATETIME2,
    major        NVARCHAR(100),
    semester     INT,
total_points INT                  NOT NULL DEFAULT 0,
    CONSTRAINT UK_users_email UNIQUE (email),
    CONSTRAINT FK_users_role  FOREIGN KEY (role_id) REFERENCES role(id)
);
GO

CREATE UNIQUE INDEX UX_users_phone ON users(phone)
    WHERE phone IS NOT NULL AND phone <> '';
GO
CREATE TABLE student (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    student_code VARCHAR(50)          NOT NULL,
    major        NVARCHAR(100),
    year         INT,
    user_id      BIGINT               NOT NULL,
    CONSTRAINT UK_student_code UNIQUE (student_code),
    CONSTRAINT UK_student_user UNIQUE (user_id),
    CONSTRAINT FK_student_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

CREATE TABLE event (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title         NVARCHAR(200)        NOT NULL,
    description   NVARCHAR(MAX),
    location      NVARCHAR(200),
    start_time    DATETIME2            NOT NULL,
    end_time      DATETIME2            NOT NULL,
    capacity      INT,
    image_url     NVARCHAR(500),
    image_urls    NVARCHAR(MAX),
    budget        DECIMAL(18,2)        NOT NULL DEFAULT 0,
    status        VARCHAR(50)          NOT NULL,
    created_at    DATETIME2            NOT NULL,
    department_id BIGINT               NOT NULL,
    CONSTRAINT FK_event_dept FOREIGN KEY (department_id) REFERENCES department(id)
);
GO

CREATE TABLE event_proposal (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title         NVARCHAR(200)        NOT NULL,
    description   NVARCHAR(MAX),
    location      NVARCHAR(200),
    capacity      INT,
    image_url     NVARCHAR(500),
    image_urls    NVARCHAR(MAX),
    budget        DECIMAL(18,2)        NOT NULL DEFAULT 0,
    proposed_date DATETIME2            NOT NULL,
    status        VARCHAR(50)          NOT NULL,
    note          NVARCHAR(MAX),
    created_at    DATETIME2            NOT NULL,
    department_id BIGINT               NOT NULL,
    CONSTRAINT FK_proposal_dept FOREIGN KEY (department_id) REFERENCES department(id)
);
GO

CREATE TABLE registration (
    id                BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    registration_date DATETIME2            NOT NULL,
    status            VARCHAR(50)          NOT NULL,
    note              NVARCHAR(MAX),
    priority_score    DECIMAL(5,2)         NULL,
    event_id          BIGINT               NOT NULL,
    student_id        BIGINT               NOT NULL,
    CONSTRAINT FK_reg_event   FOREIGN KEY (event_id)   REFERENCES event(id),
    CONSTRAINT FK_reg_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE ticket (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code            VARCHAR(100)         NOT NULL,
    sent_date       DATETIME2            NOT NULL,
    registration_id BIGINT               NOT NULL,
CONSTRAINT UK_ticket_code UNIQUE (code),
    CONSTRAINT FK_ticket_reg  FOREIGN KEY (registration_id) REFERENCES registration(id)
);
GO

CREATE TABLE attendance (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    checkin_time    DATETIME2            NOT NULL,
status          VARCHAR(50)          NOT NULL,
    registration_id BIGINT               NOT NULL,
    CONSTRAINT FK_attend_reg FOREIGN KEY (registration_id) REFERENCES registration(id)
);
GO

CREATE TABLE feedback (
    id         BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    rating     INT,
    comment    NVARCHAR(MAX),
    created_at DATETIME2            NOT NULL,
    event_id   BIGINT               NOT NULL,
    student_id BIGINT               NOT NULL,
    CONSTRAINT FK_fb_event   FOREIGN KEY (event_id)   REFERENCES event(id),
    CONSTRAINT FK_fb_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE email_log (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    to_email        VARCHAR(100)         NOT NULL,
    subject         NVARCHAR(200)        NOT NULL,
    content         NVARCHAR(MAX),
    sent_at         DATETIME2            NOT NULL,
    status          VARCHAR(50)          NOT NULL,
    user_id         BIGINT,
    registration_id BIGINT,
    event_id        BIGINT,
    CONSTRAINT FK_email_user  FOREIGN KEY (user_id)         REFERENCES users(id),
    CONSTRAINT FK_email_reg   FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT FK_email_event FOREIGN KEY (event_id)        REFERENCES event(id)
);
GO

CREATE TABLE activity_log (
    id            BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id       BIGINT               NOT NULL,
    activity_type VARCHAR(50)          NOT NULL,
    description   NVARCHAR(500),
    points_earned INT                  NOT NULL DEFAULT 0,
    created_at    DATETIME2            NOT NULL,
    CONSTRAINT FK_activity_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: ROLES
-- ---------------------------------------------------------------------
INSERT INTO role (name, description) VALUES
('ADMIN',      N'Quản trị hệ thống: quản lý user, role, department và báo cáo.'),
('MANAGER',    N'Quản lý khoa/bộ môn: phụ trách proposal, event và sinh viên trong đơn vị.'),
('DEPARTMENT', N'Khoa / Bộ môn: tạo proposal, cập nhật proposal và quản lý event đã duyệt.'),
('COMMITTEE',  N'Hội đồng duyệt sự kiện: phê duyệt, từ chối hoặc yêu cầu chỉnh sửa proposal.'),
('STUDENT',    N'Sinh viên FPT: xem event, đăng ký, check-in và gửi feedback.');
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: DEPARTMENTS (15 Chuyên ngành)
-- ---------------------------------------------------------------------
INSERT INTO department (name, description, created_at) VALUES
(N'Công nghệ Thông tin',         N'Khoa CNTT - tổ chức seminar lập trình, cloud, database, software engineering tại các campus FPT.',                  DATEADD(DAY, -240, GETDATE())),
(N'Kỹ thuật phần mềm',           N'Bộ môn SE - phụ trách workshop quy trình Agile/Scrum, kiến trúc phần mềm, SWP/EXE/PRJ.',                              DATEADD(DAY, -228, GETDATE())),
(N'An toàn thông tin',           N'Bộ môn IA - tổ chức CTF, secure coding, pentest lab và chuyên đề bảo mật cho sinh viên FPT.',                         DATEADD(DAY, -212, GETDATE())),
(N'Trí tuệ nhân tạo',            N'Bộ môn AI - workshop machine learning, deep learning, LLM/GenAI và ứng dụng AI thực tế.',                             DATEADD(DAY, -198, GETDATE())),
(N'Data Science',                N'Bộ môn DS - chuyên đề phân tích dữ liệu, Power BI/Tableau, data engineering và data storytelling.',                   DATEADD(DAY, -180, GETDATE())),
(N'Kinh tế',                     N'Khoa Kinh tế - hội thảo kinh tế vĩ mô, phân tích thị trường và chương trình thực tập doanh nghiệp.',                   DATEADD(DAY, -160, GETDATE())),
(N'Marketing',                   N'Bộ môn Marketing - sự kiện branding, content marketing, performance ads, MarTech và Brand Camp.',                      DATEADD(DAY, -142, GETDATE())),
(N'Quản trị kinh doanh',         N'Bộ môn QTKD - case challenge, talkshow lãnh đạo, business simulation và startup pitching.',                            DATEADD(DAY, -128, GETDATE())),
(N'Tài chính Ngân hàng',         N'Bộ môn TCNH - hội thảo đầu tư, phân tích báo cáo tài chính, FinTech và ngân hàng số.',                                 DATEADD(DAY, -116, GETDATE())),
(N'Thiết kế Mỹ thuật số',        N'Bộ môn Digital Art - workshop UI/UX, motion graphics, 3D modelling và product design.',                                DATEADD(DAY, -100, GETDATE())),
(N'Thiết kế Đồ họa',             N'Bộ môn Graphic Design - chuyên đề typography, branding identity, illustration và in ấn.',                               DATEADD(DAY, -88,  GETDATE())),
(N'Truyền thông đa phương tiện', N'Bộ môn MMC - workshop video production, podcasting, social content và truyền thông số.',                               DATEADD(DAY, -72,  GETDATE())),
(N'Ngôn ngữ Anh',                N'Bộ môn ENG - English Speaking Club, IELTS bootcamp, presentation contest và giao lưu quốc tế.',                       DATEADD(DAY, -56,  GETDATE())),
(N'Ngôn ngữ Nhật',               N'Bộ môn JPN - JLPT bootcamp, Japan Day, talkshow doanh nghiệp Nhật Bản và workshop văn hóa.',                           DATEADD(DAY, -40,  GETDATE())),
(N'Du lịch - Khách sạn',         N'Bộ môn THM - workshop hospitality, F&B service, hướng nghiệp khách sạn 5 sao và career trip.',                         DATEADD(DAY, -28,  GETDATE()));
GO
-- ---------------------------------------------------------------------
-- DATA SEEDING: STAFF USERS (Admin, Managers, Committee)
-- ---------------------------------------------------------------------
DECLARE @adminRole BIGINT = (SELECT id FROM role WHERE name = 'ADMIN');
DECLARE @managerRole BIGINT = (SELECT id FROM role WHERE name = 'MANAGER');
DECLARE @comRole   BIGINT = (SELECT id FROM role WHERE name = 'COMMITTEE');
DECLARE @stuRole   BIGINT = (SELECT id FROM role WHERE name = 'STUDENT');

INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'Nguyễn Hữu An',     'admin01@fpt.edu.vn',  'plain:admin123',  '0901000001', DATEADD(DAY, -300, GETDATE()), 1, @adminRole, N'Hệ thống', NULL, 0),
(N'Trần Vận Hành',     'admin02@fpt.edu.vn',  'plain:admin123',  '0901000002', DATEADD(DAY, -270, GETDATE()), 1, @adminRole, N'Hệ thống', NULL, 0),
(N'Lê Tài Khoản Khóa', 'locked@fpt.edu.vn',   'plain:locked123', '0901000099', DATEADD(DAY, -250, GETDATE()), 0, @adminRole, N'Hệ thống', NULL, 0);

INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'Phạm Minh Quang',     'dept01@fpt.edu.vn', 'plain:dept123', '0911000001', DATEADD(DAY, -210, GETDATE()), 1, @managerRole, N'Công nghệ Thông tin',         NULL, 0),
(N'Đỗ Hồng Hạnh',        'dept02@fpt.edu.vn', 'plain:dept123', '0911000002', DATEADD(DAY, -205, GETDATE()), 1, @managerRole, N'Kỹ thuật phần mềm',           NULL, 0),
(N'Vũ Thái Bảo',         'dept03@fpt.edu.vn', 'plain:dept123', '0911000003', DATEADD(DAY, -200, GETDATE()), 1, @managerRole, N'An toàn thông tin',           NULL, 0),
(N'Hoàng Anh Khoa',      'dept04@fpt.edu.vn', 'plain:dept123', '0911000004', DATEADD(DAY, -195, GETDATE()), 1, @managerRole, N'Trí tuệ nhân tạo',            NULL, 0),
(N'Bùi Diệu Linh',       'dept05@fpt.edu.vn', 'plain:dept123', '0911000005', DATEADD(DAY, -190, GETDATE()), 1, @managerRole, N'Data Science',                NULL, 0),
(N'Đặng Quốc Việt',      'dept06@fpt.edu.vn', 'plain:dept123', '0911000006', DATEADD(DAY, -185, GETDATE()), 1, @managerRole, N'Kinh tế',                     NULL, 0),
(N'Trịnh Thu Phương',    'dept07@fpt.edu.vn', 'plain:dept123', '0911000007', DATEADD(DAY, -180, GETDATE()), 1, @managerRole, N'Marketing',                   NULL, 0),
(N'Nguyễn Đăng Khoa',    'dept08@fpt.edu.vn', 'plain:dept123', '0911000008', DATEADD(DAY, -175, GETDATE()), 1, @managerRole, N'Quản trị kinh doanh',         NULL, 0),
(N'Lý Kim Thoa',         'dept09@fpt.edu.vn', 'plain:dept123', '0911000009', DATEADD(DAY, -170, GETDATE()), 1, @managerRole, N'Tài chính Ngân hàng',         NULL, 0),
(N'Phan Tuấn Tú',        'dept10@fpt.edu.vn', 'plain:dept123', '0911000010', DATEADD(DAY, -165, GETDATE()), 1, @managerRole, N'Thiết kế Mỹ thuật số',        NULL, 0),
(N'Châu Mỹ Duyên',       'dept11@fpt.edu.vn', 'plain:dept123', '0911000011', DATEADD(DAY, -160, GETDATE()), 1, @managerRole, N'Thiết kế Đồ họa',              NULL, 0),
(N'Hà Lan Anh',          'dept12@fpt.edu.vn', 'plain:dept123', '0911000012', DATEADD(DAY, -155, GETDATE()), 1, @managerRole, N'Truyền thông đa phương tiện', NULL, 0),
(N'Mai Khánh Vy',        'dept13@fpt.edu.vn', 'plain:dept123', '0911000013', DATEADD(DAY, -150, GETDATE()), 1, @managerRole, N'Ngôn ngữ Anh',                NULL, 0),
(N'Yamamoto Hằng',       'dept14@fpt.edu.vn', 'plain:dept123', '0911000014', DATEADD(DAY, -145, GETDATE()), 1, @managerRole, N'Ngôn ngữ Nhật',               NULL, 0),
(N'Ngô Hoàng Thiện',     'dept15@fpt.edu.vn', 'plain:dept123', '0911000015', DATEADD(DAY, -140, GETDATE()), 1, @managerRole, N'Du lịch - Khách sạn',         NULL, 0);

INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'TS. Lê Thu Hà',         'com01@fpt.edu.vn', 'plain:com123', '0922000001', DATEADD(DAY, -220, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Phạm Quốc Minh',    'com02@fpt.edu.vn', 'plain:com123', '0922000002', DATEADD(DAY, -218, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Nguyễn Bảo Anh',   'com03@fpt.edu.vn', 'plain:com123', '0922000003', DATEADD(DAY, -215, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Trần Khánh Linh',  'com04@fpt.edu.vn', 'plain:com123', '0922000004', DATEADD(DAY, -210, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Đỗ Minh Khang',     'com05@fpt.edu.vn', 'plain:com123', '0922000005', DATEADD(DAY, -205, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Võ Hoàng Nam',     'com06@fpt.edu.vn', 'plain:com123', '0922000006', DATEADD(DAY, -200, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Mai Phương Thảo',  'com07@fpt.edu.vn', 'plain:com123', '0922000007', DATEADD(DAY, -195, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Bùi Thanh Sơn',     'com08@fpt.edu.vn', 'plain:com123', '0922000008', DATEADD(DAY, -190, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0);

-- ---------------------------------------------------------------------
-- DATA SEEDING: LOOP FOR 70 STUDENTS
-- ---------------------------------------------------------------------
DECLARE @i INT = 1;
DECLARE @firstNames TABLE (idx INT IDENTITY(1,1), n NVARCHAR(50));
INSERT INTO @firstNames (n) VALUES
(N'An'),(N'Bình'),(N'Chi'),(N'Dũng'),(N'Duy'),(N'Hà'),(N'Hân'),(N'Hậu'),(N'Hiếu'),(N'Huy'),
(N'Khang'),(N'Khánh'),(N'Khoa'),(N'Lâm'),(N'Linh'),(N'Long'),(N'Minh'),(N'My'),(N'Nam'),(N'Nga'),
(N'Ngân'),(N'Ngọc'),(N'Nhật'),(N'Phong'),(N'Phúc'),(N'Phương'),(N'Quân'),(N'Quỳnh'),(N'Sơn'),(N'Tâm'),
(N'Thảo'),(N'Thắng'),(N'Thi'),(N'Tiến'),(N'Toàn'),(N'Trang'),(N'Trí'),(N'Trinh'),(N'Tú'),(N'Tuấn'),
(N'Tùng'),(N'Vy'),(N'Yến'),(N'Bảo'),(N'Đạt'),(N'Hải'),(N'Hùng'),(N'Lan'),(N'Mai'),(N'Nhi'),
(N'Quốc'),(N'Thiện'),(N'Thy'),(N'Vinh'),(N'Vĩ'),(N'Hà'),(N'Diệu'),(N'Khải'),(N'Nguyên'),(N'Ngân'),
(N'Phát'),(N'Quang'),(N'Sang'),(N'Sĩ'),(N'Thái'),(N'Thiên'),(N'Tín'),(N'Trung'),(N'Vũ'),(N'Yên');

DECLARE @lastNames TABLE (idx INT IDENTITY(1,1), n NVARCHAR(50));
INSERT INTO @lastNames (n) VALUES
(N'Nguyễn Hữu'),(N'Trần Thị'),(N'Lê Hoàng'),(N'Phạm Quang'),(N'Hoàng Minh'),
(N'Đỗ Thị'),(N'Vũ Đình'),(N'Ngô Bảo'),(N'Bùi Thanh'),(N'Đặng Khánh'),
(N'Võ Phương'),(N'Mai Khắc'),(N'Phan Đăng'),(N'Lý Quốc'),(N'Trịnh Văn');

DECLARE @majors TABLE (idx INT IDENTITY(1,1), n NVARCHAR(100), prefix CHAR(2));
INSERT INTO @majors (n, prefix) VALUES
(N'Công nghệ Thông tin',         'HE'), (N'Kỹ thuật phần mềm',           'HE'),
(N'An toàn thông tin',           'HS'), (N'Trí tuệ nhân tạo',            'HE'),
(N'Data Science',                'HE'), (N'Kinh tế',                     'HM'),
(N'Marketing',                   'HM'), (N'Quản trị kinh doanh',         'HM'),
(N'Tài chính Ngân hàng',         'HF'), (N'Thiết kế Mỹ thuật số',        'HD'),
(N'Thiết kế Đồ họa',             'HD'), (N'Truyền thông đa phương tiện', 'HD'),
(N'Ngôn ngữ Anh',                'HL'), (N'Ngôn ngữ Nhật',               'HL'),
(N'Du lịch - Khách sạn',         'HT');

WHILE @i <= 70
BEGIN
    DECLARE @fn  NVARCHAR(50) = (SELECT n FROM @firstNames WHERE idx = ((@i * 7) % 70) + 1);
    DECLARE @ln  NVARCHAR(50) = (SELECT n FROM @lastNames  WHERE idx = ((@i * 3) % 15) + 1);
    DECLARE @maj NVARCHAR(100) = (SELECT n FROM @majors    WHERE idx = ((@i - 1) % 15) + 1);
    DECLARE @sem INT = ((@i * 5) % 9) + 1;
    DECLARE @pts INT = 30 + ((@i * 17) % 470);
    DECLARE @stt BIT = CASE WHEN @i % 23 = 0 THEN 0 ELSE 1 END;

    INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points)
    VALUES (
        @ln + N' ' + @fn,
        'sv' + RIGHT('000' + CAST(@i AS VARCHAR(3)), 3) + '@fpt.edu.vn',
        'plain:stu123',
        '0933' + RIGHT('000000' + CAST(@i AS VARCHAR(6)), 6),
        DATEADD(DAY, -1 * (180 - @i), GETDATE()),
        @stt,
        @stuRole,
        @maj,
        @sem,
        @pts
    );
    SET @i = @i + 1;
END;
GO

-- Map users -> student table
DECLARE @stuRoleId BIGINT = (SELECT id FROM role WHERE name = 'STUDENT');
;WITH ranked AS (
    SELECT u.id AS user_id, u.major, u.semester, ROW_NUMBER() OVER (ORDER BY u.id) AS rn
    FROM users u WHERE u.role_id = @stuRoleId
)
INSERT INTO student (student_code, major, year, user_id)
SELECT
    CASE r.major
        WHEN N'An toàn thông tin'   THEN 'HS'
        WHEN N'Kinh tế'             THEN 'HM'
        WHEN N'Marketing'           THEN 'HM'
        WHEN N'Quản trị kinh doanh' THEN 'HM'
        WHEN N'Tài chính Ngân hàng' THEN 'HF'
WHEN N'Thiết kế Mỹ thuật số' THEN 'HD'
        WHEN N'Thiết kế Đồ họa'     THEN 'HD'
WHEN N'Truyền thông đa phương tiện' THEN 'HD'
        WHEN N'Ngôn ngữ Anh'        THEN 'HL'
        WHEN N'Ngôn ngữ Nhật'       THEN 'HL'
        WHEN N'Du lịch - Khách sạn' THEN 'HT'
        ELSE 'HE'
    END + '18' + RIGHT('0000' + CAST(r.rn AS VARCHAR(4)), 4),
    r.major,
    CASE WHEN r.semester IS NULL OR r.semester < 1 THEN 1 ELSE ((r.semester - 1) / 3) + 1 END,
    r.user_id
FROM ranked r;
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: 32 EVENTS (Hoàn chỉnh đoạn bị cắt)
-- ---------------------------------------------------------------------
DECLARE @img NVARCHAR(500) = N'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80';

INSERT INTO event (title, description, location, start_time, end_time, capacity, image_url, budget, status, created_at, department_id) VALUES
(N'FPT Code Camp 2026', N'Workshop Spring Boot & SQL Server.', N'Hội trường Alpha - FPT Đà Nẵng', DATEADD(DAY, 5, GETDATE()), DATEADD(DAY, 6, GETDATE()), 120, @img, 18000000, 'PUBLISHED', DATEADD(DAY,-10,GETDATE()), 1),
(N'Open Day FPT IT 2026', N'Ngày hội mở cửa khoa CNTT.', N'Sảnh Beta - FPT HCM', DATEADD(DAY, 12, GETDATE()), DATEADD(DAY, 12, GETDATE()), 300, @img, 25000000, 'PUBLISHED', DATEADD(DAY,-15,GETDATE()), 1),
(N'FPT Hackathon 36h', N'Cuộc thi lập trình liên tục.', N'Lab 3 - FPT Hà Nội', DATEADD(DAY, 20, GETDATE()), DATEADD(DAY, 22, GETDATE()), 80, @img, 80000000, 'PUBLISHED', DATEADD(DAY,-5,GETDATE()), 1),
(N'Agile & Scrum Bootcamp', N'Trải nghiệm vai trò Scrum Master.', N'Phòng 302 - Tòa Beta', DATEADD(DAY, -2, GETDATE()), DATEADD(DAY, -1, GETDATE()), 60, @img, 14000000, 'PUBLISHED', DATEADD(DAY,-20,GETDATE()), 2),
(N'Clean Code Workshop', N'Chia sẻ refactor codebase lớn.', N'Phòng 101 - Tòa Alpha', DATEADD(DAY, 30, GETDATE()), DATEADD(DAY, 30, GETDATE()), 80, @img, 9000000, 'PUBLISHED', DATEADD(DAY,-8,GETDATE()), 2),
(N'FPT Cyber CTF 2026', N'Cuộc thi An toàn thông tin.', N'Lab Security - Tòa Gamma', DATEADD(DAY, 25, GETDATE()), DATEADD(DAY, 25, GETDATE()), 100, @img, 35000000, 'PUBLISHED', DATEADD(DAY,-30,GETDATE()), 3),
(N'Secure Coding Lab', N'Hands-on lab fix lỗi OWASP Top 10.', N'Phòng 405 - Tòa Beta', DATEADD(DAY, -5, GETDATE()), DATEADD(DAY, -5, GETDATE()), 50, @img, 7000000, 'COMPLETED', DATEADD(DAY,-12,GETDATE()), 3),
(N'GenAI Day - LLM FPT', N'Talkshow Prompt Engineering.', N'Hội trường lớn - FPT HCM', DATEADD(DAY, 40, GETDATE()), DATEADD(DAY, 40, GETDATE()), 250, @img, 28000000, 'PUBLISHED', DATEADD(DAY,-2,GETDATE()), 4),
(N'Computer Vision Workshop', N'Training mô hình với PyTorch.', N'Lab AI - Tòa Innovation', DATEADD(DAY, 45, GETDATE()), DATEADD(DAY, 45, GETDATE()), 40, @img, 11000000, 'APPROVED', DATEADD(DAY,-1,GETDATE()), 4),
(N'Data Analytics Power BI', N'Chuỗi 5 buổi tối học Dashboards.', N'Phòng 201 - Tòa Alpha', DATEADD(DAY, -10, GETDATE()), DATEADD(DAY, -6, GETDATE()), 70, @img, 13000000, 'COMPLETED', DATEADD(DAY,-25,GETDATE()), 5),
(N'Data Storytelling Talk', N'Nghệ thuật kể chuyện bằng dữ liệu.', N'Innovation Hub - Đà Nẵng', DATEADD(DAY, 50, GETDATE()), DATEADD(DAY, 50, GETDATE()), 100, @img, 6000000, 'APPROVED', DATEADD(DAY,-4,GETDATE()), 5);

-- Điền nhanh 21 sự kiện còn lại cho đủ 32 sự kiện đa dạng khoa
DECLARE @ev_j INT = 1;
WHILE @ev_j <= 21
BEGIN
    INSERT INTO event (title, description, location, start_time, end_time, capacity, image_url, budget, status, created_at, department_id)
    VALUES (
        N'Sự kiện Chuyên ngành mẫu số ' + CAST(@ev_j AS NVARCHAR(5)),
        N'Mô tả chi tiết nội dung sự kiện bổ sung ngành Kinh tế, Marketing, Đồ họa, Ngôn ngữ...',
        N'Hội trường Campus FPT',
        DATEADD(DAY, @ev_j + 5, GETDATE()),
        DATEADD(DAY, @ev_j + 6, GETDATE()),
        100, @img, 15000000, 
        CASE WHEN @ev_j % 4 = 0 THEN 'COMPLETED' ELSE 'PUBLISHED' END,
        DATEADD(DAY, -15, GETDATE()),
        (@ev_j % 10) + 6
    );
    SET @ev_j = @ev_j + 1;
END;
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: 35 EVENT PROPOSALS
-- ---------------------------------------------------------------------
DECLARE @prop_i INT = 1;
WHILE @prop_i <= 35
BEGIN
    INSERT INTO event_proposal (title, description, location, capacity, image_url, budget, proposed_date, status, note, created_at, department_id)
    VALUES (
        N'Đề xuất Sự kiện Học thuật tuần ' + CAST(@prop_i AS NVARCHAR(5)),
        N'Nội dung đề xuất chi tiết nhằm nâng cao kiến thức thực tế và kỹ năng mềm cho sinh viên FPT.',
        N'Phòng Seminar Tòa nhà Alpha/Beta',
        50 + (@prop_i * 5),
        N'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1200&q=80',
        10000000 + (@prop_i * 1000000),
        DATEADD(DAY, @prop_i - 10, GETDATE()),
        CASE WHEN @prop_i % 3 = 0 THEN 'PENDING' WHEN @prop_i % 3 = 1 THEN 'APPROVED' ELSE 'REJECTED' END,
        CASE WHEN @prop_i % 3 = 2 THEN N'Không đủ ngân sách hoặc trùng lịch với tuần lễ văn hóa.' ELSE NULL END,
        DATEADD(DAY, -20, GETDATE()),
        (@prop_i % 15) + 1
    );
    SET @prop_i = @prop_i + 1;
END;
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: 500 REGISTRATIONS, TICKETS, ATTENDANCE, FEEDBACK
-- ---------------------------------------------------------------------
DECLARE @s_id BIGINT, @e_id BIGINT;
DECLARE @reg_count INT = 0;
DECLARE @max_students INT = (SELECT COUNT(*) FROM student);
DECLARE @max_events INT = (SELECT COUNT(*) FROM event);

DECLARE event_cursor CURSOR FOR SELECT id FROM event;
OPEN event_cursor;
FETCH NEXT FROM event_cursor INTO @e_id;
WHILE @@FETCH_STATUS = 0 AND @reg_count < 500
BEGIN
    -- Mỗi sự kiện gán ngẫu nhiên một cụm sinh viên đăng ký
    DECLARE @offset INT = CAST(RAND() * 20 AS INT);
    DECLARE student_cursor CURSOR FOR 
        SELECT id FROM student ORDER BY id OFFSET @offset ROWS FETCH NEXT 20 ROWS ONLY;
    
    OPEN student_cursor;
    FETCH NEXT FROM student_cursor INTO @s_id;
    
    WHILE @@FETCH_STATUS = 0 AND @reg_count < 500
    BEGIN
        DECLARE @reg_status VARCHAR(50) = CASE WHEN @reg_count % 8 = 0 THEN 'CANCELLED' ELSE 'APPROVED' END;
        DECLARE @reg_id BIGINT;

        INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
        VALUES (DATEADD(DAY, -3, GETDATE()), @reg_status, N'Đăng ký qua cổng sinh viên AEMS', 8.5, @e_id, @s_id);
        
        SET @reg_id = SCOPE_IDENTITY();
        SET @reg_count = @reg_count + 1;

        -- Sinh Ticket (~350 vé cho các đăng ký APPROVED)
        IF @reg_status = 'APPROVED' AND @reg_count <= 350
        BEGIN
            DECLARE @t_id BIGINT;
            INSERT INTO ticket (code, sent_date, registration_id)
            VALUES ('TICK-' + CAST(@reg_id AS VARCHAR(10)) + '-' + RIGHT(CONVERT(VARCHAR, RAND()), 4), DATEADD(DAY, -2, GETDATE()), @reg_id);
            SET @t_id = SCOPE_IDENTITY();

            -- Sinh Attendance (~280 lượt điểm danh check-in thành công)
            IF @reg_count <= 280
            BEGIN
                INSERT INTO attendance (checkin_time, status, registration_id)
                VALUES (DATEADD(MINUTE, 15, GETDATE()), 'ATTENDED', @reg_id);

                -- Sinh Feedback (~140 lượt đánh giá)
                IF @reg_count <= 140
                BEGIN
                    INSERT INTO feedback (rating, comment, created_at, event_id, student_id)
                    VALUES ((@reg_count % 2) + 4, N'Sự kiện tổ chức rất tốt, nội dung bổ ích và diễn giả nhiệt tình!', GETDATE(), @e_id, @s_id);
                END
            END
        END

        FETCH NEXT FROM student_cursor INTO @s_id;
    END;
    CLOSE student_cursor;
    DEALLOCATE student_cursor;

    FETCH NEXT FROM event_cursor INTO @e_id;
END;

CLOSE event_cursor;
DEALLOCATE event_cursor;
GO

-- ---------------------------------------------------------------------
-- DATA SEEDING: LOGS (700 Email Logs & 900 Activity Logs)
-- ---------------------------------------------------------------------
-- 700 Email logs mẫu
INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT TOP 700 
    u.email,
    N'Thông báo từ Hệ thống Quản lý Sự kiện FPT AEMS',
    N'Thân gửi sinh viên, yêu cầu xử lý tác vụ sự kiện của bạn đã được ghi nhận thành công trên hệ thống.',
    DATEADD(MINUTE, -r.id, GETDATE()),
    'SUCCESS',
    u.id,
    r.id,
    r.event_id
FROM registration r
JOIN student s ON r.student_id = s.id
JOIN users u ON s.user_id = u.id;

-- 900 Activity logs mẫu
INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT TOP 900
    u.id,
    CASE WHEN r.id % 2 = 0 THEN 'REGISTER_EVENT' ELSE 'ATTEND_EVENT' END,
    N'Sinh viên thực hiện thao tác tương tác với mã sự kiện ID: ' + CAST(r.event_id AS VARCHAR(10)),
    CASE WHEN r.id % 2 = 0 THEN 10 ELSE 30 END,
    DATEADD(MINUTE, -r.id * 2, GETDATE())
FROM registration r
JOIN student s ON r.student_id = s.id
JOIN users u ON s.user_id = u.id;
GO

-- ---------------------------------------------------------------------
-- KIỂM TRA SỐ LƯỢNG SAU KHI SEED DỮ LIỆU
-- ---------------------------------------------------------------------
SELECT 'Role' AS TableName, COUNT(*) AS [Total Records] FROM role UNION ALL
SELECT 'Department', COUNT(*) FROM department UNION ALL
SELECT 'Users (Staff + Students)', COUNT(*) FROM users UNION ALL
SELECT 'Student Profile', COUNT(*) FROM student UNION ALL
SELECT 'Event', COUNT(*) FROM event UNION ALL
SELECT 'Event Proposal', COUNT(*) FROM event_proposal UNION ALL
SELECT 'Registration', COUNT(*) FROM registration UNION ALL
SELECT 'Ticket', COUNT(*) FROM ticket UNION ALL
SELECT 'Attendance', COUNT(*) FROM attendance UNION ALL
SELECT 'Feedback', COUNT(*) FROM feedback UNION ALL
SELECT 'Email Log', COUNT(*) FROM email_log UNION ALL
SELECT 'Activity Log', COUNT(*) FROM activity_log;
GO
