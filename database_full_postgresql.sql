-- =====================================================================
--  CampusEvent / AEMS - FULL DATABASE SEED  (PostgreSQL / Neon)
--  Bản chuyển đổi "y chang" từ database_full.sql (SQL Server).
--  Chạy 1 lần duy nhất trên PostgreSQL (psql / pgAdmin / DBeaver / Neon).
--  Script này:
--    1. Drop + tạo lại toàn bộ bảng theo đúng schema JPA
--    2. Insert 5 role, 15 chuyên ngành
--    3. Insert ~95 user (3 admin, 15 điều phối khoa, 8 hội đồng, 70 sinh viên)
--    4. Insert ~70 student với mã FPT (HE/HS/HM/HF/HD ...)
--    5. Insert 33 event - mỗi event đều có ảnh FPT/campus chất lượng cao
--    6. Insert 35 event_proposal đủ các trạng thái
--    7. Sinh ~500 registration / ticket / attendance / feedback
--    8. Sinh QR attendance_session, quiz_question/submission/answer, event_feedback
--    9. Sinh email_log và activity_log
--
--  Mọi mật khẩu seed đều là chuỗi placeholder. Sau khi import, gọi:
--    GET http://localhost:8081/api/auth/init-passwords
--  để hash lại bằng BCrypt. Khi đó các tài khoản đăng nhập được với mật khẩu:
--    admin01@fpt.edu.vn / admin123
--    dept01@fpt.edu.vn  / dept123 (MANAGER)
--    com01@fpt.edu.vn   / com123
--    sv001@fpt.edu.vn   / stu123
-- =====================================================================

-- ---------------------------------------------------------------------
-- DROP theo đúng thứ tự khóa ngoại (CASCADE cho an toàn)
-- ---------------------------------------------------------------------
DROP TABLE IF EXISTS activity_log       CASCADE;
DROP TABLE IF EXISTS email_log          CASCADE;
DROP TABLE IF EXISTS quiz_answer        CASCADE;
DROP TABLE IF EXISTS quiz_submission    CASCADE;
DROP TABLE IF EXISTS quiz_question      CASCADE;
DROP TABLE IF EXISTS event_feedback     CASCADE;
DROP TABLE IF EXISTS feedback           CASCADE;
DROP TABLE IF EXISTS attendance_session CASCADE;
DROP TABLE IF EXISTS attendance         CASCADE;
DROP TABLE IF EXISTS ticket             CASCADE;
DROP TABLE IF EXISTS registration       CASCADE;
DROP TABLE IF EXISTS event_proposal     CASCADE;
DROP TABLE IF EXISTS event              CASCADE;
DROP TABLE IF EXISTS student            CASCADE;
DROP TABLE IF EXISTS users              CASCADE;
DROP TABLE IF EXISTS department         CASCADE;
DROP TABLE IF EXISTS role               CASCADE;

