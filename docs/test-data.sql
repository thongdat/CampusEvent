-- =====================================================================
-- CampusEvent (AEMS) — BỘ TEST DATA NGHIỆP VỤ (tất định, dễ kiểm thử)
-- =====================================================================
-- Mục đích: bổ sung dữ liệu test có giá trị ĐOÁN TRƯỚC ĐƯỢC cho mọi tính năng
-- (đăng ký + waitlist, check-in/attendance, quiz, feedback, phân tích AI, report).
--
-- ĐIỀU KIỆN TIÊN QUYẾT:
--   1. DB đã chạy schema-postgresql.sql (app khởi động 1 lần là có).
--   2. DataSeeder đã chạy (app.seed.enabled=true — mặc định) để có sẵn:
--        - 5 role, 12 department, các user/student (student001..student096@uni.edu.vn).
--   File này KHÔNG tạo user đăng nhập mới (dùng tài khoản đã seed — xem test-data.md).
--
-- CÁCH CHẠY (PowerShell, đã cài psql):
--   psql "postgresql://postgres:postgres@localhost:5432/campus_event" -f docs/test-data.sql
--
-- AN TOÀN: tất cả bản ghi dùng tiền tố [QA-TEST]. Phần CLEANUP ở đầu xoá dữ liệu
--   [QA-TEST] cũ trước khi chèn lại -> chạy lại nhiều lần vẫn sạch, không đụng dữ liệu seed.
-- =====================================================================

BEGIN;

