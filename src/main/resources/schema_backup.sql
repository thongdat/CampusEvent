-- =====================================================
-- Event Management System - Full Setup Script
-- Chạy 1 lần duy nhất trong SSMS.
-- Script này tự tạo bảng và dữ liệu mẫu cho SQL Server.
-- =====================================================

IF NOT EXISTS (SELECT name FROM sys.databases WHERE name = 'event_management_db')
BEGIN
    CREATE DATABASE event_management_db;
END
GO

USE event_management_db;
GO

-- Xóa bảng cũ theo đúng thứ tự khóa ngoại.
IF OBJECT_ID('activity_log', 'U') IS NOT NULL DROP TABLE activity_log;
IF OBJECT_ID('emailLog', 'U') IS NOT NULL DROP TABLE emailLog;
IF OBJECT_ID('email_log', 'U') IS NOT NULL DROP TABLE email_log;
IF OBJECT_ID('feedback', 'U') IS NOT NULL DROP TABLE feedback;
IF OBJECT_ID('attendance', 'U') IS NOT NULL DROP TABLE attendance;
IF OBJECT_ID('ticket', 'U') IS NOT NULL DROP TABLE ticket;
IF OBJECT_ID('registration', 'U') IS NOT NULL DROP TABLE registration;
IF OBJECT_ID('eventProposal', 'U') IS NOT NULL DROP TABLE eventProposal;
IF OBJECT_ID('event_proposal', 'U') IS NOT NULL DROP TABLE event_proposal;
IF OBJECT_ID('event', 'U') IS NOT NULL DROP TABLE event;
IF OBJECT_ID('student', 'U') IS NOT NULL DROP TABLE student;
IF OBJECT_ID('users', 'U') IS NOT NULL DROP TABLE users;
IF OBJECT_ID('[user]', 'U') IS NOT NULL DROP TABLE [user];
IF OBJECT_ID('department', 'U') IS NOT NULL DROP TABLE department;
IF OBJECT_ID('role', 'U') IS NOT NULL DROP TABLE role;
GO

CREATE TABLE role (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description NVARCHAR(MAX),
    CONSTRAINT UK_role_name UNIQUE (name)
);
GO

CREATE TABLE users (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    full_name NVARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL,
    password VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    created_at DATETIME2 NOT NULL,
    status BIT NOT NULL,
    role_id BIGINT NOT NULL,
    otp_code VARCHAR(6),
    otp_expiry DATETIME2,
    major NVARCHAR(100),
    semester INT,
    total_points INT NOT NULL DEFAULT 0,
    CONSTRAINT UK_users_email UNIQUE (email),
    CONSTRAINT FK_users_role FOREIGN KEY (role_id) REFERENCES role(id)
);
GO

CREATE UNIQUE INDEX UX_users_phone ON users(phone)
WHERE phone IS NOT NULL AND phone <> '';
GO

CREATE TABLE department (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    name NVARCHAR(100) NOT NULL,
    description NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL
);
GO

