package com.example.tools;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * One-time data copy from the local SQL Server database to Neon PostgreSQL.
 *
 * Required environment variables:
 *   TARGET_DB_URL, TARGET_DB_USERNAME, TARGET_DB_PASSWORD
 *
 * Optional source overrides:
 *   SOURCE_DB_URL, SOURCE_DB_USERNAME, SOURCE_DB_PASSWORD
 */
public final class SqlServerToPostgresMigration {

    private static final String DEFAULT_SOURCE_URL =
            "jdbc:sqlserver://localhost:1433;databaseName=event_management_db;" +
            "integratedSecurity=true;encrypt=false;trustServerCertificate=true";

    private static final List<TableSpec> TABLES = Arrays.asList(
            table("role"),
            table("department"),
            table("users"),
            table("student"),
            table("event"),
            table("event_proposal", "eventProposal", "eventproposal"),
            table("registration"),
            table("ticket"),
            table("attendance"),
            table("attendance_session"),
            table("feedback"),
            table("event_feedback"),
            table("quiz_question"),
            table("quiz_submission"),
            table("quiz_answer"),
            table("email_log", "emailLog", "emaillog"),
            table("activity_log")
    );

    private SqlServerToPostgresMigration() {
    }

    public static void main(String[] args) throws Exception {
        String sourceUrl = env("SOURCE_DB_URL", DEFAULT_SOURCE_URL);
        String sourceUsername = env("SOURCE_DB_USERNAME", "");
        String sourcePassword = env("SOURCE_DB_PASSWORD", "");

        Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
        if (Arrays.asList(args).contains("--inspect-source")) {
            inspectSource(sourceUrl, sourceUsername, sourcePassword);
            return;
        }

        String targetUrl = requiredEnv("TARGET_DB_URL");
        String targetUsername = requiredEnv("TARGET_DB_USERNAME");
        String targetPassword = requiredEnv("TARGET_DB_PASSWORD");

        Class.forName("org.postgresql.Driver");

        System.out.println("Connecting to SQL Server source...");
        try (Connection source = open(sourceUrl, sourceUsername, sourcePassword);
             Connection target = open(targetUrl, targetUsername, targetPassword)) {
            target.setAutoCommit(false);
            try {
                Map<String, String> sourceTables = tableNames(source, "dbo");
                Map<String, String> targetTables = tableNames(target, "public");
                validateTargetTables(targetTables);
                truncateTarget(target);

                long totalRows = 0;
                for (TableSpec spec : TABLES) {
                    String sourceTable = spec.resolveSource(sourceTables);
                    if (sourceTable == null) {
                        System.out.printf("Skipping %-22s (not present in SQL Server)%n", spec.target);
                        continue;
                    }
                    String targetTable = lookup(targetTables, spec.target);
                    long copied = copyTable(source, target, sourceTable, targetTable);
                    totalRows += copied;
                    System.out.printf("Copied   %-22s %,d rows%n", spec.target, copied);
                }

                resetPostgresSequences(target, targetTables);
                target.commit();
                System.out.printf("Migration completed successfully: %,d rows copied.%n", totalRows);
            } catch (Exception exception) {
                target.rollback();
                throw exception;
            }
        }
    }

    private static void inspectSource(String sourceUrl,
                                      String sourceUsername,
                                      String sourcePassword) throws SQLException {
        try (Connection source = open(sourceUrl, sourceUsername, sourcePassword)) {
            Map<String, String> sourceTables = tableNames(source, "dbo");
            long totalRows = 0;
            for (TableSpec spec : TABLES) {
                String table = spec.resolveSource(sourceTables);
                if (table == null) {
                    System.out.printf("Missing  %s%n", spec.target);
                    continue;
                }
                try (Statement statement = source.createStatement();
                     ResultSet result = statement.executeQuery(
                             "SELECT COUNT(*) FROM " + quoteSqlServer(table))) {
                    result.next();
                    long rows = result.getLong(1);
                    totalRows += rows;
                    System.out.printf("Source   %-22s %,d rows%n", spec.target, rows);
                }
            }
            System.out.printf("SQL Server source is ready: %,d total rows.%n", totalRows);
        }
    }

    private static Connection open(String url, String username, String password) throws SQLException {
        if (username == null || username.trim().isEmpty()) {
            return DriverManager.getConnection(url);
        }
        return DriverManager.getConnection(url, username, password);
    }

    private static void validateTargetTables(Map<String, String> targetTables) {
        List<String> missing = TABLES.stream()
                .map(spec -> spec.target)
                .filter(name -> lookup(targetTables, name) == null)
                .collect(Collectors.toList());
        if (!missing.isEmpty()) {
            throw new IllegalStateException(
                    "Neon schema is incomplete. Missing tables: " + String.join(", ", missing) +
                    ". Start the Render application once so schema-postgresql.sql can run.");
        }
    }

    private static void truncateTarget(Connection target) throws SQLException {
        String names = TABLES.stream()
                .map(spec -> quotePostgres(spec.target))
                .collect(Collectors.joining(", "));
        try (Statement statement = target.createStatement()) {
            // SQL Server source permits duplicate placeholder phone numbers.
            // Remove the old Neon-only constraint so rows can be copied exactly.
            statement.execute("DROP INDEX IF EXISTS ux_users_phone");
            statement.execute("TRUNCATE TABLE " + names + " RESTART IDENTITY CASCADE");
        }
    }

