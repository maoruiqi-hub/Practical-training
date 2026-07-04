import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.LinkedHashMap;
import java.util.Map;

public class RenumberPythonLessons {
    private static final String COURSE_CODE = "1";

    public static void main(String[] args) throws Exception {
        String url = getenv("DB_URL", "jdbc:postgresql://localhost:5432/course_platform");
        String username = getenv("DB_USERNAME", "postgres");
        String password = getenv("DB_PASSWORD", "");

        Map<String, String> mapping = new LinkedHashMap<>();
        mapping.put("18", "2");
        mapping.put("19", "3");
        mapping.put("20", "4");
        mapping.put("21", "5");
        mapping.put("22", "6");
        mapping.put("23", "7");
        mapping.put("24", "8");
        mapping.put("25", "9");
        mapping.put("26", "10");
        mapping.put("27", "11");
        mapping.put("28", "12");
        mapping.put("29", "13");
        mapping.put("30", "14");
        mapping.put("31", "15");
        mapping.put("32", "16");
        mapping.put("33", "17");

        Class.forName("org.postgresql.Driver");
        try (Connection connection = DriverManager.getConnection(url, username, password)) {
            connection.setAutoCommit(false);
            try {
                for (String target : mapping.values()) {
                    ensureNoTargetConflict(connection, target);
                }

                for (Map.Entry<String, String> entry : mapping.entrySet()) {
                    moveReferences(connection, entry.getKey(), "tmp_py_" + entry.getKey());
                }
                for (Map.Entry<String, String> entry : mapping.entrySet()) {
                    moveReferences(connection, "tmp_py_" + entry.getKey(), entry.getValue());
                }

                connection.commit();
            } catch (Exception e) {
                connection.rollback();
                throw e;
            }
        }
    }

    private static void ensureNoTargetConflict(Connection connection, String targetLessonNo) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT course_code FROM lesson WHERE lesson_no = ?")) {
            statement.setString(1, targetLessonNo);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next() && !COURSE_CODE.equals(rs.getString("course_code"))) {
                    throw new IllegalStateException("目标课时编号 " + targetLessonNo + " 已被其他课程使用");
                }
            }
        }
    }

    private static void moveReferences(Connection connection, String from, String to) throws Exception {
        update(connection, "UPDATE lesson SET lesson_no = ? WHERE course_code = ? AND lesson_no = ?", to, COURSE_CODE, from);
        update(connection, "UPDATE knowledge_point SET lesson_no = ? WHERE course_code = ? AND lesson_no = ?", to, COURSE_CODE, from);
        update(connection, "UPDATE question SET lesson_no = ? WHERE course_code = ? AND lesson_no = ?", to, COURSE_CODE, from);
        update(connection, "UPDATE learning_task SET lesson_no = ? WHERE course_code = ? AND lesson_no = ?", to, COURSE_CODE, from);
    }

    private static void update(Connection connection, String sql, String to, String courseCode, String from) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, to);
            statement.setString(2, courseCode);
            statement.setString(3, from);
            statement.executeUpdate();
        }
    }

    private static String getenv(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }
}
