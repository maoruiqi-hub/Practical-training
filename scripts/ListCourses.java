import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Properties;

public class ListCourses {
    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(env.getProperty("DB_DRIVER", "org.postgresql.Driver"));
        try (Connection conn = DriverManager.getConnection(required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"));
             PreparedStatement ps = conn.prepareStatement(
                     "SELECT course_code, course_name, cover_url FROM course ORDER BY course_code::INTEGER");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                System.out.println(rs.getString("course_code") + " | "
                        + rs.getString("course_name") + " | "
                        + rs.getString("cover_url"));
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
