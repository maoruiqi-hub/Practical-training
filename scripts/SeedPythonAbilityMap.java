import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;

public class SeedPythonAbilityMap {
    private static final String COURSE_CODE = "1";

    private static class AbilitySeed {
        final String id;
        final String name;
        final String description;
        final List<String> lessonNos;

        AbilitySeed(String id, String name, String description, String... lessonNos) {
            this.id = id;
            this.name = name;
            this.description = description;
            this.lessonNos = Arrays.asList(lessonNos);
        }
    }

    private static class Student {
        final int no;
        final String name;

        Student(int no, String name) {
            this.no = no;
            this.name = name;
        }
    }

    private static final List<AbilitySeed> ABILITIES = List.of(
            new AbilitySeed("9001", "Python编程基础能力", "能运用Python基础语法、表达式和流程控制完成基础程序逻辑。", "1", "2", "3", "4"),
            new AbilitySeed("9002", "数据结构与字符串处理能力", "能合理使用内置数据结构和字符串工具组织、处理数据。", "5", "6", "7"),
            new AbilitySeed("9003", "函数、模块与异常处理能力", "能使用函数、模块和异常处理组织可靠的Python程序。", "8", "9", "11"),
            new AbilitySeed("9004", "面向对象编程能力", "能使用类、对象、继承和多态完成领域建模。", "12", "13"),
            new AbilitySeed("9005", "文件与数据处理能力", "能读写常用文件格式并完成基础数据分析。", "10", "14"),
            new AbilitySeed("9006", "Web与爬虫开发能力", "能完成基础Web应用和网络数据采集任务。", "15", "16"),
            new AbilitySeed("9007", "综合项目实战能力", "能综合数据分析、Web、爬虫和项目工程能力完成完整Python应用。", "14", "15", "16", "17")
    );

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(env.getProperty("DB_DRIVER", "org.postgresql.Driver"));
        try (Connection conn = DriverManager.getConnection(required(env, "DB_URL"), required(env, "DB_USERNAME"), required(env, "DB_PASSWORD"))) {
            conn.setAutoCommit(false);
            try {
                List<Student> students = listStudents(conn);
                Map<String, String> abilityIds = upsertAbilities(conn);
                bindKnowledgePoints(conn, abilityIds);
                conn.commit();
                System.out.println("Seeded Python ability map:");
                System.out.println("  abilityPoints=" + ABILITIES.size());
                System.out.println("  students=" + students.size());
                System.out.println("  competencyScores=not seeded (derived from knowledge mastery)");
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

    private static List<Student> listStudents(Connection conn) throws SQLException {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT student_no, name FROM student WHERE student_no ~ '^[0-9]+$' ORDER BY student_no::INTEGER";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) students.add(new Student(Integer.parseInt(rs.getString(1)), rs.getString(2)));
        }
        return students;
    }

    private static Map<String, String> upsertAbilities(Connection conn) throws SQLException {
        Map<String, String> resolvedIds = new LinkedHashMap<>();
        String findSql = "SELECT ability_point_id FROM ability_point "
                + "WHERE course_code = ? AND LOWER(TRIM(name)) = LOWER(TRIM(?))";
        String insertSql = "INSERT INTO ability_point (ability_point_id, course_code, name, description) VALUES (?, ?, ?, ?)";
        String updateSql = "UPDATE ability_point SET name = ?, description = ? WHERE ability_point_id = ?";
        try (PreparedStatement find = conn.prepareStatement(findSql);
             PreparedStatement insert = conn.prepareStatement(insertSql);
             PreparedStatement update = conn.prepareStatement(updateSql)) {
            for (AbilitySeed ability : ABILITIES) {
                find.setString(1, COURSE_CODE);
                find.setString(2, ability.name);
                String resolvedId = null;
                try (ResultSet rs = find.executeQuery()) {
                    if (rs.next()) resolvedId = rs.getString(1);
                }
                if (resolvedId == null) {
                    resolvedId = ability.id;
                    insert.setString(1, resolvedId);
                    insert.setString(2, COURSE_CODE);
                    insert.setString(3, ability.name);
                    insert.setString(4, ability.description);
                    insert.executeUpdate();
                } else {
                    update.setString(1, ability.name);
                    update.setString(2, ability.description);
                    update.setString(3, resolvedId);
                    update.executeUpdate();
                }
                resolvedIds.put(ability.id, resolvedId);
            }
        }
        return resolvedIds;
    }

    private static void bindKnowledgePoints(Connection conn, Map<String, String> resolvedIds) throws SQLException {
        long nextMappingId = nextNumericId(conn, "ability_knowledge_point", "id");
        String sql = "INSERT INTO ability_knowledge_point (id, ability_point_id, knowledge_point_id) "
                + "SELECT ?, ?, ? "
                + "WHERE NOT EXISTS ("
                + "SELECT 1 FROM ability_knowledge_point WHERE ability_point_id = ? AND knowledge_point_id = ?"
                + ")";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (AbilitySeed ability : ABILITIES) {
                String abilityPointId = resolvedIds.get(ability.id);
                for (String knowledgePointId : knowledgePointIdsByLesson(conn, ability.lessonNos)) {
                    ps.setString(1, String.valueOf(nextMappingId++));
                    ps.setString(2, abilityPointId);
                    ps.setString(3, knowledgePointId);
                    ps.setString(4, abilityPointId);
                    ps.setString(5, knowledgePointId);
                    ps.addBatch();
                }
            }
            ps.executeBatch();
        }
    }

