-- =====================================================================
--  CampusEvent / AEMS - FULL DATABASE SEED  (FPT-themed, rich data)
--  Chạy 1 lần duy nhất trong SQL Server Management Studio.
--  Script này:
--    1. Tạo database event_management_db (nếu chưa có)
--    2. Drop + tạo lại toàn bộ bảng theo đúng schema JPA
--    3. Insert 5 role, 15 chuyên ngành (đúng theo AcademicStructure)
--    4. Insert ~95 user (3 admin, 15 điều phối khoa, 8 hội đồng, 70 sinh viên)
--    5. Insert ~70 student với mã FPT (HE/HS/HM/HF/HD ...)
--    6. Insert 32 event - mỗi event đều có ảnh FPT/campus chất lượng cao
--    7. Insert 35 event_proposal đủ các trạng thái
--    8. Sinh ~500 registration / 350 ticket / 280 attendance / 140 feedback
--    9. Sinh ~700 email_log và ~900 activity_log
--
--  Mọi mật khẩu seed đều là chuỗi placeholder. Sau khi import, gọi:
--    GET http://localhost:8081/api/auth/init-passwords
--  để hash lại bằng BCrypt. Khi đó các tài khoản đăng nhập được với mật khẩu:
--    admin01@fpt.edu.vn / admin123
--    dept01@fpt.edu.vn  / dept123 (MANAGER)
--    com01@fpt.edu.vn   / com123
--    sv001@fpt.edu.vn   / stu123
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
-- SCHEMA
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
-- ROLES
-- ---------------------------------------------------------------------
INSERT INTO role (name, description) VALUES
('ADMIN',      N'Quản trị hệ thống: quản lý user, role, department và báo cáo.'),
('MANAGER',    N'Quản lý khoa/bộ môn: phụ trách proposal, event và sinh viên trong đơn vị.'),
('DEPARTMENT', N'Khoa / Bộ môn: tạo proposal, cập nhật proposal và quản lý event đã duyệt.'),
('COMMITTEE',  N'Hội đồng duyệt sự kiện: phê duyệt, từ chối hoặc yêu cầu chỉnh sửa proposal.'),
('STUDENT',    N'Sinh viên FPT: xem event, đăng ký, check-in và gửi feedback.');
GO

-- ---------------------------------------------------------------------
-- DEPARTMENTS  (15 chuyên ngành theo AcademicStructure.java)
-- ---------------------------------------------------------------------
INSERT INTO department (name, description, created_at) VALUES
(N'Công nghệ Thông tin',         N'Khoa CNTT - tổ chức seminar lập trình, cloud, database, software engineering tại các campus FPT.',                    DATEADD(DAY, -240, GETDATE())),
(N'Kỹ thuật phần mềm',           N'Bộ môn SE - phụ trách workshop quy trình Agile/Scrum, kiến trúc phần mềm, SWP/EXE/PRJ.',                              DATEADD(DAY, -228, GETDATE())),
(N'An toàn thông tin',           N'Bộ môn IA - tổ chức CTF, secure coding, pentest lab và chuyên đề bảo mật cho sinh viên FPT.',                         DATEADD(DAY, -212, GETDATE())),
(N'Trí tuệ nhân tạo',            N'Bộ môn AI - workshop machine learning, deep learning, LLM/GenAI và ứng dụng AI thực tế.',                             DATEADD(DAY, -198, GETDATE())),
(N'Data Science',                N'Bộ môn DS - chuyên đề phân tích dữ liệu, Power BI/Tableau, data engineering và data storytelling.',                   DATEADD(DAY, -180, GETDATE())),
(N'Kinh tế',                     N'Khoa Kinh tế - hội thảo kinh tế vĩ mô, phân tích thị trường và chương trình thực tập doanh nghiệp.',                   DATEADD(DAY, -160, GETDATE())),
(N'Marketing',                   N'Bộ môn Marketing - sự kiện branding, content marketing, performance ads, MarTech và Brand Camp.',                      DATEADD(DAY, -142, GETDATE())),
(N'Quản trị kinh doanh',         N'Bộ môn QTKD - case challenge, talkshow lãnh đạo, business simulation và startup pitching.',                            DATEADD(DAY, -128, GETDATE())),
(N'Tài chính Ngân hàng',         N'Bộ môn TCNH - hội thảo đầu tư, phân tích báo cáo tài chính, FinTech và ngân hàng số.',                                 DATEADD(DAY, -116, GETDATE())),
(N'Thiết kế Mỹ thuật số',        N'Bộ môn Digital Art - workshop UI/UX, motion graphics, 3D modelling và product design.',                                DATEADD(DAY, -100, GETDATE())),
(N'Thiết kế Đồ họa',             N'Bộ môn Graphic Design - chuyên đề typography, branding identity, illustration và in ấn.',                              DATEADD(DAY, -88,  GETDATE())),
(N'Truyền thông đa phương tiện', N'Bộ môn MMC - workshop video production, podcasting, social content và truyền thông số.',                              DATEADD(DAY, -72,  GETDATE())),
(N'Ngôn ngữ Anh',                N'Bộ môn ENG - English Speaking Club, IELTS bootcamp, presentation contest và giao lưu quốc tế.',                       DATEADD(DAY, -56,  GETDATE())),
(N'Ngôn ngữ Nhật',               N'Bộ môn JPN - JLPT bootcamp, Japan Day, talkshow doanh nghiệp Nhật Bản và workshop văn hóa.',                          DATEADD(DAY, -40,  GETDATE())),
(N'Du lịch - Khách sạn',         N'Bộ môn THM - workshop hospitality, F&B service, hướng nghiệp khách sạn 5 sao và career trip.',                        DATEADD(DAY, -28,  GETDATE()));
GO

-- ---------------------------------------------------------------------
-- USERS
-- ---------------------------------------------------------------------
DECLARE @adminRole BIGINT = (SELECT id FROM role WHERE name = 'ADMIN');
DECLARE @managerRole BIGINT = (SELECT id FROM role WHERE name = 'MANAGER');
DECLARE @deptRole  BIGINT = (SELECT id FROM role WHERE name = 'DEPARTMENT');
DECLARE @comRole   BIGINT = (SELECT id FROM role WHERE name = 'COMMITTEE');
DECLARE @stuRole   BIGINT = (SELECT id FROM role WHERE name = 'STUDENT');

