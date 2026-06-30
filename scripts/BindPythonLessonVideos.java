import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.List;
import java.util.Properties;

public class BindPythonLessonVideos {
    private static final String COURSE_CODE = "1";

    private static class LessonVideo {
        final int lessonNo;
        final String slug;

        LessonVideo(int lessonNo, String slug) {
            this.lessonNo = lessonNo;
            this.slug = slug;
        }
    }

    private static final List<LessonVideo> VIDEOS = List.of(
            new LessonVideo(1, "python_intro"),
            new LessonVideo(2, "basic_syntax"),
            new LessonVideo(3, "operators"),
            new LessonVideo(4, "control_flow"),
            new LessonVideo(5, "list_tuple"),
            new LessonVideo(6, "dict_set"),
            new LessonVideo(7, "string_processing"),
            new LessonVideo(8, "functions"),
            new LessonVideo(9, "modules"),
            new LessonVideo(10, "file_io"),
            new LessonVideo(11, "exceptions"),
            new LessonVideo(12, "oop_basic"),
            new LessonVideo(13, "oop_advanced"),
            new LessonVideo(14, "data_analysis"),
            new LessonVideo(15, "web_intro"),
            new LessonVideo(16, "crawler"),
            new LessonVideo(17, "project_practice")
    );

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(env.getProperty("DB_DRIVER", "org.postgresql.Driver"));
        try (Connection conn = DriverManager.getConnection(required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            conn.setAutoCommit(false);
            try {
                int updated = bindVideos(conn);
                conn.commit();
                System.out.println("Updated lesson videos: " + updated);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            }
        }
    }

    private static int bindVideos(Connection conn) throws Exception {
        String sql = "UPDATE lesson SET resource_type = 'video', resource_url = ? "
                + "WHERE course_code = ? AND lesson_no = ?";
        int updated = 0;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (LessonVideo video : VIDEOS) {
                String stem = String.format("lesson_%02d_%s_voice.mp4", video.lessonNo, video.slug);
                ps.setString(1, "/LessonResource/python/" + stem);
                ps.setString(2, COURSE_CODE);
                ps.setString(3, String.valueOf(video.lessonNo));
                updated += ps.executeUpdate();
            }
        }
        return updated;
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