    private static List<String> knowledgePointIdsByLesson(Connection conn, List<String> lessonNos) throws SQLException {
        List<String> ids = new ArrayList<>();
        String placeholders = String.join(",", java.util.Collections.nCopies(lessonNos.size(), "?"));
        String sql = "SELECT knowledge_point_id FROM knowledge_point WHERE course_code = ? AND lesson_no IN ("
                + placeholders + ") ORDER BY lesson_no::INTEGER, knowledge_point_id";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, COURSE_CODE);
            for (int i = 0; i < lessonNos.size(); i++) ps.setString(i + 2, lessonNos.get(i));
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) ids.add(rs.getString(1));
            }
        }
        return ids;
    }

    private static void seedCompetencyScores(Connection conn, List<Student> students) throws SQLException {
        clearSeededCompetencyData(conn);
        long nextScoreId = nextNumericId(conn, "competency_score", "id");
        long nextHistoryId = nextNumericId(conn, "competency_score_history", "id");
        String insertScore = "INSERT INTO competency_score "
                + "(id, student_no, course_code, ability_point_id, ability_point_name, score, last_updated) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?)";
        String insertHistory = "INSERT INTO competency_score_history "
                + "(id, student_no, course_code, ability_point_id, old_score, new_score, change_reason, changed_at) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement scorePs = conn.prepareStatement(insertScore);
             PreparedStatement historyPs = conn.prepareStatement(insertHistory)) {
            for (Student student : students) {
                for (int i = 0; i < ABILITIES.size(); i++) {
                    AbilitySeed ability = ABILITIES.get(i);
                    int score = scoreFor(student.no, i);
                    int oldScore = Math.max(20, score - historyDelta(student.no, i));
                    Timestamp now = Timestamp.valueOf(LocalDateTime.now().minusMinutes((long) student.no + i));
                    Timestamp oldTime = Timestamp.valueOf(LocalDateTime.now().minusDays(6).minusHours(i));

                    scorePs.setString(1, String.valueOf(nextScoreId++));
                    scorePs.setInt(2, student.no);
                    scorePs.setInt(3, Integer.parseInt(COURSE_CODE));
                    scorePs.setString(4, ability.id);
                    scorePs.setString(5, ability.name);
                    scorePs.setInt(6, score);
                    scorePs.setTimestamp(7, now);
                    scorePs.addBatch();

                    historyPs.setString(1, String.valueOf(nextHistoryId++));
                    historyPs.setInt(2, student.no);
                    historyPs.setInt(3, Integer.parseInt(COURSE_CODE));
                    historyPs.setString(4, ability.id);
                    historyPs.setInt(5, oldScore);
                    historyPs.setInt(6, score);
                    historyPs.setString(7, score >= oldScore ? "模拟阶段测验后能力提升" : "模拟阶段测验后能力回落");
                    historyPs.setTimestamp(8, oldTime);
                    historyPs.addBatch();
                }
            }
            scorePs.executeBatch();
            historyPs.executeBatch();
        }
    }

    private static void clearSeededCompetencyData(Connection conn) throws SQLException {
        String ids = abilityIdList();
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM competency_score_history WHERE course_code = ? AND ability_point_id IN (" + ids + ")")) {
            ps.setInt(1, Integer.parseInt(COURSE_CODE));
            ps.executeUpdate();
        }
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM competency_score WHERE course_code = ? AND ability_point_id IN (" + ids + ")")) {
            ps.setInt(1, Integer.parseInt(COURSE_CODE));
            ps.executeUpdate();
        }
    }

    private static String abilityIdList() {
        List<String> ids = new ArrayList<>();
        for (AbilitySeed ability : ABILITIES) ids.add("'" + ability.id + "'");
        return String.join(",", ids);
    }

    private static long nextNumericId(Connection conn, String table, String column) throws SQLException {
        String sql = "SELECT COALESCE(MAX((" + column + ")::BIGINT), 0) + 1 FROM " + table
                + " WHERE " + column + " ~ '^[0-9]+$'";
        try (PreparedStatement ps = conn.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getLong(1);
        }
    }

    private static int scoreFor(int studentNo, int abilityIndex) {
        int base;
        if (studentNo == 2) base = 88;
        else if (studentNo == 3) base = 82;
        else if (studentNo == 4) base = 66;
        else if (studentNo == 5) base = 49;
        else if (studentNo == 6) base = 36;
        else if (studentNo >= 7 && studentNo <= 9) base = 78;
        else if (studentNo >= 10 && studentNo <= 11) base = 62;
        else if (studentNo >= 19 && studentNo <= 22) base = 44;
        else if (studentNo >= 23) base = 52;
        else base = 56;

        int[] offsets = {4, 2, 0, -2, -4, -5, -7, -8};
        int wave = ((studentNo + abilityIndex) % 5) - 2;
        return clamp(base + offsets[abilityIndex] + wave, 18, 96);
    }

    private static int historyDelta(int studentNo, int abilityIndex) {
        if (studentNo == 6 || studentNo == 5 || (studentNo >= 19 && studentNo <= 22)) return 4 + abilityIndex % 3;
        if (studentNo == 2 || studentNo == 3) return 8 + abilityIndex % 4;
        return 6 + abilityIndex % 3;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }
}
