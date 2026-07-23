import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Properties;

/** Seeds the Python course's class roster, lessons, and checked-in video resources. */
public class SeedPythonCourseStructure {
    private static final String COURSE_CODE = "1";
    private static final String TEACHER_ID = "2";
    private static final String SEMESTER = "2025-2026-2";

    private static final class LessonSeed {
        final int no;
        final String slug;
        final String title;
        final String description;

        LessonSeed(int no, String slug, String title, String description) {
            this.no = no;
            this.slug = slug;
            this.title = title;
            this.description = description;
        }
    }

    private static final class ClassSeed {
        final String id;
        final String name;

        ClassSeed(String id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    private static final List<LessonSeed> LESSONS = List.of(
            new LessonSeed(1, "python_intro", "Python 课程导论", "认识 Python 语言特点、开发环境与学习路径。"),
            new LessonSeed(2, "basic_syntax", "Python 基础语法与数据类型", "掌握变量、缩进、注释、基础数据类型和类型转换。"),
            new LessonSeed(3, "operators", "运算符与表达式", "掌握算术、比较、逻辑等运算符及表达式优先级。"),
            new LessonSeed(4, "control_flow", "程序控制结构", "使用条件分支和循环结构组织程序流程。"),
            new LessonSeed(5, "list_tuple", "列表与元组", "使用列表、元组、切片和常用容器操作。"),
            new LessonSeed(6, "dict_set", "字典与集合", "使用字典和集合完成键值处理与去重运算。"),
            new LessonSeed(7, "string_processing", "字符串处理", "掌握字符串切片、格式化、常用方法和基础正则。"),
            new LessonSeed(8, "functions", "函数定义与调用", "定义函数、设计参数与返回值，并理解作用域。"),
            new LessonSeed(9, "modules", "模块与包", "使用模块、包和常用标准库组织代码。"),
            new LessonSeed(10, "file_io", "文件读写", "读写文本、CSV 和 JSON 等常见文件。"),
            new LessonSeed(11, "exceptions", "异常处理", "通过异常捕获和自定义异常提升程序健壮性。"),
            new LessonSeed(12, "oop_basic", "面向对象基础", "理解类、对象、属性和方法的基本建模方式。"),
            new LessonSeed(13, "oop_advanced", "面向对象进阶", "掌握继承、多态和常用高级面向对象特性。"),
            new LessonSeed(14, "data_analysis", "数据分析基础", "使用 Python 完成基础数据处理与分析。"),
            new LessonSeed(15, "web_intro", "Web 开发入门", "认识 Web 请求、路由和基础 Web 应用开发。"),
            new LessonSeed(16, "crawler", "网络爬虫基础", "掌握网页请求、解析和合规采集的基本方法。"),
            new LessonSeed(17, "project_practice", "Python 项目综合实践", "综合运用所学知识完成一个小型 Python 项目。")
    );

    private static final List<ClassSeed> CLASSES = List.of(
            new ClassSeed("7b8d2e3f-9f8a-4ab6-a2cc-101010101001", "软件工程1班"),
            new ClassSeed("7b8d2e3f-9f8a-4ab6-a2cc-101010101002", "软件工程2班")
    );

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(required(env, "DB_DRIVER"));
        try (Connection connection = DriverManager.getConnection(
                required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            connection.setAutoCommit(false);
            try {
                int lessonCount = 0;
                int resourceCount = 0;
                for (LessonSeed lesson : LESSONS) {
                    Path video = videoPath(lesson);
                    if (!Files.isRegularFile(video)) throw new IllegalStateException("Missing video: " + video);
                    upsertLesson(connection, lesson, video);
                    resourceCount += upsertResource(connection, lesson, video);
                    lessonCount++;
                }

                for (ClassSeed classSeed : CLASSES) {
                    upsertClass(connection, classSeed);
                    enrollStoredClassMembers(connection, classSeed);
                }
                connection.commit();
                System.out.println("Seeded lessons=" + lessonCount + ", newResources=" + resourceCount);
                for (ClassSeed classSeed : CLASSES) {
                    System.out.println(classSeed.name + " students=" + countStudents(connection, classSeed.id));
                }
            } catch (Exception exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    private static void upsertLesson(Connection connection, LessonSeed lesson, Path video) throws Exception {
        String resourceUrl = publicUrl(video);
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE lesson SET lesson_title=?, resource_type='video', resource_url=?, description=? "
                        + "WHERE course_code=? AND lesson_no=?")) {
            update.setString(1, lesson.title);
            update.setString(2, resourceUrl);
            update.setString(3, lesson.description);
            update.setString(4, COURSE_CODE);
            update.setString(5, String.valueOf(lesson.no));
            if (update.executeUpdate() > 0) return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO lesson (lesson_no, course_code, lesson_title, resource_type, resource_url, description) "
                        + "VALUES (?, ?, ?, 'video', ?, ?)")) {
            insert.setString(1, String.valueOf(lesson.no));
            insert.setString(2, COURSE_CODE);
            insert.setString(3, lesson.title);
            insert.setString(4, resourceUrl);
            insert.setString(5, lesson.description);
            insert.executeUpdate();
        }
    }

    private static int upsertResource(Connection connection, LessonSeed lesson, Path video) throws Exception {
        String resourceUrl = publicUrl(video);
        try (PreparedStatement exists = connection.prepareStatement(
                "SELECT 1 FROM course_resource WHERE course_code=? AND file_url=?")) {
            exists.setString(1, COURSE_CODE);
            exists.setString(2, resourceUrl);
            try (ResultSet result = exists.executeQuery()) {
                if (result.next()) return 0;
            }
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO course_resource (course_code, title, resource_type, file_url, preview_file_url, "
                        + "preview_status, original_filename, chapter, knowledge_point_id, file_size, uploaded_by, uploaded_at) "
                        + "VALUES (?, ?, 'video', ?, ?, 'ready', ?, ?, ?, ?, ?, ?)")) {
            insert.setString(1, COURSE_CODE);
            insert.setString(2, "第" + lesson.no + "讲 " + lesson.title);
            insert.setString(3, resourceUrl);
            insert.setString(4, resourceUrl.replace("_voice.mp4", "_cover.png"));
            insert.setString(5, video.getFileName().toString());
            insert.setString(6, "第" + lesson.no + "讲");
            insert.setString(7, knowledgePointId(connection, lesson.no));
            insert.setLong(8, Files.size(video));
            insert.setString(9, TEACHER_ID);
            insert.setTimestamp(10, Timestamp.valueOf(LocalDateTime.now()));
            insert.executeUpdate();
        }
        return 1;
    }

    private static void upsertClass(Connection connection, ClassSeed classSeed) throws Exception {
        try (PreparedStatement update = connection.prepareStatement(
                "UPDATE analytics_class SET course_id=?, teacher_id=?, semester=?, updated_at=? WHERE id=?")) {
            update.setString(1, COURSE_CODE);
            update.setString(2, TEACHER_ID);
            update.setString(3, SEMESTER);
            update.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
            update.setString(5, classSeed.id);
            if (update.executeUpdate() > 0) return;
        }
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO analytics_class (id, name, course_id, teacher_id, semester, created_at, updated_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?)")) {
            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            insert.setString(1, classSeed.id);
            insert.setString(2, classSeed.name);
            insert.setString(3, COURSE_CODE);
            insert.setString(4, TEACHER_ID);
            insert.setString(5, SEMESTER);
            insert.setTimestamp(6, now);
            insert.setTimestamp(7, now);
            insert.executeUpdate();
        }
    }

