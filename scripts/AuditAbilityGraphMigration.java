import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;

public class AuditAbilityGraphMigration {
    public static void main(String[] args) throws Exception {
        try (Connection connection = DriverManager.getConnection(
                required("DB_URL"), required("DB_USERNAME"), required("DB_PASSWORD"));
             Statement statement = connection.createStatement()) {
            try (ResultSet rows = statement.executeQuery("""
                    SELECT
                      (SELECT COUNT(*) FROM ability_point) AS abilities,
                      (SELECT COUNT(*) FROM (
                         SELECT course_code, LOWER(TRIM(name))
                         FROM ability_point
                         GROUP BY course_code, LOWER(TRIM(name))
                         HAVING COUNT(*) > 1
                      ) duplicate) AS duplicate_groups,
                      (SELECT COUNT(*) FROM learning_answer_evidence) AS evidence,
                      (SELECT COUNT(*) FROM knowledge_mastery_history) AS history,
                      (SELECT COUNT(*) FROM student_ability_snapshot) AS snapshots,
                      (SELECT COUNT(*) FROM knowledge_mastery WHERE mastery_score = 50) AS baseline_rows
                    """)) {
                rows.next();
                System.out.printf("abilities=%d duplicateGroups=%d evidence=%d history=%d snapshots=%d baselineRows=%d%n",
                        rows.getInt("abilities"), rows.getInt("duplicate_groups"), rows.getInt("evidence"),
                        rows.getInt("history"), rows.getInt("snapshots"), rows.getInt("baseline_rows"));
            }
            try (ResultSet indexes = statement.executeQuery("""
                    SELECT indexname
                    FROM pg_indexes
                    WHERE tablename IN ('ability_point', 'learning_answer_evidence', 'student_ability_snapshot')
                      AND indexname IN (
                        'uk_ability_point_course_normalized_name',
                        'uk_learning_evidence_student_key',
                        'uk_ability_snapshot_evaluation_phase_point'
                      )
                    ORDER BY indexname
                    """)) {
                while (indexes.next()) System.out.println("index=" + indexes.getString("indexname"));
            }
        }
    }

    private static String required(String name) {
        String value = System.getenv(name);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing environment variable " + name);
        return value;
    }
}