-- ---------------------------------------------------------------------
-- 0) CLEANUP dữ liệu [QA-TEST] cũ (xoá theo thứ tự phụ thuộc khoá ngoại)
-- ---------------------------------------------------------------------
DELETE FROM quiz_answer      WHERE submission_id IN (SELECT id FROM quiz_submission WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%'));
DELETE FROM quiz_submission  WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM quiz_question    WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM event_feedback   WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM feedback         WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM attendance       WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%')
                                OR registration_id IN (SELECT id FROM registration WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%'));
DELETE FROM ticket           WHERE registration_id IN (SELECT id FROM registration WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%'));
DELETE FROM email_log        WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%') OR subject LIKE '[QA-TEST]%';
DELETE FROM attendance_session WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM registration     WHERE event_id IN (SELECT id FROM event WHERE title LIKE '[QA-TEST]%');
DELETE FROM activity_log     WHERE description LIKE '%[QA-TEST]%';
DELETE FROM event            WHERE title LIKE '[QA-TEST]%';
DELETE FROM event_proposal   WHERE title LIKE '[QA-TEST]%';

-- ---------------------------------------------------------------------
-- 1) EVENT PROPOSALS — mỗi trạng thái 1 cái (PENDING / REVISION / APPROVED / REJECTED)
-- ---------------------------------------------------------------------
INSERT INTO event_proposal (title, description, location, capacity, budget, proposed_date, proposed_end_date, organizer, speakers, support_staff_needed, status, note, created_at, quiz_payload, department_id)
SELECT v.title, v.description, v.location, v.capacity, v.budget, v.proposed_date, v.proposed_end_date, v.organizer, v.speakers, v.support, v.status, v.note, now(), v.quiz_payload,
       (SELECT id FROM department WHERE name = 'Công nghệ Thông tin' ORDER BY id LIMIT 1)
FROM (VALUES
    ('[QA-TEST] Proposal PENDING - Workshop Git',     'Đề xuất đang chờ hội đồng duyệt.',          'Phòng 101 - Tòa A', 80,  8000000::numeric, now() + interval '20 days', now() + interval '20 days' + interval '2 hours', 'CLB CNTT', 'Thầy Nguyễn Văn A', 3, 'PENDING',  'Đang chờ hội đồng duyệt.',                NULL),
    ('[QA-TEST] Proposal REVISION - Seminar Cloud',   'Đề xuất bị trả về để bổ sung.',             'Lab 3 - Tòa B',     60,  12000000::numeric, now() + interval '25 days', now() + interval '25 days' + interval '3 hours', 'CLB CNTT', 'Cô Trần Thị B', 4, 'REVISION', 'Cần bổ sung ngân sách & kế hoạch truyền thông.', NULL),
    ('[QA-TEST] Proposal APPROVED - AI Talk',         'Đề xuất đã được duyệt.',                    'Hội trường Alpha',  120, 15000000::numeric, now() + interval '30 days', now() + interval '30 days' + interval '2 hours', 'CLB CNTT', 'Mr. John (AI Lead)', 5, 'APPROVED', 'Đã đủ thông tin, chuyển sang tạo event.', '[{"questionText":"AI là viết tắt của?","questionType":"MULTIPLE_CHOICE","optionA":"Artificial Intelligence","optionB":"Auto Input","optionC":"Apple Inc","optionD":"None","correctAnswer":"A","points":1}]'),
    ('[QA-TEST] Proposal REJECTED - Trùng lịch',      'Đề xuất bị từ chối do trùng lịch hội trường.', 'Hội trường Alpha', 100, 10000000::numeric, now() + interval '15 days', now() + interval '15 days' + interval '2 hours', 'CLB CNTT', 'N/A', 2, 'REJECTED', 'Trùng lịch hội trường, không phù hợp lịch học kỳ.', NULL)
) AS v(title, description, location, capacity, budget, proposed_date, proposed_end_date, organizer, speakers, support, status, note, quiz_payload);

-- ---------------------------------------------------------------------
-- 2) EVENTS — phủ các trạng thái: PUBLISHED (đăng ký), PUBLISHED (hôm nay/check-in),
--    COMPLETED (report/feedback/quiz), COMPLETED (quiz 1 câu), CANCELLED
-- ---------------------------------------------------------------------
INSERT INTO event (title, description, location, start_time, end_time, capacity, status, created_at, budget, organizer, speakers, support_staff_needed, department_id)
SELECT v.title, v.description, v.location, v.start_time, v.end_time, v.capacity, v.status, now(), v.budget, v.organizer, v.speakers, v.support,
       (SELECT id FROM department WHERE name = 'Công nghệ Thông tin' ORDER BY id LIMIT 1)
FROM (VALUES
    ('[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)', 'Sự kiện sắp diễn ra, sức chứa nhỏ để test waitlist.', 'Hội trường Alpha', now() + interval '7 days',  now() + interval '7 days'  + interval '2 hours', 3,   'PUBLISHED', 9000000::numeric,  'CLB CNTT', 'Diễn giả khách mời', 3),
    ('[QA-TEST] Sự kiện HÔM NAY (check-in)',   'Đang diễn ra hôm nay — test QR check-in.',            'Lab 3 - Tòa B',    now() - interval '1 hours',  now() + interval '3 hours',                       100, 'PUBLISHED', 7000000::numeric,  'CLB CNTT', 'Speaker A, Speaker B', 4),
    ('[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)', 'Đã kết thúc — test report, feedback, phân tích AI.',   'Auditorium Beta',  now() - interval '10 days',  now() - interval '10 days' + interval '2 hours',  80,  'COMPLETED', 12000000::numeric, 'CLB CNTT', 'TS. Phạm Văn C, ThS. Lê Thị D', 5),
    ('[QA-TEST] Sự kiện QUIZ 1 CÂU',           'Quiz đúng 1 câu / 1 điểm — test phân tích AI quiz.',  'Phòng 205 - Tòa C', now() - interval '5 days',   now() - interval '5 days'  + interval '2 hours',  50,  'COMPLETED', 5000000::numeric,  'CLB CNTT', 'Mr. Quiz', 2),
    ('[QA-TEST] Sự kiện ĐÃ HUỶ',               'Sự kiện đã huỷ — không cho đăng ký/mời.',             'Sảnh sự kiện',     now() + interval '12 days',  now() + interval '12 days' + interval '2 hours',  60,  'CANCELLED', 6000000::numeric,  'CLB CNTT', 'N/A', 1)
) AS v(title, description, location, start_time, end_time, capacity, status, budget, organizer, speakers, support);

-- Tiện ích đọc id nhanh trong các phần dưới:
--   event:   (SELECT id FROM event WHERE title='[QA-TEST] ...')
--   student: (SELECT s.id FROM student s JOIN users u ON s.user_id=u.id WHERE u.email='studentNNN@uni.edu.vn')

-- ---------------------------------------------------------------------
-- 3) REGISTRATIONS
--    a) Sự kiện ĐĂNG KÝ: 3 REGISTERED (đầy chỗ) + 1 WAITLIST + 1 CANCELLED
-- ---------------------------------------------------------------------
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT now() - interval '2 days', v.status, v.note, v.score,
       (SELECT id FROM event WHERE title = '[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)'),
       (SELECT s.id FROM student s JOIN users u ON s.user_id = u.id WHERE u.email = v.email)