    private static void enrollStoredClassMembers(Connection connection, ClassSeed classSeed) throws Exception {
        try (PreparedStatement insert = connection.prepareStatement(
                "INSERT INTO analytics_class_student (class_id, student_id, enrolled_at) "
                        + "SELECT ?, s.student_no, CURRENT_TIMESTAMP FROM student s "
                        + "WHERE s.class_name=? AND NOT EXISTS (SELECT 1 FROM analytics_class_student acs "
                        + "WHERE acs.class_id=? AND acs.student_id=s.student_no)")) {
            insert.setString(1, classSeed.id);
            insert.setString(2, classSeed.name);
            insert.setString(3, classSeed.id);
            insert.executeUpdate();
        }
    }

    private static int countStudents(Connection connection, String classId) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT COUNT(*) FROM analytics_class_student WHERE class_id=?")) {
            statement.setString(1, classId);
            try (ResultSet result = statement.executeQuery()) {
                result.next();
                return result.getInt(1);
            }
        }
    }

    private static String knowledgePointId(Connection connection, int lessonNo) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(
                "SELECT knowledge_point_id FROM knowledge_point WHERE course_code=? AND lesson_no=? ORDER BY knowledge_point_id LIMIT 1")) {
            statement.setString(1, COURSE_CODE);
            statement.setString(2, String.valueOf(lessonNo));
            try (ResultSet result = statement.executeQuery()) {
                return result.next() ? result.getString(1) : null;
            }
        }
    }

    private static Path videoPath(LessonSeed lesson) {
        return Path.of("resource", "LessonResource", "python",
                String.format("lesson_%02d_%s_voice.mp4", lesson.no, lesson.slug));
    }

    private static String publicUrl(Path video) {
        return "/" + video.toString().replace('\\', '/').replaceFirst("^resource/", "");
    }

    private static Properties loadEnv(Path path) throws IOException {
        Properties env = new Properties();
        for (String line : Files.readAllLines(path)) {
            String trimmed = line.trim();
            if (trimmed.isEmpty() || trimmed.startsWith("#") || !trimmed.contains("=")) continue;
            int separator = trimmed.indexOf('=');
            String value = trimmed.substring(separator + 1).trim();
            if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                value = value.substring(1, value.length() - 1);
            }
            env.setProperty(trimmed.substring(0, separator).trim(), value);
        }
        return env;
    }

    private static String required(Properties env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + key);
        return value;
    }
}
