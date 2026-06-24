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
<<<<<<< HEAD
=======
--  Script này:
--    1. Tạo database event_management_db (nếu chưa có)
--    2. Drop + tạo lại toàn bộ bảng theo đúng schema JPA
--    3. Insert 5 role, 15 chuyên ngành (đúng theo AcademicStructure)
--    4. Insert ~95 user (3 admin, 15 điều phối khoa, 8 hội đồng, 70 sinh viên)
--    5. Insert ~70 student với mã FPT (HE/HS/HM/HF/HD ...)
--    6. Insert 32 event - mỗi event đều có ảnh FPT/campus chất lượng cao
--    7. Insert 35 event_proposal đủ các trạng thái
--    8. Sinh ~500 registration / 350 ticket / 280 attendance / feedback
--    9. Sinh QR attendance_session, quiz_question/submission/answer, event_feedback
--   10. Sinh ~700 email_log và ~900 activity_log
--
--  Mọi mật khẩu seed đều là chuỗi placeholder. Sau khi import, gọi:
--    GET http://localhost:8081/api/auth/init-passwords
--  để hash lại bằng BCrypt. Khi đó các tài khoản đăng nhập được với mật khẩu:
--    admin01@fpt.edu.vn / admin123
--    dept01@fpt.edu.vn  / dept123 (MANAGER)
--    com01@fpt.edu.vn   / com123
--    sv001@fpt.edu.vn   / stu123
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
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
IF OBJECT_ID('activity_log',       'U') IS NOT NULL DROP TABLE activity_log;
IF OBJECT_ID('email_log',          'U') IS NOT NULL DROP TABLE email_log;
IF OBJECT_ID('quiz_answer',        'U') IS NOT NULL DROP TABLE quiz_answer;
IF OBJECT_ID('quiz_submission',    'U') IS NOT NULL DROP TABLE quiz_submission;
IF OBJECT_ID('quiz_question',      'U') IS NOT NULL DROP TABLE quiz_question;
IF OBJECT_ID('event_feedback',     'U') IS NOT NULL DROP TABLE event_feedback;
IF OBJECT_ID('feedback',           'U') IS NOT NULL DROP TABLE feedback;
IF OBJECT_ID('attendance_session', 'U') IS NOT NULL DROP TABLE attendance_session;
IF OBJECT_ID('attendance',         'U') IS NOT NULL DROP TABLE attendance;
IF OBJECT_ID('ticket',             'U') IS NOT NULL DROP TABLE ticket;
IF OBJECT_ID('registration',       'U') IS NOT NULL DROP TABLE registration;
IF OBJECT_ID('event_proposal',     'U') IS NOT NULL DROP TABLE event_proposal;
IF OBJECT_ID('event',              'U') IS NOT NULL DROP TABLE event;
IF OBJECT_ID('student',            'U') IS NOT NULL DROP TABLE student;
IF OBJECT_ID('users',              'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('department',         'U') IS NOT NULL DROP TABLE department;
IF OBJECT_ID('role',               'U') IS NOT NULL DROP TABLE role;
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
<<<<<<< HEAD
total_points INT                  NOT NULL DEFAULT 0,
=======
    total_points INT                  NOT NULL DEFAULT 0,
    department_position VARCHAR(30)    NULL DEFAULT 'STAFF',
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
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
    no_show_count INT                 NOT NULL DEFAULT 0,
    attendance_reputation FLOAT       NOT NULL DEFAULT 100,
    gender       NVARCHAR(10),
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
    google_form_url NVARCHAR(1000),
    checkin_form_id NVARCHAR(120),
    checkin_sheet_id NVARCHAR(120),
    checkout_form_url NVARCHAR(1000),
    checkout_form_id NVARCHAR(120),
    checkout_sheet_id NVARCHAR(120),
    last_sheet_sync_at DATETIME2,
    auto_closed_at DATETIME2,
    speakers      NVARCHAR(800),
    organizer     NVARCHAR(200),
    support_staff_needed INT,
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
    proposed_end_date DATETIME2,
    organizer     NVARCHAR(200),
    support_staff_needed INT,
    status        VARCHAR(50)          NOT NULL,
    note          NVARCHAR(MAX),
    created_at    DATETIME2            NOT NULL,
    updated_at    DATETIME2,
    quiz_payload  NVARCHAR(MAX),
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
    invitation_sent_at DATETIME2           NULL,
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
<<<<<<< HEAD
status          VARCHAR(50)          NOT NULL,
=======
    mid_verify_time DATETIME2,
    checkout_time   DATETIME2,
    status          VARCHAR(50)          NOT NULL,
    participation_score FLOAT,
    note            NVARCHAR(MAX),
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
    registration_id BIGINT               NOT NULL,
    event_id        BIGINT,
    student_id      BIGINT,
    CONSTRAINT FK_attend_reg     FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT FK_attend_event   FOREIGN KEY (event_id)        REFERENCES event(id),
    CONSTRAINT FK_attend_student FOREIGN KEY (student_id)      REFERENCES student(id)
);
GO