FROM (VALUES
    ('student001@uni.edu.vn', 'REGISTERED', NULL,                88.50),
    ('student002@uni.edu.vn', 'REGISTERED', NULL,                76.00),
    ('student003@uni.edu.vn', 'REGISTERED', NULL,                64.25),
    ('student004@uni.edu.vn', 'WAITLIST',   'Chờ mở thêm slot',  52.00),
    ('student005@uni.edu.vn', 'CANCELLED',  'SV tự huỷ',         40.00)
) AS v(email, status, note, score);

--    b) Sự kiện ĐÃ KẾT THÚC: 4 REGISTERED (để có attendance + feedback)
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT now() - interval '14 days', 'REGISTERED', NULL, v.score,
       (SELECT id FROM event WHERE title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)'),
       (SELECT s.id FROM student s JOIN users u ON s.user_id = u.id WHERE u.email = v.email)
FROM (VALUES
    ('student001@uni.edu.vn', 90.00),
    ('student002@uni.edu.vn', 70.00),
    ('student003@uni.edu.vn', 60.00),
    ('student004@uni.edu.vn', 55.00)
) AS v(email, score);

--    c) Sự kiện QUIZ 1 CÂU: 1 REGISTERED (1 lượt làm quiz đúng -> 100%)
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT now() - interval '7 days', 'REGISTERED', NULL, 80.00,
       (SELECT id FROM event WHERE title = '[QA-TEST] Sự kiện QUIZ 1 CÂU'),
       (SELECT s.id FROM student s JOIN users u ON s.user_id = u.id WHERE u.email = 'student001@uni.edu.vn');

--    d) Sự kiện HÔM NAY: 2 REGISTERED (để test check-in)
INSERT INTO registration (registration_date, status, note, priority_score, event_id, student_id)
SELECT now() - interval '3 days', 'REGISTERED', NULL, 85.00,
       (SELECT id FROM event WHERE title = '[QA-TEST] Sự kiện HÔM NAY (check-in)'),
       (SELECT s.id FROM student s JOIN users u ON s.user_id = u.id WHERE u.email = v.email)
FROM (VALUES ('student001@uni.edu.vn'), ('student002@uni.edu.vn')) AS v(email);

-- ---------------------------------------------------------------------
-- 4) TICKETS — cho các đăng ký REGISTERED của sự kiện ĐĂNG KÝ + ĐÃ KẾT THÚC
-- ---------------------------------------------------------------------
INSERT INTO ticket (code, sent_date, registration_id)
SELECT v.code, now() - interval '1 days', r.id
FROM (VALUES
    ('[QA] reg-pub-1', '[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)', 'student001@uni.edu.vn', 'QA-TICKET-0001'),
    ('[QA] reg-pub-2', '[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)', 'student002@uni.edu.vn', 'QA-TICKET-0002'),
    ('[QA] reg-pub-3', '[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)', 'student003@uni.edu.vn', 'QA-TICKET-0003'),
    ('[QA] reg-done-1','[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)', 'student001@uni.edu.vn', 'QA-TICKET-0011'),
    ('[QA] reg-done-2','[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)', 'student002@uni.edu.vn', 'QA-TICKET-0012')
) AS v(tag, ev_title, email, code)
JOIN event e ON e.title = v.ev_title
JOIN users u ON u.email = v.email
JOIN student s ON s.user_id = u.id
JOIN registration r ON r.event_id = e.id AND r.student_id = s.id;

