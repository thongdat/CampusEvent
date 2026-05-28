package com.example.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@Order(-1000)
public class AemsFeatureSchemaMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AemsFeatureSchemaMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        addColumnIfMissing("student", "no_show_count", "INT NOT NULL DEFAULT 0");
        addColumnIfMissing("student", "attendance_reputation", "FLOAT NOT NULL DEFAULT 100");
        addColumnIfMissing("student", "gender", "NVARCHAR(10) NULL");
        addColumnIfMissing("users", "department_position", "VARCHAR(30) NULL");
        addColumnIfMissing("event", "google_form_url", "NVARCHAR(1000) NULL");
        addColumnIfMissing("event", "speakers", "NVARCHAR(800) NULL");

        addColumnIfMissing("attendance", "mid_verify_time", "DATETIME2 NULL");
        addColumnIfMissing("attendance", "checkout_time", "DATETIME2 NULL");
        addColumnIfMissing("attendance", "participation_score", "FLOAT NULL");
        addColumnIfMissing("attendance", "note", "NVARCHAR(MAX) NULL");
        addColumnIfMissing("attendance", "event_id", "BIGINT NULL");
        addColumnIfMissing("attendance", "student_id", "BIGINT NULL");

        addColumnIfMissing(resolveProposalTable(), "quiz_payload", "NVARCHAR(MAX) NULL");

        executeIfTableMissing("attendance_session",
                "CREATE TABLE attendance_session (" +
                        "id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "event_id BIGINT NOT NULL, " +
                        "token VARCHAR(120) NOT NULL, " +
                        "session_type VARCHAR(30) NOT NULL, " +
                        "created_at DATETIME2 NOT NULL, " +
                        "expired_at DATETIME2 NOT NULL, " +
                        "status VARCHAR(30) NOT NULL)");

        executeIfTableMissing("quiz_question",
                "CREATE TABLE quiz_question (" +
                        "id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "event_id BIGINT NOT NULL, " +
                        "question_text NVARCHAR(MAX) NOT NULL, " +
                        "question_type VARCHAR(30) NOT NULL, " +
                        "option_a NVARCHAR(500) NULL, " +
                        "option_b NVARCHAR(500) NULL, " +
                        "option_c NVARCHAR(500) NULL, " +
                        "option_d NVARCHAR(500) NULL, " +
                        "correct_answer VARCHAR(20) NULL, " +
                        "points INT NOT NULL DEFAULT 1)");

        executeIfTableMissing("quiz_submission",
                "CREATE TABLE quiz_submission (" +
                        "id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "event_id BIGINT NOT NULL, " +
                        "student_id BIGINT NOT NULL, " +
                        "total_score FLOAT NOT NULL DEFAULT 0, " +
                        "submitted_at DATETIME2 NOT NULL)");

        executeIfTableMissing("quiz_answer",
                "CREATE TABLE quiz_answer (" +
                        "id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "submission_id BIGINT NOT NULL, " +
                        "question_id BIGINT NOT NULL, " +
                        "selected_answer VARCHAR(20) NULL, " +
                        "answer_text NVARCHAR(MAX) NULL, " +
                        "is_correct BIT NULL, " +
                        "score FLOAT NOT NULL DEFAULT 0, " +
                        "submitted_at DATETIME2 NOT NULL)");

        executeIfTableMissing("event_feedback",
                "CREATE TABLE event_feedback (" +
                        "id BIGINT IDENTITY(1,1) NOT NULL PRIMARY KEY, " +
                        "event_id BIGINT NOT NULL, " +
                        "student_id BIGINT NOT NULL, " +
                        "content_rating INT NOT NULL, " +
                        "speaker_rating INT NOT NULL, " +
                        "organization_rating INT NOT NULL, " +
                        "overall_rating INT NOT NULL, " +
                        "comment NVARCHAR(MAX) NULL, " +
                        "submitted_at DATETIME2 NOT NULL)");
    }

    private void addColumnIfMissing(String table, String column, String definition) {
        if (table == null) {
            return;
        }
        Integer tableExists = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                table);
        if (tableExists == null || tableExists == 0) {
            return;
        }
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                table,
                column);
        if (count == null || count == 0) {
            jdbcTemplate.execute("ALTER TABLE [" + table + "] ADD " + column + " " + definition);
        }
    }

    private String resolveProposalTable() {
        for (String candidate : new String[]{"eventProposal", "event_proposal", "eventproposal"}) {
            Integer exists = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                    Integer.class,
                    candidate);
            if (exists != null && exists > 0) {
                return candidate;
            }
        }
        return null;
    }

    private void executeIfTableMissing(String table, String sql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?",
                Integer.class,
                table);
        if (count == null || count == 0) {
            jdbcTemplate.execute(sql);
        }
    }
}