    private static long copyTable(Connection source,
                                  Connection target,
                                  String sourceTable,
                                  String targetTable) throws SQLException {
        Map<String, String> sourceColumns = columnNames(source, "dbo", sourceTable);
        Map<String, String> targetColumns = columnNames(target, "public", targetTable);

        List<ColumnPair> columns = new ArrayList<>();
        for (Map.Entry<String, String> targetColumn : targetColumns.entrySet()) {
            String sourceColumn = sourceColumns.get(targetColumn.getKey());
            if (sourceColumn != null) {
                columns.add(new ColumnPair(sourceColumn, targetColumn.getValue()));
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalStateException("No shared columns for table " + targetTable);
        }

        String selectColumns = columns.stream()
                .map(column -> quoteSqlServer(column.source))
                .collect(Collectors.joining(", "));
        String insertColumns = columns.stream()
                .map(column -> quotePostgres(column.target))
                .collect(Collectors.joining(", "));
        String placeholders = columns.stream().map(column -> "?").collect(Collectors.joining(", "));

        String selectSql = "SELECT " + selectColumns + " FROM " + quoteSqlServer(sourceTable);
        String insertSql = "INSERT INTO " + quotePostgres(targetTable) +
                " (" + insertColumns + ") VALUES (" + placeholders + ")";

        long copied = 0;
        try (Statement select = source.createStatement();
             ResultSet rows = select.executeQuery(selectSql);
             PreparedStatement insert = target.prepareStatement(insertSql)) {
            while (rows.next()) {
                for (int index = 1; index <= columns.size(); index++) {
                    ColumnPair column = columns.get(index - 1);
                    Object value = normalizeValue(targetTable, column.target, rows.getObject(index));
                    insert.setObject(index, value);
                }
                insert.addBatch();
                copied++;
                if (copied % 500 == 0) {
                    insert.executeBatch();
                }
            }
            insert.executeBatch();
        }
        return copied;
    }

    private static Object normalizeValue(String table, String column, Object value) {
        if (value != null) {
            return value;
        }
        String key = table.toLowerCase(Locale.ROOT) + "." + column.toLowerCase(Locale.ROOT);
        switch (key) {
            case "users.total_points":
            case "student.no_show_count":
            case "event.support_staff_needed":
            case "event_proposal.support_staff_needed":
            case "quiz_question.points":
            case "activity_log.points_earned":
                return 0;
            case "student.attendance_reputation":
                return 100.0d;
            case "event.budget":
            case "event_proposal.budget":
                return java.math.BigDecimal.ZERO;
            case "quiz_submission.total_score":
            case "quiz_answer.score":
                return 0.0d;
            default:
                return null;
        }
    }

    private static void resetPostgresSequences(Connection target,
                                               Map<String, String> targetTables) throws SQLException {
        for (TableSpec spec : TABLES) {
            String table = lookup(targetTables, spec.target);
            if (table == null) {
                continue;
            }
            String sql = "SELECT setval(pg_get_serial_sequence(?, 'id'), " +
                    "COALESCE((SELECT MAX(id) FROM " + quotePostgres(table) + "), 1), " +
                    "EXISTS(SELECT 1 FROM " + quotePostgres(table) + "))";
            try (PreparedStatement statement = target.prepareStatement(sql)) {
                statement.setString(1, table);
                statement.execute();
            }
        }
    }

    private static Map<String, String> tableNames(Connection connection, String schema) throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getTables(null, schema, "%", new String[]{"TABLE"})) {
            while (rows.next()) {
                String name = rows.getString("TABLE_NAME");
                names.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return names;
    }

    private static Map<String, String> columnNames(Connection connection,
                                                   String schema,
                                                   String table) throws SQLException {
        Map<String, String> names = new LinkedHashMap<>();
        DatabaseMetaData metadata = connection.getMetaData();
        try (ResultSet rows = metadata.getColumns(null, schema, table, "%")) {
            while (rows.next()) {
                String name = rows.getString("COLUMN_NAME");
                names.put(name.toLowerCase(Locale.ROOT), name);
            }
        }
        return names;
    }

    private static String lookup(Map<String, String> names, String requested) {
        return names.get(requested.toLowerCase(Locale.ROOT));
    }

    private static String quoteSqlServer(String identifier) {
        return "[" + identifier.replace("]", "]]" ) + "]";
    }

    private static String quotePostgres(String identifier) {
        return "\"" + identifier.replace("\"", "\"\"") + "\"";
    }

    private static String env(String name, String defaultValue) {
        String value = System.getenv(name);
        return value == null || value.trim().isEmpty() ? defaultValue : value.trim();
    }

    private static String requiredEnv(String name) {
        String value = env(name, "");
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Missing required environment variable: " + name);
        }
        return value;
    }

    private static TableSpec table(String target, String... aliases) {
        return new TableSpec(target, aliases);
    }

    private static final class TableSpec {
        private final String target;
        private final Set<String> sourceCandidates;

        private TableSpec(String target, String... aliases) {
            this.target = target;
            this.sourceCandidates = new LinkedHashSet<>();
            this.sourceCandidates.add(target);
            this.sourceCandidates.addAll(Arrays.asList(aliases));
        }

        private String resolveSource(Map<String, String> sourceTables) {
            for (String candidate : sourceCandidates) {
                String match = lookup(sourceTables, candidate);
                if (match != null) {
                    return match;
                }
            }
            return null;
        }
    }

    private static final class ColumnPair {
        private final String source;
        private final String target;

        private ColumnPair(String source, String target) {
            this.source = source;
            this.target = target;
        }
    }
}