-- 3 ADMIN
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'Nguyễn Hữu An',     'admin01@fpt.edu.vn',  'plain:admin123',  '0901000001', DATEADD(DAY, -300, GETDATE()), 1, @adminRole, N'Hệ thống', NULL, 0),
(N'Trần Vận Hành',     'admin02@fpt.edu.vn',  'plain:admin123',  '0901000002', DATEADD(DAY, -270, GETDATE()), 1, @adminRole, N'Hệ thống', NULL, 0),
(N'Lê Tài Khoản Khóa', 'locked@fpt.edu.vn',   'plain:locked123', '0901000099', DATEADD(DAY, -250, GETDATE()), 0, @adminRole, N'Hệ thống', NULL, 0);

-- 15 DEPARTMENT (1 điều phối / khoa) - email dept01..dept15
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
(N'Châu Mỹ Duyên',       'dept11@fpt.edu.vn', 'plain:dept123', '0911000011', DATEADD(DAY, -160, GETDATE()), 1, @managerRole, N'Thiết kế Đồ họa',             NULL, 0),
(N'Hà Lan Anh',          'dept12@fpt.edu.vn', 'plain:dept123', '0911000012', DATEADD(DAY, -155, GETDATE()), 1, @managerRole, N'Truyền thông đa phương tiện', NULL, 0),
(N'Mai Khánh Vy',        'dept13@fpt.edu.vn', 'plain:dept123', '0911000013', DATEADD(DAY, -150, GETDATE()), 1, @managerRole, N'Ngôn ngữ Anh',                NULL, 0),
(N'Yamamoto Hằng',       'dept14@fpt.edu.vn', 'plain:dept123', '0911000014', DATEADD(DAY, -145, GETDATE()), 1, @managerRole, N'Ngôn ngữ Nhật',               NULL, 0),
(N'Ngô Hoàng Thiện',     'dept15@fpt.edu.vn', 'plain:dept123', '0911000015', DATEADD(DAY, -140, GETDATE()), 1, @managerRole, N'Du lịch - Khách sạn',         NULL, 0);