CREATE TABLE attendance_session (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    event_id     BIGINT               NOT NULL,
    token        VARCHAR(120)         NOT NULL,
    session_type VARCHAR(30)          NOT NULL,
    created_at   DATETIME2            NOT NULL,
    expired_at   DATETIME2            NOT NULL,
    status       VARCHAR(30)          NOT NULL,
    CONSTRAINT FK_att_session_event FOREIGN KEY (event_id) REFERENCES event(id)
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

CREATE TABLE event_feedback (
    id                  BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    event_id            BIGINT               NOT NULL,
    student_id          BIGINT               NOT NULL,
    content_rating      INT                  NOT NULL,
    speaker_rating      INT                  NOT NULL,
    organization_rating INT                  NOT NULL,
    overall_rating      INT                  NOT NULL,
    comment             NVARCHAR(MAX),
    submitted_at        DATETIME2            NOT NULL,
    CONSTRAINT FK_event_feedback_event   FOREIGN KEY (event_id)   REFERENCES event(id),
    CONSTRAINT FK_event_feedback_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE quiz_question (
    id             BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    event_id       BIGINT               NOT NULL,
    question_text  NVARCHAR(MAX)        NOT NULL,
    question_type  VARCHAR(30)          NOT NULL,
    option_a       NVARCHAR(500),
    option_b       NVARCHAR(500),
    option_c       NVARCHAR(500),
    option_d       NVARCHAR(500),
    correct_answer VARCHAR(20),
    points         INT                  NOT NULL DEFAULT 1,
    CONSTRAINT FK_quiz_question_event FOREIGN KEY (event_id) REFERENCES event(id)
);
GO

CREATE TABLE quiz_submission (
    id           BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    event_id     BIGINT               NOT NULL,
    student_id   BIGINT               NOT NULL,
    total_score  FLOAT                NOT NULL DEFAULT 0,
    submitted_at DATETIME2            NOT NULL,
    CONSTRAINT FK_quiz_submission_event   FOREIGN KEY (event_id)   REFERENCES event(id),
    CONSTRAINT FK_quiz_submission_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE quiz_answer (
    id              BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    submission_id   BIGINT               NOT NULL,
    question_id     BIGINT               NOT NULL,
    selected_answer VARCHAR(20),
    answer_text     NVARCHAR(MAX),
    is_correct      BIT,
    score           FLOAT                NOT NULL DEFAULT 0,
    submitted_at    DATETIME2            NOT NULL,
    CONSTRAINT FK_quiz_answer_submission FOREIGN KEY (submission_id) REFERENCES quiz_submission(id),
    CONSTRAINT FK_quiz_answer_question   FOREIGN KEY (question_id)   REFERENCES quiz_question(id)
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

UPDATE users
SET department_position =
    CASE
        WHEN role_id = (SELECT id FROM role WHERE name = 'MANAGER') AND id % 3 = 1 THEN 'HEAD'
        WHEN role_id = (SELECT id FROM role WHERE name = 'MANAGER') THEN 'STAFF'
        ELSE department_position
    END;

UPDATE student
SET
    gender = CASE WHEN id % 3 = 0 THEN N'Nữ' WHEN id % 3 = 1 THEN N'Nam' ELSE N'Khác' END,
    no_show_count = id % 4,
    attendance_reputation = 100 - ((id % 4) * 7.5);
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

<<<<<<< HEAD
-- Điền nhanh 21 sự kiện còn lại cho đủ 32 sự kiện đa dạng khoa
DECLARE @ev_j INT = 1;
WHILE @ev_j <= 21
=======
UPDATE event
SET image_urls = image_url
WHERE image_url IS NOT NULL;

UPDATE e
SET
    organizer = COALESCE(organizer, N'FPT University - ' + d.name),
    speakers = COALESCE(speakers,
        CASE e.department_id % 5
            WHEN 0 THEN N'Chuyên gia FPT Software; Mentor doanh nghiệp'
            WHEN 1 THEN N'Giảng viên FPT University; Alumni khách mời'
            WHEN 2 THEN N'Đại diện FPT Corporation; Trưởng bộ môn'
            WHEN 3 THEN N'Chuyên gia đối tác; CLB sinh viên'
            ELSE N'Hội đồng chuyên môn; Mentor cộng đồng'
        END),
    support_staff_needed = COALESCE(support_staff_needed, 3 + (e.id % 8)),
    google_form_url = CASE WHEN e.id % 4 = 0 THEN N'https://forms.gle/demo-checkin-' + CAST(e.id AS NVARCHAR(20)) ELSE google_form_url END,
    checkin_form_id = CASE WHEN e.id % 4 = 0 THEN N'checkin-form-' + CAST(e.id AS NVARCHAR(20)) ELSE checkin_form_id END,
    checkin_sheet_id = CASE WHEN e.id % 4 = 0 THEN N'checkin-sheet-' + CAST(e.id AS NVARCHAR(20)) ELSE checkin_sheet_id END,
    checkout_form_url = CASE WHEN e.id % 5 = 0 THEN N'https://forms.gle/demo-checkout-' + CAST(e.id AS NVARCHAR(20)) ELSE checkout_form_url END,
    checkout_form_id = CASE WHEN e.id % 5 = 0 THEN N'checkout-form-' + CAST(e.id AS NVARCHAR(20)) ELSE checkout_form_id END,
    checkout_sheet_id = CASE WHEN e.id % 5 = 0 THEN N'checkout-sheet-' + CAST(e.id AS NVARCHAR(20)) ELSE checkout_sheet_id END,
    last_sheet_sync_at = CASE WHEN e.id % 4 = 0 THEN DATEADD(HOUR, -e.id, GETDATE()) ELSE last_sheet_sync_at END,
    auto_closed_at = CASE WHEN e.end_time < GETDATE() THEN DATEADD(MINUTE, 15, e.end_time) ELSE auto_closed_at END
FROM event e
JOIN department d ON d.id = e.department_id;
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

UPDATE event_proposal
SET
    proposed_end_date = COALESCE(proposed_end_date, DATEADD(HOUR, 3 + (id % 5), proposed_date)),
    organizer = COALESCE(organizer, N'Đơn vị đề xuất #' + CAST(department_id AS NVARCHAR(10))),
    support_staff_needed = COALESCE(support_staff_needed, 2 + (id % 6)),
    quiz_payload = COALESCE(quiz_payload,
        N'[{"question":"Bạn kỳ vọng điều gì ở sự kiện này?","type":"SHORT_ANSWER"},{"question":"Bạn đã sẵn sàng tham gia đầy đủ?","type":"MULTIPLE_CHOICE","options":["Có","Chưa chắc"]}]');
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
INSERT INTO attendance (
    checkin_time, mid_verify_time, checkout_time, status,
    participation_score, note, registration_id, event_id, student_id
)
SELECT
    DATEADD(MINUTE, -10 + (ABS(CHECKSUM(NEWID())) % 20), e.start_time),
    CASE WHEN (r.id % 11) = 0 THEN NULL ELSE DATEADD(MINUTE, 45 + (r.id % 30), e.start_time) END,
    CASE WHEN (r.id % 11) = 0 THEN NULL ELSE DATEADD(MINUTE, -5 + (r.id % 20), e.end_time) END,
    CASE
        WHEN (r.id % 11) = 0 THEN 'ABSENT'
        WHEN (r.id % 5) = 0 THEN 'CHECKED_IN'
        WHEN (r.id % 7) = 0 THEN 'MID_VERIFIED'
        ELSE 'CHECKED_OUT'
    END,
    CASE
        WHEN (r.id % 11) = 0 THEN 0
        WHEN (r.id % 5) = 0 THEN 55 + (r.id % 15)
        WHEN (r.id % 7) = 0 THEN 70 + (r.id % 12)
        ELSE 85 + (r.id % 15)
    END,
    CASE WHEN (r.id % 11) = 0 THEN N'Tự động đánh vắng do không check-in.' ELSE NULL END,
    r.id,
    r.event_id,
    r.student_id
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE r.status = 'REGISTERED'
  AND (r.id % 10) < 7;
GO

INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
SELECT
    e.id,
    'AEMS-IN-' + RIGHT('0000' + CAST(e.id AS VARCHAR(4)), 4),
    'CHECK_IN',
    DATEADD(MINUTE, -30, e.start_time),
    DATEADD(MINUTE, 45, e.start_time),
    CASE WHEN e.end_time < GETDATE() THEN 'EXPIRED' ELSE 'ACTIVE' END
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
SELECT
    e.id,
    'AEMS-MID-' + RIGHT('0000' + CAST(e.id AS VARCHAR(4)), 4),
    'MID_SESSION',
    DATEADD(MINUTE, DATEDIFF(MINUTE, e.start_time, e.end_time) / 2 - 15, e.start_time),
    DATEADD(MINUTE, DATEDIFF(MINUTE, e.start_time, e.end_time) / 2 + 30, e.start_time),
    CASE WHEN e.end_time < GETDATE() THEN 'EXPIRED' ELSE 'ACTIVE' END
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');
GO

-- ---------------------------------------------------------------------
-- FEEDBACK  (~50% attendance đã xác minh có feedback)
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
WHERE a.status IN ('MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 2) = 0;
GO

-- ---------------------------------------------------------------------
-- QUIZ + EVENT FEEDBACK MỚI
-- ---------------------------------------------------------------------
INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, N'Mục tiêu chính của sự kiện "' + e.title + N'" là gì?', 'MULTIPLE_CHOICE',
       N'Cập nhật kiến thức và thực hành', N'Chỉ điểm danh', N'Bán sản phẩm', N'Thi cuối kỳ', 'A', 2
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, N'Bạn nên làm gì sau khi tham dự workshop/talkshow?', 'MULTIPLE_CHOICE',
       N'Hoàn thành feedback và áp dụng nội dung đã học', N'Bỏ qua tài liệu', N'Chỉ lấy ticket', N'Không cần checkout', 'A', 2
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, N'Điều nào giúp ban tổ chức cải thiện sự kiện tiếp theo?', 'MULTIPLE_CHOICE',
       N'Feedback cụ thể, lịch sự', N'Không phản hồi', N'Đăng ký rồi vắng mặt', N'Gửi spam', 'A', 1
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');
GO

INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT
    a.event_id,
    a.student_id,
    CASE WHEN (a.id % 6) = 0 THEN 3 ELSE 5 END,
    DATEADD(MINUTE, 10, COALESCE(a.checkout_time, a.mid_verify_time, a.checkin_time))
FROM attendance a
WHERE a.status IN ('MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 3) <> 0;
GO

INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT
    qs.id,
    qq.id,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN 'B' ELSE 'A' END,
    NULL,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN 0 ELSE 1 END,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN 0 ELSE qq.points END,
    DATEADD(MINUTE, qq.id % 5, qs.submitted_at)
FROM quiz_submission qs
JOIN quiz_question qq ON qq.event_id = qs.event_id;
GO

UPDATE qs
SET total_score = agg.score
FROM quiz_submission qs
JOIN (
    SELECT submission_id, SUM(score) AS score
    FROM quiz_answer
    GROUP BY submission_id
) agg ON agg.submission_id = qs.id;
GO

INSERT INTO event_feedback (
    event_id, student_id, content_rating, speaker_rating,
    organization_rating, overall_rating, comment, submitted_at
)
SELECT
    a.event_id,
    a.student_id,
    3 + (a.id % 3),
    3 + ((a.id + 1) % 3),
    3 + ((a.id + 2) % 3),
    3 + ((a.id + 3) % 3),
    CASE (a.id % 7)
        WHEN 0 THEN @cmt1
        WHEN 1 THEN @cmt2
        WHEN 2 THEN @cmt3
        WHEN 3 THEN @cmt4
        WHEN 4 THEN @cmt5
        WHEN 5 THEN @cmt6
        ELSE @cmt7
    END,
    DATEADD(MINUTE, 15, COALESCE(a.checkout_time, a.mid_verify_time, a.checkin_time))
FROM attendance a
WHERE a.status IN ('MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 2) = 0;
GO

UPDATE a
SET participation_score =
    CASE
        WHEN a.status = 'ABSENT' THEN 0
        ELSE
            50
            + CASE WHEN a.mid_verify_time IS NOT NULL THEN 20 ELSE 0 END
            + CASE WHEN qs.id IS NOT NULL THEN 20 ELSE 0 END
            + CASE WHEN ef.id IS NOT NULL THEN 10 ELSE 0 END
    END
FROM attendance a
LEFT JOIN quiz_submission qs ON qs.event_id = a.event_id AND qs.student_id = a.student_id
LEFT JOIN event_feedback ef ON ef.event_id = a.event_id AND ef.student_id = a.student_id;
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
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
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
<<<<<<< HEAD
DECLARE @prop_i INT = 1;
WHILE @prop_i <= 35
=======
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
    CASE WHEN a.status IN ('MID_VERIFIED', 'CHECKED_OUT') THEN 10 ELSE 0 END,
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

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'QUIZ_SUBMIT',
    N'Nộp quiz sau sự kiện ' + e.title + N' - điểm ' + CAST(qs.total_score AS NVARCHAR(10)),
    CAST(qs.total_score AS INT),
    qs.submitted_at
FROM quiz_submission qs
JOIN student s ON s.id = qs.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = qs.event_id;

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'EVENT_FEEDBACK',
    N'Gửi feedback chi tiết cho ' + e.title,
    10,
    ef.submitted_at
FROM event_feedback ef
JOIN student s ON s.id = ef.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = ef.event_id;

DECLARE @adminUserId BIGINT = (SELECT TOP 1 id FROM users WHERE email = 'admin01@fpt.edu.vn');
DECLARE @j INT = 1;
WHILE @j <= 30
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
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
<<<<<<< HEAD
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
=======
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
PRINT N'  - attendance_session: ' + CAST((SELECT COUNT(*) FROM attendance_session) AS NVARCHAR(10));
PRINT N'  - quiz_question:  ' + CAST((SELECT COUNT(*) FROM quiz_question)  AS NVARCHAR(10));
PRINT N'  - quiz_submission:' + CAST((SELECT COUNT(*) FROM quiz_submission) AS NVARCHAR(10));
PRINT N'  - quiz_answer:    ' + CAST((SELECT COUNT(*) FROM quiz_answer)    AS NVARCHAR(10));
PRINT N'  - event_feedback: ' + CAST((SELECT COUNT(*) FROM event_feedback) AS NVARCHAR(10));
PRINT N'  - email_log:      ' + CAST((SELECT COUNT(*) FROM email_log)      AS NVARCHAR(10));
PRINT N'  - activity_log:   ' + CAST((SELECT COUNT(*) FROM activity_log)   AS NVARCHAR(10));
PRINT N'';
PRINT N'  Tài khoản mẫu (mật khẩu placeholder, gọi /api/auth/init-passwords để hash):';
PRINT N'    admin01@fpt.edu.vn / admin123';
PRINT N'    dept01@fpt.edu.vn  / dept123';
PRINT N'    com01@fpt.edu.vn   / com123';
PRINT N'    sv001@fpt.edu.vn   / stu123';
PRINT N'=====================================================';
>>>>>>> a4dfe83916a51c29358f20bfa25155def4251119
GO
