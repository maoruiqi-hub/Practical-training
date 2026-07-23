import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.util.Properties;

/** Updates the local development demo records without changing account credentials. */
public class LocalizeDemoData {
    public static void main(String[] args) throws Exception {
        Properties env = new Properties();
        try (FileInputStream input = new FileInputStream("backend/.env")) {
            env.load(input);
        }

        Class.forName(required(env, "DB_DRIVER"));
        try (Connection connection = DriverManager.getConnection(
                required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            update(connection, "UPDATE teacher SET name=?, college=?, major=? WHERE username=?",
                    "教务管理员", "教务处", "教育技术", "admin");
            update(connection, "UPDATE teacher SET name=?, college=?, major=? WHERE username=?",
                    "李明", "软件学院", "软件工程", "liming");
            update(connection, "UPDATE student SET name=?, college=?, class_name=? WHERE username=?",
                    "张三", "软件学院", "软件工程2班", "zhangsan");
            update(connection, "UPDATE course SET course_name=?, teacher=?, description=? WHERE course_code=?",
                    "Python 程序设计", "李明", "面向软件工程专业的 Python 编程基础课程", "1");
            update(connection, "UPDATE lesson SET lesson_title=?, description=? WHERE course_code=? AND lesson_no=?",
                    "Python 课程导论", "Python 语言概览与开发环境搭建", "1", "1");
        }
        System.out.println("Demo teacher, student, and course labels localized.");
    }

    private static void update(Connection connection, String sql, String... values) throws Exception {
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int index = 0; index < values.length; index++) statement.setString(index + 1, values[index]);
            statement.executeUpdate();
        }
    }

    private static String required(Properties env, String key) {
        String value = env.getProperty(key);
        if (value == null || value.isBlank()) throw new IllegalStateException("Missing " + key);
        if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
            value = value.substring(1, value.length() - 1);
        }
        return value;
    }
}