-- ---------------------------------------------------------------------
-- 5) ATTENDANCE — cho sự kiện ĐÃ KẾT THÚC (COMPLETED / CHECKED_IN / ABSENT)
-- ---------------------------------------------------------------------
INSERT INTO attendance (checkin_time, checkout_time, status, participation_score, note, registration_id, event_id, student_id)
SELECT
    CASE WHEN v.status = 'ABSENT' THEN e.end_time ELSE e.start_time + interval '5 minutes' END,
    CASE WHEN v.status = 'COMPLETED' THEN e.end_time - interval '5 minutes' ELSE NULL END,
    v.status, v.score, v.note, r.id, e.id, s.id
FROM (VALUES
    ('student001@uni.edu.vn', 'COMPLETED',  90.0, 'Tham gia đầy đủ, làm quiz + feedback.'),
    ('student002@uni.edu.vn', 'CHECKED_IN', 40.0, 'Chỉ check-in.'),
    ('student003@uni.edu.vn', 'ABSENT',     0.0,  'Đăng ký nhưng không đến.'),
    ('student004@uni.edu.vn', 'CHECKED_IN', 40.0, 'Chỉ check-in.')
) AS v(email, status, score, note)
JOIN event e ON e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)'
JOIN users u ON u.email = v.email
JOIN student s ON s.user_id = u.id
JOIN registration r ON r.event_id = e.id AND r.student_id = s.id;

-- Attendance cho sự kiện QUIZ 1 CÂU (student001 đã check-in)
INSERT INTO attendance (checkin_time, status, participation_score, note, registration_id, event_id, student_id)
SELECT e.start_time + interval '5 minutes', 'CHECKED_IN', 60.0, 'Check-in + làm quiz.', r.id, e.id, s.id
FROM event e
JOIN users u ON u.email = 'student001@uni.edu.vn'
JOIN student s ON s.user_id = u.id
JOIN registration r ON r.event_id = e.id AND r.student_id = s.id
WHERE e.title = '[QA-TEST] Sự kiện QUIZ 1 CÂU';

-- ---------------------------------------------------------------------
-- 6) QUIZ cho sự kiện ĐÃ KẾT THÚC: 2 trắc nghiệm + 1 tự luận
-- ---------------------------------------------------------------------
INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT (SELECT id FROM event WHERE title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)'),
       v.qtext, v.qtype, v.a, v.b, v.c, v.d, v.correct, v.points
FROM (VALUES
    ('Spring Boot dùng để làm gì?',        'MULTIPLE_CHOICE', 'Xây dựng app Java',  'Vẽ UI', 'Quản lý DB thủ công', 'Không gì cả', 'A', 2),
    ('REST API trả về định dạng phổ biến?', 'MULTIPLE_CHOICE', 'XML',               'JSON',  'CSV',                 'YAML',        'B', 1),
    ('Bạn học được điều gì hôm nay?',       'SHORT_ANSWER',    NULL,                NULL,    NULL,                  NULL,          NULL, 1)
) AS v(qtext, qtype, a, b, c, d, correct, points);

-- Quiz submission + answers cho sự kiện ĐÃ KẾT THÚC
--   student001: trả lời đúng cả 2 câu trắc nghiệm (2 + 1 = 3 điểm)
INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT e.id, s.id, 3.0, e.end_time - interval '10 minutes'
FROM event e JOIN users u ON u.email='student001@uni.edu.vn' JOIN student s ON s.user_id=u.id
WHERE e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)';