-- ---------------------------------------------------------------------
-- SCHEMA
-- ---------------------------------------------------------------------
CREATE TABLE role (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE department (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    created_at  TIMESTAMP NOT NULL
);

CREATE TABLE users (
    id           BIGSERIAL PRIMARY KEY,
    full_name    VARCHAR(100) NOT NULL,
    email        VARCHAR(100) NOT NULL UNIQUE,
    password     VARCHAR(255),
    phone        VARCHAR(20) NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    status       BOOLEAN NOT NULL,
    role_id      BIGINT NOT NULL REFERENCES role(id),
    otp_code     VARCHAR(6),
    otp_expiry   TIMESTAMP,
    major        VARCHAR(100),
    semester     INTEGER,
    total_points INTEGER NOT NULL DEFAULT 0,
    department_position VARCHAR(30) DEFAULT 'STAFF'
);

CREATE UNIQUE INDEX ux_users_phone ON users(phone)
WHERE phone IS NOT NULL AND phone <> '';

CREATE TABLE student (
    id           BIGSERIAL PRIMARY KEY,
    student_code VARCHAR(50) NOT NULL UNIQUE,
    major        VARCHAR(100),
    year         INTEGER,
    no_show_count INTEGER NOT NULL DEFAULT 0,
    attendance_reputation DOUBLE PRECISION NOT NULL DEFAULT 100,
    gender       VARCHAR(10),
    user_id      BIGINT NOT NULL UNIQUE REFERENCES users(id)
);

CREATE TABLE event (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    location      VARCHAR(200),
    start_time    TIMESTAMP NOT NULL,
    end_time      TIMESTAMP NOT NULL,
    capacity      INTEGER,
    image_url     VARCHAR(500),
    image_urls    TEXT,
    google_form_url VARCHAR(1000),
    checkin_form_id VARCHAR(120),
    checkin_sheet_id VARCHAR(120),
    checkout_form_url VARCHAR(1000),
    checkout_form_id VARCHAR(120),
    checkout_sheet_id VARCHAR(120),
    last_sheet_sync_at TIMESTAMP,
    auto_closed_at TIMESTAMP,
    speakers      VARCHAR(800),
    organizer     VARCHAR(200),
    support_staff_needed INTEGER,
    budget        NUMERIC(18,2) NOT NULL DEFAULT 0,
    status        VARCHAR(50) NOT NULL,
    created_at    TIMESTAMP NOT NULL,
    department_id BIGINT NOT NULL REFERENCES department(id)
);

CREATE TABLE event_proposal (
    id            BIGSERIAL PRIMARY KEY,
    title         VARCHAR(200) NOT NULL,
    description   TEXT,
    location      VARCHAR(200),
    capacity      INTEGER,
    image_url     VARCHAR(500),
    image_urls    TEXT,
    budget        NUMERIC(18,2) NOT NULL DEFAULT 0,
    proposed_date TIMESTAMP NOT NULL,
    proposed_end_date TIMESTAMP,
    organizer     VARCHAR(200),
    speakers      VARCHAR(800),
    support_staff_needed INTEGER,
    status        VARCHAR(50) NOT NULL,
    note          TEXT,
    created_at    TIMESTAMP NOT NULL,
    quiz_payload  TEXT,
    department_id BIGINT NOT NULL REFERENCES department(id)
);

CREATE TABLE registration (
    id                BIGSERIAL PRIMARY KEY,
    registration_date TIMESTAMP NOT NULL,
    status            VARCHAR(50) NOT NULL,
    note              TEXT,
    priority_score    NUMERIC(5,2),
    invitation_sent_at TIMESTAMP,
    event_id          BIGINT NOT NULL REFERENCES event(id),
    student_id        BIGINT NOT NULL REFERENCES student(id)
);

CREATE TABLE ticket (
    id              BIGSERIAL PRIMARY KEY,
    code            VARCHAR(100) NOT NULL UNIQUE,
    sent_date       TIMESTAMP NOT NULL,
    registration_id BIGINT NOT NULL REFERENCES registration(id)
);

CREATE TABLE attendance (
    id              BIGSERIAL PRIMARY KEY,
    checkin_time    TIMESTAMP NOT NULL,
    mid_verify_time TIMESTAMP,
    checkout_time   TIMESTAMP,
    status          VARCHAR(50) NOT NULL,
    participation_score DOUBLE PRECISION,
    note            TEXT,
    registration_id BIGINT NOT NULL REFERENCES registration(id),
    event_id        BIGINT REFERENCES event(id),
    student_id      BIGINT REFERENCES student(id)
);

CREATE TABLE attendance_session (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT NOT NULL REFERENCES event(id),
    token        VARCHAR(120) NOT NULL,
    session_type VARCHAR(30) NOT NULL,
    created_at   TIMESTAMP NOT NULL,
    expired_at   TIMESTAMP NOT NULL,
    status       VARCHAR(30) NOT NULL
);

CREATE TABLE feedback (
    id         BIGSERIAL PRIMARY KEY,
    rating     INTEGER,
    comment    TEXT,
    created_at TIMESTAMP NOT NULL,
    event_id   BIGINT NOT NULL REFERENCES event(id),
    student_id BIGINT NOT NULL REFERENCES student(id)
);

CREATE TABLE event_feedback (
    id                  BIGSERIAL PRIMARY KEY,
    event_id            BIGINT NOT NULL REFERENCES event(id),
    student_id          BIGINT NOT NULL REFERENCES student(id),
    content_rating      INTEGER NOT NULL,
    speaker_rating      INTEGER NOT NULL,
    organization_rating INTEGER NOT NULL,
    overall_rating      INTEGER NOT NULL,
    comment             TEXT,
    submitted_at        TIMESTAMP NOT NULL
);

CREATE TABLE quiz_question (
    id             BIGSERIAL PRIMARY KEY,
    event_id       BIGINT NOT NULL REFERENCES event(id),
    question_text  TEXT NOT NULL,
    question_type  VARCHAR(30) NOT NULL,
    option_a       VARCHAR(500),
    option_b       VARCHAR(500),
    option_c       VARCHAR(500),
    option_d       VARCHAR(500),
    correct_answer VARCHAR(20),
    points         INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE quiz_submission (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT NOT NULL REFERENCES event(id),
    student_id   BIGINT NOT NULL REFERENCES student(id),
    total_score  DOUBLE PRECISION NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE quiz_answer (
    id              BIGSERIAL PRIMARY KEY,
    submission_id   BIGINT NOT NULL REFERENCES quiz_submission(id),
    question_id     BIGINT NOT NULL REFERENCES quiz_question(id),
    selected_answer VARCHAR(20),
    answer_text     TEXT,
    is_correct      BOOLEAN,
    score           DOUBLE PRECISION NOT NULL DEFAULT 0,
    submitted_at    TIMESTAMP NOT NULL
);

CREATE TABLE email_log (
    id              BIGSERIAL PRIMARY KEY,
    to_email        VARCHAR(100) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    content         TEXT,
    sent_at         TIMESTAMP NOT NULL,
    status          VARCHAR(50) NOT NULL,
    user_id         BIGINT REFERENCES users(id),
    registration_id BIGINT REFERENCES registration(id),
    event_id        BIGINT REFERENCES event(id)
);

CREATE TABLE activity_log (
    id            BIGSERIAL PRIMARY KEY,
    user_id       BIGINT NOT NULL REFERENCES users(id),
    activity_type VARCHAR(50) NOT NULL,
    description   VARCHAR(500),
    points_earned INTEGER NOT NULL DEFAULT 0,
    created_at    TIMESTAMP NOT NULL
);

-- ---------------------------------------------------------------------
-- ROLES
-- ---------------------------------------------------------------------
INSERT INTO role (name, description) VALUES
('ADMIN',      'Quản trị hệ thống: quản lý user, role, department và báo cáo.'),
('MANAGER',    'Quản lý khoa/bộ môn: phụ trách proposal, event và sinh viên trong đơn vị.'),
('DEPARTMENT', 'Khoa / Bộ môn: tạo proposal, cập nhật proposal và quản lý event đã duyệt.'),
('COMMITTEE',  'Hội đồng duyệt sự kiện: phê duyệt, từ chối hoặc yêu cầu chỉnh sửa proposal.'),
('STUDENT',    'Sinh viên FPT: xem event, đăng ký, check-in và gửi feedback.');

-- ---------------------------------------------------------------------
-- DEPARTMENTS  (15 chuyên ngành theo AcademicStructure.java)
-- ---------------------------------------------------------------------
INSERT INTO department (name, description, created_at) VALUES
('Công nghệ Thông tin',         'Khoa CNTT - tổ chức seminar lập trình, cloud, database, software engineering tại các campus FPT.',                    NOW() - INTERVAL '240 days'),
('Kỹ thuật phần mềm',           'Bộ môn SE - phụ trách workshop quy trình Agile/Scrum, kiến trúc phần mềm, SWP/EXE/PRJ.',                              NOW() - INTERVAL '228 days'),
('An toàn thông tin',           'Bộ môn IA - tổ chức CTF, secure coding, pentest lab và chuyên đề bảo mật cho sinh viên FPT.',                         NOW() - INTERVAL '212 days'),
('Trí tuệ nhân tạo',            'Bộ môn AI - workshop machine learning, deep learning, LLM/GenAI và ứng dụng AI thực tế.',                             NOW() - INTERVAL '198 days'),
('Data Science',                'Bộ môn DS - chuyên đề phân tích dữ liệu, Power BI/Tableau, data engineering và data storytelling.',                   NOW() - INTERVAL '180 days'),
('Kinh tế',                     'Khoa Kinh tế - hội thảo kinh tế vĩ mô, phân tích thị trường và chương trình thực tập doanh nghiệp.',                   NOW() - INTERVAL '160 days'),
('Marketing',                   'Bộ môn Marketing - sự kiện branding, content marketing, performance ads, MarTech và Brand Camp.',                      NOW() - INTERVAL '142 days'),
('Quản trị kinh doanh',         'Bộ môn QTKD - case challenge, talkshow lãnh đạo, business simulation và startup pitching.',                            NOW() - INTERVAL '128 days'),
('Tài chính Ngân hàng',         'Bộ môn TCNH - hội thảo đầu tư, phân tích báo cáo tài chính, FinTech và ngân hàng số.',                                 NOW() - INTERVAL '116 days'),
('Thiết kế Mỹ thuật số',        'Bộ môn Digital Art - workshop UI/UX, motion graphics, 3D modelling và product design.',                                NOW() - INTERVAL '100 days'),
('Thiết kế Đồ họa',             'Bộ môn Graphic Design - chuyên đề typography, branding identity, illustration và in ấn.',                              NOW() - INTERVAL '88 days'),
('Truyền thông đa phương tiện', 'Bộ môn MMC - workshop video production, podcasting, social content và truyền thông số.',                              NOW() - INTERVAL '72 days'),
('Ngôn ngữ Anh',                'Bộ môn ENG - English Speaking Club, IELTS bootcamp, presentation contest và giao lưu quốc tế.',                       NOW() - INTERVAL '56 days'),
('Ngôn ngữ Nhật',               'Bộ môn JPN - JLPT bootcamp, Japan Day, talkshow doanh nghiệp Nhật Bản và workshop văn hóa.',                          NOW() - INTERVAL '40 days'),
('Du lịch - Khách sạn',         'Bộ môn THM - workshop hospitality, F&B service, hướng nghiệp khách sạn 5 sao và career trip.',                        NOW() - INTERVAL '28 days');

-- ---------------------------------------------------------------------
-- USERS - 3 ADMIN
-- ---------------------------------------------------------------------
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
('Nguyễn Hữu An',     'admin01@fpt.edu.vn',  'plain:admin123',  '0901000001', NOW() - INTERVAL '300 days', TRUE,  (SELECT id FROM role WHERE name='ADMIN'), 'Hệ thống', NULL, 0),
('Trần Vận Hành',     'admin02@fpt.edu.vn',  'plain:admin123',  '0901000002', NOW() - INTERVAL '270 days', TRUE,  (SELECT id FROM role WHERE name='ADMIN'), 'Hệ thống', NULL, 0),
('Lê Tài Khoản Khóa', 'locked@fpt.edu.vn',   'plain:locked123', '0901000099', NOW() - INTERVAL '250 days', FALSE, (SELECT id FROM role WHERE name='ADMIN'), 'Hệ thống', NULL, 0);

-- 15 DEPARTMENT (MANAGER) - email dept01..dept15
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
('Phạm Minh Quang',     'dept01@fpt.edu.vn', 'plain:dept123', '0911000001', NOW() - INTERVAL '210 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Công nghệ Thông tin',         NULL, 0),
('Đỗ Hồng Hạnh',        'dept02@fpt.edu.vn', 'plain:dept123', '0911000002', NOW() - INTERVAL '205 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Kỹ thuật phần mềm',           NULL, 0),
('Vũ Thái Bảo',         'dept03@fpt.edu.vn', 'plain:dept123', '0911000003', NOW() - INTERVAL '200 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'An toàn thông tin',           NULL, 0),
('Hoàng Anh Khoa',      'dept04@fpt.edu.vn', 'plain:dept123', '0911000004', NOW() - INTERVAL '195 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Trí tuệ nhân tạo',            NULL, 0),
('Bùi Diệu Linh',       'dept05@fpt.edu.vn', 'plain:dept123', '0911000005', NOW() - INTERVAL '190 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Data Science',                NULL, 0),
('Đặng Quốc Việt',      'dept06@fpt.edu.vn', 'plain:dept123', '0911000006', NOW() - INTERVAL '185 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Kinh tế',                     NULL, 0),
('Trịnh Thu Phương',    'dept07@fpt.edu.vn', 'plain:dept123', '0911000007', NOW() - INTERVAL '180 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Marketing',                   NULL, 0),
('Nguyễn Đăng Khoa',    'dept08@fpt.edu.vn', 'plain:dept123', '0911000008', NOW() - INTERVAL '175 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Quản trị kinh doanh',         NULL, 0),
('Lý Kim Thoa',         'dept09@fpt.edu.vn', 'plain:dept123', '0911000009', NOW() - INTERVAL '170 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Tài chính Ngân hàng',         NULL, 0),
('Phan Tuấn Tú',        'dept10@fpt.edu.vn', 'plain:dept123', '0911000010', NOW() - INTERVAL '165 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Thiết kế Mỹ thuật số',        NULL, 0),
('Châu Mỹ Duyên',       'dept11@fpt.edu.vn', 'plain:dept123', '0911000011', NOW() - INTERVAL '160 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Thiết kế Đồ họa',             NULL, 0),
('Hà Lan Anh',          'dept12@fpt.edu.vn', 'plain:dept123', '0911000012', NOW() - INTERVAL '155 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Truyền thông đa phương tiện', NULL, 0),
('Mai Khánh Vy',        'dept13@fpt.edu.vn', 'plain:dept123', '0911000013', NOW() - INTERVAL '150 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Ngôn ngữ Anh',                NULL, 0),
('Yamamoto Hằng',       'dept14@fpt.edu.vn', 'plain:dept123', '0911000014', NOW() - INTERVAL '145 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Ngôn ngữ Nhật',               NULL, 0),
('Ngô Hoàng Thiện',     'dept15@fpt.edu.vn', 'plain:dept123', '0911000015', NOW() - INTERVAL '140 days', TRUE, (SELECT id FROM role WHERE name='MANAGER'), 'Du lịch - Khách sạn',         NULL, 0);

-- 8 COMMITTEE
INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points) VALUES
('TS. Lê Thu Hà',         'com01@fpt.edu.vn', 'plain:com123', '0922000001', NOW() - INTERVAL '220 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('TS. Phạm Quốc Minh',    'com02@fpt.edu.vn', 'plain:com123', '0922000002', NOW() - INTERVAL '218 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('ThS. Nguyễn Bảo Anh',   'com03@fpt.edu.vn', 'plain:com123', '0922000003', NOW() - INTERVAL '215 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('ThS. Trần Khánh Linh',  'com04@fpt.edu.vn', 'plain:com123', '0922000004', NOW() - INTERVAL '210 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('TS. Đỗ Minh Khang',     'com05@fpt.edu.vn', 'plain:com123', '0922000005', NOW() - INTERVAL '205 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('ThS. Võ Hoàng Nam',     'com06@fpt.edu.vn', 'plain:com123', '0922000006', NOW() - INTERVAL '200 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('ThS. Mai Phương Thảo',  'com07@fpt.edu.vn', 'plain:com123', '0922000007', NOW() - INTERVAL '195 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0),
('TS. Bùi Thanh Sơn',     'com08@fpt.edu.vn', 'plain:com123', '0922000008', NOW() - INTERVAL '190 days', TRUE, (SELECT id FROM role WHERE name='COMMITTEE'), 'Hội đồng duyệt', NULL, 0);

-- ---------------------------------------------------------------------
-- 70 STUDENT users - tên Việt + email sv001..sv070  (DO block thay WHILE)
-- ---------------------------------------------------------------------
DO $$
DECLARE
    first_names TEXT[] := ARRAY[
        'An','Bình','Chi','Dũng','Duy','Hà','Hân','Hậu','Hiếu','Huy',
        'Khang','Khánh','Khoa','Lâm','Linh','Long','Minh','My','Nam','Nga',
        'Ngân','Ngọc','Nhật','Phong','Phúc','Phương','Quân','Quỳnh','Sơn','Tâm',
        'Thảo','Thắng','Thi','Tiến','Toàn','Trang','Trí','Trinh','Tú','Tuấn',
        'Tùng','Vy','Yến','Bảo','Đạt','Hải','Hùng','Lan','Mai','Nhi',
        'Quốc','Thiện','Thy','Vinh','Vĩ','Hà','Diệu','Khải','Nguyên','Ngân',
        'Phát','Quang','Sang','Sĩ','Thái','Thiên','Tín','Trung','Vũ','Yên'];
    last_names TEXT[] := ARRAY[
        'Nguyễn Hữu','Trần Thị','Lê Hoàng','Phạm Quang','Hoàng Minh',
        'Đỗ Thị','Vũ Đình','Ngô Bảo','Bùi Thanh','Đặng Khánh',
        'Võ Phương','Mai Khắc','Phan Đăng','Lý Quốc','Trịnh Văn'];
    major_names TEXT[] := ARRAY[
        'Công nghệ Thông tin','Kỹ thuật phần mềm','An toàn thông tin','Trí tuệ nhân tạo','Data Science',
        'Kinh tế','Marketing','Quản trị kinh doanh','Tài chính Ngân hàng','Thiết kế Mỹ thuật số',
        'Thiết kế Đồ họa','Truyền thông đa phương tiện','Ngôn ngữ Anh','Ngôn ngữ Nhật','Du lịch - Khách sạn'];
    stu_role BIGINT;
    i INT;
    fn TEXT; ln TEXT; maj TEXT;
    sem INT; pts INT; stt BOOLEAN;
BEGIN
    SELECT id INTO stu_role FROM role WHERE name = 'STUDENT';
    FOR i IN 1..70 LOOP
        fn  := first_names[((i * 7) % 70) + 1];
        ln  := last_names[((i * 3) % 15) + 1];
        maj := major_names[((i - 1) % 15) + 1];
        sem := ((i * 5) % 9) + 1;
        pts := 30 + ((i * 17) % 470);
        stt := NOT (i % 23 = 0);

        INSERT INTO users (full_name, email, password, phone, created_at, status, role_id, major, semester, total_points)
        VALUES (
            ln || ' ' || fn,
            'sv' || lpad(i::text, 3, '0') || '@fpt.edu.vn',
            'plain:stu123',
            '0933' || lpad(i::text, 6, '0'),
            NOW() - make_interval(days => (180 - i)),
            stt,
            stu_role,
            maj,
            sem,
            pts
        );
    END LOOP;
END $$;

-- ---------------------------------------------------------------------
-- STUDENT  (link users -> mã sinh viên FPT chuẩn HE/HS/HM/HF/HD/HL/HT)
-- ---------------------------------------------------------------------
WITH ranked AS (
    SELECT u.id AS user_id,
           u.major,
           u.semester,
           ROW_NUMBER() OVER (ORDER BY u.id) AS rn
    FROM users u
    WHERE u.role_id = (SELECT id FROM role WHERE name = 'STUDENT')
)
INSERT INTO student (student_code, major, year, user_id)
SELECT
    CASE r.major
        WHEN 'An toàn thông tin'   THEN 'HS'
        WHEN 'Kinh tế'             THEN 'HM'
        WHEN 'Marketing'           THEN 'HM'
        WHEN 'Quản trị kinh doanh' THEN 'HM'
        WHEN 'Tài chính Ngân hàng' THEN 'HF'
        WHEN 'Thiết kế Mỹ thuật số' THEN 'HD'
        WHEN 'Thiết kế Đồ họa'     THEN 'HD'
        WHEN 'Truyền thông đa phương tiện' THEN 'HD'
        WHEN 'Ngôn ngữ Anh'        THEN 'HL'
        WHEN 'Ngôn ngữ Nhật'       THEN 'HL'
        WHEN 'Du lịch - Khách sạn' THEN 'HT'
        ELSE 'HE'
    END || '18' || lpad(r.rn::text, 4, '0'),
    r.major,
    CASE WHEN r.semester IS NULL OR r.semester < 1 THEN 1
         ELSE ((r.semester - 1) / 3) + 1
    END,
    r.user_id
FROM ranked r;

UPDATE users
SET department_position =
    CASE
        WHEN role_id = (SELECT id FROM role WHERE name = 'MANAGER') AND id % 3 = 1 THEN 'HEAD'
        WHEN role_id = (SELECT id FROM role WHERE name = 'MANAGER') THEN 'STAFF'
        ELSE department_position
    END;

UPDATE student
SET
    gender = CASE WHEN id % 3 = 0 THEN 'Nữ' WHEN id % 3 = 1 THEN 'Nam' ELSE 'Khác' END,
    no_show_count = id % 4,
    attendance_reputation = 100 - ((id % 4) * 7.5);

-- ---------------------------------------------------------------------
-- EVENTS  (33 sự kiện - mỗi sự kiện kèm ảnh FPT/campus chất lượng cao)
-- ---------------------------------------------------------------------
INSERT INTO event (title, description, location, start_time, end_time, capacity, image_url, budget, status, created_at, department_id) VALUES
-- Công nghệ Thông tin (id=1)
('FPT Code Camp 2026 - Spring Boot & SQL Server', 'Workshop 1 ngày hướng dẫn xây dựng REST API với Spring Boot, kết nối SQL Server và deploy lên Docker. Diễn giả từ FPT Software.', 'Hội trường Alpha - FPT Đà Nẵng', '2026-06-08 08:00:00', '2026-06-08 17:00:00', 120, 'https://images.unsplash.com/photo-1517048676732-d65bc937f952?auto=format&fit=crop&w=1200&q=80',   18000000, 'PUBLISHED', NOW() - INTERVAL '45 days', 1),
('Open Day FPT IT 2026',                          'Ngày hội mở cửa khoa CNTT: tham quan lab, gặp gỡ doanh nghiệp đối tác và tư vấn lộ trình nghề nghiệp cho sinh viên năm 1-2.',         'Sảnh sự kiện Beta - FPT HCM',    '2026-06-15 09:00:00', '2026-06-15 16:00:00', 300, 'https://images.unsplash.com/photo-1541339907198-e08756dedf3f?auto=format&fit=crop&w=1200&q=80',    25000000, 'PUBLISHED', NOW() - INTERVAL '40 days', 1),
('AEMS - FPT Hackathon 36h',                      'Cuộc thi lập trình 36 giờ liên tục, chủ đề Smart Campus. Giải nhất 30 triệu + suất thực tập FPT Software.',                            'Lab 3 + Lab 4 - FPT Hà Nội',     '2026-07-04 08:00:00', '2026-07-05 20:00:00', 80,  'https://images.unsplash.com/photo-1591453089816-0fbb971b454c?auto=format&fit=crop&w=1200&q=80', 80000000, 'PUBLISHED', NOW() - INTERVAL '30 days', 1),
-- Kỹ thuật phần mềm (id=2)
('Agile & Scrum Bootcamp', '2 ngày trải nghiệm vai trò Scrum Master, PO, Dev. Có chứng nhận PSM1 mock từ Scrum.org.', 'Phòng 302 - Tòa Beta', '2026-06-20 08:30:00', '2026-06-21 17:00:00', 60, 'https://images.unsplash.com/photo-1504384308090-c894fdcc538d?auto=format&fit=crop&w=1200&q=80', 14000000, 'PUBLISHED', NOW() - INTERVAL '50 days', 2),
('Clean Code & Refactoring Workshop', 'Diễn giả từ FPT Software chia sẻ kinh nghiệm refactor codebase 100k LOC, kèm hands-on lab.', 'Phòng 101 - Tòa Alpha', '2026-06-28 13:30:00', '2026-06-28 17:30:00', 80, 'https://images.unsplash.com/photo-1531482615713-2afd69097998?auto=format&fit=crop&w=1200&q=80', 9000000, 'PUBLISHED', NOW() - INTERVAL '44 days', 2),
-- An toàn thông tin (id=3)
('FPT Cyber CTF Spring 2026', 'Cuộc thi Capture The Flag dạng Jeopardy: web, pwn, crypto, reverse. Mở cho sinh viên toàn FPT.', 'Lab Security - Tòa Gamma', '2026-06-12 08:00:00', '2026-06-12 20:00:00', 100, 'https://images.unsplash.com/photo-1550751827-4bd374c3f58b?auto=format&fit=crop&w=1200&q=80', 35000000, 'PUBLISHED', NOW() - INTERVAL '55 days', 3),
('Secure Coding Lab - OWASP Top 10 2025', 'Hands-on lab fix các lỗi SQLi, XSS, SSRF trong ứng dụng Spring Boot mẫu.', 'Phòng 405 - Tòa Beta', '2026-06-25 13:00:00', '2026-06-25 17:00:00', 50, 'https://images.unsplash.com/photo-1563013544-824ae1b704d3?auto=format&fit=crop&w=1200&q=80', 7000000, 'APPROVED', NOW() - INTERVAL '32 days', 3),
-- Trí tuệ nhân tạo (id=4)
('GenAI Day - LLM cho sinh viên FPT', 'Talkshow ứng dụng LLM, prompt engineering, RAG. Diễn giả từ FPT.AI và OpenAI partner.', 'Hội trường lớn - FPT HCM', '2026-07-10 08:30:00', '2026-07-10 12:00:00', 250, 'https://images.unsplash.com/photo-1620712943543-bcc4688e7485?auto=format&fit=crop&w=1200&q=80', 28000000, 'PUBLISHED', NOW() - INTERVAL '25 days', 4),
('Workshop Computer Vision với PyTorch', 'Hands-on training mô hình nhận diện ảnh, theo dõi đối tượng trên webcam.', 'Lab AI - Tòa Innovation', '2026-07-18 13:00:00', '2026-07-18 17:00:00', 40, 'https://images.unsplash.com/photo-1485827404703-89b55fcc595e?auto=format&fit=crop&w=1200&q=80', 11000000, 'PUBLISHED', NOW() - INTERVAL '20 days', 4),
-- Data Science (id=5)
('Data Analytics Bootcamp - Power BI', '5 buổi tối từ căn bản đến dashboard hoàn chỉnh cho phân tích kinh doanh.', 'Phòng 201 - Tòa Alpha', '2026-06-30 18:00:00', '2026-07-04 21:00:00', 70, 'https://images.unsplash.com/photo-1555255707-c07966088b7b?auto=format&fit=crop&w=1200&q=80', 13000000, 'PUBLISHED', NOW() - INTERVAL '38 days', 5),
('Data Storytelling Talk', 'Cách kể chuyện bằng dữ liệu, từ insight đến slide thuyết trình ấn tượng.', 'Innovation Hub - FPT Đà Nẵng', '2026-07-22 14:00:00', '2026-07-22 17:00:00', 100, 'https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&w=1200&q=80', 6000000, 'APPROVED', NOW() - INTERVAL '15 days', 5),
-- Kinh tế (id=6)
('FPT Economic Forum 2026', 'Diễn đàn kinh tế thường niên: vĩ mô Việt Nam 2026, cơ hội cho sinh viên.', 'Hội trường Alpha - FPT HCM', '2026-08-02 08:30:00', '2026-08-02 12:00:00', 200, 'https://images.unsplash.com/photo-1542744094-3a31f272c490?auto=format&fit=crop&w=1200&q=80', 22000000, 'PUBLISHED', NOW() - INTERVAL '10 days', 6),
('Talkshow: Phân tích thị trường ngành công nghệ', 'Chuyên gia FPT Securities phân tích dòng tiền và cơ hội đầu tư công nghệ.', 'Phòng 501 - Tòa Beta', '2026-07-26 14:00:00', '2026-07-26 17:00:00', 90, 'https://images.unsplash.com/photo-1556761175-b413da4baf72?auto=format&fit=crop&w=1200&q=80', 8000000, 'PUBLISHED', NOW() - INTERVAL '12 days', 6),
-- Marketing (id=7)
('Brand Camp 2026 - Build Your Brand', '4 ngày training thực chiến: persona, brand voice, social content, KPI.', 'Sảnh sự kiện Gamma', '2026-08-10 08:30:00', '2026-08-13 17:00:00', 120, 'https://images.unsplash.com/photo-1542744173-8e7e53415bb0?auto=format&fit=crop&w=1200&q=80', 32000000, 'PUBLISHED', NOW() - INTERVAL '8 days', 7),
('Workshop Performance Ads - Meta & Google', 'Tối ưu chiến dịch quảng cáo từ A-Z, có ngân sách thực tập 5tr/nhóm.', 'Lab Marketing - Tòa Alpha', '2026-07-30 13:00:00', '2026-07-30 17:00:00', 60, 'https://images.unsplash.com/photo-1517245386807-bb43f82c33c4?auto=format&fit=crop&w=1200&q=80', 15000000, 'APPROVED', NOW() - INTERVAL '5 days', 7),
-- Quản trị kinh doanh (id=8)
('FPT Business Case Challenge 2026', 'Vòng loại quốc gia: 3 vòng, đề thực từ FPT Retail và FPT Telecom.', 'Hội trường lớn - FPT Hà Nội', '2026-08-18 08:00:00', '2026-08-18 18:00:00', 150, 'https://images.unsplash.com/photo-1543269664-7eef42226a21?auto=format&fit=crop&w=1200&q=80', 50000000, 'PUBLISHED', NOW() - INTERVAL '7 days', 8),
('Startup Pitching Night', 'Sinh viên pitch ý tưởng trước nhà đầu tư FPT Ventures.', 'Innovation Hub - FPT HCM', '2026-08-22 18:00:00', '2026-08-22 21:30:00', 80, 'https://images.unsplash.com/photo-1543269664-7eef42226a21?auto=format&fit=crop&w=1200&q=80', 12000000, 'PUBLISHED', NOW() - INTERVAL '4 days', 8),
-- Tài chính Ngân hàng (id=9)
('FinTech Day - Mobile Banking & eKYC', 'Chuyên đề công nghệ tài chính: open banking, eKYC, blockchain settlement.', 'Phòng 202 - Tòa Beta', '2026-07-12 08:30:00', '2026-07-12 12:00:00', 100, 'https://images.unsplash.com/photo-1611974789855-9c2a0a7236a3?auto=format&fit=crop&w=1200&q=80', 10000000, 'PUBLISHED', NOW() - INTERVAL '22 days', 9),
('Workshop: Đọc hiểu Báo cáo tài chính', 'Hướng dẫn phân tích BCTC doanh nghiệp niêm yết bằng Excel + Power BI.', 'Phòng 305 - Tòa Alpha', '2026-07-28 18:00:00', '2026-07-28 21:00:00', 70, 'https://images.unsplash.com/photo-1559136555-9303baea8ebd?auto=format&fit=crop&w=1200&q=80', 5000000, 'PUBLISHED', NOW() - INTERVAL '9 days', 9),
-- Thiết kế Mỹ thuật số (id=10)
('UX Research Lab Day', 'Trải nghiệm nghiên cứu UX với người dùng thật, phân tích heuristic và affinity mapping.', 'Design Studio - FPT Đà Nẵng', '2026-06-26 09:00:00', '2026-06-26 16:00:00', 50, 'https://images.unsplash.com/photo-1558655146-d09347e92766?auto=format&fit=crop&w=1200&q=80', 9000000, 'PUBLISHED', NOW() - INTERVAL '28 days', 10),
('Workshop Motion Graphics với After Effects', 'Tạo intro 15s và promo video cho thương hiệu cá nhân.', 'Lab Design - Tòa Innovation', '2026-07-15 13:30:00', '2026-07-15 17:30:00', 40, 'https://images.unsplash.com/photo-1542744095-fcf48d80b0fd?auto=format&fit=crop&w=1200&q=80', 8000000, 'APPROVED', NOW() - INTERVAL '14 days', 10),
-- Thiết kế Đồ họa (id=11)
('Portfolio Review Day - GD/UX 2026', '1-1 review portfolio với art director từ studio đối tác FPT.', 'Studio Media - FPT HCM', '2026-07-20 09:00:00', '2026-07-20 17:00:00', 60, 'https://images.unsplash.com/photo-1561070791-2526d30994b8?auto=format&fit=crop&w=1200&q=80', 6000000, 'PUBLISHED', NOW() - INTERVAL '18 days', 11),
('Typography Workshop - Tiếng Việt trong design', 'Phong cách typography cho tiếng Việt, font dấu và xử lý kerning.', 'Phòng 401 - Tòa Beta', '2026-08-05 14:00:00', '2026-08-05 17:00:00', 50, 'https://images.unsplash.com/photo-1542744095-fcf48d80b0fd?auto=format&fit=crop&w=1200&q=80', 4500000, 'APPROVED', NOW() - INTERVAL '6 days', 11),
-- Truyền thông đa phương tiện (id=12)
('Podcast Production Bootcamp', '2 buổi setup studio, thu - mix - phát hành podcast lên Spotify.', 'Studio Podcast - Tòa Gamma', '2026-07-08 18:00:00', '2026-07-09 21:00:00', 30, 'https://images.unsplash.com/photo-1598618443855-232ee0f819f6?auto=format&fit=crop&w=1200&q=80', 7500000, 'PUBLISHED', NOW() - INTERVAL '26 days', 12),
('Video Production Workshop với Premiere Pro', 'Quay - dựng video TVC 30s, từ ý tưởng đến xuất bản TikTok/YouTube.', 'Studio Media', '2026-07-24 09:00:00', '2026-07-24 17:00:00', 35, 'https://images.unsplash.com/photo-1574717024653-61fd2cf4d44d?auto=format&fit=crop&w=1200&q=80', 9000000, 'APPROVED', NOW() - INTERVAL '11 days', 12),
-- Ngôn ngữ Anh (id=13)
('FPT English Speaking Contest 2026', 'Vòng chung kết cuộc thi nói tiếng Anh toàn FPT, chủ đề "AI & The Future".', 'Hội trường Alpha - FPT HCM', '2026-08-15 14:00:00', '2026-08-15 18:00:00', 200, 'https://images.unsplash.com/photo-1503428593586-e225b39bddfe?auto=format&fit=crop&w=1200&q=80', 18000000, 'PUBLISHED', NOW() - INTERVAL '13 days', 13),
('IELTS Bootcamp 5.5 -> 6.5', '4 tuần học cấp tốc, mock test cuối khóa, mentor 1-1.', 'Phòng 203 - Tòa Beta', '2026-09-01 18:00:00', '2026-09-28 21:00:00', 40, 'https://images.unsplash.com/photo-1481627834876-b7833e8f5570?auto=format&fit=crop&w=1200&q=80', 16000000, 'APPROVED', NOW() - INTERVAL '3 days', 13),
-- Ngôn ngữ Nhật (id=14)
('Japan Day 2026 - Connect Vietnam & Japan', 'Lễ hội văn hóa Nhật: trà đạo, kimono, gian hàng doanh nghiệp Nhật Bản.', 'Sân khấu trung tâm - FPT Hà Nội', '2026-08-25 09:00:00', '2026-08-25 17:00:00', 400, 'https://images.unsplash.com/photo-1542051841857-5f90071e7989?auto=format&fit=crop&w=1200&q=80', 45000000, 'PUBLISHED', NOW() - INTERVAL '2 days', 14),
('JLPT N3 Mock Test Day', 'Thi thử JLPT N3 chuẩn format, có chấm điểm và tư vấn lộ trình.', 'Phòng 102 - Tòa Alpha', '2026-09-10 08:00:00', '2026-09-10 12:30:00', 80, 'https://images.unsplash.com/photo-1503676260728-1c00da094a0b?auto=format&fit=crop&w=1200&q=80', 6000000, 'APPROVED', NOW() - INTERVAL '1 days', 14),
-- Du lịch - Khách sạn (id=15)
('Career Trip - InterContinental Đà Nẵng', 'Tham quan, kiến tập 1 ngày tại khách sạn 5 sao, gặp gỡ HR.', 'InterContinental Sun Peninsula', '2026-08-30 07:00:00', '2026-08-30 17:00:00', 45, 'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80', 14000000, 'PUBLISHED', NOW() - INTERVAL '16 days', 15),
('Workshop F&B Service Excellence', 'Kỹ năng setup bàn fine dining, phục vụ rượu vang và xử lý phàn nàn.', 'Lab F&B - FPT Đà Nẵng', '2026-09-05 09:00:00', '2026-09-05 12:00:00', 35, 'https://images.unsplash.com/photo-1496442226666-8d4d0e62e6e9?auto=format&fit=crop&w=1200&q=80', 5500000, 'APPROVED', NOW() - INTERVAL '21 days', 15),
-- Sự kiện chéo khoa (cross-faculty)
('FPT Career Fair 2026 - Spring',                  'Ngày hội việc làm lớn nhất FPT năm 2026 với 60+ doanh nghiệp, có khu vực phỏng vấn trực tiếp.', 'Sảnh trung tâm - FPT HCM', '2026-09-20 08:00:00', '2026-09-20 17:00:00', 800, 'https://images.unsplash.com/photo-1551836022-d5d88e9218df?auto=format&fit=crop&w=1200&q=80',    120000000, 'PUBLISHED', NOW() - INTERVAL '2 days',  6),
('Lễ tốt nghiệp FPT khóa 2022-2026', 'Lễ tốt nghiệp trang trọng cho gần 2,000 sinh viên FPT tại 3 cơ sở.', 'Nhà thi đấu Đa năng - FPT', '2026-10-15 08:00:00', '2026-10-15 12:00:00', 2000, 'https://images.unsplash.com/photo-1523050854058-8df90110c9f1?auto=format&fit=crop&w=1200&q=80', 250000000, 'PENDING', NOW() - INTERVAL '1 days', 1);

UPDATE event
SET image_urls = image_url
WHERE image_url IS NOT NULL;

UPDATE event e
SET
    organizer = COALESCE(e.organizer, 'FPT University - ' || d.name),
    speakers = COALESCE(e.speakers,
        CASE e.department_id % 5
            WHEN 0 THEN 'Chuyên gia FPT Software; Mentor doanh nghiệp'
            WHEN 1 THEN 'Giảng viên FPT University; Alumni khách mời'
            WHEN 2 THEN 'Đại diện FPT Corporation; Trưởng bộ môn'
            WHEN 3 THEN 'Chuyên gia đối tác; CLB sinh viên'
            ELSE 'Hội đồng chuyên môn; Mentor cộng đồng'
        END),
    support_staff_needed = COALESCE(e.support_staff_needed, 3 + (e.id % 8)),
    google_form_url = CASE WHEN e.id % 4 = 0 THEN 'https://forms.gle/demo-checkin-' || e.id::text ELSE e.google_form_url END,
    checkin_form_id = CASE WHEN e.id % 4 = 0 THEN 'checkin-form-' || e.id::text ELSE e.checkin_form_id END,
    checkin_sheet_id = CASE WHEN e.id % 4 = 0 THEN 'checkin-sheet-' || e.id::text ELSE e.checkin_sheet_id END,
    checkout_form_url = CASE WHEN e.id % 5 = 0 THEN 'https://forms.gle/demo-checkout-' || e.id::text ELSE e.checkout_form_url END,
    checkout_form_id = CASE WHEN e.id % 5 = 0 THEN 'checkout-form-' || e.id::text ELSE e.checkout_form_id END,
    checkout_sheet_id = CASE WHEN e.id % 5 = 0 THEN 'checkout-sheet-' || e.id::text ELSE e.checkout_sheet_id END,
    last_sheet_sync_at = CASE WHEN e.id % 4 = 0 THEN NOW() - make_interval(hours => e.id::int) ELSE e.last_sheet_sync_at END,
    auto_closed_at = CASE WHEN e.end_time < NOW() THEN e.end_time + INTERVAL '15 minutes' ELSE e.auto_closed_at END
FROM department d
WHERE d.id = e.department_id;

-- ---------------------------------------------------------------------
-- EVENT PROPOSALS  (35 - đủ trạng thái PENDING/APPROVED/REVISION_REQUIRED/REJECTED)
-- ---------------------------------------------------------------------
INSERT INTO event_proposal (title, description, proposed_date, status, note, created_at, department_id) VALUES
('Đề xuất: Database Performance Clinic',          'Buổi clinic tối ưu truy vấn SQL Server, đối tượng SE/CS năm 3-4.',                                         '2026-09-10 09:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '30 days', 1),
('Đề xuất: Workshop Microservices với Spring Cloud', 'Tối ưu architecture cho hệ thống lớn, 60 chỗ.',                                                       '2026-09-18 13:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '26 days', 1),
('Đề xuất: Code Review Best Practices',           'Talkshow chia sẻ kinh nghiệm review code hiệu quả.',                                                       '2026-09-25 14:00:00', 'APPROVED',          'Duyệt - cần lên kế hoạch truyền thông.',         NOW() - INTERVAL '24 days', 2),
('Đề xuất: Pair Programming Day',                  'Sinh viên pair programming trong môi trường mô phỏng startup.',                                          '2026-10-02 09:00:00', 'REVISION_REQUIRED', 'Bổ sung ngân sách trà nước + kế hoạch chia nhóm.', NOW() - INTERVAL '20 days', 2),
('Đề xuất: CTF Mid-Year 2026',                     'Cuộc thi CTF giữa năm, mở cho sinh viên IA năm 1-4.',                                                    '2026-10-12 08:00:00', 'APPROVED',          'Duyệt - phối hợp với CLB Security.',             NOW() - INTERVAL '18 days', 3),
('Đề xuất: Pentest Workshop với Kali Linux',       'Hướng dẫn pentest từ recon đến exploitation.',                                                            '2026-10-20 13:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '16 days', 3),
('Đề xuất: LLM Fine-tuning Hands-on',              'Hands-on fine-tune mô hình ngôn ngữ với LoRA.',                                                            '2026-10-25 09:00:00', 'APPROVED',          'Duyệt - cần thuê GPU server.',                   NOW() - INTERVAL '15 days', 4),
('Đề xuất: Reinforcement Learning Bootcamp',       '5 buổi tối, từ Q-learning đến PPO.',                                                                       '2026-11-02 18:00:00', 'REVISION_REQUIRED', 'Cần làm rõ tài liệu và slide từng buổi.',        NOW() - INTERVAL '13 days', 4),
('Đề xuất: Data Engineering Day',                  'Chia sẻ pipeline ETL của FPT Telecom, có demo Airflow.',                                                   '2026-11-08 13:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '12 days', 5),
('Đề xuất: Tableau Champion Contest',              'Cuộc thi dashboard Tableau, 3 vòng.',                                                                       '2026-11-15 09:00:00', 'REJECTED',          'Trùng lịch với FPT Hackathon, dời sang Q1/2027.', NOW() - INTERVAL '10 days', 5),
('Đề xuất: Talkshow ESG cho doanh nghiệp Việt',     'Diễn giả FPT Corporation chia sẻ thực hành ESG.',                                                          '2026-11-20 14:00:00', 'APPROVED',          'Duyệt - mời thêm doanh nghiệp đối tác.',         NOW() - INTERVAL '9 days',  6),
('Đề xuất: Workshop Excel cho Kinh tế',            'Học Excel nâng cao: PowerQuery, Pivot, macro VBA.',                                                        '2026-11-28 18:00:00', 'APPROVED',          'Duyệt - sắp xếp 2 lớp song song.',               NOW() - INTERVAL '8 days',  6),
('Đề xuất: TikTok Marketing Bootcamp',             'Học làm content viral trên TikTok, có giải thưởng.',                                                       '2026-12-02 13:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '7 days',  7),
('Đề xuất: Workshop Personal Branding cho sinh viên', 'Cách build personal brand trên LinkedIn cho fresher.',                                                  '2026-12-08 18:00:00', 'REVISION_REQUIRED', 'Bổ sung diễn giả ngoài FPT.',                    NOW() - INTERVAL '6 days',  7),
('Đề xuất: Business Simulation Day',               'Mô phỏng vận hành startup trong 8 giờ.',                                                                    '2026-12-15 08:00:00', 'APPROVED',          'Duyệt - chuẩn bị 10 case study.',                NOW() - INTERVAL '5 days',  8),
('Đề xuất: Talkshow CEO FPT Retail',                'Giao lưu với CEO về hành trình kinh doanh.',                                                               '2026-12-22 14:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '4 days',  8),
('Đề xuất: Investment Game 2026',                  'Sinh viên giao dịch ảo trong 1 tuần, có giải thưởng.',                                                     '2027-01-10 09:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '3 days',  9),
('Đề xuất: Crypto & DeFi Talk',                    'Talkshow tổng quan DeFi, hợp tác với MoMo.',                                                               '2027-01-18 14:00:00', 'REJECTED',          'Chủ đề cần phê duyệt từ ban học vụ.',            NOW() - INTERVAL '2 days',  9),
('Đề xuất: Figma Mastery Bootcamp',                 '4 buổi học Figma từ cơ bản đến nâng cao.',                                                                 '2027-01-25 18:00:00', 'APPROVED',          'Duyệt - sắp xếp phòng có 40 máy.',               NOW() - INTERVAL '2 days',  10),
('Đề xuất: 3D Modeling với Blender',               'Workshop modeling 3D cho game/animation.',                                                                  '2027-02-02 13:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  10),
('Đề xuất: Adobe Illustrator Battle',              'Cuộc thi vẽ illustrator trong 3 giờ.',                                                                      '2027-02-10 14:00:00', 'APPROVED',          'Duyệt - chuẩn bị máy có Adobe license.',         NOW() - INTERVAL '1 days',  11),
('Đề xuất: Workshop In ấn cho Designer',           'Hiểu quy trình in từ file đến thành phẩm.',                                                                '2027-02-18 09:00:00', 'REVISION_REQUIRED', 'Cần thêm chi phí thuê xưởng in.',                NOW() - INTERVAL '1 days',  11),
('Đề xuất: Content Creator Day',                    'Networking + workshop cho content creator FPT.',                                                            '2027-02-25 14:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  12),
('Đề xuất: Workshop Live Streaming',                'Hướng dẫn setup livestream cho event.',                                                                     '2027-03-05 13:00:00', 'APPROVED',          'Duyệt - mượn thiết bị từ studio.',               NOW() - INTERVAL '1 days',  12),
('Đề xuất: FPT English Got Talent',                 'Cuộc thi tài năng tiếng Anh: hát, kịch, hùng biện.',                                                       '2027-03-15 18:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  13),
('Đề xuất: TOEIC Bootcamp Cấp tốc',                 'Cấp tốc 4 tuần luyện TOEIC từ 500 đến 700+.',                                                              '2027-03-22 18:00:00', 'APPROVED',          'Duyệt - mở 3 lớp.',                              NOW() - INTERVAL '1 days',  13),
('Đề xuất: Japan Career Fair 2026',                 'Hội chợ việc làm Nhật Bản với 20 công ty Nhật.',                                                            '2027-04-02 09:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  14),
('Đề xuất: Workshop văn hóa doanh nghiệp Nhật',     'Buổi chia sẻ về 報連相 (Houren-sou).',                                                                  '2027-04-10 14:00:00', 'APPROVED',          'Duyệt - mời thêm diễn giả từ Nhật.',             NOW() - INTERVAL '1 days',  14),
('Đề xuất: Workshop Wine Tasting',                  'Học cảm vị rượu vang và pairing với món ăn.',                                                               '2027-04-18 18:00:00', 'REVISION_REQUIRED', 'Cần xét lại độ tuổi tham gia.',                  NOW() - INTERVAL '1 days',  15),
('Đề xuất: Career Trip - JW Marriott HN',           'Tham quan và kiến tập tại khách sạn 5 sao.',                                                                '2027-04-26 08:00:00', 'APPROVED',          'Duyệt - giới hạn 40 sinh viên.',                 NOW() - INTERVAL '1 days',  15),
('Đề xuất: Spring Festival FPT 2027',               'Lễ hội xuân toàn FPT, có gian hàng và biểu diễn.',                                                          '2027-02-01 16:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  6),
('Đề xuất: Open Day FPT cho học sinh THPT',         'Mời học sinh THPT đến tham quan FPT.',                                                                      '2027-03-30 08:00:00', 'APPROVED',          'Duyệt - kết hợp với phòng tuyển sinh.',          NOW() - INTERVAL '1 days',  1),
('Đề xuất: Workshop Cloud Native với Kubernetes',   'Hands-on triển khai microservices trên K8s.',                                                               '2027-05-08 09:00:00', 'PENDING',           'Đang chờ hội đồng duyệt.',                       NOW() - INTERVAL '1 days',  1),
('Đề xuất: Workshop Privacy & GDPR',                'Tổng quan về luật bảo vệ dữ liệu cá nhân.',                                                                 '2027-05-15 14:00:00', 'REJECTED',          'Chủ đề trùng với khóa học bắt buộc.',            NOW() - INTERVAL '1 days',  3),
('Đề xuất: Workshop Personal Finance cho sinh viên', 'Quản lý tài chính cá nhân khi mới đi làm.',                                                              '2027-05-22 18:00:00', 'APPROVED',          'Duyệt - mời chuyên gia từ MB Bank.',             NOW() - INTERVAL '1 days',  9);

UPDATE event_proposal
SET
    location = COALESCE(location, 'FPT Campus'),
    capacity = COALESCE(capacity, 100),
    budget = COALESCE(budget, 5000000 + (id % 8) * 1500000),
    image_url = COALESCE(image_url, 'https://images.unsplash.com/photo-1540575467063-027a26d3b38c?auto=format&fit=crop&w=1200&q=80');

UPDATE event_proposal
SET image_urls = image_url
WHERE image_url IS NOT NULL;

UPDATE event_proposal
SET
    proposed_end_date = COALESCE(proposed_end_date, proposed_date + make_interval(hours => (3 + (id % 5))::int)),
    organizer = COALESCE(organizer, 'Đơn vị đề xuất #' || department_id::text),
    support_staff_needed = COALESCE(support_staff_needed, 2 + (id % 6)),
    quiz_payload = COALESCE(quiz_payload,
        '[{"question":"Bạn kỳ vọng điều gì ở sự kiện này?","type":"SHORT_ANSWER"},{"question":"Bạn đã sẵn sàng tham gia đầy đủ?","type":"MULTIPLE_CHOICE","options":["Có","Chưa chắc"]}]');

-- ---------------------------------------------------------------------
-- REGISTRATIONS  (~500 dòng, sinh tự động từ cross-product event x student)
-- ---------------------------------------------------------------------
INSERT INTO registration (registration_date, status, note, event_id, student_id)
SELECT
    e.start_time - make_interval(hours => ((floor(random() * 240))::int + 24)),
    CASE
        WHEN ((e.id * 7 + s.id) % 23) = 0 THEN 'WAITLIST'
        WHEN ((e.id * 5 + s.id) % 41) = 0 THEN 'CANCELLED'
        ELSE 'REGISTERED'
    END,
    CASE
        WHEN ((e.id * 7 + s.id) % 23) = 0 THEN 'Chờ mở thêm slot'
        WHEN ((e.id * 5 + s.id) % 41) = 0 THEN 'Hủy do trùng lịch học'
        ELSE NULL
    END,
    e.id,
    s.id
FROM event e
CROSS JOIN student s
WHERE ((e.id * 13 + s.id * 7) % 4) = 0;

-- ---------------------------------------------------------------------
-- BACKFILL priority_score (xấp xỉ theo M + S + P + T = 100%)
-- ---------------------------------------------------------------------
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
        ELSE 100
    END
    + 0.10 * 70.0
    AS NUMERIC(5,2))
FROM student s, users u, event e, department d
WHERE s.id = r.student_id
  AND u.id = s.user_id
  AND e.id = r.event_id
  AND d.id = e.department_id;

-- ---------------------------------------------------------------------
-- TICKETS  (1 ticket / REGISTERED registration)
-- ---------------------------------------------------------------------
WITH eligible AS (
    SELECT r.id, r.registration_date,
           ROW_NUMBER() OVER (ORDER BY r.id) AS rn
    FROM registration r
    WHERE r.status = 'REGISTERED'
)
INSERT INTO ticket (code, sent_date, registration_id)
SELECT
    'AEMS-TICKET-' || lpad(e.rn::text, 5, '0'),
    e.registration_date + INTERVAL '1 hour',
    e.id
FROM eligible e;

-- ---------------------------------------------------------------------
-- ATTENDANCE  (~70% REGISTERED check-in, có cả ABSENT)
-- ---------------------------------------------------------------------
INSERT INTO attendance (
    checkin_time, mid_verify_time, checkout_time, status,
    participation_score, note, registration_id, event_id, student_id
)
SELECT
    e.start_time + make_interval(mins => (-10 + (floor(random() * 20))::int)),
    CASE WHEN (r.id % 11) = 0 THEN NULL ELSE e.start_time + make_interval(mins => (45 + (r.id % 30))::int) END,
    CASE WHEN (r.id % 11) = 0 THEN NULL ELSE e.end_time + make_interval(mins => (-5 + (r.id % 20))::int) END,
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
    CASE WHEN (r.id % 11) = 0 THEN 'Tự động đánh vắng do không check-in.' ELSE NULL END,
    r.id,
    r.event_id,
    r.student_id
FROM registration r
JOIN event e ON e.id = r.event_id
WHERE r.status = 'REGISTERED'
  AND (r.id % 10) < 7;

INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
SELECT
    e.id,
    'AEMS-IN-' || lpad(e.id::text, 4, '0'),
    'CHECK_IN',
    e.start_time - INTERVAL '30 minutes',
    e.start_time + INTERVAL '45 minutes',
    CASE WHEN e.end_time < NOW() THEN 'EXPIRED' ELSE 'ACTIVE' END
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
SELECT
    e.id,
    'AEMS-MID-' || lpad(e.id::text, 4, '0'),
    'MID_SESSION',
    e.start_time + make_interval(mins => ((EXTRACT(EPOCH FROM (e.end_time - e.start_time)) / 60)::int / 2 - 15)),
    e.start_time + make_interval(mins => ((EXTRACT(EPOCH FROM (e.end_time - e.start_time)) / 60)::int / 2 + 30)),
    CASE WHEN e.end_time < NOW() THEN 'EXPIRED' ELSE 'ACTIVE' END
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

-- ---------------------------------------------------------------------
-- FEEDBACK  (~50% attendance đã xác minh có feedback)
-- ---------------------------------------------------------------------
INSERT INTO feedback (rating, comment, created_at, event_id, student_id)
SELECT
    3 + (a.id % 3),
    CASE (a.id % 7)
        WHEN 0 THEN 'Nội dung rõ ràng, diễn giả nhiệt tình. Sẽ tiếp tục theo dõi các sự kiện sau.'
        WHEN 1 THEN 'Phần demo rất hay nhưng thời gian Q&A hơi ngắn, hy vọng lần sau có thêm.'
        WHEN 2 THEN 'Quy trình check-in nhanh gọn, email gửi ticket tự động rất tiện.'
        WHEN 3 THEN 'Sự kiện hữu ích cho định hướng nghề nghiệp, cảm ơn FPT.'
        WHEN 4 THEN 'Không gian thoải mái, hậu cần chu đáo. Tài liệu nên gửi trước qua email.'
        WHEN 5 THEN 'Hy vọng có thêm các workshop hands-on dài 2-3 ngày kiểu này.'
        ELSE 'Mentor 1-1 rất tận tâm, nội dung đúng nhu cầu sinh viên năm 3.'
    END,
    a.checkin_time + INTERVAL '6 hours',
    r.event_id,
    r.student_id
FROM attendance a
JOIN registration r ON r.id = a.registration_id
WHERE a.status IN ('MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 2) = 0;

-- ---------------------------------------------------------------------
-- QUIZ + EVENT FEEDBACK
-- ---------------------------------------------------------------------
INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, 'Mục tiêu chính của sự kiện "' || e.title || '" là gì?', 'MULTIPLE_CHOICE',
       'Cập nhật kiến thức và thực hành', 'Chỉ điểm danh', 'Bán sản phẩm', 'Thi cuối kỳ', 'A', 2
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, 'Bạn nên làm gì sau khi tham dự workshop/talkshow?', 'MULTIPLE_CHOICE',
       'Hoàn thành feedback và áp dụng nội dung đã học', 'Bỏ qua tài liệu', 'Chỉ lấy ticket', 'Không cần checkout', 'A', 2
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT e.id, 'Điều nào giúp ban tổ chức cải thiện sự kiện tiếp theo?', 'MULTIPLE_CHOICE',
       'Feedback cụ thể, lịch sự', 'Không phản hồi', 'Đăng ký rồi vắng mặt', 'Gửi spam', 'A', 1
FROM event e
WHERE e.status IN ('PUBLISHED', 'APPROVED');

-- Sinh bài nộp quiz cho MỌI sự kiện PUBLISHED/APPROVED có người tham dự (đã check-in),
-- bao phủ toàn bộ event để phần "Phân tích AI từ quiz" luôn có dữ liệu.
INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT
    a.event_id,
    a.student_id,
    CASE WHEN (a.id % 6) = 0 THEN 3 ELSE 5 END,
    COALESCE(a.checkout_time, a.mid_verify_time, a.checkin_time) + INTERVAL '10 minutes'
FROM attendance a
JOIN event e ON e.id = a.event_id
WHERE e.status IN ('PUBLISHED', 'APPROVED')
  AND a.status IN ('CHECKED_IN', 'MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 4) <> 0;

INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT
    qs.id,
    qq.id,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN 'B' ELSE 'A' END,
    NULL,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN FALSE ELSE TRUE END,
    CASE WHEN (qs.id + qq.id) % 6 = 0 THEN 0 ELSE qq.points END,
    qs.submitted_at + make_interval(mins => (qq.id % 5)::int)
FROM quiz_submission qs
JOIN quiz_question qq ON qq.event_id = qs.event_id;

UPDATE quiz_submission qs
SET total_score = agg.score
FROM (
    SELECT submission_id, SUM(score) AS score
    FROM quiz_answer
    GROUP BY submission_id
) agg
WHERE agg.submission_id = qs.id;

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
        WHEN 0 THEN 'Nội dung rõ ràng, diễn giả nhiệt tình. Sẽ tiếp tục theo dõi các sự kiện sau.'
        WHEN 1 THEN 'Phần demo rất hay nhưng thời gian Q&A hơi ngắn, hy vọng lần sau có thêm.'
        WHEN 2 THEN 'Quy trình check-in nhanh gọn, email gửi ticket tự động rất tiện.'
        WHEN 3 THEN 'Sự kiện hữu ích cho định hướng nghề nghiệp, cảm ơn FPT.'
        WHEN 4 THEN 'Không gian thoải mái, hậu cần chu đáo. Tài liệu nên gửi trước qua email.'
        WHEN 5 THEN 'Hy vọng có thêm các workshop hands-on dài 2-3 ngày kiểu này.'
        ELSE 'Mentor 1-1 rất tận tâm, nội dung đúng nhu cầu sinh viên năm 3.'
    END,
    COALESCE(a.checkout_time, a.mid_verify_time, a.checkin_time) + INTERVAL '15 minutes'
FROM attendance a
WHERE a.status IN ('MID_VERIFIED', 'CHECKED_OUT')
  AND (a.id % 2) = 0;

UPDATE attendance a
SET participation_score =
    CASE
        WHEN a2.status = 'ABSENT' THEN 0
        ELSE
            50
            + CASE WHEN a2.mid_verify_time IS NOT NULL THEN 20 ELSE 0 END
            + CASE WHEN qs.id IS NOT NULL THEN 20 ELSE 0 END
            + CASE WHEN ef.id IS NOT NULL THEN 10 ELSE 0 END
    END
FROM attendance a2
LEFT JOIN quiz_submission qs ON qs.event_id = a2.event_id AND qs.student_id = a2.student_id
LEFT JOIN event_feedback ef ON ef.event_id = a2.event_id AND ef.student_id = a2.student_id
WHERE a2.id = a.id;

-- ---------------------------------------------------------------------
-- EMAIL LOG  (xác nhận đăng ký + gửi ticket + báo cáo admin)
-- ---------------------------------------------------------------------
INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT
    u.email,
    'AEMS - Xác nhận đăng ký: ' || e.title,
    'Hệ thống AEMS đã ghi nhận trạng thái đăng ký "' || r.status || '" của bạn cho sự kiện "' || e.title || '" lúc ' || to_char(r.registration_date, 'YYYY-MM-DD HH24:MI:SS') || '.',
    r.registration_date + INTERVAL '5 minutes',
    CASE WHEN (r.id % 37) = 0 THEN 'FAILED' ELSE 'SENT' END,
    u.id,
    r.id,
    e.id
FROM registration r
JOIN student s ON s.id = r.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = r.event_id;

INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT
    u.email,
    'AEMS - Ticket & QR check-in cho ' || e.title,
    'Mã ticket: ' || t.code || '. Vui lòng xuất trình mã này tại quầy check-in của sự kiện "' || e.title || '".',
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

INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, registration_id, event_id)
SELECT
    a.email,
    'AEMS - Báo cáo vận hành tuần ' || k,
    'Tổng hợp user mới, proposal, registration, attendance và feedback của tuần ' || k || '.',
    NOW() - make_interval(days => (7 * k)),
    'SENT',
    a.id, NULL, NULL
FROM (SELECT id, email FROM users WHERE email = 'admin01@fpt.edu.vn' LIMIT 1) a
CROSS JOIN generate_series(1, 20) AS k;

-- ---------------------------------------------------------------------
-- ACTIVITY LOG  (REGISTER_EVENT / CHECK_IN / FEEDBACK / QUIZ / EVENT_FEEDBACK / ADMIN)
-- ---------------------------------------------------------------------
INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'REGISTER_EVENT',
    'Đăng ký sự kiện ' || e.title,
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
    'Check-in ' || e.title || ' (' || a.status || ')',
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
    'Gửi feedback ' || f.rating::text || '/5 cho ' || e.title,
    8,
    f.created_at
FROM feedback f
JOIN student s ON s.id = f.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = f.event_id;

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'QUIZ_SUBMIT',
    'Nộp quiz sau sự kiện ' || e.title || ' - điểm ' || qs.total_score::text,
    qs.total_score::int,
    qs.submitted_at
FROM quiz_submission qs
JOIN student s ON s.id = qs.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = qs.event_id;

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    u.id, 'EVENT_FEEDBACK',
    'Gửi feedback chi tiết cho ' || e.title,
    10,
    ef.submitted_at
FROM event_feedback ef
JOIN student s ON s.id = ef.student_id
JOIN users   u ON u.id = s.user_id
JOIN event   e ON e.id = ef.event_id;

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT
    a.id,
    CASE WHEN j % 2 = 0 THEN 'ADMIN_REPORT' ELSE 'ADMIN_USER' END,
    CASE WHEN j % 2 = 0 THEN 'Xuất báo cáo thống kê dashboard tuần ' || j
         ELSE 'Cập nhật tài khoản hoặc phân quyền lô #' || j END,
    0,
    NOW() - make_interval(hours => (5 * j))
FROM (SELECT id FROM users WHERE email = 'admin01@fpt.edu.vn' LIMIT 1) a
CROSS JOIN generate_series(1, 30) AS j;

-- ---------------------------------------------------------------------
-- KẾT QUẢ
-- ---------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM role)              AS role,
    (SELECT COUNT(*) FROM department)        AS department,
    (SELECT COUNT(*) FROM users)             AS users,
    (SELECT COUNT(*) FROM student)           AS student,
    (SELECT COUNT(*) FROM event)             AS event,
    (SELECT COUNT(*) FROM event_proposal)    AS event_proposal,
    (SELECT COUNT(*) FROM registration)      AS registration,
    (SELECT COUNT(*) FROM ticket)            AS ticket,
    (SELECT COUNT(*) FROM attendance)        AS attendance,
    (SELECT COUNT(*) FROM feedback)          AS feedback,
    (SELECT COUNT(*) FROM attendance_session) AS attendance_session,
    (SELECT COUNT(*) FROM quiz_question)     AS quiz_question,
    (SELECT COUNT(*) FROM quiz_submission)   AS quiz_submission,
    (SELECT COUNT(*) FROM quiz_answer)       AS quiz_answer,
    (SELECT COUNT(*) FROM event_feedback)    AS event_feedback,
    (SELECT COUNT(*) FROM email_log)         AS email_log,
    (SELECT COUNT(*) FROM activity_log)      AS activity_log;
