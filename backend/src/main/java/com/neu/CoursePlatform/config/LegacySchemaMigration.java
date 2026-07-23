package com.neu.CoursePlatform.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Component
public class LegacySchemaMigration implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(LegacySchemaMigration.class);

    private final DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;

    public LegacySchemaMigration(DataSource dataSource, JdbcTemplate jdbcTemplate) {
        this.dataSource = dataSource;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        createTowerRunTables();
        createLearningEvidenceTables();
        normalizeLegacyMastery();
        cleanupDuplicateAbilityPoints();
        migrateKnowledgeRelations();
        migrateExams();
    }

    private void createLearningEvidenceTables() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS learning_answer_evidence (
                        evidence_id VARCHAR(64) PRIMARY KEY,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        question_id VARCHAR(64) NOT NULL,
                        knowledge_point_id VARCHAR(64) NOT NULL,
                        difficulty INTEGER NOT NULL,
                        attempt_no INTEGER NOT NULL,
                        first_attempt BOOLEAN NOT NULL,
                        correct BOOLEAN NOT NULL,
                        answer_content TEXT,
                        source_type VARCHAR(32) NOT NULL,
                        source_id VARCHAR(64) NOT NULL,
                        idempotency_key VARCHAR(160) NOT NULL,
                        formula_version VARCHAR(32) NOT NULL,
                        answered_at TIMESTAMP NOT NULL
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_learning_evidence_student_key
                    ON learning_answer_evidence(student_no, idempotency_key)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_learning_evidence_student_question
                    ON learning_answer_evidence(student_no, question_id, answered_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS knowledge_mastery_history (
                        history_id VARCHAR(64) PRIMARY KEY,
                        evidence_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        knowledge_point_id VARCHAR(64) NOT NULL,
                        before_score INTEGER NOT NULL,
                        after_score INTEGER NOT NULL,
                        target_score INTEGER NOT NULL,
                        alpha NUMERIC(6,5) NOT NULL,
                        formula_version VARCHAR(32) NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_mastery_history_evidence
                    ON knowledge_mastery_history(evidence_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_mastery_history_student_point
                    ON knowledge_mastery_history(student_no, course_code, knowledge_point_id, created_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_ability_snapshot (
                        snapshot_id VARCHAR(64) PRIMARY KEY,
                        evaluation_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        run_id VARCHAR(64) NOT NULL,
                        node_id VARCHAR(64) NOT NULL,
                        phase VARCHAR(16) NOT NULL,
                        ability_point_id VARCHAR(64) NOT NULL,
                        ability_point_name VARCHAR(128) NOT NULL,
                        score INTEGER NOT NULL,
                        evidence_knowledge_count INTEGER NOT NULL,
                        total_knowledge_count INTEGER NOT NULL,
                        knowledge_point_ids_json TEXT NOT NULL,
                        weights_json TEXT NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_snapshot_evaluation_phase_point
                    ON student_ability_snapshot(evaluation_id, phase, ability_point_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_ability_snapshot_student_node
                    ON student_ability_snapshot(student_no, course_code, run_id, node_id, created_at)
                    """);
        } catch (Exception e) {
            log.warn("Learning evidence table migration skipped", e);
        }
    }

    private void normalizeLegacyMastery() {
        if (!tableExists("knowledge_mastery") || !tableExists("learning_answer_evidence")) return;
        try {
            int updated = jdbcTemplate.update("""
                    UPDATE knowledge_mastery km
                    SET mastery_score = 50,
                        source_type = 'progressive_migration',
                        source_id = 'reset_to_50',
                        updated_at = CURRENT_TIMESTAMP
                    WHERE km.mastery_score <> 50
                      AND NOT EXISTS (
                          SELECT 1 FROM learning_answer_evidence evidence
                          WHERE evidence.student_no = km.student_no
                            AND evidence.course_code = km.course_code
                            AND evidence.knowledge_point_id = km.knowledge_point_id
                      )
                    """);
            if (updated > 0) log.info("Reset {} legacy mastery rows to progressive baseline 50", updated);
        } catch (Exception e) {
            log.warn("Legacy mastery normalization skipped", e);
        }
    }

    private void cleanupDuplicateAbilityPoints() {
        if (!tableExists("ability_point") || !tableExists("ability_knowledge_point")) return;
        try {
            List<AbilityRow> rows = jdbcTemplate.query(
                    "SELECT ability_point_id, course_code, name FROM ability_point",
                    (rs, rowNum) -> new AbilityRow(rs.getString(1), rs.getString(2), rs.getString(3)));
            Map<String, List<AbilityRow>> groups = new LinkedHashMap<>();
            for (AbilityRow row : rows) groups.computeIfAbsent(row.normalizedKey(), ignored -> new ArrayList<>()).add(row);
            int removed = 0;
            for (List<AbilityRow> group : groups.values()) {
                if (group.size() < 2) continue;
                group.sort(Comparator.comparing(AbilityRow::id, this::compareIds));
                AbilityRow keep = group.get(0);
                for (int i = 1; i < group.size(); i++) {
                    mergeAbilityPoint(keep, group.get(i));
                    removed++;
                }
            }
            jdbcTemplate.execute("""
                    CREATE UNIQUE INDEX IF NOT EXISTS uk_ability_point_course_normalized_name
                    ON ability_point(course_code, LOWER(TRIM(name)))
                    """);
            if (removed > 0) log.info("Merged and removed {} newer duplicate ability points", removed);
        } catch (Exception e) {
            log.warn("Duplicate ability point cleanup skipped", e);
        }
    }

    private void mergeAbilityPoint(AbilityRow keep, AbilityRow duplicate) {
        jdbcTemplate.update("""
                INSERT INTO ability_knowledge_point (ability_point_id, knowledge_point_id)
                SELECT ?, duplicate_mapping.knowledge_point_id
                FROM ability_knowledge_point duplicate_mapping
                WHERE duplicate_mapping.ability_point_id = ?
                  AND NOT EXISTS (
                      SELECT 1 FROM ability_knowledge_point kept_mapping
                      WHERE kept_mapping.ability_point_id = ?
                        AND kept_mapping.knowledge_point_id = duplicate_mapping.knowledge_point_id
                  )
                """, keep.id(), duplicate.id(), keep.id());
        jdbcTemplate.update("DELETE FROM ability_knowledge_point WHERE ability_point_id = ?", duplicate.id());

        migrateSimpleReference("student_tower_node", keep.id(), duplicate.id());
        migrateSimpleReference("student_ability_delta_log", keep.id(), duplicate.id());
        migrateSimpleReference("competency_score_history", keep.id(), duplicate.id());
        mergeCompetencyScores(keep, duplicate);
        mergeAbilitySnapshots(keep, duplicate);
        jdbcTemplate.update("DELETE FROM ability_point WHERE ability_point_id = ?", duplicate.id());
    }

    private void migrateSimpleReference(String table, String keepId, String duplicateId) {
        if (!tableExists(table)) return;
        jdbcTemplate.update("UPDATE " + table + " SET ability_point_id = ? WHERE ability_point_id = ?", keepId, duplicateId);
    }

    private void mergeCompetencyScores(AbilityRow keep, AbilityRow duplicate) {
        if (!tableExists("competency_score")) return;
        jdbcTemplate.update("""
                DELETE FROM competency_score duplicate_score
                WHERE duplicate_score.ability_point_id = ?
                  AND EXISTS (
                      SELECT 1 FROM competency_score kept_score
                      WHERE kept_score.student_no = duplicate_score.student_no
                        AND kept_score.course_code = duplicate_score.course_code
                        AND kept_score.ability_point_id = ?
                  )
                """, duplicate.id(), keep.id());
        jdbcTemplate.update("""
                UPDATE competency_score
                SET ability_point_id = ?, ability_point_name = ?
                WHERE ability_point_id = ?
                """, keep.id(), keep.name(), duplicate.id());
    }

    private void mergeAbilitySnapshots(AbilityRow keep, AbilityRow duplicate) {
        if (!tableExists("student_ability_snapshot")) return;
        jdbcTemplate.update("""
                DELETE FROM student_ability_snapshot duplicate_snapshot
                WHERE duplicate_snapshot.ability_point_id = ?
                  AND EXISTS (
                      SELECT 1 FROM student_ability_snapshot kept_snapshot
                      WHERE kept_snapshot.evaluation_id = duplicate_snapshot.evaluation_id
                        AND kept_snapshot.phase = duplicate_snapshot.phase
                        AND kept_snapshot.ability_point_id = ?
                  )
                """, duplicate.id(), keep.id());
        jdbcTemplate.update("""
                UPDATE student_ability_snapshot
                SET ability_point_id = ?, ability_point_name = ?
                WHERE ability_point_id = ?
                """, keep.id(), keep.name(), duplicate.id());
    }

    private int compareIds(String first, String second) {
        try {
            return Long.compare(Long.parseLong(first), Long.parseLong(second));
        } catch (Exception ignored) {
            return String.valueOf(first).compareTo(String.valueOf(second));
        }
    }

    private record AbilityRow(String id, String courseCode, String name) {
        private String normalizedKey() {
            return String.valueOf(courseCode) + "\u0000" + String.valueOf(name).trim().toLowerCase(Locale.ROOT);
        }
    }

    private void createTowerRunTables() {
        try {
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_run (
                        run_id VARCHAR(64) PRIMARY KEY,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        version INTEGER DEFAULT 1,
                        status VARCHAR(32) DEFAULT 'active',
                        route_source VARCHAR(32) DEFAULT 'rule',
                        current_node_id VARCHAR(64),
                        ai_snapshot_json TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_run_student_course
                    ON student_tower_run(student_no, course_code, status)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_node (
                        node_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_order INTEGER NOT NULL,
                        row_no INTEGER NOT NULL,
                        col_no INTEGER NOT NULL,
                        room_type VARCHAR(32) NOT NULL,
                        status VARCHAR(32) DEFAULT 'locked',
                        knowledge_point_id VARCHAR(64),
                        ability_point_id VARCHAR(64),
                        parent_node_id VARCHAR(64),
                        unlock_after_node_id VARCHAR(64),
                        difficulty INTEGER DEFAULT 1,
                        ai_reason TEXT,
                        payload_json TEXT,
                        cleared_at TIMESTAMP,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_node_run_order
                    ON student_tower_node(run_id, node_order)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_node_unlock
                    ON student_tower_node(run_id, unlock_after_node_id)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_attempt (
                        attempt_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        room_type VARCHAR(32) NOT NULL,
                        result VARCHAR(32) NOT NULL,
                        correct_rate NUMERIC(5,2),
                        hp_left INTEGER,
                        answer_summary_json TEXT,
                        ai_report_json TEXT,
                        started_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        finished_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_attempt_node
                    ON student_tower_attempt(node_id, finished_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_ability_delta_log (
                        id VARCHAR(64) PRIMARY KEY,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        run_id VARCHAR(64),
                        node_id VARCHAR(64),
                        knowledge_point_id VARCHAR(64),
                        ability_point_id VARCHAR(64),
                        delta_score INTEGER DEFAULT 0,
                        before_score INTEGER,
                        after_score INTEGER,
                        reason TEXT,
                        ai_summary TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_ability_delta_student_course
                    ON student_ability_delta_log(student_no, course_code, created_at)
                    """);
            jdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS student_tower_question_pack (
                        pack_id VARCHAR(64) PRIMARY KEY,
                        run_id VARCHAR(64) NOT NULL,
                        node_id VARCHAR(64) NOT NULL,
                        student_no VARCHAR(64) NOT NULL,
                        course_code VARCHAR(64) NOT NULL,
                        mode VARCHAR(32) NOT NULL,
                        question_ids_json TEXT NOT NULL,
                        source VARCHAR(32) DEFAULT 'rule',
                        strategy_json TEXT,
                        ai_reason TEXT,
                        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                        updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                    )
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_tower_question_pack_node
                    ON student_tower_question_pack(run_id, node_id, mode)
                    """);
        } catch (Exception e) {
            log.warn("Tower run table migration skipped", e);
        }
    }

    private void migrateKnowledgeRelations() {
        if (!tableExists("knowledge_edge") || !tableExists("knowledge_relation")) return;
        try {
            jdbcTemplate.update("""
                    INSERT INTO knowledge_relation (relation_id, course_code, from_knowledge_point_id, to_knowledge_point_id, relation_type)
                    SELECT edge_id, course_code, source_id, target_id, relation_type
                    FROM knowledge_edge old_edge
                    WHERE NOT EXISTS (
                        SELECT 1 FROM knowledge_relation kr
                        WHERE kr.relation_id = old_edge.edge_id
                    )
                    """);
        } catch (Exception e) {
            log.warn("Legacy knowledge_edge migration skipped", e);
        }
    }

    private void migrateExams() {
        if (tableExists("paper") && tableExists("exam")) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO exam (exam_id, course_code, task_no, title, generate_type, target_count, total_score, status, create_time)
                        SELECT paper_id, course_code, task_no, title, strategy, target_count, total_score, status, create_time
                        FROM paper old_paper
                        WHERE NOT EXISTS (
                            SELECT 1 FROM exam ex
                            WHERE ex.exam_id = old_paper.paper_id
                        )
                        """);
            } catch (Exception e) {
                log.warn("Legacy paper migration skipped", e);
            }
        }
        if (tableExists("paper_question") && tableExists("exam_question")) {
            try {
                jdbcTemplate.update("""
                        INSERT INTO exam_question (id, exam_id, question_id, sort_order, score_snapshot, question_type, knowledge_point_id, difficulty)
                        SELECT id, paper_id, question_id, sort_order, score_snapshot, question_type, knowledge_point_id, difficulty
                        FROM paper_question old_question
                        WHERE NOT EXISTS (
                            SELECT 1 FROM exam_question exam_question
                            WHERE exam_question.id = old_question.id
                        )
                        """);
            } catch (Exception e) {
                log.warn("Legacy paper_question migration skipped", e);
            }
        }
    }

    private boolean tableExists(String tableName) {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData metaData = connection.getMetaData();
            return tableExists(metaData, tableName)
                    || tableExists(metaData, tableName.toUpperCase())
                    || tableExists(metaData, tableName.toLowerCase());
        } catch (Exception e) {
            log.warn("Could not inspect table {}", tableName, e);
            return false;
        }
    }

    private boolean tableExists(DatabaseMetaData metaData, String tableName) throws Exception {
        try (ResultSet rs = metaData.getTables(null, null, tableName, new String[]{"TABLE"})) {
            return rs.next();
        }
    }
}