--   student002: chỉ đúng câu 2 (0 + 1 = 1 điểm)
INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT e.id, s.id, 1.0, e.end_time - interval '8 minutes'
FROM event e JOIN users u ON u.email='student002@uni.edu.vn' JOIN student s ON s.user_id=u.id
WHERE e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)';

-- Answers của student001 (đúng A, đúng B, tự luận không chấm)
INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT sub.id, q.id,
       CASE q.question_type WHEN 'SHORT_ANSWER' THEN NULL ELSE q.correct_answer END,
       CASE q.question_type WHEN 'SHORT_ANSWER' THEN 'Em học được cách build REST API.' ELSE NULL END,
       CASE q.question_type WHEN 'SHORT_ANSWER' THEN NULL ELSE TRUE END,
       CASE q.question_type WHEN 'SHORT_ANSWER' THEN 0.0 ELSE q.points::double precision END,
       sub.submitted_at
FROM quiz_submission sub
JOIN event e ON e.id = sub.event_id
JOIN users u ON u.email = 'student001@uni.edu.vn'
JOIN student s ON s.user_id = u.id AND s.id = sub.student_id
JOIN quiz_question q ON q.event_id = e.id
WHERE e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)';

-- Answers của student002 (sai câu 1: chọn B; đúng câu 2: chọn B; tự luận)
INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT sub.id, q.id,
       CASE WHEN q.question_type = 'SHORT_ANSWER' THEN NULL ELSE 'B' END,
       CASE WHEN q.question_type = 'SHORT_ANSWER' THEN 'Khá hay ạ.' ELSE NULL END,
       CASE WHEN q.question_type = 'SHORT_ANSWER' THEN NULL ELSE (UPPER(q.correct_answer) = 'B') END,
       CASE WHEN q.question_type = 'SHORT_ANSWER' THEN 0.0
            WHEN UPPER(q.correct_answer) = 'B' THEN q.points::double precision ELSE 0.0 END,
       sub.submitted_at
FROM quiz_submission sub
JOIN event e ON e.id = sub.event_id
JOIN users u ON u.email = 'student002@uni.edu.vn'
JOIN student s ON s.user_id = u.id AND s.id = sub.student_id
JOIN quiz_question q ON q.event_id = e.id
WHERE e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)';

-- ---------------------------------------------------------------------
-- 7) QUIZ cho sự kiện QUIZ 1 CÂU: đúng 1 câu trắc nghiệm / 1 điểm
--    student001 trả lời đúng -> total 1.0 -> AI tính 100% ("Hiểu bài tốt")
-- ---------------------------------------------------------------------
INSERT INTO quiz_question (event_id, question_text, question_type, option_a, option_b, option_c, option_d, correct_answer, points)
SELECT (SELECT id FROM event WHERE title='[QA-TEST] Sự kiện QUIZ 1 CÂU'),
       'Ngôn ngữ chính của Spring Boot?', 'MULTIPLE_CHOICE', 'Java', 'Python', 'C#', 'Go', 'A', 1;

INSERT INTO quiz_submission (event_id, student_id, total_score, submitted_at)
SELECT e.id, s.id, 1.0, e.end_time - interval '15 minutes'
FROM event e JOIN users u ON u.email='student001@uni.edu.vn' JOIN student s ON s.user_id=u.id
WHERE e.title = '[QA-TEST] Sự kiện QUIZ 1 CÂU';

INSERT INTO quiz_answer (submission_id, question_id, selected_answer, answer_text, is_correct, score, submitted_at)
SELECT sub.id, q.id, 'A', NULL, TRUE, 1.0, sub.submitted_at
FROM quiz_submission sub
JOIN event e ON e.id = sub.event_id AND e.title = '[QA-TEST] Sự kiện QUIZ 1 CÂU'
JOIN quiz_question q ON q.event_id = e.id;