CREATE TABLE student (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    student_code VARCHAR(50) NOT NULL,
    major NVARCHAR(100),
    year INT,
    user_id BIGINT NOT NULL,
    CONSTRAINT UK_student_code UNIQUE (student_code),
    CONSTRAINT UK_student_user UNIQUE (user_id),
    CONSTRAINT FK_student_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

CREATE TABLE event (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    location NVARCHAR(200),
    start_time DATETIME2 NOT NULL,
    end_time DATETIME2 NOT NULL,
    capacity INT,
    image_url NVARCHAR(500),
    budget DECIMAL(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at DATETIME2 NOT NULL,
    department_id BIGINT NOT NULL,
    CONSTRAINT FK_event_dept FOREIGN KEY (department_id) REFERENCES department(id)
);
GO

CREATE TABLE event_proposal (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    title NVARCHAR(200) NOT NULL,
    description NVARCHAR(MAX),
    proposed_date DATETIME2 NOT NULL,
    status VARCHAR(50) NOT NULL,
    note NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL,
    department_id BIGINT NOT NULL,
    CONSTRAINT FK_proposal_dept FOREIGN KEY (department_id) REFERENCES department(id)
);
GO

CREATE TABLE registration (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    registration_date DATETIME2 NOT NULL,
    status VARCHAR(50) NOT NULL,
    note NVARCHAR(MAX),
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT FK_reg_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT FK_reg_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE ticket (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    code VARCHAR(100) NOT NULL,
    sent_date DATETIME2 NOT NULL,
    registration_id BIGINT NOT NULL,
    CONSTRAINT UK_ticket_code UNIQUE (code),
    CONSTRAINT FK_ticket_reg FOREIGN KEY (registration_id) REFERENCES registration(id)
);
GO

CREATE TABLE attendance (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    checkin_time DATETIME2 NOT NULL,
    status VARCHAR(50) NOT NULL,
    registration_id BIGINT NOT NULL,
    CONSTRAINT FK_attend_reg FOREIGN KEY (registration_id) REFERENCES registration(id)
);
GO

CREATE TABLE feedback (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    rating INT,
    comment NVARCHAR(MAX),
    created_at DATETIME2 NOT NULL,
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    CONSTRAINT FK_fb_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT FK_fb_student FOREIGN KEY (student_id) REFERENCES student(id)
);
GO

CREATE TABLE email_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    to_email VARCHAR(100) NOT NULL,
    subject NVARCHAR(200) NOT NULL,
    content NVARCHAR(MAX),
    sent_at DATETIME2 NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id BIGINT,
    registration_id BIGINT,
    event_id BIGINT,
    CONSTRAINT FK_email_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT FK_email_reg FOREIGN KEY (registration_id) REFERENCES registration(id),
    CONSTRAINT FK_email_event FOREIGN KEY (event_id) REFERENCES event(id)
);
GO

CREATE TABLE activity_log (
    id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    activity_type VARCHAR(50) NOT NULL,
    description NVARCHAR(500),
    points_earned INT NOT NULL DEFAULT 0,
    created_at DATETIME2 NOT NULL,
    CONSTRAINT FK_activity_user FOREIGN KEY (user_id) REFERENCES users(id)
);
GO

INSERT INTO role (name, description) VALUES
('ADMIN', N'Quản trị viên hệ thống'),
('DEPARTMENT', N'Khoa / Bộ môn'),
('COMMITTEE', N'Hội đồng duyệt sự kiện'),
('STUDENT', N'Sinh viên');
GO

INSERT INTO department (name, description, created_at) VALUES
(N'Công nghệ Thông tin', N'Quản lý seminar, workshop lập trình, cloud, database và software engineering.', GETDATE()),
(N'Quản trị Kinh doanh', N'Tổ chức hội thảo quản trị, khởi nghiệp và kỹ năng lãnh đạo.', GETDATE()),
(N'Thiết kế Đồ họa', N'Phụ trách seminar UX/UI, product design và multimedia design.', GETDATE()),
(N'Marketing', N'Quản lý sự kiện truyền thông, branding và digital marketing.', GETDATE()),
(N'Ngôn ngữ Anh', N'Quản lý English club, workshop học thuật và giao lưu quốc tế.', GETDATE());
GO

INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
(N'Admin User',          'admin@example.com',      'hashed_password_123', '0901234567', GETDATE(), 1, 1, NULL, NULL, 0),
(N'Department IT',       'department@example.com', 'hashed_password_456', '0912345678', GETDATE(), 1, 2, NULL, NULL, 0),
(N'Committee User',      'committee@example.com',  'hashed_password_com', '0911111111', GETDATE(), 1, 3, NULL, NULL, 0),
(N'Nguyễn Văn A',        'student1@example.com',   'hashed_password_789', '0923456789', GETDATE(), 1, 4, N'Công nghệ Thông tin', 3, 42),
(N'Trần Thị B',          'student2@example.com',   'hashed_password_101', '0934567890', GETDATE(), 1, 4, N'Công nghệ Thông tin', 2, 36),
(N'Phạm Văn C',          'student3@example.com',   'hashed_password_202', '0945678901', GETDATE(), 1, 4, N'Công nghệ Thông tin', 4, 28),
(N'Hoàng Thị D',         'student4@example.com',   'hashed_password_303', '0956789012', GETDATE(), 1, 4, N'Quản trị Kinh doanh', 3, 31),
(N'Lê Văn E',            'student5@example.com',   'hashed_password_404', '0967890123', GETDATE(), 1, 4, N'Công nghệ Thông tin', 2, 25),
(N'Đỗ Thị F',            'student6@example.com',   'hashed_password_505', '0978901234', GETDATE(), 1, 4, N'Quản trị Kinh doanh', 3, 22),
(N'Vũ Văn G',            'student7@example.com',   'hashed_password_606', '0989012345', GETDATE(), 1, 4, N'Công nghệ Thông tin', 4, 30),
(N'Ngô Thị H',           'student8@example.com',   'hashed_password_707', '0990123456', GETDATE(), 1, 4, N'Thiết kế Đồ họa', 2, 18),
(N'Locked Admin',        'locked@example.com',     'hashed_password_999', '0991111111', GETDATE(), 0, 1, NULL, NULL, 0);
GO

INSERT INTO student (student_code, major, year, user_id) VALUES
('HE180001', N'Công nghệ Thông tin', 3, 4),
('HE180002', N'Công nghệ Thông tin', 2, 5),
('HE180003', N'Công nghệ Thông tin', 4, 6),
('HE180004', N'Quản trị Kinh doanh', 3, 7),
('HE180005', N'Công nghệ Thông tin', 2, 8),
('HE180006', N'Quản trị Kinh doanh', 3, 9),
('HE180007', N'Công nghệ Thông tin', 4, 10),
('HE180008', N'Thiết kế Đồ họa', 2, 11);
GO

INSERT INTO event (title, description, location, start_time, end_time, capacity, image_url, budget, status, created_at, department_id) VALUES
(N'Workshop: Web Development', N'Hướng dẫn phát triển web hiện đại với React và Node.js', N'Phòng 101 - Tòa A', '2026-05-15 14:00:00', '2026-05-15 17:00:00', 50, N'/assets/events/web-development.jpg', 12000000, 'PUBLISHED', GETDATE(), 1),
(N'Hội thảo: Career Path', N'Tìm hiểu lộ trình sự nghiệp trong ngành IT', N'Hội trường lớn', '2026-05-20 15:00:00', '2026-05-20 17:00:00', 100, N'/assets/events/career-path.jpg', 18000000, 'PUBLISHED', GETDATE(), 2),
(N'Networking Event 2026', N'Gặp gỡ và kết nối với các lập trình viên', N'Cafe Sinh viên', '2026-05-22 18:00:00', '2026-05-22 20:00:00', 60, N'/assets/events/networking.jpg', 9000000, 'PUBLISHED', GETDATE(), 1),
(N'Hackathon 2026', N'Cuộc thi lập trình 24 tiếng', N'Lab 3 - Tòa B', '2026-05-25 08:00:00', '2026-05-26 08:00:00', 30, N'/assets/events/hackathon.jpg', 25000000, 'PENDING', GETDATE(), 1),
(N'Seminar: AI & Machine Learning', N'Giới thiệu AI và ML trong thực tế', N'Phòng 201 - Tòa A', '2026-06-01 13:00:00', '2026-06-01 15:00:00', 80, N'/assets/events/ai-ml.jpg', 15000000, 'PUBLISHED', GETDATE(), 1);
GO

INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id) VALUES
(N'Database Performance Clinic', N'Tối ưu truy vấn SQL Server cho sinh viên SWP.', '2026-06-10 09:00:00', 'PENDING', N'Chờ hội đồng duyệt.', GETDATE(), 1),
(N'Business Case Challenge', N'Cuộc thi phân tích tình huống kinh doanh.', '2026-06-14 13:30:00', 'APPROVED', N'Đã duyệt, chuẩn bị tạo event.', GETDATE(), 2),
(N'Portfolio Review Day', N'Góp ý portfolio thiết kế cho sinh viên.', '2026-06-18 08:30:00', 'REVISION_REQUIRED', N'Cần bổ sung ngân sách.', GETDATE(), 3),
(N'English Speaking Workshop', N'Luyện phỏng vấn bằng tiếng Anh.', '2026-06-22 15:00:00', 'REJECTED', N'Lịch trùng sự kiện khác.', GETDATE(), 5);
GO

INSERT INTO registration (registration_date, status, note, event_id, student_id) VALUES
('2026-05-10 10:00:00', 'REGISTERED', N'Không có ghi chú', 1, 1),
('2026-05-11 14:30:00', 'REGISTERED', N'Sắp đầy chỗ', 1, 2),
('2026-05-12 09:15:00', 'REGISTERED', NULL, 2, 1),
('2026-05-13 11:45:00', 'REGISTERED', N'Mong chờ sự kiện này', 2, 3),
('2026-05-14 16:20:00', 'REGISTERED', NULL, 3, 1);
GO

INSERT INTO ticket (code, sent_date, registration_id) VALUES
('TICKET_001_WD2026', '2026-05-14 09:00:00', 1),
('TICKET_002_WD2026', '2026-05-14 09:00:00', 2),
('TICKET_003_CAREER2026', '2026-05-14 10:00:00', 3);
GO

INSERT INTO attendance (checkin_time, status, registration_id) VALUES
('2026-05-15 13:50:00', 'ATTENDED', 1),
('2026-05-15 13:55:00', 'ATTENDED', 2),
('2026-05-20 14:55:00', 'ATTENDED', 3);
GO

INSERT INTO feedback (rating, comment, created_at, event_id, student_id) VALUES
(5, N'Sự kiện rất tuyệt vời, học được nhiều thứ mới!', '2026-05-15 18:00:00', 1, 1),
(4, N'Nội dung hay, nhưng thời gian hơi ngắn.', '2026-05-15 18:30:00', 1, 2),
(5, N'Giảng viên giải thích rất rõ ràng.', '2026-05-20 18:00:00', 2, 1);
GO

INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id) VALUES
('student1@example.com', N'Xác nhận đăng ký Workshop', N'Bạn đã đăng ký thành công.', '2026-05-10 10:05:00', 'SENT', 4, 1, 1),
('student2@example.com', N'Xác nhận đăng ký Workshop', N'Bạn đã đăng ký thành công.', '2026-05-11 14:35:00', 'SENT', 5, 2, 1),
('admin@example.com', N'Báo cáo thống kê hàng tuần', N'Báo cáo chi tiết đã được tạo.', '2026-05-14 23:00:00', 'SENT', 1, NULL, NULL);
GO

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at) VALUES
(4, 'REGISTER_EVENT', N'Đăng ký sự kiện Workshop: Web Development', 5, '2026-05-10 10:00:00'),
(5, 'REGISTER_EVENT', N'Đăng ký sự kiện Workshop: Web Development', 5, '2026-05-11 14:30:00'),
(4, 'CHECK_IN', N'Check-in thành công Workshop: Web Development', 10, '2026-05-15 13:50:00'),
(5, 'CHECK_IN', N'Check-in thành công Workshop: Web Development', 10, '2026-05-15 13:55:00'),
(4, 'FEEDBACK', N'Gửi feedback sau sự kiện', 3, '2026-05-15 18:00:00');
GO

PRINT N'';
PRINT N'=====================================================';
PRINT N'  HOÀN TẤT! Database đã sẵn sàng.';
PRINT N'  - 4 roles, 5 departments, 12 users, 8 students';
PRINT N'  - Student code dùng mã thật HE180001...HE180008, không dùng SV001';
PRINT N'  - Phone và student_code đều có ràng buộc duy nhất';
PRINT N'=====================================================';
GO
