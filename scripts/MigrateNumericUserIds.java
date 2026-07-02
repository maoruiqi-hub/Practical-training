import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class MigrateNumericUserIds {
    private static final String NUMERIC_REGEX = "^[0-9]+$";

    private static class Mapping {
        final String oldId;
        final String newId;
        final String label;

        Mapping(String oldId, String newId, String label) {
            this.oldId = oldId;
            this.newId = newId;
            this.label = label;
        }
    }

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        String driver = env.getProperty("DB_DRIVER", "org.postgresql.Driver");
        String url = required(env, "DB_URL");
        String username = required(env, "DB_USERNAME");
        String password = required(env, "DB_PASSWORD");
        Class.forName(driver);

        try (Connection conn = DriverManager.getConnection(url, username, password)) {
            conn.setAutoCommit(false);
            try {
                List<Mapping> studentMappings = buildMappings(conn, "student", "student_no", "name");
                List<Mapping> teacherMappings = buildMappings(conn, "teacher", "teacher_no", "name");

                System.out.println("Student mappings:");
                printMappings(studentMappings);
                System.out.println("Teacher mappings:");
                printMappings(teacherMappings);

                migrateStudents(conn, studentMappings);
                migrateTeachers(conn, teacherMappings);
                updateSequences(conn);
                verifyNumericReferences(conn);

                conn.commit();
                System.out.println("Migration committed.");
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static Properties loadEnv(Path path) throws IOException {
        Properties props = new Properties();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
            int idx = trimmed.indexOf('=');
            String key = trimmed.substring(0, idx).trim();
            String value = trimmed.substring(idx + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            props.setProperty(key, value);
        }
        return props;
    }

    private static String required(Properties props, String key) {
        String value = props.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static List<Mapping> buildMappings(Connection conn, String table, String idColumn, String labelColumn) throws SQLException {
        int next = maxNumericId(conn, table, idColumn) + 1;
        List<Mapping> mappings = new ArrayList<>();
        String sql = "SELECT " + idColumn + ", " + labelColumn + " FROM " + table
                + " WHERE " + idColumn + " !~ ? ORDER BY " + idColumn;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, NUMERIC_REGEX);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    mappings.add(new Mapping(rs.getString(1), String.valueOf(next++), rs.getString(2)));
                }
            }
        }
        return mappings;
    }

    private static int maxNumericId(Connection conn, String table, String idColumn) throws SQLException {
        String sql = "SELECT COALESCE(MAX((" + idColumn + ")::INTEGER), 0) FROM " + table + " WHERE " + idColumn + " ~ ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, NUMERIC_REGEX);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static void printMappings(List<Mapping> mappings) {
        if (mappings.isEmpty()) {
            System.out.println("  none");
            return;
        }
        for (Mapping mapping : mappings) {
            System.out.println("  " + mapping.oldId + " -> " + mapping.newId + " (" + mapping.label + ")");
        }
    }

    private static void migrateStudents(Connection conn, List<Mapping> mappings) throws SQLException {
        if (mappings.isEmpty()) return;
        Map<String, String> tables = new LinkedHashMap<>();
        tables.put("knowledge_mastery", "student_no");
        tables.put("task_submission", "student_no");
        tables.put("submission_ai_review", "student_no");
        tables.put("submission_answer", "student_no");
        tables.put("knowledge_point_floor_status", "student_id");
        tables.put("student_profile", "student_no");
        tables.put("competency_score", "student_no");
        tables.put("recommendation", "student_no");
        tables.put("achievement", "student_no");
        tables.put("competency_score_history", "student_no");
        tables.put("growth_history", "student_no");
        tables.put("analytics_class_student", "student_id");
        tables.put("analytics_risk_alert", "student_id");
        tables.put("learning_behavior_log", "user_id");
        applyMappings(conn, tables, mappings);
        applyMappings(conn, Map.of("student", "student_no"), mappings);
    }

    private static void migrateTeachers(Connection conn, List<Mapping> mappings) throws SQLException {
        if (mappings.isEmpty()) return;
        Map<String, String> tables = new LinkedHashMap<>();
        tables.put("course", "teacher_no");
        tables.put("analytics_class", "teacher_id");
        tables.put("learning_behavior_log", "user_id");
        applyMappings(conn, tables, mappings);
        applyMappings(conn, Map.of("teacher", "teacher_no"), mappings);
    }

    private static void applyMappings(Connection conn, Map<String, String> tables, List<Mapping> mappings) throws SQLException {
        for (Map.Entry<String, String> table : tables.entrySet()) {
            String sqlType = columnCastType(conn, table.getKey(), table.getValue());
            for (Mapping mapping : mappings) {
                String sql = "UPDATE " + table.getKey() + " SET " + table.getValue()
                        + " = CAST(? AS " + sqlType + ") WHERE " + table.getValue() + "::text = ?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, mapping.newId);
                    ps.setString(2, mapping.oldId);
                    int updated = ps.executeUpdate();
                    if (updated > 0) {
                        System.out.println("  updated " + table.getKey() + "." + table.getValue()
                                + " " + mapping.oldId + " -> " + mapping.newId + ": " + updated);
                    }
                }
            }
        }
    }

    private static String columnCastType(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT data_type "
                + "FROM information_schema.columns "
                + "WHERE table_schema = current_schema() "
                + "AND table_name = ? "
                + "AND column_name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, table);
            ps.setString(2, column);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new SQLException("Column not found: " + table + "." + column);
                String dataType = rs.getString(1);
                if ("integer".equalsIgnoreCase(dataType)) return "INTEGER";
                if ("bigint".equalsIgnoreCase(dataType)) return "BIGINT";
                return "VARCHAR";
            }
        }
    }

    private static void updateSequences(Connection conn) throws SQLException {
        setSequenceIfExists(conn, "student_no_seq", "student", "student_no");
        setSequenceIfExists(conn, "student_student_no_seq", "student", "student_no");
        setSequenceIfExists(conn, "teacher_no_seq", "teacher", "teacher_no");
        setSequenceIfExists(conn, "teacher_teacher_no_seq", "teacher", "teacher_no");
    }

    private static void setSequenceIfExists(Connection conn, String sequenceName, String table, String column) throws SQLException {
        try (PreparedStatement exists = conn.prepareStatement("SELECT to_regclass(?)")) {
            exists.setString(1, sequenceName);
            try (ResultSet rs = exists.executeQuery()) {
                rs.next();
                if (rs.getString(1) == null) return;
            }
        }
        String sql = "SELECT setval('" + sequenceName + "', COALESCE((SELECT MAX(" + column
                + "::INTEGER) FROM " + table + " WHERE " + column + " ~ '^[0-9]+$'), 0) + 1, false)";
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        }
        System.out.println("  sequence updated: " + sequenceName);
    }

    private static void verifyNumericReferences(Connection conn) throws SQLException {
        Map<String, String> references = new LinkedHashMap<>();
        references.put("student", "student_no");
        references.put("knowledge_mastery", "student_no");
        references.put("task_submission", "student_no");
        references.put("submission_ai_review", "student_no");
        references.put("submission_answer", "student_no");
        references.put("knowledge_point_floor_status", "student_id");
        references.put("student_profile", "student_no");
        references.put("competency_score", "student_no");
        references.put("recommendation", "student_no");
        references.put("achievement", "student_no");
        references.put("competency_score_history", "student_no");
        references.put("growth_history", "student_no");
        references.put("analytics_class_student", "student_id");
        references.put("analytics_risk_alert", "student_id");
        references.put("teacher", "teacher_no");
        references.put("course", "teacher_no");
        references.put("analytics_class", "teacher_id");

        int remaining = 0;
        for (Map.Entry<String, String> ref : references.entrySet()) {
            int count = countNonNumeric(conn, ref.getKey(), ref.getValue());
            if (count > 0) {
                System.out.println("  non-numeric remains: " + ref.getKey() + "." + ref.getValue() + " = " + count);
                remaining += count;
            }
        }
        int behaviorLogCount = countNonNumericUserLogs(conn);
        if (behaviorLogCount > 0) {
            System.out.println("  non-numeric remains: learning_behavior_log.user_id = " + behaviorLogCount);
            remaining += behaviorLogCount;
        }
        if (remaining > 0) throw new SQLException("Non-numeric user ids remain: " + remaining);
        System.out.println("All student/teacher id references are numeric.");
    }

    private static int countNonNumeric(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT COUNT(*) FROM " + table + " WHERE " + column + " IS NOT NULL AND " + column + "::text !~ ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, NUMERIC_REGEX);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int countNonNumericUserLogs(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM learning_behavior_log WHERE user_id IS NOT NULL "
                + "AND user_type IN ('student', 'teacher', 'admin') AND user_id::text !~ ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, NUMERIC_REGEX);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }
}
