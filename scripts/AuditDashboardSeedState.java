import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.util.Properties;

public class AuditDashboardSeedState {
    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(required(env, "DB_DRIVER"));
        try (Connection conn = DriverManager.getConnection(
                required(env, "DB_URL"),
                required(env, "DB_USERNAME"),
                required(env, "DB_PASSWORD"));
             Statement st = conn.createStatement()) {
            query(st, "COURSES", "SELECT course_code, course_name, teacher_no FROM course ORDER BY course_code");
            query(st, "TASKS", "SELECT task_no, course_code, task_name, task_type, deadline, status, allow_late, max_attempts FROM learning_task ORDER BY task_no");
            query(st, "STUDENTS", "SELECT student_no, name, class_name FROM student ORDER BY student_no LIMIT 20");
            query(st, "ASSIGNMENTS", "SELECT task_no, student_no, status, assigned_at FROM task_assignment ORDER BY task_no, student_no LIMIT 40");
            query(st, "SUBMISSIONS_BY_TASK", "SELECT task_no, status, COUNT(*) FROM task_submission GROUP BY task_no, status ORDER BY task_no, status");
            query(st, "LATEST_SUBMISSIONS", "SELECT submission_id, task_no, student_no, submit_time, status, score, is_overdue FROM task_submission ORDER BY submit_time DESC NULLS LAST, submission_id DESC LIMIT 20");
            query(st, "QUESTIONS_SAMPLE", "SELECT question_id, course_code, type, LEFT(stem, 45) AS stem, answer, score FROM question WHERE course_code='1' ORDER BY question_id LIMIT 20");
        }
    }

    private static Properties loadEnv(Path path) throws Exception {
        Properties properties = new Properties();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
            int separator = trimmed.indexOf('=');
            String value = trimmed.substring(separator + 1).trim();
            if ((value.startsWith("\"") && value.endsWith("\""))
                    || (value.startsWith("'") && value.endsWith("'"))) {
                value = value.substring(1, value.length() - 1);
            }
            properties.setProperty(trimmed.substring(0, separator).trim(), value);
        }
        return properties;
    }

    private static String required(Properties properties, String key) {
        String value = properties.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing " + key);
        return value;
    }

    private static void query(Statement st, String title, String sql) throws Exception {
        System.out.println("# " + title);
        try (ResultSet rs = st.executeQuery(sql)) {
            int cols = rs.getMetaData().getColumnCount();
            while (rs.next()) {
                StringBuilder line = new StringBuilder();
                for (int i = 1; i <= cols; i++) {
                    if (i > 1) line.append('\t');
                    line.append(rs.getString(i));
                }
                System.out.println(line);
            }
        }
    }
}