-- ---------------------------------------------------------------------
-- 8) FEEDBACK (legacy 1-5) cho sự kiện ĐÃ KẾT THÚC
-- ---------------------------------------------------------------------
INSERT INTO feedback (rating, comment, created_at, event_id, student_id)
SELECT v.rating, v.comment, e.end_time + interval '1 days', e.id, s.id
FROM (VALUES
    ('student001@uni.edu.vn', 5, 'Nội dung rất bổ ích, diễn giả nhiệt tình.'),
    ('student002@uni.edu.vn', 4, 'Hay nhưng thời gian hơi dài.')
) AS v(email, rating, comment)
JOIN event e ON e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)'
JOIN users u ON u.email = v.email
JOIN student s ON s.user_id = u.id;

-- ---------------------------------------------------------------------
-- 9) EVENT_FEEDBACK (checkout, 4 thang điểm) — dữ liệu cho phân tích AI
-- ---------------------------------------------------------------------
INSERT INTO event_feedback (event_id, student_id, content_rating, speaker_rating, organization_rating, overall_rating, comment, submitted_at)
SELECT e.id, s.id, v.c, v.sp, v.o, v.ov, v.comment, e.end_time - interval '2 minutes'
FROM (VALUES
    ('student001@uni.edu.vn', 5, 5, 4, 5, 'Diễn giả truyền cảm hứng, nội dung rõ ràng và bổ ích.'),
    ('student002@uni.edu.vn', 3, 4, 3, 3, 'Phần trình bày ổn nhưng âm thanh hơi nhỏ và thời gian dài, hơi chán.')
) AS v(email, c, sp, o, ov, comment)
JOIN event e ON e.title = '[QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)'
JOIN users u ON u.email = v.email
JOIN student s ON s.user_id = u.id;

-- ---------------------------------------------------------------------
-- 10) ATTENDANCE_SESSION — token QR đang ACTIVE cho sự kiện HÔM NAY (test check-in)
-- ---------------------------------------------------------------------
INSERT INTO attendance_session (event_id, token, session_type, created_at, expired_at, status)
SELECT id, 'QATESTTOKEN0001', 'CHECK_IN', now(), now() + interval '1 days', 'ACTIVE'
FROM event WHERE title = '[QA-TEST] Sự kiện HÔM NAY (check-in)';

-- ---------------------------------------------------------------------
-- 11) EMAIL LOG — 1 SENT + 1 FAILED
-- ---------------------------------------------------------------------
INSERT INTO email_log (to_email, subject, content, sent_at, status, user_id, event_id)
SELECT u.email, '[QA-TEST] Xác nhận đăng ký', 'Bạn đã đăng ký thành công.', now() - interval '1 days', v.status, u.id, e.id
FROM (VALUES
    ('student001@uni.edu.vn', 'SENT'),
    ('student002@uni.edu.vn', 'FAILED')
) AS v(email, status)
JOIN users u ON u.email = v.email
JOIN event e ON e.title = '[QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)';

-- ---------------------------------------------------------------------
-- 12) ACTIVITY LOG — REGISTER_EVENT (+5) và FEEDBACK (+8)
-- ---------------------------------------------------------------------
INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT u.id, 'REGISTER_EVENT', 'Đăng ký [QA-TEST] Sự kiện ĐĂNG KÝ (sức chứa 3)', 5, now() - interval '2 days'
FROM users u WHERE u.email = 'student001@uni.edu.vn';

INSERT INTO activity_log (user_id, activity_type, description, points_earned, created_at)
SELECT u.id, 'FEEDBACK', 'Gửi feedback 5/5 cho [QA-TEST] Sự kiện ĐÃ KẾT THÚC (report)', 8, now() - interval '9 days'
FROM users u WHERE u.email = 'student001@uni.edu.vn';

COMMIT;

-- =====================================================================
-- KIỂM TRA NHANH (chạy riêng nếu muốn):
--   SELECT title, status FROM event WHERE title LIKE '[QA-TEST]%' ORDER BY id;
--   SELECT status, count(*) FROM registration r JOIN event e ON e.id=r.event_id
--     WHERE e.title LIKE '[QA-TEST]%' GROUP BY status;
-- =====================================================================
