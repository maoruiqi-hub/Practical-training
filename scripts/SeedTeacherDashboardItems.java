import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

public class SeedTeacherDashboardItems {
    private static final String COURSE = "1";
    private static final String TASK = "5";
    private static final String[] STUDENTS = {"10", "11", "19", "20", "21", "22"};

    public static void main(String[] args) throws Exception {
        Properties env = loadEnv(Path.of("backend/.env"));
        Class.forName(required(env, "DB_DRIVER"));
        try (Connection conn = DriverManager.getConnection(
                required(env, "DB_URL"),
                required(env, "DB_USERNAME"),
                required(env, "DB_PASSWORD"))) {
            conn.setAutoCommit(false);
            ensureDueSoonTask(conn);
            List<QuestionRow> questions = loadQuestions(conn);
            if (questions.size() < 4) throw new IllegalStateException("task " + TASK + " question count is too small: " + questions.size());

            int insertedSubmissions = 0;
            int insertedAnswers = 0;
            int nextSubmissionId = maxNumericId(conn, "task_submission", "submission_id") + 1;
            int nextAnswerId = maxNumericId(conn, "submission_answer", "id") + 1;
            LocalDateTime base = LocalDateTime.now().minusMinutes(8);
            for (int i = 0; i < STUDENTS.length; i++) {
                String studentNo = STUDENTS[i];
                if (hasPendingSubmission(conn, TASK, studentNo)) continue;
                String submissionId = String.valueOf(nextSubmissionId++);
                int attempt = countSubmissions(conn, TASK, studentNo) + 1;
                LocalDateTime submitTime = base.plusMinutes(i * 2L);
                String content = buildContent(questions, i);
                insertSubmission(conn, submissionId, TASK, studentNo, attempt, content, submitTime);
                markAssignmentSubmitted(conn, TASK, studentNo, submitTime);
                insertedSubmissions++;
                for (QuestionRow question : questions) {
                    String answerId = String.valueOf(nextAnswerId++);
                    String response = responseFor(question, i);
                    boolean autoGradable = isAutoGradable(question.type);
                    Boolean correct = autoGradable ? isCorrect(question.answer, response) : null;
                    int score = Boolean.TRUE.equals(correct) ? question.score : 0;
                    insertAnswer(conn, answerId, submissionId, TASK, studentNo, question, response, correct, score, autoGradable, submitTime);
                    insertedAnswers++;
                }
            }
            conn.commit();
            System.out.println("inserted_submissions=" + insertedSubmissions);
            System.out.println("inserted_answers=" + insertedAnswers);
            printDashboardPreview(conn);
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

    private static void ensureDueSoonTask(Connection conn) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE learning_task SET deadline = ?, status = 'published', allow_late = 1 WHERE task_no = ?")) {
            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusDays(2).withHour(22).withMinute(0).withSecond(0).withNano(0)));
            ps.setString(2, "2");
            ps.executeUpdate();
        }
    }

    private static List<QuestionRow> loadQuestions(Connection conn) throws Exception {
        List<QuestionRow> rows = new ArrayList<>();
        String sql = "SELECT q.question_id, q.type, q.stem, q.answer, q.score, q.knowledge_point_id "
                + "FROM task_question tq "
                + "JOIN question q ON q.question_id = tq.question_id "
                + "WHERE tq.task_no = ? "
                + "ORDER BY q.type DESC, q.question_id "
                + "LIMIT 5";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, TASK);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    rows.add(new QuestionRow(rs.getString(1), rs.getString(2), rs.getString(3),
                            rs.getString(4), rs.getInt(5), rs.getString(6)));
                }
            }
        }
        return rows;
    }

    private static boolean hasPendingSubmission(Connection conn, String taskNo, String studentNo) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM task_submission WHERE task_no=? AND student_no=? AND status='submitted'")) {
            ps.setString(1, taskNo);
            ps.setString(2, studentNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        }
    }

    private static int countSubmissions(Connection conn, String taskNo, String studentNo) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT COUNT(*) FROM task_submission WHERE task_no=? AND student_no=?")) {
            ps.setString(1, taskNo);
            ps.setString(2, studentNo);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private static int maxNumericId(Connection conn, String table, String column) throws Exception {
        int max = 0;
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery("SELECT " + column + " FROM " + table)) {
            while (rs.next()) {
                String value = rs.getString(1);
                if (value == null || !value.matches("\\d+")) continue;
                max = Math.max(max, Integer.parseInt(value));
            }
        }
        return max;
    }

    private static void insertSubmission(Connection conn, String id, String taskNo, String studentNo, int attempt,
                                         String content, LocalDateTime submitTime) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO task_submission(submission_id, task_no, student_no, attempt_number, content, file_path, submit_time, is_overdue, score, status, feedback) "
                        + "VALUES (?, ?, ?, ?, ?, NULL, ?, 0, NULL, 'submitted', '客观题已自动记录，含主观题待教师复核')")) {
            ps.setString(1, id);
            ps.setString(2, taskNo);
            ps.setString(3, studentNo);
            ps.setInt(4, attempt);
            ps.setString(5, content);
            ps.setTimestamp(6, Timestamp.valueOf(submitTime));
            ps.executeUpdate();
        }
    }

    private static void markAssignmentSubmitted(Connection conn, String taskNo, String studentNo, LocalDateTime submitTime) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE task_assignment SET status='submitted', note='学生已提交，待教师复核', assigned_at=COALESCE(assigned_at, ?) "
                        + "WHERE task_no=? AND student_no=?")) {
            ps.setTimestamp(1, Timestamp.valueOf(submitTime.minusDays(1)));
            ps.setString(2, taskNo);
            ps.setString(3, studentNo);
            ps.executeUpdate();
        }
    }

    private static void insertAnswer(Connection conn, String id, String submissionId, String taskNo, String studentNo,
                                     QuestionRow q, String response, Boolean correct, int score,
                                     boolean autoGradable, LocalDateTime createTime) throws Exception {
        try (PreparedStatement ps = conn.prepareStatement(
                "INSERT INTO submission_answer(id, submission_id, task_no, student_no, question_id, question_stem, question_type, knowledge_point_id, student_answer, correct_answer, correct, score, max_score, auto_gradable, create_time) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
            ps.setString(1, id);
            ps.setString(2, submissionId);
            ps.setString(3, taskNo);
            ps.setString(4, studentNo);
            ps.setString(5, q.id);
            ps.setString(6, q.stem);
            ps.setString(7, q.type);
            ps.setString(8, q.knowledgePointId);
            ps.setString(9, response);
            ps.setString(10, q.answer);
            if (correct == null) ps.setNull(11, Types.BOOLEAN); else ps.setBoolean(11, correct);
            ps.setInt(12, score);
            ps.setInt(13, q.score);
            ps.setBoolean(14, autoGradable);
            ps.setTimestamp(15, Timestamp.valueOf(createTime));
            ps.executeUpdate();
        }
    }

    private static String buildContent(List<QuestionRow> questions, int studentIndex) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < questions.size(); i++) {
            if (i > 0) sb.append(',');
            QuestionRow q = questions.get(i);
            sb.append("{\"no\":\"").append(q.id).append("\",\"response\":\"")
                    .append(escapeJson(responseFor(q, studentIndex))).append("\"}");
        }
        sb.append(']');
        return sb.toString();
    }

    private static String responseFor(QuestionRow q, int studentIndex) {
        if ("single".equals(q.type)) {
            return studentIndex % 3 == 0 ? "B" : safeAnswer(q.answer);
        }
        if ("multi".equals(q.type)) {
            return studentIndex % 2 == 0 ? safeAnswer(q.answer) : "A,C";
        }
        if ("fill".equals(q.type)) {
            return studentIndex % 2 == 0 ? safeAnswer(q.answer) : "函数作用域与文件上下文管理说明不完整";
        }
        if ("essay".equals(q.type) || "program".equals(q.type)) {
            return "已完成核心思路，代码覆盖基本场景，但异常处理和边界测试仍需教师复核。";
        }
        return safeAnswer(q.answer);
    }

    private static String safeAnswer(String answer) {
        return answer == null || answer.isBlank() ? "待教师判定" : answer;
    }

    private static boolean isAutoGradable(String type) {
        return "single".equals(type) || "multi".equals(type);
    }

    private static boolean isCorrect(String answer, String response) {
        if (answer == null || response == null) return false;
        return normalize(answer).equals(normalize(response));
    }

    private static String normalize(String value) {
        String[] parts = value.replace("，", ",").split(",");
        List<String> tokens = new ArrayList<>();
        for (String part : parts) {
            String token = part.trim().toUpperCase();
            if (!token.isEmpty()) tokens.add(token);
        }
        Collections.sort(tokens);
        return String.join(",", tokens);
    }

    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    private static void printDashboardPreview(Connection conn) throws Exception {
        query(conn, "PENDING_PREVIEW",
                "SELECT s.submission_id, s.task_no, t.task_name, s.student_no, st.name, s.submit_time, s.status "
                        + "FROM task_submission s "
                        + "JOIN learning_task t ON t.task_no = s.task_no "
                        + "LEFT JOIN student st ON st.student_no = s.student_no "
                        + "WHERE s.status <> 'graded' AND s.status <> 'superseded' "
                        + "ORDER BY s.submit_time DESC "
                        + "LIMIT 10");
        query(conn, "DUE_SOON_OR_OVERDUE",
                "SELECT task_no, task_name, deadline, status "
                        + "FROM learning_task "
                        + "WHERE course_code='1' AND status <> 'closed' AND deadline IS NOT NULL "
                        + "ORDER BY deadline");
    }

    private static void query(Connection conn, String title, String sql) throws Exception {
        System.out.println("# " + title);
        try (Statement st = conn.createStatement(); ResultSet rs = st.executeQuery(sql)) {
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

    private static class QuestionRow {
        final String id;
        final String type;
        final String stem;
        final String answer;
        final int score;
        final String knowledgePointId;

        QuestionRow(String id, String type, String stem, String answer, int score, String knowledgePointId) {
            this.id = id;
            this.type = type;
            this.stem = stem;
            this.answer = answer;
            this.score = score;
            this.knowledgePointId = knowledgePointId;
        }
    }
}