-- 8 COMMITTEE
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'TS. Lê Thu Hà',         'com01@fpt.edu.vn', 'plain:com123', '0922000001', DATEADD(DAY, -220, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Phạm Quốc Minh',    'com02@fpt.edu.vn', 'plain:com123', '0922000002', DATEADD(DAY, -218, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Nguyễn Bảo Anh',   'com03@fpt.edu.vn', 'plain:com123', '0922000003', DATEADD(DAY, -215, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Trần Khánh Linh',  'com04@fpt.edu.vn', 'plain:com123', '0922000004', DATEADD(DAY, -210, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Đỗ Minh Khang',     'com05@fpt.edu.vn', 'plain:com123', '0922000005', DATEADD(DAY, -205, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Võ Hoàng Nam',     'com06@fpt.edu.vn', 'plain:com123', '0922000006', DATEADD(DAY, -200, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'ThS. Mai Phương Thảo',  'com07@fpt.edu.vn', 'plain:com123', '0922000007', DATEADD(DAY, -195, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0),
(N'TS. Bùi Thanh Sơn',     'com08@fpt.edu.vn', 'plain:com123', '0922000008', DATEADD(DAY, -190, GETDATE()), 1, @comRole, N'Hội đồng duyệt', NULL, 0);

-- 70 STUDENT - tên Việt + email sv001..sv070
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
(N'Công nghệ Thông tin',         'HE'),
(N'Kỹ thuật phần mềm',           'HE'),
(N'An toàn thông tin',           'HS'),
(N'Trí tuệ nhân tạo',            'HE'),
(N'Data Science',                'HE'),
(N'Kinh tế',                     'HM'),
(N'Marketing',                   'HM'),
(N'Quản trị kinh doanh',         'HM'),
(N'Tài chính Ngân hàng',         'HF'),
(N'Thiết kế Mỹ thuật số',        'HD'),
(N'Thiết kế Đồ họa',             'HD'),
(N'Truyền thông đa phương tiện', 'HD'),
(N'Ngôn ngữ Anh',                'HL'),
(N'Ngôn ngữ Nhật',               'HL'),
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

-- ---------------------------------------------------------------------
-- STUDENT  (link users -> mã sinh viên FPT chuẩn HE/HS/HM/HF/HD/HL/HT)
-- ---------------------------------------------------------------------
DECLARE @stuRoleId BIGINT = (SELECT id FROM role WHERE name = 'STUDENT');

;WITH ranked AS (
    SELECT u.id AS user_id,
           u.major,
           u.semester,
           ROW_NUMBER() OVER (ORDER BY u.id) AS rn
    FROM users u
    WHERE u.role_id = @stuRoleId
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
    CASE WHEN r.semester IS NULL OR r.semester < 1 THEN 1
         ELSE ((r.semester - 1) / 3) + 1
    END,
    r.user_id
FROM ranked r;
GO

-- ---------------------------------------------------------------------
-- EVENTS  (32 sự kiện - mỗi sự kiện kèm ảnh FPT/campus chất lượng cao)
-- ---------------------------------------------------------------------
DECLARE @img_codelab   NVARCHAR(500) = N'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_laptop    NVARCHAR(500) = N'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_hackathon NVARCHAR(500) = N'https://images.unsplash.com/photo-1591453089816-0fbb971b454c?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_cloud     NVARCHAR(500) = N'https://images.unsplash.com/photo-1451187580459-43490279c0fa?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_database  NVARCHAR(500) = N'https://images.unsplash.com/photo-1542903660-eedba2cda473?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_devops    NVARCHAR(500) = N'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_ai        NVARCHAR(500) = N'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_data      NVARCHAR(500) = N'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_matrix    NVARCHAR(500) = N'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_security  NVARCHAR(500) = N'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_padlock   NVARCHAR(500) = N'https://images.unsplash.com/photo-1563013544-824ae1b704d3?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_uxlab     NVARCHAR(500) = N'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_sketch    NVARCHAR(500) = N'https://images.unsplash.com/photo-1561070791-2526d30994b8?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_designer  NVARCHAR(500) = N'https://images.unsplash.com/photo-1542744095-fcf48d80b0fd?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_business  NVARCHAR(500) = N'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_charts    NVARCHAR(500) = N'https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_meeting   NVARCHAR(500) = N'https://images.unsplash.com/photo-1542744094-3a31f272c490?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_startup   NVARCHAR(500) = N'https://images.unsplash.com/photo-1543269664-7eef42226a21?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_marketing NVARCHAR(500) = N'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_branding  NVARCHAR(500) = N'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_finance   NVARCHAR(500) = N'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_library   NVARCHAR(500) = N'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_books     NVARCHAR(500) = N'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_speaking  NVARCHAR(500) = N'https://images.unsplash.com/photo-1503428593586-e225b39bddfe?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_japan     NVARCHAR(500) = N'https://images.unsplash.com/photo-1542051841857-5f90071e7989?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_lecture   NVARCHAR(500) = N'https://images.unsplash.com/photo-1562774053-701939374585?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_graduation NVARCHAR(500) = N'https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_campus    NVARCHAR(500) = N'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_studygroup NVARCHAR(500) = N'https://images.unsplash.com/photo-1571260899304-425eee4c7efc?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_career    NVARCHAR(500) = N'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_hotel     NVARCHAR(500) = N'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_travel    NVARCHAR(500) = N'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_studio    NVARCHAR(500) = N'https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=1200&q=80';
DECLARE @img_podcast   NVARCHAR(500) = N'https://images.unsplash.com/photo-1598618443855-232ee0f819f6?auto=format&fit=crop&w=1200&q=80';

INSERT INTO event (title, description, location, start_time, end_time, capacity, image_url, budget, status, created_at, department_id) VALUES
-- Công nghệ Thông tin (id=1)
(N'FPT Code Camp 2026 - Spring Boot & SQL Server', N'Workshop 1 ngày hướng dẫn xây dựng REST API với Spring Boot, kết nối SQL Server và deploy lên Docker. Diễn giả từ FPT Software.', N'Hội trường Alpha - FPT Đà Nẵng', '2026-06-08 08:00:00', '2026-06-08 17:00:00', 120, @img_codelab,   18000000, 'PUBLISHED', DATEADD(DAY,-45,GETDATE()), 1),
(N'Open Day FPT IT 2026',                          N'Ngày hội mở cửa khoa CNTT: tham quan lab, gặp gỡ doanh nghiệp đối tác và tư vấn lộ trình nghề nghiệp cho sinh viên năm 1-2.',         N'Sảnh sự kiện Beta - FPT HCM',    '2026-06-15 09:00:00', '2026-06-15 16:00:00', 300, @img_campus,    25000000, 'PUBLISHED', DATEADD(DAY,-40,GETDATE()), 1),
(N'AEMS - FPT Hackathon 36h',                      N'Cuộc thi lập trình 36 giờ liên tục, chủ đề Smart Campus. Giải nhất 30 triệu + suất thực tập FPT Software.',                            N'Lab 3 + Lab 4 - FPT Hà Nội',     '2026-07-04 08:00:00', '2026-07-05 20:00:00', 80,  @img_hackathon, 80000000, 'PUBLISHED', DATEADD(DAY,-30,GETDATE()), 1),
-- Kỹ thuật phần mềm (id=2)
(N'Agile & Scrum Bootcamp', N'2 ngày trải nghiệm vai trò Scrum Master, PO, Dev. Có chứng nhận PSM1 mock từ Scrum.org.', N'Phòng 302 - Tòa Beta', '2026-06-20 08:30:00', '2026-06-21 17:00:00', 60, @img_devops, 14000000, 'PUBLISHED', DATEADD(DAY,-50,GETDATE()), 2),
(N'Clean Code & Refactoring Workshop', N'Diễn giả từ FPT Software chia sẻ kinh nghiệm refactor codebase 100k LOC, kèm hands-on lab.', N'Phòng 101 - Tòa Alpha', '2026-06-28 13:30:00', '2026-06-28 17:30:00', 80, @img_laptop, 9000000, 'PUBLISHED', DATEADD(DAY,-44,GETDATE()), 2),
-- An toàn thông tin (id=3)
(N'FPT Cyber CTF Spring 2026', N'Cuộc thi Capture The Flag dạng Jeopardy: web, pwn, crypto, reverse. Mở cho sinh viên toàn FPT.', N'Lab Security - Tòa Gamma', '2026-06-12 08:00:00', '2026-06-12 20:00:00', 100, @img_security, 35000000, 'PUBLISHED', DATEADD(DAY,-55,GETDATE()), 3),
(N'Secure Coding Lab - OWASP Top 10 2025', N'Hands-on lab fix các lỗi SQLi, XSS, SSRF trong ứng dụng Spring Boot mẫu.', N'Phòng 405 - Tòa Beta', '2026-06-25 13:00:00', '2026-06-25 17:00:00', 50, @img_padlock, 7000000, 'APPROVED', DATEADD(DAY,-32,GETDATE()), 3),
-- Trí tuệ nhân tạo (id=4)
(N'GenAI Day - LLM cho sinh viên FPT', N'Talkshow ứng dụng LLM, prompt engineering, RAG. Diễn giả từ FPT.AI và OpenAI partner.', N'Hội trường lớn - FPT HCM', '2026-07-10 08:30:00', '2026-07-10 12:00:00', 250, @img_ai, 28000000, 'PUBLISHED', DATEADD(DAY,-25,GETDATE()), 4),
(N'Workshop Computer Vision với PyTorch', N'Hands-on training mô hình nhận diện ảnh, theo dõi đối tượng trên webcam.', N'Lab AI - Tòa Innovation', '2026-07-18 13:00:00', '2026-07-18 17:00:00', 40, @img_matrix, 11000000, 'PUBLISHED', DATEADD(DAY,-20,GETDATE()), 4),
-- Data Science (id=5)
(N'Data Analytics Bootcamp - Power BI', N'5 buổi tối từ căn bản đến dashboard hoàn chỉnh cho phân tích kinh doanh.', N'Phòng 201 - Tòa Alpha', '2026-06-30 18:00:00', '2026-07-04 21:00:00', 70, @img_data, 13000000, 'PUBLISHED', DATEADD(DAY,-38,GETDATE()), 5),
(N'Data Storytelling Talk', N'Cách kể chuyện bằng dữ liệu, từ insight đến slide thuyết trình ấn tượng.', N'Innovation Hub - FPT Đà Nẵng', '2026-07-22 14:00:00', '2026-07-22 17:00:00', 100, @img_charts, 6000000, 'APPROVED', DATEADD(DAY,-15,GETDATE()), 5),
-- Kinh tế (id=6)
(N'FPT Economic Forum 2026', N'Diễn đàn kinh tế thường niên: vĩ mô Việt Nam 2026, cơ hội cho sinh viên.', N'Hội trường Alpha - FPT HCM', '2026-08-02 08:30:00', '2026-08-02 12:00:00', 200, @img_meeting, 22000000, 'PUBLISHED', DATEADD(DAY,-10,GETDATE()), 6),
(N'Talkshow: Phân tích thị trường ngành công nghệ', N'Chuyên gia FPT Securities phân tích dòng tiền và cơ hội đầu tư công nghệ.', N'Phòng 501 - Tòa Beta', '2026-07-26 14:00:00', '2026-07-26 17:00:00', 90, @img_business, 8000000, 'PUBLISHED', DATEADD(DAY,-12,GETDATE()), 6),
-- Marketing (id=7)
(N'Brand Camp 2026 - Build Your Brand', N'4 ngày training thực chiến: persona, brand voice, social content, KPI.', N'Sảnh sự kiện Gamma', '2026-08-10 08:30:00', '2026-08-13 17:00:00', 120, @img_marketing, 32000000, 'PUBLISHED', DATEADD(DAY,-8,GETDATE()), 7),
(N'Workshop Performance Ads - Meta & Google', N'Tối ưu chiến dịch quảng cáo từ A-Z, có ngân sách thực tập 5tr/nhóm.', N'Lab Marketing - Tòa Alpha', '2026-07-30 13:00:00', '2026-07-30 17:00:00', 60, @img_branding, 15000000, 'APPROVED', DATEADD(DAY,-5,GETDATE()), 7),
-- Quản trị kinh doanh (id=8)
(N'FPT Business Case Challenge 2026', N'Vòng loại quốc gia: 3 vòng, đề thực từ FPT Retail và FPT Telecom.', N'Hội trường lớn - FPT Hà Nội', '2026-08-18 08:00:00', '2026-08-18 18:00:00', 150, @img_startup, 50000000, 'PUBLISHED', DATEADD(DAY,-7,GETDATE()), 8),
(N'Startup Pitching Night', N'Sinh viên pitch ý tưởng trước nhà đầu tư FPT Ventures.', N'Innovation Hub - FPT HCM', '2026-08-22 18:00:00', '2026-08-22 21:30:00', 80, @img_startup, 12000000, 'PUBLISHED', DATEADD(DAY,-4,GETDATE()), 8),
-- Tài chính Ngân hàng (id=9)
(N'FinTech Day - Mobile Banking & eKYC', N'Chuyên đề công nghệ tài chính: open banking, eKYC, blockchain settlement.', N'Phòng 202 - Tòa Beta', '2026-07-12 08:30:00', '2026-07-12 12:00:00', 100, @img_finance, 10000000, 'PUBLISHED', DATEADD(DAY,-22,GETDATE()), 9),
(N'Workshop: Đọc hiểu Báo cáo tài chính', N'Hướng dẫn phân tích BCTC doanh nghiệp niêm yết bằng Excel + Power BI.', N'Phòng 305 - Tòa Alpha', '2026-07-28 18:00:00', '2026-07-28 21:00:00', 70, @img_charts, 5000000, 'PUBLISHED', DATEADD(DAY,-9,GETDATE()), 9),
-- Thiết kế Mỹ thuật số (id=10)
(N'UX Research Lab Day', N'Trải nghiệm nghiên cứu UX với người dùng thật, phân tích heuristic và affinity mapping.', N'Design Studio - FPT Đà Nẵng', '2026-06-26 09:00:00', '2026-06-26 16:00:00', 50, @img_uxlab, 9000000, 'PUBLISHED', DATEADD(DAY,-28,GETDATE()), 10),
(N'Workshop Motion Graphics với After Effects', N'Tạo intro 15s và promo video cho thương hiệu cá nhân.', N'Lab Design - Tòa Innovation', '2026-07-15 13:30:00', '2026-07-15 17:30:00', 40, @img_designer, 8000000, 'APPROVED', DATEADD(DAY,-14,GETDATE()), 10),
-- Thiết kế Đồ họa (id=11)
(N'Portfolio Review Day - GD/UX 2026', N'1-1 review portfolio với art director từ studio đối tác FPT.', N'Studio Media - FPT HCM', '2026-07-20 09:00:00', '2026-07-20 17:00:00', 60, @img_sketch, 6000000, 'PUBLISHED', DATEADD(DAY,-18,GETDATE()), 11),
(N'Typography Workshop - Tiếng Việt trong design', N'Phong cách typography cho tiếng Việt, font dấu và xử lý kerning.', N'Phòng 401 - Tòa Beta', '2026-08-05 14:00:00', '2026-08-05 17:00:00', 50, @img_designer, 4500000, 'APPROVED', DATEADD(DAY,-6,GETDATE()), 11),
-- Truyền thông đa phương tiện (id=12)
(N'Podcast Production Bootcamp', N'2 buổi setup studio, thu - mix - phát hành podcast lên Spotify.', N'Studio Podcast - Tòa Gamma', '2026-07-08 18:00:00', '2026-07-09 21:00:00', 30, @img_podcast, 7500000, 'PUBLISHED', DATEADD(DAY,-26,GETDATE()), 12),
(N'Video Production Workshop với Premiere Pro', N'Quay - dựng video TVC 30s, từ ý tưởng đến xuất bản TikTok/YouTube.', N'Studio Media', '2026-07-24 09:00:00', '2026-07-24 17:00:00', 35, @img_studio, 9000000, 'APPROVED', DATEADD(DAY,-11,GETDATE()), 12),
-- Ngôn ngữ Anh (id=13)
(N'FPT English Speaking Contest 2026', N'Vòng chung kết cuộc thi nói tiếng Anh toàn FPT, chủ đề "AI & The Future".', N'Hội trường Alpha - FPT HCM', '2026-08-15 14:00:00', '2026-08-15 18:00:00', 200, @img_speaking, 18000000, 'PUBLISHED', DATEADD(DAY,-13,GETDATE()), 13),
(N'IELTS Bootcamp 5.5 -> 6.5', N'4 tuần học cấp tốc, mock test cuối khóa, mentor 1-1.', N'Phòng 203 - Tòa Beta', '2026-09-01 18:00:00', '2026-09-28 21:00:00', 40, @img_library, 16000000, 'APPROVED', DATEADD(DAY,-3,GETDATE()), 13),
-- Ngôn ngữ Nhật (id=14)
(N'Japan Day 2026 - Connect Vietnam & Japan', N'Lễ hội văn hóa Nhật: trà đạo, kimono, gian hàng doanh nghiệp Nhật Bản.', N'Sân khấu trung tâm - FPT Hà Nội', '2026-08-25 09:00:00', '2026-08-25 17:00:00', 400, @img_japan, 45000000, 'PUBLISHED', DATEADD(DAY,-2,GETDATE()), 14),
(N'JLPT N3 Mock Test Day', N'Thi thử JLPT N3 chuẩn format, có chấm điểm và tư vấn lộ trình.', N'Phòng 102 - Tòa Alpha', '2026-09-10 08:00:00', '2026-09-10 12:30:00', 80, @img_books, 6000000, 'APPROVED', DATEADD(DAY,-1,GETDATE()), 14),
-- Du lịch - Khách sạn (id=15)
(N'Career Trip - InterContinental Đà Nẵng', N'Tham quan, kiến tập 1 ngày tại khách sạn 5 sao, gặp gỡ HR.', N'InterContinental Sun Peninsula', '2026-08-30 07:00:00', '2026-08-30 17:00:00', 45, @img_hotel, 14000000, 'PUBLISHED', DATEADD(DAY,-16,GETDATE()), 15),
(N'Workshop F&B Service Excellence', N'Kỹ năng setup bàn fine dining, phục vụ rượu vang và xử lý phàn nàn.', N'Lab F&B - FPT Đà Nẵng', '2026-09-05 09:00:00', '2026-09-05 12:00:00', 35, @img_travel, 5500000, 'APPROVED', DATEADD(DAY,-21,GETDATE()), 15),
-- Sự kiện chéo khoa (cross-faculty)
(N'FPT Career Fair 2026 - Spring',                  N'Ngày hội việc làm lớn nhất FPT năm 2026 với 60+ doanh nghiệp, có khu vực phỏng vấn trực tiếp.', N'Sảnh trung tâm - FPT HCM', '2026-09-20 08:00:00', '2026-09-20 17:00:00', 800, @img_career,    120000000, 'PUBLISHED', DATEADD(DAY,-2,GETDATE()),  6),
(N'Lễ tốt nghiệp FPT khóa 2022-2026', N'Lễ tốt nghiệp trang trọng cho gần 2,000 sinh viên FPT tại 3 cơ sở.', N'Nhà thi đấu Đa năng - FPT', '2026-10-15 08:00:00', '2026-10-15 12:00:00', 2000, @img_graduation, 250000000, 'PENDING', DATEADD(DAY,-1,GETDATE()), 1);
GO

UPDATE event
SET image_urls = image_url
WHERE image_url IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- EVENT PROPOSALS  (35 - đủ trạng thái PENDING/APPROVED/REVISION_REQUIRED/REJECTED)
-- ---------------------------------------------------------------------
INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id) VALUES
(N'Đề xuất: Database Performance Clinic',          N'Buổi clinic tối ưu truy vấn SQL Server, đối tượng SE/CS năm 3-4.',                                         '2026-09-10 09:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-30,GETDATE()), 1),
(N'Đề xuất: Workshop Microservices với Spring Cloud', N'Tối ưu architecture cho hệ thống lớn, 60 chỗ.',                                                       '2026-09-18 13:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-26,GETDATE()), 1),
(N'Đề xuất: Code Review Best Practices',           N'Talkshow chia sẻ kinh nghiệm review code hiệu quả.',                                                       '2026-09-25 14:00:00', 'APPROVED',          N'Duyệt - cần lên kế hoạch truyền thông.',         DATEADD(DAY,-24,GETDATE()), 2),
(N'Đề xuất: Pair Programming Day',                  N'Sinh viên pair programming trong môi trường mô phỏng startup.',                                          '2026-10-02 09:00:00', 'REVISION_REQUIRED', N'Bổ sung ngân sách trà nước + kế hoạch chia nhóm.', DATEADD(DAY,-20,GETDATE()), 2),
(N'Đề xuất: CTF Mid-Year 2026',                     N'Cuộc thi CTF giữa năm, mở cho sinh viên IA năm 1-4.',                                                    '2026-10-12 08:00:00', 'APPROVED',          N'Duyệt - phối hợp với CLB Security.',             DATEADD(DAY,-18,GETDATE()), 3),
(N'Đề xuất: Pentest Workshop với Kali Linux',       N'Hướng dẫn pentest từ recon đến exploitation.',                                                            '2026-10-20 13:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-16,GETDATE()), 3),
(N'Đề xuất: LLM Fine-tuning Hands-on',              N'Hands-on fine-tune mô hình ngôn ngữ với LoRA.',                                                            '2026-10-25 09:00:00', 'APPROVED',          N'Duyệt - cần thuê GPU server.',                   DATEADD(DAY,-15,GETDATE()), 4),
(N'Đề xuất: Reinforcement Learning Bootcamp',       N'5 buổi tối, từ Q-learning đến PPO.',                                                                       '2026-11-02 18:00:00', 'REVISION_REQUIRED', N'Cần làm rõ tài liệu và slide từng buổi.',        DATEADD(DAY,-13,GETDATE()), 4),
(N'Đề xuất: Data Engineering Day',                  N'Chia sẻ pipeline ETL của FPT Telecom, có demo Airflow.',                                                   '2026-11-08 13:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-12,GETDATE()), 5),
(N'Đề xuất: Tableau Champion Contest',              N'Cuộc thi dashboard Tableau, 3 vòng.',                                                                       '2026-11-15 09:00:00', 'REJECTED',          N'Trùng lịch với FPT Hackathon, dời sang Q1/2027.', DATEADD(DAY,-10,GETDATE()), 5),
(N'Đề xuất: Talkshow ESG cho doanh nghiệp Việt',     N'Diễn giả FPT Corporation chia sẻ thực hành ESG.',                                                          '2026-11-20 14:00:00', 'APPROVED',          N'Duyệt - mời thêm doanh nghiệp đối tác.',         DATEADD(DAY,-9,GETDATE()),  6),
(N'Đề xuất: Workshop Excel cho Kinh tế',            N'Học Excel nâng cao: PowerQuery, Pivot, macro VBA.',                                                        '2026-11-28 18:00:00', 'APPROVED',          N'Duyệt - sắp xếp 2 lớp song song.',               DATEADD(DAY,-8,GETDATE()),  6),
(N'Đề xuất: TikTok Marketing Bootcamp',             N'Học làm content viral trên TikTok, có giải thưởng.',                                                       '2026-12-02 13:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-7,GETDATE()),  7),
(N'Đề xuất: Workshop Personal Branding cho sinh viên', N'Cách build personal brand trên LinkedIn cho fresher.',                                                  '2026-12-08 18:00:00', 'REVISION_REQUIRED', N'Bổ sung diễn giả ngoài FPT.',                    DATEADD(DAY,-6,GETDATE()),  7),
(N'Đề xuất: Business Simulation Day',               N'Mô phỏng vận hành startup trong 8 giờ.',                                                                    '2026-12-15 08:00:00', 'APPROVED',          N'Duyệt - chuẩn bị 10 case study.',                DATEADD(DAY,-5,GETDATE()),  8),
(N'Đề xuất: Talkshow CEO FPT Retail',                N'Giao lưu với CEO về hành trình kinh doanh.',                                                               '2026-12-22 14:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-4,GETDATE()),  8),
(N'Đề xuất: Investment Game 2026',                  N'Sinh viên giao dịch ảo trong 1 tuần, có giải thưởng.',                                                     '2027-01-10 09:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-3,GETDATE()),  9),
(N'Đề xuất: Crypto & DeFi Talk',                    N'Talkshow tổng quan DeFi, hợp tác với MoMo.',                                                               '2027-01-18 14:00:00', 'REJECTED',          N'Chủ đề cần phê duyệt từ ban học vụ.',            DATEADD(DAY,-2,GETDATE()),  9),
(N'Đề xuất: Figma Mastery Bootcamp',                 N'4 buổi học Figma từ cơ bản đến nâng cao.',                                                                 '2027-01-25 18:00:00', 'APPROVED',          N'Duyệt - sắp xếp phòng có 40 máy.',               DATEADD(DAY,-2,GETDATE()),  10),
(N'Đề xuất: 3D Modeling với Blender',               N'Workshop modeling 3D cho game/animation.',                                                                  '2027-02-02 13:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  10),
(N'Đề xuất: Adobe Illustrator Battle',              N'Cuộc thi vẽ illustrator trong 3 giờ.',                                                                      '2027-02-10 14:00:00', 'APPROVED',          N'Duyệt - chuẩn bị máy có Adobe license.',         DATEADD(DAY,-1,GETDATE()),  11),
(N'Đề xuất: Workshop In ấn cho Designer',           N'Hiểu quy trình in từ file đến thành phẩm.',                                                                '2027-02-18 09:00:00', 'REVISION_REQUIRED', N'Cần thêm chi phí thuê xưởng in.',                DATEADD(DAY,-1,GETDATE()),  11),
(N'Đề xuất: Content Creator Day',                    N'Networking + workshop cho content creator FPT.',                                                            '2027-02-25 14:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  12),
(N'Đề xuất: Workshop Live Streaming',                N'Hướng dẫn setup livestream cho event.',                                                                     '2027-03-05 13:00:00', 'APPROVED',          N'Duyệt - mượn thiết bị từ studio.',               DATEADD(DAY,-1,GETDATE()),  12),
(N'Đề xuất: FPT English Got Talent',                 N'Cuộc thi tài năng tiếng Anh: hát, kịch, hùng biện.',                                                       '2027-03-15 18:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  13),
(N'Đề xuất: TOEIC Bootcamp Cấp tốc',                 N'Cấp tốc 4 tuần luyện TOEIC từ 500 đến 700+.',                                                              '2027-03-22 18:00:00', 'APPROVED',          N'Duyệt - mở 3 lớp.',                              DATEADD(DAY,-1,GETDATE()),  13),
(N'Đề xuất: Japan Career Fair 2026',                 N'Hội chợ việc làm Nhật Bản với 20 công ty Nhật.',                                                            '2027-04-02 09:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  14),
(N'Đề xuất: Workshop văn hóa doanh nghiệp Nhật',     N'Buổi chia sẻ về 報連相 (Houren-sou).',                                                                  '2027-04-10 14:00:00', 'APPROVED',          N'Duyệt - mời thêm diễn giả từ Nhật.',             DATEADD(DAY,-1,GETDATE()),  14),
(N'Đề xuất: Workshop Wine Tasting',                  N'Học cảm vị rượu vang và pairing với món ăn.',                                                               '2027-04-18 18:00:00', 'REVISION_REQUIRED', N'Cần xét lại độ tuổi tham gia.',                  DATEADD(DAY,-1,GETDATE()),  15),
(N'Đề xuất: Career Trip - JW Marriott HN',           N'Tham quan và kiến tập tại khách sạn 5 sao.',                                                                '2027-04-26 08:00:00', 'APPROVED',          N'Duyệt - giới hạn 40 sinh viên.',                 DATEADD(DAY,-1,GETDATE()),  15),
(N'Đề xuất: Spring Festival FPT 2027',               N'Lễ hội xuân toàn FPT, có gian hàng và biểu diễn.',                                                          '2027-02-01 16:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  6),
(N'Đề xuất: Open Day FPT cho học sinh THPT',         N'Mời học sinh THPT đến tham quan FPT.',                                                                      '2027-03-30 08:00:00', 'APPROVED',          N'Duyệt - kết hợp với phòng tuyển sinh.',          DATEADD(DAY,-1,GETDATE()),  1),
(N'Đề xuất: Workshop Cloud Native với Kubernetes',   N'Hands-on triển khai microservices trên K8s.',                                                               '2027-05-08 09:00:00', 'PENDING',           N'Đang chờ hội đồng duyệt.',                       DATEADD(DAY,-1,GETDATE()),  1),
(N'Đề xuất: Workshop Privacy & GDPR',                N'Tổng quan về luật bảo vệ dữ liệu cá nhân.',                                                                 '2027-05-15 14:00:00', 'REJECTED',          N'Chủ đề trùng với khóa học bắt buộc.',            DATEADD(DAY,-1,GETDATE()),  3),
(N'Đề xuất: Workshop Personal Finance cho sinh viên', N'Quản lý tài chính cá nhân khi mới đi làm.',                                                              '2027-05-22 18:00:00', 'APPROVED',          N'Duyệt - mời chuyên gia từ MB Bank.',             DATEADD(DAY,-1,GETDATE()),  9);
GO

UPDATE event_proposal
SET
    location = COALESCE(location, N'FPT Campus'),
    capacity = COALESCE(capacity, 100),
    budget = COALESCE(budget, 5000000 + (id % 8) * 1500000),
    image_url = COALESCE(image_url, N'https://images.unsplash.com/photo-1540575467063-027a26d3b38c?auto=format&fit=crop&w=1200&q=80');

UPDATE event_proposal
SET image_urls = image_url
WHERE image_url IS NOT NULL;
GO

-- ---------------------------------------------------------------------
-- REGISTRATIONS  (~500 dòng, sinh tự động từ cross-product event x student)
-- ---------------------------------------------------------------------
DECLARE @stuRoleId2 BIGINT = (SELECT id FROM role WHERE name = 'STUDENT');

INSERT INTO registration (registration_date, status, note, event_id, student_id)
    SELECT
    DATEADD(HOUR, -1 * ((ABS(CHECKSUM(NEWID())) % 240) + 24), e.start_time),
    CASE
        WHEN ((CAST(e.id AS INT) * 7 + CAST(s.id AS INT)) % 23) = 0 THEN 'WAITLIST'
        WHEN ((CAST(e.id AS INT) * 5 + CAST(s.id AS INT)) % 41) = 0 THEN 'CANCELLED'
        ELSE 'REGISTERED'
    END,
    CASE
        WHEN ((CAST(e.id AS INT) * 7 + CAST(s.id AS INT)) % 23) = 0 THEN N'Chờ mở thêm slot'
        WHEN ((CAST(e.id AS INT) * 5 + CAST(s.id AS INT)) % 41) = 0 THEN N'Hủy do trùng lịch học'
        ELSE NULL
    END,
    e.id,
    s.id
FROM event e
CROSS JOIN student s
WHERE ((CAST(e.id AS INT) * 13 + CAST(s.id AS INT) * 7) % 4) = 0;
GO

-- ---------------------------------------------------------------------
-- BACKFILL priority_score cho seed (xấp xỉ theo M + S + P + T = 100%)
-- Bản SQL đơn giản: M dùng so khớp tên khoa/major, S dùng semester của user.
-- ---------------------------------------------------------------------
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
        ELSE 100
    END
    + 0.10 * 70.0
    AS DECIMAL(5,2))
FROM registration r
JOIN student   s ON s.id = r.student_id
JOIN users     u ON u.id = s.user_id
JOIN event     e ON e.id = r.event_id
JOIN department d ON d.id = e.department_id;
GO

-- ---------------------------------------------------------------------
-- TICKETS  (1 ticket / REGISTERED registration)
-- ---------------------------------------------------------------------
;WITH eligible AS (
    SELECT r.id, r.registration_date,
           ROW_NUMBER() OVER (ORDER BY r.id) AS rn
    FROM registration r
    WHERE r.status = 'REGISTERED'
)
INSERT INTO ticket (code, sent_date, registration_id)
SELECT
    'AEMS-TICKET-' + RIGHT('00000' + CAST(e.rn AS VARCHAR(5)), 5),
    DATEADD(HOUR, 1, e.registration_date),
    e.id
FROM eligible e;
GO

-- ---------------------------------------------------------------------
-- ATTENDANCE  (~70% REGISTERED check-in, có cả ABSENT)
-- ---------------------------------------------------------------------
INSERT INTO attendance (checkin_time, status, registration_id)
SELECT
    DATEADD(MINUTE, -10 + (ABS(CHECKSUM(NEWID())) % 20), e.start_time),
    CASE WHEN (r.id % 11) = 0 THEN 'ABSENT' ELSE 'ATTENDED' END,
    r.id
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE r.status = 'REGISTERED'
  AND (r.id % 10) < 7;
GO

-- ---------------------------------------------------------------------
-- FEEDBACK  (~50% attendance ATTENDED có feedback)
-- ---------------------------------------------------------------------
DECLARE @cmt1 NVARCHAR(MAX) = N'Nội dung rõ ràng, diễn giả nhiệt tình. Sẽ tiếp tục theo dõi các sự kiện sau.';
DECLARE @cmt2 NVARCHAR(MAX) = N'Phần demo rất hay nhưng thời gian Q&A hơi ngắn, hy vọng lần sau có thêm.';
DECLARE @cmt3 NVARCHAR(MAX) = N'Quy trình check-in nhanh gọn, email gửi ticket tự động rất tiện.';
DECLARE @cmt4 NVARCHAR(MAX) = N'Sự kiện hữu ích cho định hướng nghề nghiệp, cảm ơn FPT.';
DECLARE @cmt5 NVARCHAR(MAX) = N'Không gian thoải mái, hậu cần chu đáo. Tài liệu nên gửi trước qua email.';
DECLARE @cmt6 NVARCHAR(MAX) = N'Hy vọng có thêm các workshop hands-on dài 2-3 ngày kiểu này.';
DECLARE @cmt7 NVARCHAR(MAX) = N'Mentor 1-1 rất tận tâm, nội dung đúng nhu cầu sinh viên năm 3.';

INSERT INTO feedback (rating, comment, created_at, event_id, student_id)
SELECT
    3 + (a.id % 3),
    CASE (a.id % 7)
        WHEN 0 THEN @cmt1
        WHEN 1 THEN @cmt2
        WHEN 2 THEN @cmt3
        WHEN 3 THEN @cmt4
        WHEN 4 THEN @cmt5
        WHEN 5 THEN @cmt6
        ELSE @cmt7
    END,
    DATEADD(HOUR, 6, a.checkin_time),
    r.event_id,
    r.student_id
FROM attendance a
JOIN registration r ON r.id = a.registration_id
WHERE a.status = 'ATTENDED'
  AND (a.id % 2) = 0;
GO

-- ---------------------------------------------------------------------
-- EMAIL LOG  (xác nhận đăng ký + gửi ticket + báo cáo admin)
-- ---------------------------------------------------------------------
INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT
    u.email,
    N'AEMS - Xác nhận đăng ký: ' + e.title,
    N'Hệ thống AEMS đã ghi nhận trạng thái đăng ký "' + r.status + N'" của bạn cho sự kiện "' + e.title + N'" lúc ' + CONVERT(NVARCHAR(20), r.registration_date, 120) + N'.',
    DATEADD(MINUTE, 5, r.registration_date),
    CASE WHEN (r.id % 37) = 0 THEN 'FAILED' ELSE 'SENT' END,
    u.id,
    r.id,
    e.id
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id;
GO

INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT
    u.email,
    N'AEMS - Ticket & QR check-in cho ' + e.title,
    N'Mã ticket: ' + t.code + N'. Vui lòng xuất trình mã này tại quầy check-in của sự kiện "' + e.title + N'".',
    t.sent_date,
    CASE WHEN (t.id % 41) = 0 THEN 'FAILED' ELSE 'SENT' END,
    u.id,
    r.id,
    e.id
FROM ticket t
JOIN registration r ON r.id = t.registration_id
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id;
GO

DECLARE @adminId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'admin01@fpt.edu.vn');
DECLARE @adminEmail VARCHAR(100) = (SELECT email FROM users WHERE id = @adminId);
DECLARE @k INT = 1;
WHILE @k <= 20
BEGIN
    INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
    VALUES (
        @adminEmail,
        N'AEMS - Báo cáo vận hành tuần ' + CAST(@k AS NVARCHAR(2)),
        N'Tổng hợp user mới, proposal, registration, attendance và feedback của tuần ' + CAST(@k AS NVARCHAR(2)) + N'.',
        DATEADD(DAY, -7 * @k, GETDATE()),
        'SENT',
        @adminId, NULL, NULL
    );
    SET @k = @k + 1;
END;
GO

-- ---------------------------------------------------------------------
-- ACTIVITY LOG  (REGISTER_EVENT / CHECK_IN / FEEDBACK / ADMIN_REPORT)
-- ---------------------------------------------------------------------
INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'REGISTER_EVENT',
    N'Đăng ký sự kiện ' + e.title,
    5,
    r.registration_date
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id
WHERE r.status = 'REGISTERED';

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
    SELECT
    u.id, 'CHECK_IN',
    N'Check-in ' + e.title + N' (' + a.status + N')',
    CASE WHEN a.status = 'ATTENDED' THEN 10 ELSE 0 END,
    a.checkin_time
FROM attendance a
JOIN registration r ON r.id = a.registration_id
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id;

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'FEEDBACK',
    N'Gửi feedback ' + CAST(f.rating AS NVARCHAR(2)) + N'/5 cho ' + e.title,
    8,
    f.created_at
FROM feedback f
JOIN student s ON s.id = f.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = f.event_id;

DECLARE @adminUserId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'admin01@fpt.edu.vn');
DECLARE @j INT = 1;
WHILE @j <= 30
BEGIN
    INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
    VALUES (
        @adminUserId,
        CASE WHEN @j % 2 = 0 THEN 'ADMIN_REPORT' ELSE 'ADMIN_USER' END,
        CASE WHEN @j % 2 = 0 THEN N'Xuất báo cáo thống kê dashboard tuần ' + CAST(@j AS NVARCHAR(2))
             ELSE N'Cập nhật tài khoản hoặc phân quyền lô #' + CAST(@j AS NVARCHAR(2)) END,
        0,
        DATEADD(HOUR, -5 * @j, GETDATE())
    );
    SET @j = @j + 1;
END;
GO

-- ---------------------------------------------------------------------
-- KẾT QUẢ
-- ---------------------------------------------------------------------
PRINT N'';
PRINT N'=====================================================';
PRINT N'  AEMS FPT seed completed.';
PRINT N'  - role:           ' + CAST((SELECT COUNT(*) FROM role)           AS NVARCHAR(10));
PRINT N'  - department:     ' + CAST((SELECT COUNT(*) FROM department)     AS NVARCHAR(10));
PRINT N'  - users:          ' + CAST((SELECT COUNT(*) FROM users)          AS NVARCHAR(10));
PRINT N'  - student:        ' + CAST((SELECT COUNT(*) FROM student)        AS NVARCHAR(10));
PRINT N'  - event:          ' + CAST((SELECT COUNT(*) FROM event)          AS NVARCHAR(10));
PRINT N'  - event_proposal: ' + CAST((SELECT COUNT(*) FROM event_proposal) AS NVARCHAR(10));
PRINT N'  - registration:   ' + CAST((SELECT COUNT(*) FROM registration)   AS NVARCHAR(10));
PRINT N'  - ticket:         ' + CAST((SELECT COUNT(*) FROM ticket)         AS NVARCHAR(10));
PRINT N'  - attendance:     ' + CAST((SELECT COUNT(*) FROM attendance)     AS NVARCHAR(10));
PRINT N'  - feedback:       ' + CAST((SELECT COUNT(*) FROM feedback)       AS NVARCHAR(10));
PRINT N'  - email_log:      ' + CAST((SELECT COUNT(*) FROM email_log)      AS NVARCHAR(10));
PRINT N'  - activity_log:   ' + CAST((SELECT COUNT(*) FROM activity_log)   AS NVARCHAR(10));
PRINT N'';
PRINT N'  Tài khoản mẫu (mật khẩu placeholder, gọi /api/auth/init-passwords để hash):';
PRINT N'    admin01@fpt.edu.vn / admin123';
PRINT N'    dept01@fpt.edu.vn  / dept123';
PRINT N'    com01@fpt.edu.vn   / com123';
PRINT N'    sv001@fpt.edu.vn   / stu123';
PRINT N'=====================================================';
GO
