import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class BindPythonCourseCover {
    private static final String COURSE_CODE = "1";
    private static final String COVER_URL = "/CourseResource/python.png";

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(env.getProperty("DB_DRIVER", "org.postgresql.Driver"));
        try (Connection conn = DriverManager.getConnection(required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            printCurrent(conn);
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE course SET cover_url = ? WHERE course_code = ?")) {
                ps.setString(1, COVER_URL);
                ps.setString(2, COURSE_CODE);
                int updated = ps.executeUpdate();
                System.out.println("updated=" + updated + ", coverUrl=" + COVER_URL);
            }
            printCurrent(conn);
        }
    }

    private static void printCurrent(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT course_code, course_name, cover_url FROM course WHERE course_code = ?")) {
            ps.setString(1, COURSE_CODE);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    System.out.println("course=" + rs.getString("course_code")
                            + ", name=" + rs.getString("course_name")
                            + ", coverUrl=" + rs.getString("cover_url"));
                } else {
                    System.out.println("course not found: " + COURSE_CODE);
                }
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
