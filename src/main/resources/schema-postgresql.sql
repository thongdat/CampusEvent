CREATE TABLE IF NOT EXISTS role (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(50) NOT NULL UNIQUE,
    description TEXT
);

CREATE TABLE IF NOT EXISTS department (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    description TEXT,
    created_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS users (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255),
    phone VARCHAR(20) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    status BOOLEAN NOT NULL,
    role_id BIGINT NOT NULL REFERENCES role(id),
    otp_code VARCHAR(6),
    otp_expiry TIMESTAMP,
    major VARCHAR(100),
    semester INTEGER,
    total_points INTEGER NOT NULL DEFAULT 0,
    department_position VARCHAR(30) DEFAULT 'STAFF'
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_users_phone
    ON users(phone) WHERE phone IS NOT NULL AND phone <> '';

CREATE TABLE IF NOT EXISTS student (
    id BIGSERIAL PRIMARY KEY,
    student_code VARCHAR(50) NOT NULL UNIQUE,
    major VARCHAR(100),
    year INTEGER,
    no_show_count INTEGER NOT NULL DEFAULT 0,
    attendance_reputation DOUBLE PRECISION NOT NULL DEFAULT 100,
    gender VARCHAR(10),
    user_id BIGINT NOT NULL UNIQUE REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS event (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP NOT NULL,
    capacity INTEGER,
    image_url VARCHAR(500),
    image_urls TEXT,
    google_form_url VARCHAR(1000),
    checkin_form_id VARCHAR(120),
    checkin_sheet_id VARCHAR(120),
    checkout_form_url VARCHAR(1000),
    checkout_form_id VARCHAR(120),
    checkout_sheet_id VARCHAR(120),
    last_sheet_sync_at TIMESTAMP,
    auto_closed_at TIMESTAMP,
    speakers VARCHAR(800),
    organizer VARCHAR(200),
    support_staff_needed INTEGER,
    budget NUMERIC(18,2) NOT NULL DEFAULT 0,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    department_id BIGINT NOT NULL REFERENCES department(id)
);

CREATE TABLE IF NOT EXISTS event_proposal (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    location VARCHAR(200),
    capacity INTEGER,
    image_url VARCHAR(500),
    image_urls TEXT,
    budget NUMERIC(18,2) NOT NULL DEFAULT 0,
    proposed_date TIMESTAMP NOT NULL,
    proposed_end_date TIMESTAMP,
    organizer VARCHAR(200),
    speakers VARCHAR(800),
    support_staff_needed INTEGER,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    created_at TIMESTAMP NOT NULL,
    quiz_payload TEXT,
    department_id BIGINT NOT NULL REFERENCES department(id)
);

CREATE TABLE IF NOT EXISTS registration (
    id BIGSERIAL PRIMARY KEY,
    registration_date TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    note TEXT,
    priority_score NUMERIC(5,2),
    invitation_sent_at TIMESTAMP,
    event_id BIGINT NOT NULL REFERENCES event(id),
    student_id BIGINT NOT NULL REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS ticket (
    id BIGSERIAL PRIMARY KEY,
    code VARCHAR(100) NOT NULL UNIQUE,
    sent_date TIMESTAMP NOT NULL,
    registration_id BIGINT NOT NULL REFERENCES registration(id)
);

CREATE TABLE IF NOT EXISTS attendance (
    id BIGSERIAL PRIMARY KEY,
    checkin_time TIMESTAMP NOT NULL,
    mid_verify_time TIMESTAMP,
    checkout_time TIMESTAMP,
    status VARCHAR(50) NOT NULL,
    participation_score DOUBLE PRECISION,
    note TEXT,
    registration_id BIGINT NOT NULL REFERENCES registration(id),
    event_id BIGINT REFERENCES event(id),
    student_id BIGINT REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS attendance_session (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    token VARCHAR(120) NOT NULL,
    session_type VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    expired_at TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS feedback (
    id BIGSERIAL PRIMARY KEY,
    rating INTEGER,
    comment TEXT,
    created_at TIMESTAMP NOT NULL,
    event_id BIGINT NOT NULL REFERENCES event(id),
    student_id BIGINT NOT NULL REFERENCES student(id)
);

CREATE TABLE IF NOT EXISTS event_feedback (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    student_id BIGINT NOT NULL REFERENCES student(id),
    content_rating INTEGER NOT NULL,
    speaker_rating INTEGER NOT NULL,
    organization_rating INTEGER NOT NULL,
    overall_rating INTEGER NOT NULL,
    comment TEXT,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz_question (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    question_text TEXT NOT NULL,
    question_type VARCHAR(30) NOT NULL,
    option_a VARCHAR(500),
    option_b VARCHAR(500),
    option_c VARCHAR(500),
    option_d VARCHAR(500),
    correct_answer VARCHAR(20),
    points INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS quiz_submission (
    id BIGSERIAL PRIMARY KEY,
    event_id BIGINT NOT NULL REFERENCES event(id),
    student_id BIGINT NOT NULL REFERENCES student(id),
    total_score DOUBLE PRECISION NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS quiz_answer (
    id BIGSERIAL PRIMARY KEY,
    submission_id BIGINT NOT NULL REFERENCES quiz_submission(id),
    question_id BIGINT NOT NULL REFERENCES quiz_question(id),
    selected_answer VARCHAR(20),
    answer_text TEXT,
    is_correct BOOLEAN,
    score DOUBLE PRECISION NOT NULL DEFAULT 0,
    submitted_at TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS email_log (
    id BIGSERIAL PRIMARY KEY,
    to_email VARCHAR(100) NOT NULL,
    subject VARCHAR(200) NOT NULL,
    content TEXT,
    sent_at TIMESTAMP NOT NULL,
    status VARCHAR(50) NOT NULL,
    user_id BIGINT REFERENCES users(id),
    registration_id BIGINT REFERENCES registration(id),
    event_id BIGINT REFERENCES event(id)
);

CREATE TABLE IF NOT EXISTS activity_log (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL REFERENCES users(id),
    activity_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    points_earned INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL
);
