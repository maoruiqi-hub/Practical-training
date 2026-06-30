import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

public class DeleteDataStructureCourse {
    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(env.getProperty("DB_DRIVER", "org.postgresql.Driver"));
        try (Connection conn = DriverManager.getConnection(required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            conn.setAutoCommit(false);
            try {
                List<String> courseCodes = findTargetCourses(conn);
                if (courseCodes.isEmpty()) {
                    System.out.println("No data-structure course found.");
                    conn.rollback();
                    return;
                }
                Set<String> tables = listTables(conn);
                Map<String, String> columnTypes = listColumnTypes(conn);
                for (String courseCode : courseCodes) {
                    deleteCourseData(conn, tables, columnTypes, courseCode);
                }
                conn.commit();
                System.out.println("Deleted courses: " + courseCodes);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static List<String> findTargetCourses(Connection conn) throws Exception {
        List<String> codes = new ArrayList<>();
        String sql = "SELECT course_code, course_name FROM course "
                + "WHERE course_code <> '1' AND (course_name LIKE '%数据结构%' OR course_name LIKE '%算法%') "
                + "ORDER BY course_code";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                String code = rs.getString("course_code");
                System.out.println("target course=" + code + ", name=" + rs.getString("course_name"));
                codes.add(code);
            }
        }
        return codes;
    }

    private static void deleteCourseData(Connection conn, Set<String> tables, Map<String, String> columnTypes, String courseCode) throws Exception {
        List<String> taskNos = queryIds(conn, "SELECT task_no FROM learning_task WHERE course_code = ?", courseCode);
        List<String> examIds = queryIds(conn, "SELECT exam_id FROM exam WHERE course_code = ?", courseCode);
        List<String> questionIds = queryIds(conn, "SELECT question_id FROM question WHERE course_code = ?", courseCode);
        List<String> abilityPointIds = queryIds(conn, "SELECT ability_point_id FROM ability_point WHERE course_code = ?", courseCode);
        List<String> classIds = queryIds(conn, "SELECT id FROM analytics_class WHERE course_id = ?", courseCode);

        for (String examId : examIds) {
            delete(conn, tables, columnTypes, "exam_question", "exam_id", examId);
        }
        for (String questionId : questionIds) {
            delete(conn, tables, columnTypes, "task_question", "question_id", questionId);
        }
        for (String taskNo : taskNos) {
            List<String> submissionIds = queryIds(conn, "SELECT submission_id FROM task_submission WHERE task_no = ?", taskNo);
            for (String submissionId : submissionIds) {
            delete(conn, tables, columnTypes, "submission_ai_review", "submission_id", submissionId);
            delete(conn, tables, columnTypes, "submission_answer", "submission_id", submissionId);
            }
            delete(conn, tables, columnTypes, "submission_ai_review", "task_no", taskNo);
            delete(conn, tables, columnTypes, "submission_answer", "task_no", taskNo);
            delete(conn, tables, columnTypes, "task_submission", "task_no", taskNo);
            delete(conn, tables, columnTypes, "task_question", "task_no", taskNo);
            delete(conn, tables, columnTypes, "learning_behavior_log", "task_no", taskNo);
        }
        for (String abilityPointId : abilityPointIds) {
            delete(conn, tables, columnTypes, "ability_knowledge_point", "ability_point_id", abilityPointId);
        }
        for (String classId : classIds) {
            delete(conn, tables, columnTypes, "analytics_class_student", "class_id", classId);
            delete(conn, tables, columnTypes, "analytics_report", "class_id", classId);
            delete(conn, tables, columnTypes, "analytics_teaching_suggestion", "class_id", classId);
        }

        delete(conn, tables, columnTypes, "analytics_risk_alert", "course_id", courseCode);
        delete(conn, tables, columnTypes, "analytics_teaching_suggestion", "course_id", courseCode);
        delete(conn, tables, columnTypes, "analytics_class", "course_id", courseCode);
        delete(conn, tables, columnTypes, "knowledge_point_floor_status", "course_id", courseCode);
        delete(conn, tables, columnTypes, "course_game_config", "course_id", courseCode);
        delete(conn, tables, columnTypes, "growth_history", "course_code", courseCode);
        delete(conn, tables, columnTypes, "competency_score_history", "course_code", courseCode);
        delete(conn, tables, columnTypes, "achievement", "course_code", courseCode);
        delete(conn, tables, columnTypes, "recommendation", "course_code", courseCode);
        delete(conn, tables, columnTypes, "competency_score", "course_code", courseCode);
        delete(conn, tables, columnTypes, "student_profile", "course_code", courseCode);
        delete(conn, tables, columnTypes, "knowledge_mastery", "course_code", courseCode);
        delete(conn, tables, columnTypes, "knowledge_extraction_candidate", "course_code", courseCode);
        delete(conn, tables, columnTypes, "ability_point", "course_code", courseCode);
        delete(conn, tables, columnTypes, "knowledge_relation", "course_code", courseCode);
        delete(conn, tables, columnTypes, "course_resource", "course_code", courseCode);
        delete(conn, tables, columnTypes, "question", "course_code", courseCode);
        delete(conn, tables, columnTypes, "exam", "course_code", courseCode);
        delete(conn, tables, columnTypes, "learning_task", "course_code", courseCode);
        delete(conn, tables, columnTypes, "lesson", "course_code", courseCode);
        delete(conn, tables, columnTypes, "knowledge_point", "course_code", courseCode);
        delete(conn, tables, columnTypes, "course", "course_code", courseCode);
    }

    private static Set<String> listTables(Connection conn) throws Exception {
        Set<String> tables = new HashSet<>();
        String sql = "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) tables.add(rs.getString(1));
        }
        return tables;
    }

    private static Map<String, String> listColumnTypes(Connection conn) throws Exception {
        Map<String, String> types = new HashMap<>();
        String sql = "SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public'";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                types.put(rs.getString(1) + "." + rs.getString(2), rs.getString(3));
            }
        }
        return types;
    }

    private static List<String> queryIds(Connection conn, String sql, String value) throws Exception {
        List<String> ids = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, value);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    private static void delete(Connection conn, Set<String> tables, Map<String, String> columnTypes, String table, String column, String value) throws Exception {
        if (!tables.contains(table)) {
            System.out.println("skip missing table " + table);
            return;
        }
        String sql = "DELETE FROM " + table + " WHERE " + column + " = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            String type = columnTypes.get(table + "." + column);
            if (("integer".equals(type) || "bigint".equals(type) || "smallint".equals(type)) && value.matches("\\d+")) {
                ps.setLong(1, Long.parseLong(value));
            } else {
                ps.setString(1, value);
            }
            int count = ps.executeUpdate();
            if (count > 0) {
                System.out.println("deleted " + table + "." + column + "=" + value + " count=" + count);
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
}
