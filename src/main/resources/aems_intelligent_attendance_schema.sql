-- Intelligent Attendance Verification & Engagement System
-- Optional SQL Server schema reference. The app can also create these columns/tables with spring.jpa.hibernate.ddl-auto=update.

CREATE TABLE attendance_session (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_id BIGINT NOT NULL,
    token VARCHAR(120) NOT NULL,
    session_type VARCHAR(30) NOT NULL,
    created_at DATETIME2 NOT NULL,
    expired_at DATETIME2 NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_attendance_session_event FOREIGN KEY (event_id) REFERENCES event(id)
);

ALTER TABLE attendance ADD mid_verify_time DATETIME2 NULL;
ALTER TABLE attendance ADD checkout_time DATETIME2 NULL;
ALTER TABLE attendance ADD participation_score FLOAT NULL;
ALTER TABLE attendance ADD note NVARCHAR(MAX) NULL;
ALTER TABLE attendance ADD event_id BIGINT NULL;
ALTER TABLE attendance ADD student_id BIGINT NULL;

ALTER TABLE student ADD no_show_count INT NOT NULL DEFAULT 0;
ALTER TABLE student ADD attendance_reputation FLOAT NOT NULL DEFAULT 100;
ALTER TABLE users ADD department_position VARCHAR(30) NULL;

CREATE TABLE quiz_question (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_id BIGINT NOT NULL,
    question_text NVARCHAR(MAX) NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    option_a NVARCHAR(500) NULL,
    option_b NVARCHAR(500) NULL,
    option_c NVARCHAR(500) NULL,
    option_d NVARCHAR(500) NULL,
    correct_answer VARCHAR(20) NULL,
    points INT NOT NULL DEFAULT 1,
    CONSTRAINT fk_quiz_question_event FOREIGN KEY (event_id) REFERENCES event(id)
);

CREATE TABLE quiz_submission (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    total_score FLOAT NOT NULL DEFAULT 0,
    submitted_at DATETIME2 NOT NULL,
    CONSTRAINT uq_quiz_submission_once UNIQUE (event_id, student_id),
    CONSTRAINT fk_quiz_submission_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_quiz_submission_student FOREIGN KEY (student_id) REFERENCES student(id)
);

CREATE TABLE quiz_answer (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    submission_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    selected_answer VARCHAR(20) NULL,
    answer_text NVARCHAR(MAX) NULL,
    is_correct BIT NULL,
    score FLOAT NOT NULL DEFAULT 0,
    submitted_at DATETIME2 NOT NULL,
    CONSTRAINT fk_quiz_answer_submission FOREIGN KEY (submission_id) REFERENCES quiz_submission(id),
    CONSTRAINT fk_quiz_answer_question FOREIGN KEY (question_id) REFERENCES quiz_question(id)
);

CREATE TABLE event_feedback (
    id BIGINT IDENTITY(1,1) PRIMARY KEY,
    event_id BIGINT NOT NULL,
    student_id BIGINT NOT NULL,
    content_rating INT NOT NULL,
    speaker_rating INT NOT NULL,
    organization_rating INT NOT NULL,
    overall_rating INT NOT NULL,
    comment NVARCHAR(MAX) NULL,
    submitted_at DATETIME2 NOT NULL,
    CONSTRAINT uq_event_feedback_once UNIQUE (event_id, student_id),
    CONSTRAINT fk_event_feedback_event FOREIGN KEY (event_id) REFERENCES event(id),
    CONSTRAINT fk_event_feedback_student FOREIGN KEY (student_id) REFERENCES student(id)
);
